package com.dreamwing.serverville.client;

import com.dreamwing.serverville.net.APIAuthenticator;
import com.dreamwing.serverville.net.HttpRequestInfo;

public class ClientFormAuthenticator implements APIAuthenticator
{
	@Override
	public boolean isAuthenticated(HttpRequestInfo req)
	{
		return req.Connection.User != null;
	}

}
