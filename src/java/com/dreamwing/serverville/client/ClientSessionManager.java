package com.dreamwing.serverville.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.data.UserSession;

public class ClientSessionManager {

	private static ConcurrentMap<String,ClientConnectionHandler> OnlineSessions;
	private static ConcurrentMap<String,ClientConnectionHandler> OnlineSessionsByUserId;
	
	public static void init()
	{
		OnlineSessions = new ConcurrentHashMap<String,ClientConnectionHandler>();
		OnlineSessionsByUserId = new ConcurrentHashMap<String,ClientConnectionHandler>();
	}
	
	public static void addSession(ClientConnectionHandler handler)
	{
		UserSession session = handler.getSession();
		OnlineSessions.put(session.Id, handler);
		OnlineSessionsByUserId.put(session.UserId, handler);
	}
	
	public static void removeSession(ClientConnectionHandler handler)
	{
		UserSession session = handler.getSession();
		OnlineSessions.remove(session.Id, handler);
		OnlineSessionsByUserId.remove(session.UserId, handler);
	}
	
	public static ClientConnectionHandler getSession(String sessionId)
	{
		return OnlineSessions.get(sessionId);
	}
	
	public static ClientConnectionHandler getSessionByUserId(String userId)
	{
		return OnlineSessionsByUserId.get(userId);
	}
}
