package com.dreamwing.serverville;

public class OnlineUserManager
{
	/*
	private static ConcurrentHashMap<String, OnlineUser> ConnectedUsers = new ConcurrentHashMap<String, OnlineUser>();
	
	public static OnlineUser getOnlineUser(String id)
	{
		return ConnectedUsers.get(id);
	}
	
	public static void addUser(OnlineUser user)
	{
		ConnectedUsers.put(user.UserId, user);
	}
	
	public static void removeUser(String userId)
	{
		OnlineUser user = ConnectedUsers.remove(userId);
		
		if(user != null)
		{
			//user.leaveAllRooms();
		}
	}
	
	public static Collection<OnlineUser> getAllUsers()
	{
		return ConnectedUsers.values();
	}
	
	public static void sendBroadcast(String messageType, Object message, OnlineUser sender) throws Exception
	{
		String serializedMessage = JSON.serializeToString(message);
		
		for(OnlineUser user : ConnectedUsers.values())
		{
			if(user == sender)
				continue;
			
			user.Connection.sendMessage(messageType, serializedMessage);

		}
	}
	*/
}
