package com.dreamwing.serverville.admin;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.APIAuthenticator;
import com.dreamwing.serverville.net.HttpRequestInfo;

import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;

public class AdminAuthentiator implements APIAuthenticator
{
	private static final Logger l = LogManager.getLogger(AdminAuthentiator.class);

	@Override
	public boolean isAuthenticated(HttpRequestInfo req)
	{
		ServervilleUser user = getUser(req);
		if(user == null)
			return false;
		
		int privNeeded = 0;
		if(req.Request.getMethod() == HttpMethod.GET)
		{
			// If it's a get, they only need read privileges
			privNeeded = ServervilleUser.Admin_ReadPriv;
		}
		else
		{
			// Otherwise they need read/write
			privNeeded = ServervilleUser.Admin_WritePriv;
		}
		
		boolean hasPriv = (user.AdminLevel & privNeeded) != 0;
		if(hasPriv)
			return true;
		
		return false;
	}
	
	private ServervilleUser getUser(HttpRequestInfo req)
	{
		String authToken = req.getOneHeader(Names.AUTHORIZATION, null);
		if(authToken == null)
			return null;
		
		try {
			
			// Already authenticated
			if(req.Connection.User != null)
			{
				// Make sure our session is still valid
				AdminUserSession session = AdminUserSession.findByUserId(req.Connection.User.getId());
				if(session == null)
					return null;
				if(!session.getId().equals(authToken))
					return null;
				
				return req.Connection.User;
			}
			
			AdminUserSession session = AdminUserSession.findById(authToken);
			
			if(session == null)
				return null;
			
			ServervilleUser user = ServervilleUser.findById(session.UserId);
			if(user == null)
			{
				l.warn("We had an admin session that points to a missing user: "+session.UserId);
				return null;
			}
			
			req.Connection.User = user;
			
			return user;
			
		} catch (SQLException e) {
			l.warn("Sql excpetion while trying to authenticate admin connection", e);
		}
		
		return null;
	}

}
