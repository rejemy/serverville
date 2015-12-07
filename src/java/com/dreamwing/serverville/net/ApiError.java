package com.dreamwing.serverville.net;

public class ApiError {
	public boolean isError;
	public String errorMessage;
	
	public ApiError() {}
	
	public ApiError(String message)
	{
		isError = true;
		errorMessage = message;
	}
}
