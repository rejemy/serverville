package com.dreamwing.serverville.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.client.ClientMessages.PendingNotification;
import com.dreamwing.serverville.client.ClientMessages.PendingNotificationList;
import com.dreamwing.serverville.client.ClientMessages.UserMessageNotification;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.UserMessage;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.log.SVLog;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.ApiError;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpConnectionInfo;
import com.dreamwing.serverville.net.HttpDispatcher;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.OnlineUser;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.serialize.SerializeUtil;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleStateEvent;

public class ClientConnectionHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger l = LogManager.getLogger(ClientConnectionHandler.class);
	
	private static final String WEBSOCKET_PATH = "/websocket";
	
	private ClientDispatcher JsonDispatcher;
	private HttpDispatcher FormDispatcher;
	private WebSocketServerHandshaker Handshaker;
	
	private HttpConnectionInfo Info;
	private OnlineUser UserPresence;
	
	private boolean Keepalive = false;
	private boolean WebsocketConnected = false;
	private boolean BinaryConnected = false;

	private List<PendingNotification> Notifications;
	
	public ClientConnectionHandler(ClientDispatcher jsonDispatcher, HttpDispatcher formDispatcher)
	{
		super();
		
		JsonDispatcher = jsonDispatcher;
		FormDispatcher = formDispatcher;
		
		Notifications = new LinkedList<PendingNotification>();
	}
	
	public ServervilleUser getUser()
	{
		return Info != null ? Info.User : null;
	}
	
	public UserSession getSession()
	{
		return Info != null ? (UserSession)Info.Session : null;
	}
	
	@Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception
    {
		
		super.channelActive(ctx);
		Info = new HttpConnectionInfo();
		Info.Ctx = ctx;
		Info.ConnectionId = SVID.makeSVID();
		
		l.debug(new SVLog("Client HTTP connection opened", Info));
    }
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		super.channelInactive(ctx);
		
		WebsocketConnected = false;
		BinaryConnected = false;
		
		l.debug(new SVLog("Client HTTP connection closed", Info));
		
		signOut();
    }
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
	{
		if (evt instanceof IdleStateEvent)
		{
			ctx.close();
		}
	}
	
	public void signIn(ServervilleUser user) throws SQLException, JsonApiException
	{
		UserSession session = UserSession.startNewSession(user.getId());
		
		signIn(user, session);
	}
	
	public void signIn(ServervilleUser user, UserSession session) throws SQLException, JsonApiException
	{
		if(user == null)
		{
			signOut();
			return;
		}
		if(user.equals(Info.User))
		{
			// ALready logged in, just ignore
			return;
		}
		else if(Info.User != null)
		{
			// Already logged in as someone else, can happen on a re-used HTTP connection
			signOut();
		}
		
		user.setSessionId(session.Id);
		Info.User = user;
		Info.Session = session;
		
		if((WebsocketConnected || Keepalive) && session.Connected == false)
		{
			session.Connected = true;
			session.update();
		}
		
		if(WebsocketConnected)
		{
			UserPresence = new OnlineUser(this);
		}
		
		ClientSessionManager.addSession(this);
		
		loadGuaranteedMessages();
		
		ScriptManager.onUserSignIn(this);
	}
	
	private void signOut()
	{
		signOut(true);
	}
	
	private void signOut(boolean updateSession)
	{
		if(Info.User != null)
		{
			ScriptManager.onUserSignOut(this);
			
			if(updateSession)
			{
				UserSession session = (UserSession) Info.Session;
				session.Connected = false;
				
				try {
					session.update();
				} catch (Exception e) {
					l.warn("Exception updating user session:", e);
				}
			}
			
			ClientSessionManager.removeSession(this);
			
			Info.User = null;
			Info.Session = null;
		}

		if(UserPresence != null)
		{
			UserPresence.destroy();
			UserPresence = null;
		}
		
	}
	
	
	private void authenticate(FullHttpRequest request) throws SQLException, JsonApiException
	{
		String authToken = request.headers().get(HttpHeaderNames.AUTHORIZATION);
		if(authToken == null)
		{
			if(Info.User != null)
			{
				signOut();
			}
			return;
		}
		

		if(Info.User != null)
		{
			if(!authToken.equals(Info.Session.getId()))
			{
				signOut();
				throw new JsonApiException(ApiErrors.BAD_AUTH);
			}
			return;
		}
		
		UserSession session = UserSession.findById(authToken);
		if(session == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH);
		}
		
		ServervilleUser user = ServervilleUser.findById(session.UserId);
		if(user == null)
		{
			try
			{
				session.delete(false);
			}
			catch(SQLException e)
			{
				l.error("Error deleting orphaned session: ", e);
			}
			throw new JsonApiException(ApiErrors.BAD_AUTH);
		}
		
		if(session.Expired)
		{
			throw new JsonApiException(ApiErrors.SESSION_EXPIRED);
		}
		
		signIn(user, session);
	
	}
	
	public void expireSession()
	{
		if(Info.Session == null)
		{
			return;
		}
		
		UserSession session = (UserSession) Info.Session;
		session.Expired = true;
		signOut();
		
		if(WebsocketConnected)
		{
			sendNotification("error", new ApiError(ApiErrors.SESSION_EXPIRED)).addListener(ChannelFutureListener.CLOSE);
		}
		else
		{
			Info.Ctx.close();
		}

	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception
	{
		ChannelFuture lastWrite = null;
		
		if (msg instanceof FullHttpRequest)
		{
			FullHttpRequest request = (FullHttpRequest) msg;
			Keepalive = HttpUtil.isKeepAlive(request);
			lastWrite = handleHttpRequest(ctx, request);
        }
		else if (msg instanceof WebSocketFrame)
        {
        	lastWrite = handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
		else if(msg instanceof ClientJsonMessageWrapper)
		{
			lastWrite = handleBinaryMessage(ctx, (ClientJsonMessageWrapper)msg);
		}
		else
		{
			throw new Exception("Unexpected message type received from client: "+msg.getClass());
		}
		
		if (!(Keepalive || WebsocketConnected))
		{
            // Close the connection when the whole content is written out.
    		if(lastWrite != null)
    		{
    			lastWrite.addListener(ChannelFutureListener.CLOSE);
    		}
    		else
    		{
    			ctx.close();
    		}
        }
	}
	
	private ChannelFuture handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request)
	{
		if(request.method() == HttpMethod.OPTIONS)
    	{
    		return HttpHelpers.sendPreflightApproval(ctx);
    	}
		
		HttpRequestInfo currRequest = new HttpRequestInfo();
		
    	try {
			currRequest.init(Info, request, SVID.makeSVID());
		} catch (URISyntaxException e1) {
			return HttpHelpers.sendError(currRequest, ApiErrors.HTTP_DECODE_ERROR);
		}
    	
		URI uri = currRequest.RequestURI;
    	
    	l.debug(new SVLog("Client HTTP request", currRequest));
    	
		if (!request.decoderResult().isSuccess()) {
            return HttpHelpers.sendError(currRequest, ApiErrors.HTTP_DECODE_ERROR);
        }
		
		
		try {
			authenticate(request);
		} catch (SQLException e) {
			return HttpHelpers.sendError(currRequest, ApiErrors.DB_ERROR);
		} catch (JsonApiException e) {
			return HttpHelpers.sendErrorJson(currRequest, e.Error, e.HttpStatus);
		}
		
		String uriPath = uri.getPath();
		
		if(uriPath.equals(WEBSOCKET_PATH))
		{
			String websockUrl = request.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
					websockUrl, null, false);
			
			Handshaker = wsFactory.newHandshaker(request);
			if (Handshaker == null) {
	            return WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
	        } else {
	        	WebsocketConnected = true;
	        	return Handshaker.handshake(ctx.channel(), request);
	        }
		}
		
		if(uriPath.startsWith("/api/"))
		{
			//Invoking an API directly over HTTP
			
			String messageType = uriPath.substring(5);
			String messageBody = null;
			if(request.method() == HttpMethod.POST && request.content() != null)
			{
				messageBody = request.content().toString(StandardCharsets.UTF_8);
			}
		
			ClientMessageInfo info = new ClientMessageInfo();
			info.MessageId = SVID.makeSVID();
			info.MessageNum = null;
			info.ConnectionHandler = this;
			info.User = Info.User;
			info.UserPresence = UserPresence;
			
			Object reply = JsonDispatcher.dispatch(messageType, messageBody, info);
			HttpResponse response;
			
			if(reply instanceof ApiError)
			{
				ApiError error = (ApiError)reply;
				response = HttpHelpers.makeErrorJsonResponse(error, error.getHttpStatus());
			}
			else
			{
				response = HttpHelpers.makeJsonResponse(reply);
			}
			
			addNotificationHeader(response);
			
			return ctx.writeAndFlush(response);
		}
		else if(uriPath.equals("/notifications"))
		{
			// Request for pending notifications
			HttpResponse response = getNotifications();
			
			return ctx.writeAndFlush(response);
		}
		else if(uriPath.startsWith("/form/"))
		{
			// Invoking a non-Json form post API
		
			try {
				return FormDispatcher.dispatch(currRequest);
			}
			catch(JsonProcessingException e)
			{
				return HttpHelpers.sendError(currRequest, ApiErrors.JSON_ERROR, e.getMessage());
			}
			catch(SQLException e)
			{
				l.warn("Database error: ", e);
				return HttpHelpers.sendError(currRequest, ApiErrors.DB_ERROR, e.getMessage());
			}
			catch(JsonApiException e)
			{
				return HttpHelpers.sendErrorJson(currRequest, e.Error, e.HttpStatus);
			}
			catch(Exception e)
			{
				l.error("Internal server error: ", e);
				return HttpHelpers.sendError(currRequest, ApiErrors.INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}
		
		return HttpHelpers.sendError(currRequest, ApiErrors.NOT_FOUND);
	}
	
	private ChannelFuture handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
	{
		// Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
        	return Handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
        }
        else if (frame instanceof PingWebSocketFrame) {
            return ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }
        else if (frame instanceof TextWebSocketFrame)
        {
        	TextWebSocketFrame textFrame = (TextWebSocketFrame)frame;
        	String messageText = textFrame.text();
        	return handleTextMessage(messageText);
        }
        else
        {
        	throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
	}
	
	
	private ChannelFuture handleTextMessage(String messageText)
	{

		String messageParts[] = messageText.split(":", 3);
		if(messageParts.length != 3)
		{
			l.debug("Incorrectly formatted message: "+messageText);
			return null;
		}
		
		String messageType = messageParts[0];
		String messageNum = messageParts[1];
		String messageBody = messageParts[2];
		
		ClientMessageInfo info = new ClientMessageInfo();
		info.MessageId = SVID.makeSVID();
		info.MessageNum = messageNum;
		info.ConnectionHandler = this;
		info.User = Info.User;
		info.UserPresence = UserPresence;
		
		Object replyObj = JsonDispatcher.dispatch(messageType, messageBody, info);
		String sendType = replyObj instanceof ApiError ? "E" : "R";
		
		String replyStr;
		try {
			replyStr = JSON.serializeToString(replyObj);
		} catch (JsonProcessingException e) {
			l.error("Json encoding error:", e);
			replyStr = ApiError.encodingErrorReply;
			sendType = "E";
		}
		
		String messageStr = sendType+":"+messageNum+":"+replyStr;
		
		return write(messageStr);

		
	}
	
	private ChannelFuture handleBinaryMessage(ChannelHandlerContext ctx, ClientJsonMessageWrapper message)
	{
		if(!BinaryConnected)
		{
			BinaryConnected = true; 
		}
		

		ClientMessageInfo info = new ClientMessageInfo();
		info.MessageId = SVID.makeSVID();
		info.MessageNum = message.MessageNum;
		info.ConnectionHandler = this;
		info.User = Info.User;
		info.UserPresence = UserPresence;
		
		Object replyObj = JsonDispatcher.dispatch(message.Api, message.RequestJson, info);
		char sendType = replyObj instanceof ApiError ? 'E' : 'R';
		
		String replyStr;
		try {
			replyStr = JSON.serializeToString(replyObj);
		} catch (JsonProcessingException e) {
			l.error("Json encoding error:", e);
			replyStr = ApiError.encodingErrorReply;
			sendType = 'E';
		}
		
		ByteBuf replyMessage = ctx.alloc().buffer(256);
		replyMessage.writerIndex(2); // Skip over size header
		replyMessage.writeChar(sendType);
		
		try
		{
			SerializeUtil.writeUTF(message.MessageNum, replyMessage);
			SerializeUtil.writeUTF(replyStr, replyMessage);
		}
		catch(Exception e)
		{
			l.error("Exception encoding message to binary format, probably too long");
			return null;
		}
		
		int len = replyMessage.writerIndex() - 2;
		replyMessage.writerIndex(0);
		replyMessage.writeShortLE(len);
		replyMessage.writerIndex(len+2);
		
		return write(replyMessage);
	}
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        
		if(cause instanceof IOException)
		{
			// Connection closed by other end while we were trying to write something at them. Not the end of the world.
			l.debug("Exception caught in client handler: ", cause);
		}
		else
		{
			l.error("Exception caught in client handler: ", cause);
		}
		
        if (ctx.channel().isActive())
        {
        	ApiError ise = new ApiError(ApiErrors.INTERNAL_SERVER_ERROR);
        
        	HttpResponse response = HttpHelpers.makeErrorJsonResponse(ise, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        	addNotificationHeader(response);
        	ctx.writeAndFlush(response);
        }
    }
	
	public ChannelFuture sendNotification(String notificationType, Object notification)
	{
		try
		{
			return sendNotification(notificationType, JSON.serializeToString(notification));
		}
		catch(JsonProcessingException e)
		{
			l.error("Error encoding message:", e);
			return null;
		}
	}
	
	public ChannelFuture sendNotification(String notificationType, String serializedNotification)
	{
		if(WebsocketConnected)
		{
			if(BinaryConnected)
				return sendBinaryNotification(notificationType, serializedNotification);
			else
				return sendWSNotification(notificationType, serializedNotification);
		}
		else
		{
			// If it's just a normal http connection, store the notification until the next client request
			PendingNotification pending = new PendingNotification();
			pending.notification_type = notificationType;
			pending.body = serializedNotification;
			
			synchronized(Notifications)
			{
				Notifications.add(pending);
			}
			
			return null;
		}
	}
	
	private ChannelFuture sendWSNotification(String notificationType, String serializedNotification)
	{
		String messageStr = "N:"+notificationType+":"+serializedNotification;
		
		return write(messageStr);
	}
	
	private ChannelFuture sendBinaryNotification(String notificationType, String serializedNotification)
	{
		ByteBuf messageBuf = Unpooled.buffer(256);
		messageBuf.writerIndex(2); // Skip over size header
		messageBuf.writeChar('N');
		
		try
		{
			SerializeUtil.writeUTF(notificationType, messageBuf);
			SerializeUtil.writeUTF(serializedNotification, messageBuf);
		}
		catch(Exception e)
		{
			l.error("Exception encoding message to binary format, probably too long");
			return null;
		}
		
		int len = messageBuf.writerIndex() - 2;
		messageBuf.writerIndex(0);
		messageBuf.writeShortLE(len);
		messageBuf.writerIndex(len+2);
		
		return write(messageBuf);
	}
	
	private ChannelFuture write(ByteBuf data)
	{

		ChannelFuture future = Info.Ctx.channel().write(data);
		Info.Ctx.channel().flush();
		return future;
	}
	
	private ChannelFuture write(String data)
	{
		if(!WebsocketConnected)
			return null;
		
		ChannelFuture future = Info.Ctx.channel().write(new TextWebSocketFrame(data));
		Info.Ctx.channel().flush();
		return future;
	}
	
	public OnlineUser getPresence()
	{
		return UserPresence;
	}
	
	private void addNotificationHeader(HttpResponse response)
	{
		if(!Notifications.isEmpty())
		{
			response.headers().set("X-Notifications", "true");
		}
	}
	
	private HttpResponse getNotifications()
	{
		synchronized(Notifications)
		{
			PendingNotificationList reply = new PendingNotificationList();
			reply.notifications = Notifications;
			
			HttpResponse response = HttpHelpers.makeJsonResponse(reply);
			
			Notifications.clear();
			
			return response;
		}

	}
	
	private void loadGuaranteedMessages()
	{
		List<UserMessage> messages;
		try {
			messages = UserMessage.loadAllToUser(Info.User.getId());
		} catch (SQLException e) {
			l.error("Error loading pending guaranteed messages for user "+Info.User.getId(), e);
			return;
		}
		

		if(!messages.isEmpty())
		{
			final Runnable sendMessageRunnable = new Runnable()
			{
				public void run()
				{
					for(UserMessage msg : messages)
					{
						UserMessageNotification notification = msg.toNotification();
						sendNotification("msg", notification);
					}
				}
			};
			
			ServervilleMain.ServiceScheduler.schedule(sendMessageRunnable, 1, TimeUnit.SECONDS);
		}
		
	}
	

}
