package com.dreamwing.serverville.net;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class HttpHelpers {

	private static final Logger l = LogManager.getLogger(HttpHelpers.class);
	
	private static ConnectionPool SharedHttpConnectionPool;
	private static OkHttpClient SharedHttpClient;
	private static String AllowedOrigin;
	private static String Vary;
	
	static
	{
		long timeout = 0;
		try
		{
			timeout = Long.parseLong(ServervilleMain.ServerProperties.getProperty("selftest_timeout"));
		}
		catch(Exception e)
		{
			l.error("Invalid number format in selftest_timeout: ", e);
		}
		
		OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
		SharedHttpConnectionPool = new ConnectionPool();
		clientBuilder.connectionPool(SharedHttpConnectionPool)
			.connectTimeout(timeout, TimeUnit.MILLISECONDS)
			.readTimeout(timeout, TimeUnit.MILLISECONDS)
			.writeTimeout(timeout, TimeUnit.MILLISECONDS);
		SharedHttpClient = clientBuilder.build();
		
	}
	
	public static void resetHttpClient()
	{
		SharedHttpConnectionPool.evictAll();
	}
	
	private static String getAllowedOrigin()
	{
		if(AllowedOrigin != null)
			return AllowedOrigin;
		AllowedOrigin = ServervilleMain.ServerProperties.getProperty("allowed_origin");
		if(!AllowedOrigin.equals("*"))
			Vary = "Origin";
		return AllowedOrigin;
	}
	
	public static ChannelFuture sendPreflightApproval(ChannelHandlerContext ctx)
	{
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpUtil.setContentLength(response, 0);
		
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, getAllowedOrigin());
		if(Vary != null)
			response.headers().set(HttpHeaderNames.VARY, Vary);
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
		
		return ctx.writeAndFlush(response);
	}
	
	private static void setResponseCORS(HttpResponse response)
	{
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, getAllowedOrigin());
		if(Vary != null)
			response.headers().set(HttpHeaderNames.VARY, Vary);
		response.headers().set(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Notifications");
	}
	
	public static ChannelFuture sendSuccess(HttpRequestInfo req)
	{
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpUtil.setContentLength(response, 0);
		
		setResponseCORS(response);
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static ChannelFuture sendText(HttpRequestInfo req, String text)
	{
		return sendText(req, text, "text/plain");
	}
	
	public static ChannelFuture sendText(HttpRequestInfo req, String text, String contentType)
	{
		ByteBuf content = Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
				content);
		
		HttpHelpers.setContentTypeHeader(response, contentType);
		HttpUtil.setContentLength(response, content.readableBytes());
		
		setResponseCORS(response);
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static HttpResponse makeJsonResponse(Object data)
	{
		ByteBuf content;
		HttpResponseStatus status = HttpResponseStatus.OK;
		String contentType = "application/json";
		try {
			content = JSON.serializeToByteBuf(data);
		} catch (JsonProcessingException e) {
			l.error("Error encoding error reply to json: ", e);
			content = Unpooled.copiedBuffer(ApiError.encodingErrorReply, CharsetUtil.UTF_8);
			status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
		}
		
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				content);
		
		HttpHelpers.setContentTypeHeader(response, contentType);
		HttpUtil.setContentLength(response, content.readableBytes());
		
		setResponseCORS(response);
		
		return response;
	}
	
	public static ChannelFuture sendJson(HttpRequestInfo req, Object data)
	{
		HttpResponse response = makeJsonResponse(data);
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static ChannelFuture sendError(HttpRequestInfo req, ApiErrors error)
	{
		ApiError err = new ApiError(error);
		
		return sendErrorJson(req, err, error.getHttpStatus());
    }
	
	public static ChannelFuture sendError(HttpRequestInfo req, ApiErrors error, String details)
	{
		ApiError err = new ApiError(error, details);
		
		return sendErrorJson(req, err, error.getHttpStatus());
    }
	

	public static ChannelFuture sendErrorJson(HttpRequestInfo req, ApiError data, HttpResponseStatus status)
	{
		HttpResponse response = makeErrorJsonResponse(data, status);
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static HttpResponse makeErrorJsonResponse(ApiError data, HttpResponseStatus status)
	{
		data.isError = true;
		ByteBuf content;
		String contentType = "application/json";
		
		try {
			content = JSON.serializeToByteBuf(data);
		} catch (JsonProcessingException e)
		{
			l.error("Error encoding error reply to json: ", e);
			content = Unpooled.copiedBuffer(ApiError.encodingErrorReply, CharsetUtil.UTF_8);
		}
		
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				content);
		
		HttpHelpers.setContentTypeHeader(response, contentType);
		HttpUtil.setContentLength(response, content.readableBytes());
		
		setResponseCORS(response);
		
		return response;
	}
	
    public static ChannelFuture sendRedirect(HttpRequestInfo req, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);
        
        setResponseCORS(response);
        
        return req.Connection.Ctx.writeAndFlush(response);
    }
    
    public static void setContentTypeHeader(HttpResponse response, String contentType) {
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }
    
    public static class JsonResponse<S,E>
    {
    	public S Success;
    	public E Error;
    }
    
    public static <T> T getJson(String url, Class<T> replyClass) throws IOException
    {
    	Request request = new Request.Builder()
        	.url(url)
        	.build();
    	

    	Response response = SharedHttpClient.newCall(request).execute();
    	return JSON.deserialize(response.body().charStream(), replyClass);
    }
    
    public static <S,E> JsonResponse<S,E> getJson(String url, Class<S> successClass, Class<E> errorClass) throws IOException
    {
    	Request request = new Request.Builder()
        	.url(url)
        	.build();
    	
    	JsonResponse<S,E> jsonResponse = new JsonResponse<S,E>();
    	
    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		jsonResponse.Success = JSON.deserialize(response.body().charStream(), successClass);
    		return jsonResponse;
    	}
    	
    	jsonResponse.Error = JSON.deserialize(response.body().charStream(), errorClass);
    	return jsonResponse;
    }
    

    
    public static <S> S getJsonApi(String url, String sessionId, Class<S> successClass) throws IOException, JsonApiException
    {
    	Request.Builder requestBuilder = new Request.Builder().url(url);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), sessionId);
    	
    	Request request = requestBuilder.build();
    	

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return JSON.deserialize(response.body().charStream(), successClass);
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error, HttpResponseStatus.valueOf(response.code()));
    }
    
    public static byte[] getBytes(String url) throws IOException
    {
    	Request request = new Request.Builder()
    	.url(url)
    	.build();
	
    	Response response = SharedHttpClient.newCall(request).execute();
    	return response.body().bytes();
    }
    
    public static String getString(String url) throws IOException
    {
    	Request request = new Request.Builder()
    	.url(url)
    	.build();
	
    	Response response = SharedHttpClient.newCall(request).execute();
    	return response.body().string();
    }
    
    public static Response getHttpResponse(String url) throws IOException
    {
    	Request request = new Request.Builder()
    	    	.url(url)
    	    	.build();
    		
    	return SharedHttpClient.newCall(request).execute();
    }
    
    public static String getString(String url, String sessionId) throws IOException
    {
    	Request.Builder requestBuilder = new Request.Builder().url(url);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), sessionId);
    	
    	Request request = requestBuilder.build();
	
    	Response response = SharedHttpClient.newCall(request).execute();
    	return response.body().string();
    }
    
    public static <T> T postJson(String url, RequestBody body, Class<T> replyClass) throws IOException
    {
    	Request request = new Request.Builder()
        	.url(url)
        	.method("POST", body)
        	.build();
    	

    	Response response = SharedHttpClient.newCall(request).execute();
    	return JSON.deserialize(response.body().charStream(), replyClass);
    }
    
    public static <S,E> JsonResponse<S,E> postJson(String url, RequestBody body, Class<S> successClass, Class<E> errorClass) throws IOException
    {
    	Request request = new Request.Builder()
        	.url(url)
        	.method("POST", body)
        	.build();
    	
    	JsonResponse<S,E> jsonResponse = new JsonResponse<S,E>();
    	
    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		jsonResponse.Success = JSON.deserialize(response.body().charStream(), successClass);
    		return jsonResponse;
    	}
    	
    	jsonResponse.Error = JSON.deserialize(response.body().charStream(), errorClass);
    	return jsonResponse;
    }
    
    public static void postJsonApi(String url, String sessionId, RequestBody body) throws IOException, JsonApiException
    {
    	Request.Builder requestBuilder = new Request.Builder()
    			.url(url)
    			.method("POST", body);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), sessionId);
    	
    	Request request = requestBuilder.build();

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return;
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error, HttpResponseStatus.valueOf(response.code()));
    }
    
    public static <S> S postJsonApi(String url, String sessionId, RequestBody body, Class<S> successClass) throws IOException, JsonApiException
    {
    	Request.Builder requestBuilder = new Request.Builder()
    			.url(url)
    			.method("POST", body);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), sessionId);
    	
    	Request request = requestBuilder.build();

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return JSON.deserialize(response.body().charStream(), successClass);
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error, HttpResponseStatus.valueOf(response.code()));
    }
    
    public static final MediaType TEXT_CONTENT_TYPE =
  	      MediaType.parse("text/plain");
    
    public static final MediaType JSON_CONTENT_TYPE =
    	      MediaType.parse("application/json");
    
    public static final MediaType JAVASCRIPT_CONTENT_TYPE =
  	      MediaType.parse("application/javascript");
    
    @SuppressWarnings({ "rawtypes" })
    public static <S> S postClientApi(String url, String sessionId, Object body, TypeReference valueTypeRef) throws IOException, JsonApiException
    {
	    	byte[] bodyData = JSON.serializeToBytes(body);
	    	RequestBody rbody = RequestBody.create(JSON_CONTENT_TYPE, bodyData);
	    	
	    	Request.Builder requestBuilder = new Request.Builder()
	    			.url(url)
	    			.method("POST", rbody);
	    	
	    	if(sessionId != null)
	    		requestBuilder.addHeader(HttpHeaderNames.AUTHORIZATION.toString(), sessionId);
	    	
	    	Request request = requestBuilder.build();
	
	    	Response response = SharedHttpClient.newCall(request).execute();
	    	if(response.code() == 200)
	    	{
	    		return JSON.JsonMapper.readValue(response.body().charStream(), valueTypeRef);
	    	}
	    	
	    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
	    	throw new JsonApiException(error, HttpResponseStatus.valueOf(response.code()));
    }
    

    
}
