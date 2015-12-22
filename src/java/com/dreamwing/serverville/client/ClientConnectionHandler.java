package com.dreamwing.serverville.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.log.SVLog;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpUtil;
import com.dreamwing.serverville.net.ApiError;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpConnectionInfo;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class ClientConnectionHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger l = LogManager.getLogger(ClientConnectionHandler.class);
	
	private static final String WEBSOCKET_PATH = "/websocket";
	private WebSocketServerHandshaker Handshaker;
	
	private HttpConnectionInfo Info;
	
	private String CurrAuthToken;
	
	//private OnlineUser User;
	
	private ClientDispatcher Dispatcher;
	
	private volatile boolean WebsocketConnected = false;
	

	public ClientConnectionHandler(ClientDispatcher dispatcher)
	{
		super();
		
		Dispatcher = dispatcher;
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		WebsocketConnected = false;
		l.debug(new SVLog("Client HTTP connection closed", Info));
		
		/*if(User != null)
		{
			//OnlineUserManager.removeUser(User.UserId);
			
			UserLeft leftMsg = new UserLeft();
			leftMsg.user_id = User.UserId;
			leftMsg.display_name = User.DisplayName;
			
			try {
				OnlineUserManager.sendBroadcast("UserLeft", leftMsg, User);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			User = null;
		}*/
    }
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception
	{
		ChannelFuture lastWrite = null;
		boolean keepAlive = true;
		
		if (msg instanceof FullHttpRequest)
		{
			FullHttpRequest request = (FullHttpRequest) msg;
			keepAlive = HttpHeaders.isKeepAlive(request);
			lastWrite = handleHttpRequest(ctx, request);
        } else if (msg instanceof WebSocketFrame)
        {
        	lastWrite = handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
		
		if (!keepAlive) {
            // Close the connection when the whole content is written out.
    		if(lastWrite != null)
    		{
    			lastWrite.addListener(ChannelFutureListener.CLOSE);
    		}
    		//else
    		//{
    		//	ctx.close();
    		//}
        }
	}
	
	private void authenticate(FullHttpRequest request)
	{
		String authToken = request.headers().get(Names.AUTHORIZATION);
		if(authToken == null)
		{
			Info.User = null;
			return;
		}
		
		// ALready authed
		if(Info.User != null && authToken.equals(CurrAuthToken))
			return;
		
		CurrAuthToken = null;
		Info.User = null;
		
		ServervilleUser user = null;
		
		try {
			user = ServervilleUser.findBySessionId(authToken);
		} catch (SQLException e) {
			return;
		}
		
		if(user == null)
			return;
		
		Info.User = user;
	}
	
	private ChannelFuture handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request)
	{
		if(request.getMethod() == HttpMethod.OPTIONS)
    	{
    		return HttpUtil.sendPreflightApproval(ctx);
    	}
		
		HttpRequestInfo currRequest = new HttpRequestInfo();
		
    	try {
			currRequest.init(Info, request, SVID.makeSVID());
		} catch (URISyntaxException e1) {
			return HttpUtil.sendError(currRequest, ApiErrors.HTTP_DECODE_ERROR);
		}
    	
		URI uri = currRequest.RequestURI;
    	
    	l.debug(new SVLog("Client HTTP request", currRequest));
    	
		if (!request.getDecoderResult().isSuccess()) {
            return HttpUtil.sendError(currRequest, ApiErrors.HTTP_DECODE_ERROR);
        }
		
		authenticate(request);
		
		String uriPath = uri.getPath();
		
		if(uriPath.equals(WEBSOCKET_PATH))
		{
			String websockUrl = request.headers().get(Names.HOST) + WEBSOCKET_PATH;
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
			if(request.getMethod() == HttpMethod.POST && request.content() != null)
			{
				messageBody = request.content().toString(StandardCharsets.UTF_8);
			}
		
			ClientMessageInfo info = new ClientMessageInfo();
			info.MessageId = SVID.makeSVID();
			info.MessageNum = null;
			info.ConnectionHandler = this;
			info.User = Info.User;
			
			
			Object reply = Dispatcher.dispatch(messageType, messageBody, info);
			if(reply instanceof ApiError)
			{
				ApiError error = (ApiError)reply;
				return HttpUtil.sendErrorJson(currRequest, error, error.getHttpStatus());
			}
			else
			{
				ClientMessageEnvelope<Object> envelope = new ClientMessageEnvelope<Object>();
				envelope.message = reply;

				return HttpUtil.sendJson(currRequest, envelope);
			}
			
			
			/*ByteBuf reply;
			try {
				reply = JSON.serializeToByteBuf(envelope);
			} catch (JsonProcessingException e) {
				l.error("Error JSON encoding reply: ", e);
				reply = Unpooled.copiedBuffer("There was an error encoding the server's reply.", CharsetUtil.UTF_8);
			}

			HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					reply);
			
			response.headers().set(Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			
			if(reply != null)
			{
				HttpUtil.setContentTypeHeader(response, "application/json");
				HttpHeaders.setContentLength(response, reply.readableBytes());
			}
			
			return ctx.writeAndFlush(response);*/
		}
		
		return HttpUtil.sendError(currRequest, ApiErrors.NOT_FOUND);
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
		
		Object replyObj = Dispatcher.dispatch(messageType, messageBody, info);
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
	
	@Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        l.error("Exception caught in client handler: ", cause);
        
        if (ctx.channel().isActive())
        {
        	ApiError ise = new ApiError(ApiErrors.INTERNAL_SERVER_ERROR);
        
        	HttpUtil.sendErrorJson(ctx, ise, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
	
	public ChannelFuture sendMessage(String messageType, Object messageBody) throws Exception
	{
		String messageStr = "M:"+messageType+":"+JSON.serializeToString(messageBody);
		
		return write(messageStr);
	}
	
	public ChannelFuture sendMessage(String messageType, String serializedMessageBody)
	{
		String messageStr = "M:"+messageType+":"+serializedMessageBody;
		
		return write(messageStr);
	}
	
	private ChannelFuture write(String data)
	{
		if(!WebsocketConnected)
			return null;
		
		ChannelFuture future = Info.Ctx.channel().write(new TextWebSocketFrame(data));
		Info.Ctx.channel().flush();
		return future;
	}
	
	public void signedIn(ServervilleUser user) throws SQLException, JsonApiException
	{
		user.startNewSession();
		Info.User = user;
		CurrAuthToken = user.getSessionId();
		
		//User = new OnlineUser();
		//User.DisplayName = username;
		//User.UserId = userId;
		//User.Connection = this;
		
		/*UserJoined joinedMsg = new UserJoined();
		joinedMsg.user_info = new UserInfo();
		joinedMsg.user_info.user_id = User.UserId;
		joinedMsg.user_info.display_name = User.DisplayName;
		
		try {
			OnlineUserManager.sendBroadcast("UserJoined", joinedMsg, User);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		
		//OnlineUserManager.addUser(User);
	}

}
