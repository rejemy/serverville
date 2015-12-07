package com.dreamwing.serverville.net;

public interface APIAuthenticator {
	
	boolean isAuthenticated(HttpRequestInfo req);
}
