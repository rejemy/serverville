package com.dreamwing.serverville.net;


import java.io.IOException;

import com.dreamwing.serverville.client.ClientMessageEnvelope;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.util.CharsetUtil;

public class HttpUtil {

	private static OkHttpClient SharedHttpClient;
	
	static
	{
		SharedHttpClient = new OkHttpClient();
		SharedHttpClient.setConnectionPool(ConnectionPool.getDefault());
	}
	
	public static void resetHttpClient()
	{
		SharedHttpClient.getConnectionPool().evictAll();
	}
	
	public static ChannelFuture sendSuccess(HttpRequestInfo req)
	{
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders.setContentLength(response, 0);
		
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
		
		HttpUtil.setContentTypeHeader(response, contentType);
		HttpHeaders.setContentLength(response, content.readableBytes());
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static ChannelFuture sendJson(HttpRequestInfo req, Object data) throws JsonProcessingException
	{
		ByteBuf content = JSON.serializeToByteBuf(data);
		
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
				content);
		
		HttpUtil.setContentTypeHeader(response, "application/json");
		HttpHeaders.setContentLength(response, content.readableBytes());
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
	public static ChannelFuture sendError(HttpRequestInfo req, String errorText, HttpResponseStatus status) throws JsonProcessingException
	{
		ApiError err = new ApiError(errorText);
		
		return sendErrorJson(req, err, status);
    }

	public static ChannelFuture sendErrorJson(HttpRequestInfo req, ApiError data, HttpResponseStatus status) throws JsonProcessingException
	{
		data.isError = true;
		ByteBuf content = JSON.serializeToByteBuf(data);
		
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				content);
		
		HttpUtil.setContentTypeHeader(response, "application/json");
		HttpHeaders.setContentLength(response, content.readableBytes());
		
		return req.Connection.Ctx.writeAndFlush(response);
	}
	
    public static ChannelFuture sendRedirect(HttpRequestInfo req, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(Names.LOCATION, newUri);

        return req.Connection.Ctx.writeAndFlush(response);
    }
    
    public static void setContentTypeHeader(HttpResponse response, String contentType) {
        
        response.headers().set(Names.CONTENT_TYPE, contentType);
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
    
    public static class JsonApiException extends Exception
    {
		private static final long serialVersionUID = 1L;
		
		public ApiError Error;
    	
    	public JsonApiException(ApiError apiError)
    	{
    		super(apiError.errorMessage);
    		Error = apiError;
    	}
    	
    	public JsonApiException(String errorMessage)
    	{
    		super(errorMessage);
    		Error = new ApiError(errorMessage);
    	}
    }
    
    public static <S> S getJsonApi(String url, String sessionId, Class<S> successClass) throws IOException, JsonApiException
    {
    	Request.Builder requestBuilder = new Request.Builder().url(url);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(Names.AUTHORIZATION, sessionId);
    	
    	Request request = requestBuilder.build();
    	

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return JSON.deserialize(response.body().charStream(), successClass);
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error);
    }
    
    public static String getString(String url) throws IOException
    {
    	Request request = new Request.Builder()
    	.url(url)
    	.build();
	
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
    		requestBuilder.addHeader(Names.AUTHORIZATION, sessionId);
    	
    	Request request = requestBuilder.build();

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return;
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error);
    }
    
    public static <S> S postJsonApi(String url, String sessionId, RequestBody body, Class<S> successClass) throws IOException, JsonApiException
    {
    	Request.Builder requestBuilder = new Request.Builder()
    			.url(url)
    			.method("POST", body);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(Names.AUTHORIZATION, sessionId);
    	
    	Request request = requestBuilder.build();

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		return JSON.deserialize(response.body().charStream(), successClass);
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error);
    }
    
    private static final MediaType JSON_CONTENT_TYPE =
    	      MediaType.parse("application/json");
    
    @SuppressWarnings({ "rawtypes" })
    public static <S> S postClientApi(String url, String sessionId, Object body, TypeReference valueTypeRef) throws IOException, JsonApiException
    {
    	byte[] bodyData = JSON.serializeToBytes(body);
    	RequestBody rbody = RequestBody.create(JSON_CONTENT_TYPE, bodyData);
    	
    	Request.Builder requestBuilder = new Request.Builder()
    			.url(url)
    			.method("POST", rbody);
    	
    	if(sessionId != null)
    		requestBuilder.addHeader(Names.AUTHORIZATION, sessionId);
    	
    	Request request = requestBuilder.build();

    	Response response = SharedHttpClient.newCall(request).execute();
    	if(response.code() == 200)
    	{
    		ClientMessageEnvelope<S> envelope = JSON.JsonMapper.readValue(response.body().charStream(), valueTypeRef);
    		return envelope.message;
    	}
    	
    	ApiError error = JSON.deserialize(response.body().charStream(), ApiError.class);
    	throw new JsonApiException(error);
    }
    

    
}
