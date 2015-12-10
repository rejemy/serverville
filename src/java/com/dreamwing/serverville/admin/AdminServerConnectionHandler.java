package com.dreamwing.serverville.admin;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.timeout.IdleStateEvent;

import com.dreamwing.serverville.data.AdminActionLog;
import com.dreamwing.serverville.log.SVLog;
import com.dreamwing.serverville.net.HttpDispatcher;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpUtil;
import com.dreamwing.serverville.net.HttpConnectionInfo;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.JsonProcessingException;


public class AdminServerConnectionHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private HttpDispatcher Dispatcher;
	private HttpConnectionInfo Info;
	private HttpRequestInfo CurrRequest;
	
	private static final Logger l = LogManager.getLogger(AdminServerConnectionHandler.class);
	
	public AdminServerConnectionHandler(HttpDispatcher dispatcher)
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
		l.debug(new SVLog("Admin HTTP connection opened", Info));
    }
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		l.debug(new SVLog("Admin HTTP connection closed", Info));
    }
	
    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception
    {
    	ChannelFuture lastWrite = HandleHttpRequest(ctx, request);
    	
    	if (!HttpHeaders.isKeepAlive(request)) {
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

    private ChannelFuture HandleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception
    {
    	if(request.getMethod() == HttpMethod.OPTIONS)
    	{
    		return HttpUtil.sendPreflightApproval(ctx);
    	}
    	
    	CurrRequest = new HttpRequestInfo();
    	CurrRequest.init(Info, request, SVID.makeSVID());
    	
    	
    	l.debug(new SVLog("Adming HTTP request", CurrRequest));
    	
    	if (!request.getDecoderResult().isSuccess()) {
            return HttpUtil.sendError(CurrRequest, "Couldn't decode request", HttpResponseStatus.BAD_REQUEST);
        }

    	ChannelFuture result = Dispatcher.dispatch(CurrRequest);

    	// Log anything other than GETs (and logins, because BORING)
    	if(request.getMethod() != HttpMethod.GET && !CurrRequest.RequestURI.getPath().equals("/api/signIn") && Info.User != null)
    	{
    		AdminActionLog log = new AdminActionLog();
    		log.RequestId = CurrRequest.RequestId;
    		log.ConnectionId = CurrRequest.Connection.ConnectionId;
    		log.UserId = Info.User.getId();
    		log.Created = new Date();
    		log.API = CurrRequest.RequestURI.getPath();
    		log.Request = CurrRequest.getBody();
    		if(log.Request.length() >= 255)
    			log.Request = log.Request.substring(0, 255);
    		
    		log.create();
    	}
    	
    	return result;
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws JsonProcessingException {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
        	// Close connection on internal server error to simplify things
        	HttpUtil.sendError(CurrRequest, "Internal server error", HttpResponseStatus.INTERNAL_SERVER_ERROR).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent) {
        	ctx.close();
        }
    }
  

}
