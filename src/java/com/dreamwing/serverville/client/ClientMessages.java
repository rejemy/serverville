package com.dreamwing.serverville.client;

import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.data.DataItemVisibility;
import com.dreamwing.serverville.data.JsonDataType;


public class ClientMessages {

	
	public static class CreateAnonymousAccount
	{

	}
	
	public static class CreateAccount
	{
		public String username;
		public String email;
		public String password;
	}
	
	public static class CreateAccountReply
	{
		public String user_id;
		public String session_id;
	}

	public static class SignIn
	{
		public String username;
		public String email;
		public String password;
	}
	
	public static class SignInReply
	{
		public String user_id;
		public String session_id;
	}
	
	public static class SetUserDataRequest
	{
		public String key;
		public Object value;
		public JsonDataType data_type;
		public DataItemVisibility visibility;
	}
	
	public static class UserDataRequestList
	{
		public List<SetUserDataRequest> values;
	}
	
	public static class SetDataReply
	{
		public double updated_at;
	}
	
	public static class KeyRequest
	{
		public String key;
	}
	
	public static class DataItemReply
	{
		public String id;
		public String key;
		public Object value;
		public JsonDataType data_type;
		public double created;
		public double modified;
		public DataItemVisibility visibility;
	}
	
	public static class KeysRequest
	{
		public List<String> keys;
		public double since;
	}
	
	public static class UserDataReply
	{
		public Map<String,DataItemReply> values;
	}
	
	public static class AllKeysRequest
	{
		public double since;
	}
	
	
	public static class GlobalKeyRequest
	{
		public String id;
		public String key;
	}
	
	public static class GlobalKeysRequest
	{
		public String id;
		public List<String> keys;
		public double since;
	}
	
	public static class AllGlobalKeysRequest
	{
		public String id;
		public double since;
	}
	
	/*
	
	public static class UserInfo
	{
		public String display_name;
		public String user_id;
	}
	
	public static class JoinRoom
	{
		public String room_id;
		public boolean create;
	}
	
	public static class JoinRoomReply
	{
		public String room_id;
	}
	
	public static class LeaveRoom
	{
		public String room_id;
	}
	
	public static class LeaveRoomReply
	{
		public String room_id;
	}
	
	public static class GetUserList
	{
		public String room_id;
	}

	public static class GetUserListReply
	{
		public String room_id;
		public List<UserInfo> users;
	}
	
	public static class Broadcast
	{
		public String room_id;
		public String message;
		public boolean send_to_self;
	}
	
	public static class SetRoomData
	{
		public String room_id;
		public String key;
		public String value;
		public boolean broadcast;
	}
	
	public static class RoomData
	{
		public String key;
		public String value;
	}
	
	public static class GetRoomData
	{
		public String room_id;
		public List<String> keys;
	}
	
	public static class GetRoomDataReply
	{
		public String room_id;
		public List<RoomData> data;
	}
	
	// Server initiated messages
	
	public static class UserJoined
	{
		public String room_id;
		public UserInfo user_info;
	}
	
	public static class UserLeft
	{
		public String room_id;
		public String display_name;
		public String user_id;
	}
	
	public static class BroadcastSent
	{
		public String room_id;
		public String display_name;
		public String user_id;
		public String message;
	}
	
	public static class RoomDataSet
	{
		public String user_id;
		public String room_id;
		public RoomData data;
	}*/

}
