package com.dreamwing.serverville.residents;



public class Channel extends BaseResident {

	
	
	
	public Channel(String id)
	{
		super(id);
		
		
	}
	

	
	
	/*
	public void join(OnlineUser user)
	{
		if(Members.put(user.UserId, user) != null)
		{
			// Was already in room
			return;
		}
		
		user.Rooms.put(RoomId, this);
		
		UserJoined message = new UserJoined();
		message.room_id = RoomId;
		message.user_info = new UserInfo();
		message.user_info.user_id = user.UserId;
		message.user_info.display_name = user.DisplayName;
		sendBroadcast("UserJoined", message, user, false);
		
		
	}
	
	public void leave(OnlineUser user)
	{
		if(Members.remove(user.UserId) == null)
		{
			// Wasn't in room
			return;
		}
		
		user.Rooms.remove(RoomId);
		
		UserLeft message = new UserLeft();
		message.room_id = RoomId;
		message.user_id = user.UserId;
		message.display_name = user.DisplayName;
		sendBroadcast("UserLeft", message, user, false);
	}
	
	public Collection<OnlineUser> getAllUsers()
	{
		return Members.values();
	}
	
	public void sendBroadcast(String messageType, Object message, OnlineUser sender, boolean sendToSelf)
	{
		String serializedMessage;
		try {
			serializedMessage = JSON.serializeToString(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		for(OnlineUser user : Members.values())
		{
			if(!sendToSelf && user == sender)
				continue;
			
			user.Connection.sendMessage(messageType, serializedMessage);

		}
	}
	
	public void setData(String key, String value, OnlineUser sender, boolean broadcast)
	{
		RoomData data = new RoomData();
		data.key = key;
		data.value = value;
		
		State.put(key, data);
		if(broadcast)
		{
			String serializedMessage;
			RoomDataSet message = new RoomDataSet();
			message.user_id = sender.UserId;
			message.room_id = RoomId;
			message.data = data;
			
			try {
				serializedMessage = JSON.serializeToString(message);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			for(OnlineUser user : Members.values())
			{
				user.Connection.sendMessage("RoomDataSet", serializedMessage);

			}
		}
	}*/
}
