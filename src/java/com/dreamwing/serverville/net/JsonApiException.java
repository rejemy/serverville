package com.dreamwing.serverville.net;

import io.netty.handler.codec.http.HttpResponseStatus;

public class JsonApiException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public ApiError Error;
	public HttpResponseStatus HttpStatus;
	
	public JsonApiException(ApiError apiError, HttpResponseStatus status)
	{
		super(apiError.errorMessage);
		Error = apiError;
		HttpStatus = status;
	}
	
	public JsonApiException(ApiErrors error)
	{
		super(error.getMessage());
		Error = new ApiError(error);
		HttpStatus = error.getHttpStatus();
	}
	
	public JsonApiException(ApiErrors error, String details)
	{
		super(error.getMessage());
		Error = new ApiError(error, details);
		HttpStatus = error.getHttpStatus();
	}
}