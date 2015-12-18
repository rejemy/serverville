package com.dreamwing.serverville.net;

public class ApiError {
	
	public boolean isError;
	
	public int errorCode;
	public String errorMessage;
	public String errorDetails;
	
	public ApiError() {} // Default constructor needed for JSON deserializer
	
	public ApiError(ApiErrors error)
	{
		isError = true;
		errorCode = error.getCode();
		errorMessage = error.getMessage();
	}
	
	public ApiError(ApiErrors error, String details)
	{
		isError = true;
		errorCode = error.getCode();
		errorMessage = error.getMessage();
		errorDetails = details;
	}
}
