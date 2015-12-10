package com.dreamwing.serverville.client;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.*;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.HttpUtil.JsonApiException;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.PasswordUtil;

public class ClientAPI {
	
	private static final Logger l = LogManager.getLogger(ClientAPI.class);
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply SignIn(SignIn request, ClientMessageInfo info) throws JsonApiException, Exception
	{
		if(info.User != null)
			throw new JsonApiException("Already signed in");
		SignInReply reply = new SignInReply();
		
		ServervilleUser user = null;
		
		if(request.username != null)
		{
			user = ServervilleUser.findByUsername(request.username);
		}
		else if(request.email != null)
		{
			user = ServervilleUser.findByEmail(request.email);
		}
		
		if(user == null || !user.checkPassword(request.password))
		{
			throw new JsonApiException("Invalid user/password");
		}
		
		info.ConnectionHandler.signedIn(user);
		
		reply.user_id = user.getId();
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static CreateAccountReply CreateAnonymousAccount(CreateAnonymousAccount request, ClientMessageInfo info) throws JsonApiException, Exception
	{
		if(info.User != null)
			throw new JsonApiException("Already signed in");
		
		ServervilleUser user = ServervilleUser.create(null, null, null, ServervilleUser.AdminLevel_User);
		
		CreateAccountReply reply = new CreateAccountReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signedIn(user);
		
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static CreateAccountReply CreateAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, Exception
	{
		if(info.User != null)
			throw new JsonApiException("Already signed in");
		
		if(request.username == null || request.email == null)
			throw new JsonApiException("Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException("Invalid password!");
		
		ServervilleUser user = ServervilleUser.create(request.password, request.username, request.email, ServervilleUser.AdminLevel_User);
		

		CreateAccountReply reply = new CreateAccountReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signedIn(user);
		
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	public static CreateAccountReply ConvertToFullAccount(CreateAccount request, ClientMessageInfo info) throws Exception
	{
		if(info.User.getUsername() != null)
			throw new JsonApiException("Account is already converted");
		
		if(request.username == null || request.email == null)
			throw new JsonApiException("Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException("Invalid password!");
		
		info.User.register(request.password, request.username, request.email);
		
		CreateAccountReply reply = new CreateAccountReply();
		
		reply.user_id = info.User.getId();
		reply.session_id = info.User.getSessionId();
		
		return reply;
	}
	
	public static SetDataReply SetUserKey(SetUserDataRequest request, ClientMessageInfo info) throws JsonApiException, SQLException, Exception
	{
		SetDataReply reply = new SetDataReply();
		
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(request.key, request.data_type, request.value);
		long updateTime = KeyDataManager.saveKey(info.User.getId(), item);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	public static SetDataReply SetUserKeys(UserDataRequestList request, ClientMessageInfo info) throws JsonApiException, SQLException, Exception
	{
		SetDataReply reply = new SetDataReply();
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetUserDataRequest data : request.values)
		{
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException("Invalid key name: "+data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(info.User.getId(), itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	
	
	private static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item)
	{
		DataItemReply data = new DataItemReply();
		data.id = id;
		data.key = item.key;
		try
		{
			data.value = item.asDecodedObject();
		}
		catch(Exception e)
		{
			l.error("Exception decoding JSON value: ",e);
			data.value = "<error>";
		}
		data.data_type = JsonDataType.fromKeyDataType(item.datatype);
		data.created = item.created;
		data.modified = item.modified;
		return data;
	}
	
	public static DataItemReply GetUserKey(KeyRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		KeyDataItem item = KeyDataManager.loadKey(info.User.getId(), request.key);
		if(item == null)
			throw new JsonApiException("Key not found");
		
		return KeyDataItemToDataItemReply(info.User.getId(), item);
	}
	
	public static UserDataReply GetUserKeys(KeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		List<KeyDataItem> items = KeyDataManager.loadKeysSince(info.User.getId(), request.keys, (long)request.since);
		if(items == null)
			throw new JsonApiException("Key not found");
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			DataItemReply data = KeyDataItemToDataItemReply(info.User.getId(), item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	public static UserDataReply GetAllUserKeys(AllKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(info.User.getId(), (long)request.since);
		if(items == null)
			throw new JsonApiException("Key not found");
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			DataItemReply data = KeyDataItemToDataItemReply(info.User.getId(), item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	public static DataItemReply GetDataKey(GlobalKeyRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");

		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		// If it's not our data, we can't see it
		if(!request.id.equals(info.User.getId()) && KeyDataItem.isPrivateKeyname(request.key))
		{
			throw new JsonApiException("Private key: "+request.key);
		}
		
		KeyDataItem item = KeyDataManager.loadKey(request.id, request.key);
		if(item == null)
			throw new JsonApiException("Key not found");
		
		return KeyDataItemToDataItemReply(request.id, item);
	}
	
	
	public static UserDataReply GetDataKeys(GlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");
		
		boolean isMe = request.id.equals(info.User.getId());
		
		for(String keyname : request.keys)
		{
			if(!isMe && KeyDataItem.isPrivateKeyname(keyname))
				throw new JsonApiException("Private key: "+keyname);
		}
		
		List<KeyDataItem> items = KeyDataManager.loadKeysSince(request.id, request.keys, (long)request.since);
		if(items == null)
			throw new JsonApiException("Key not found");
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();

		for(KeyDataItem item : items)
		{

			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	public static UserDataReply GetAllDataKeys(AllGlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");
		
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(request.id, (long)request.since);
		if(items == null)
			throw new JsonApiException("Key not found");
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		boolean isMe = request.id.equals(info.User.getId());
		
		for(KeyDataItem item : items)
		{
			if(!isMe && KeyDataItem.isPrivateKeyname(item.key))
			{
				continue;
			}
			
			DataItemReply data = KeyDataItemToDataItemReply(info.User.getId(), item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	/*
	public static JoinRoomReply JoinRoom(JoinRoom request, ClientMessageInfo info)
	{
		JoinRoomReply reply = new JoinRoomReply();
		
		Channel room = null;
		if(request.create)
		{
			room = ResidentManager.getOrCreateRoom(request.room_id);
		}
		else
		{
			room = ResidentManager.getRoom(request.room_id);
		}
		
		if(room != null)
		{
			reply.room_id = room.getRoomId();
			room.join(info.User);
		}
		return reply;
	}
	
	public static LeaveRoomReply LeaveRoom(LeaveRoom request, ClientMessageInfo info)
	{
		LeaveRoomReply reply = new LeaveRoomReply();
		
		Channel room = ResidentManager.getRoom(request.room_id);
		
		if(room != null)
		{
			room.leave(info.User);
		}
		return reply;
	}
	
	public static GetUserListReply GetUserList(GetUserList request, ClientMessageInfo info)
	{
		GetUserListReply reply = new GetUserListReply();
		
		Channel room = ResidentManager.getRoom(request.room_id);
		if(room == null)
			return null;
		
		Collection<OnlineUser> onlineUsers = room.getAllUsers();
		
		List<UserInfo> users = new ArrayList<UserInfo>(onlineUsers.size());
		
		for(OnlineUser onlineUser : onlineUsers)
		{
			UserInfo user = new UserInfo();
			user.display_name = onlineUser.DisplayName;
			user.user_id = onlineUser.UserId;
			users.add(user);
		}
		
		reply.users = users;
		
		return reply;
	}
	
	public static void SendBroadcast(Broadcast broadcast, ClientMessageInfo info)
	{
		BroadcastSent sent = new BroadcastSent();
		sent.message = broadcast.message;
		sent.user_id = info.User.UserId;
		sent.display_name = info.User.DisplayName;
		
		Channel room = ResidentManager.getRoom(broadcast.room_id);
		
		if(room != null)
		{
			room.sendBroadcast("Broadcast", sent, info.User, broadcast.send_to_self);
		}
		
	}
	*/
}
