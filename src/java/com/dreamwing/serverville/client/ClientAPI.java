package com.dreamwing.serverville.client;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.*;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.PasswordUtil;

public class ClientAPI {
	
	private static final Logger l = LogManager.getLogger(ClientAPI.class);
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply SignIn(SignIn request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User != null)
			throw new JsonApiException(ApiErrors.ALREADY_SIGNED_IN);
		
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
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Password does not match");
		}
		
		info.ConnectionHandler.signedIn(user);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.user_id = user.getId();
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply ValidateSession(ValidateSessionRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User != null)
			throw new JsonApiException(ApiErrors.ALREADY_SIGNED_IN);
		
		SignInReply reply = new SignInReply();
		
		ServervilleUser user = ServervilleUser.findBySessionId(request.session_id);
		
		if(user == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Invalid session id");
		}
		
		info.ConnectionHandler.signedIn(user);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.user_id = user.getId();
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static CreateAccountReply CreateAnonymousAccount(CreateAnonymousAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User != null)
			throw new JsonApiException(ApiErrors.ALREADY_SIGNED_IN);
		
		ServervilleUser user = ServervilleUser.create(null, null, null, ServervilleUser.AdminLevel_User);
		
		CreateAccountReply reply = new CreateAccountReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signedIn(user);
		
		reply.username = null;
		reply.email = null;
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static CreateAccountReply CreateAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User != null)
			throw new JsonApiException(ApiErrors.ALREADY_SIGNED_IN);
		
		if(request.username == null || request.email == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
		
		ServervilleUser user = ServervilleUser.create(request.password, request.username, request.email, ServervilleUser.AdminLevel_User);
		

		CreateAccountReply reply = new CreateAccountReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signedIn(user);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.session_id = user.getSessionId();
		
		return reply;
	}
	
	public static CreateAccountReply ConvertToFullAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User.getUsername() != null)
			throw new JsonApiException(ApiErrors.ALREADY_SIGNED_IN, "Account is already converted");
		
		if(request.username == null || request.email == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
		
		info.User.register(request.password, request.username, request.email);
		
		CreateAccountReply reply = new CreateAccountReply();
		
		reply.user_id = info.User.getId();
		reply.username = info.User.getUsername();
		reply.email = info.User.getEmail();
		reply.session_id = info.User.getSessionId();
		
		return reply;
	}
	
	public static SignInReply GetUserInfo(GetUserInfo request, ClientMessageInfo info)
	{
		SignInReply reply = new SignInReply();
		
		reply.user_id = info.User.getId();
		reply.username = info.User.getUsername();
		reply.email = info.User.getEmail();
		reply.session_id = info.User.getSessionId();
		
		return reply;
	}
	
	public static SetDataReply SetUserKey(SetUserDataRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(request.key, request.data_type, request.value);
		long updateTime = KeyDataManager.saveKey(info.User.getId(), item);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	public static SetDataReply SetUserKeys(UserDataRequestList request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetUserDataRequest data : request.values)
		{
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(info.User.getId(), itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	
	
	private static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item)
	{
		DataItemReply data = null;
		
		if(item.isDeleted())
		{
			DataItemExtendedReply extData = new DataItemExtendedReply();
			extData.deleted = true;
			data = extData;
		}
		else
		{
			data = new DataItemReply();
		}

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
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		KeyDataItem item = KeyDataManager.loadKey(info.User.getId(), request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		return KeyDataItemToDataItemReply(info.User.getId(), item);
	}
	
	public static UserDataReply GetUserKeys(KeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		List<KeyDataItem> items = KeyDataManager.loadKeysSince(info.User.getId(), request.keys, (long)request.since);
		if(items == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
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
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
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
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		// If it's not our data, we can't see it
		if(!request.id.equals(info.User.getId()) && KeyDataItem.isPrivateKeyname(request.key))
		{
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		}
		
		KeyDataItem item = KeyDataManager.loadKey(request.id, request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		return KeyDataItemToDataItemReply(request.id, item);
	}
	
	
	public static UserDataReply GetDataKeys(GlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		boolean isMe = request.id.equals(info.User.getId());
		
		for(String keyname : request.keys)
		{
			if(!isMe && KeyDataItem.isPrivateKeyname(keyname))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, keyname);
		}
		
		List<KeyDataItem> items = KeyDataManager.loadKeysSince(request.id, request.keys, (long)request.since, request.include_deleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
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
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(request.id, (long)request.since, request.include_deleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		boolean isMe = request.id.equals(info.User.getId());
		
		for(KeyDataItem item : items)
		{
			if(!isMe && KeyDataItem.isPrivateKeyname(item.key))
			{
				continue;
			}
			
			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	private static ChannelInfo GetChannelInfo(Channel channel)
	{
		ChannelInfo info = new ChannelInfo();
		info.id = channel.getId();
		
		Collection<String> members = channel.getListeningTo();
		
		info.members = new ArrayList<String>(members);
		
		return info;
	}
	
	public static ChannelInfo ListenToChannel(ListenToChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.id);
		
		
		BaseResident listener = ResidentManager.getResident(info.User.getId());
		if(listener == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		}
		
		source.addListener(listener);
		if(request.two_way)
			listener.addListener(source);

		return GetChannelInfo((Channel)source);
	}
	
	public static EmptyClientReply EndListenToChannel(EndListenToChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.id);
		
		
		BaseResident listener = ResidentManager.getResident(info.User.getId());
		if(listener == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		}
		
		source.removeListener(listener);
		listener.removeListener(source);

		return new EmptyClientReply();
	}
	
}
