package com.dreamwing.serverville.agent;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.log.SVLog;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpConnectionInfo;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.net.SubnetMask;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.JsonProcessingException;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class AgentServerConnectionHandler extends SimpleChannelInboundHandler<Object>
{
	private static final Logger l = LogManager.getLogger(AgentServerConnectionHandler.class);
	
	private static final String WEBSOCKET_PATH = "/websocket";
	private WebSocketServerHandshaker Handshaker;
	
	private HttpConnectionInfo Info;
	
	private AgentDispatcher Dispatcher;
	private SubnetMask ValidAddrs;
	
	private volatile boolean WebsocketConnected = false;
	
	
	private int MessageSequence = 0;
	
	public AgentServerConnectionHandler(AgentDispatcher dispatcher)
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
		
		l.debug(new SVLog("Agent HTTP connection opened", Info));
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		WebsocketConnected = false;
		l.debug(new SVLog("Agent HTTP connection closed", Info));
		
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
			keepAlive = HttpUtil.isKeepAlive(request);
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
			else
			{
				ctx.close();
			}
		}
	}
	
	private boolean authenticate(FullHttpRequest request, InetSocketAddress remoteAddr)
	{
		String authToken = request.headers().get(HttpHeaderNames.AUTHORIZATION);
		if(authToken == null)
		{
			return false;
		}
		
		AgentKey key = null;
		
		try {
			key = AgentKey.load(authToken);
		} catch (SQLException e) {
			l.error("Error loading agent key:", e);
			return false;
		}
		
		if(key == null)
			return false;
		
		if(key.Expiration != null && key.Expiration.getTime() <= System.currentTimeMillis())
			return false;
		
		if(key.IPRange != null)
		{
			if(ValidAddrs == null || !ValidAddrs.getAddrString().equals(key.IPRange))
			{
				try
				{
					ValidAddrs = new SubnetMask(key.IPRange);
				}
				catch(Exception e)
				{
					// Should not happen
					l.error("Invalid IPRange in agent key "+authToken+" "+key.IPRange, e);
					return false;
				}
			}
			
			
			if(!ValidAddrs.match(remoteAddr.getAddress()))
				return false;
		}
		
		return true;
	}
	
	private ChannelFuture handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request)
	{
		if(request.method() == HttpMethod.OPTIONS)
		{
			return HttpHelpers.sendPreflightApproval(ctx);
		}
		
		HttpRequestInfo CurrRequest = new HttpRequestInfo();
		
		try {
			CurrRequest.init(Info, request, SVID.makeSVID());
		} catch (URISyntaxException e1) {
			return HttpHelpers.sendError(CurrRequest, ApiErrors.HTTP_DECODE_ERROR);
		}
		
		URI uri = CurrRequest.RequestURI;
		
		l.debug(new SVLog("Agent HTTP request", CurrRequest));
		
		if (!request.decoderResult().isSuccess()) {
			return HttpHelpers.sendError(CurrRequest, ApiErrors.HTTP_DECODE_ERROR);
		}
		
		if(!authenticate(request, (InetSocketAddress)ctx.channel().remoteAddress()))
		{
			return HttpHelpers.sendError(CurrRequest, ApiErrors.BAD_AUTH);
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
		
			
			ByteBuf reply = null;
			try
			{
				reply = Dispatcher.dispatch(messageType, messageBody);
			}
			catch(JsonProcessingException e)
			{
				return HttpHelpers.sendError(CurrRequest, ApiErrors.JSON_ERROR, e.getMessage());
			}
			catch(SQLException e)
			{
				return HttpHelpers.sendError(CurrRequest, ApiErrors.DB_ERROR, e.getMessage());
			}
			catch(JsonApiException e)
			{
				return HttpHelpers.sendErrorJson(CurrRequest, e.Error, e.HttpStatus);
			}
			catch(Exception e)
			{
				return HttpHelpers.sendError(CurrRequest, ApiErrors.INTERNAL_SERVER_ERROR, e.getMessage());
			}
			

			HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					reply);
			
			if(reply != null)
			{
				HttpHelpers.setContentTypeHeader(response, "application/json");
				HttpUtil.setContentLength(response, reply.readableBytes());
			}
			
			response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			
			return ctx.writeAndFlush(response);
		}
		
		return HttpHelpers.sendError(CurrRequest, ApiErrors.NOT_FOUND);
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
		
		String reply = null;
		try {
			if(messageType == null)
			{
				// It's a reply to a sever message
				
			}
			else
			{
				reply = Dispatcher.dispatch(messageType, messageNum, messageBody);
			}
		} catch (Exception e) {
			l.error("Error in message handler: "+messageType, e);
			return null;
		}
		
		if(reply != null)
		{
			return write(reply);
		}
		
		return null;
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
	
	private synchronized int getNextMessageNum()
	{
		return MessageSequence++;
	}
	
	public ChannelFuture sendMessage(String messageType, Object messageBody) throws Exception
	{
		int messageNum = getNextMessageNum();
		String messageStr = messageType+":"+messageNum+":"+JSON.serializeToString(messageBody);
		
		return write(messageStr);
	}
	
	public ChannelFuture sendMessage(String messageType, String serializedMessageBody)
	{
		int messageNum = getNextMessageNum();
		String messageStr = messageType+":"+messageNum+":"+serializedMessageBody;
		
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
	
	

}
