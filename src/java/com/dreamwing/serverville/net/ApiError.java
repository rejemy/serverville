package com.dreamwing.serverville.net;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ApiError
{
	public boolean isError;
	
	public int errorCode;
	public String errorMessage;
	public String errorDetails;
	
	private HttpResponseStatus errorStatus;
	
	public static final String encodingErrorReply = "{\"isError\":true,\"errorCode\":17,\"errorMessage\":\"Error encoding JSON. This shouldn't happen.\"}";
	
	public ApiError() {} // Default constructor needed for JSON deserializer
	
	public ApiError(ApiErrors error)
	{
		isError = true;
		errorCode = error.getCode();
		errorMessage = error.getMessage();
		errorStatus = error.getHttpStatus();
	}
	
	public ApiError(ApiErrors error, String details)
	{
		isError = true;
		errorCode = error.getCode();
		errorMessage = error.getMessage();
		errorDetails = details;
		errorStatus = error.getHttpStatus();
	}
	
	@JsonIgnore
	public HttpResponseStatus getHttpStatus()
	{
		return errorStatus;
	}
}
