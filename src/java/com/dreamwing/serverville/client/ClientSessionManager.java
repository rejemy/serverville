package com.dreamwing.serverville.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.data.UserSession;

public class ClientSessionManager {

	private static ConcurrentMap<String,ClientConnectionHandler> OnlineSessions;
	
	public static void init()
	{
		OnlineSessions = new ConcurrentHashMap<String,ClientConnectionHandler>();
	}
	
	public static void addSession(ClientConnectionHandler handler)
	{
		UserSession session = handler.getSession();
		OnlineSessions.put(session.Id, handler);
	}
	
	public static void removeSession(ClientConnectionHandler handler)
	{
		UserSession session = handler.getSession();
		OnlineSessions.remove(session.Id, handler);
	}
	
	public static ClientConnectionHandler getSession(String id)
	{
		return OnlineSessions.get(id);
	}
}
