package com.dreamwing.serverville.client;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.CurrencyInfoManager;
import com.dreamwing.serverville.ProductManager;
import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.client.ClientMessages.*;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.InviteCode;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataRecord;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.Resident;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.stripe.StripeInterface;
import com.dreamwing.serverville.util.CurrencyUtil;
import com.dreamwing.serverville.util.LocaleUtil;
import com.dreamwing.serverville.util.PasswordUtil;

public class ClientAPI {
	
	private static final Logger l = LogManager.getLogger(ClientAPI.class);
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply SignIn(SignIn request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
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
		
		info.ConnectionHandler.signIn(user);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.user_id = user.getId();
		reply.session_id = user.getSessionId();
		reply.admin_level = user.AdminLevel;
		reply.country = user.Country;
		reply.language = user.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply ValidateSession(ValidateSessionRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SignInReply reply = new SignInReply();
		
		UserSession session = UserSession.findById(request.session_id);
		if(session == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Invalid session id");
		}
		
		ServervilleUser user = ServervilleUser.findById(session.UserId);
		if(user == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Invalid session");
		}
		
		if(session.Expired)
		{
			// Unexpire it, it's back baby!
			session.Expired = false;
			session.update();
		}
		
		info.ConnectionHandler.signIn(user, session);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.user_id = user.getId();
		reply.session_id = user.getSessionId();
		reply.admin_level = user.AdminLevel;
		reply.country = user.Country;
		reply.language = user.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply CreateAnonymousAccount(CreateAnonymousAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		InviteCode invite = null;
		if(ServervilleMain.RequireInvite)
		{
			if(request.invite_code == null)
				throw new JsonApiException(ApiErrors.INVITE_REQUIRED);
			
			invite = InviteCode.findById(request.invite_code);
			if(invite == null)
				throw new JsonApiException(ApiErrors.INVALID_INVITE_CODE, "Invalid invite code: "+request.invite_code);
		}
		
		ServervilleUser user = ServervilleUser.create(null, null, null, ServervilleUser.AdminLevel_User, request.language, request.country);
		
		if(invite != null)
		{
			invite.delete();
		}
		
		SignInReply reply = new SignInReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signIn(user);
		
		reply.username = null;
		reply.email = null;
		reply.session_id = user.getSessionId();
		reply.admin_level = user.AdminLevel;
		reply.country = user.Country;
		reply.language = user.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply CreateAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.username == null || request.email == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
		
		InviteCode invite = null;
		if(ServervilleMain.RequireInvite)
		{
			if(request.invite_code == null)
				throw new JsonApiException(ApiErrors.INVITE_REQUIRED);
			
			invite = InviteCode.findById(request.invite_code);
			if(invite == null)
				throw new JsonApiException(ApiErrors.INVALID_INVITE_CODE, "Invalid invite code: "+request.invite_code);
		}
		
		ServervilleUser user = ServervilleUser.create(request.password, request.username, request.email, ServervilleUser.AdminLevel_User, request.language, request.country);
		
		if(invite != null)
		{
			invite.delete();
		}

		SignInReply reply = new SignInReply();
		reply.user_id = user.getId();
		
		info.ConnectionHandler.signIn(user);
		
		reply.username = user.getUsername();
		reply.email = user.getEmail();
		reply.session_id = user.getSessionId();
		reply.admin_level = user.AdminLevel;
		reply.country = user.Country;
		reply.language = user.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	public static SignInReply ConvertToFullAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User.getUsername() != null)
			throw new JsonApiException(ApiErrors.ALREADY_REGISTERED, "Account is already converted");
		
		if(request.username == null || request.email == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
		
		info.User.register(request.password, request.username, request.email);
		
		SignInReply reply = new SignInReply();
		
		reply.user_id = info.User.getId();
		reply.username = info.User.getUsername();
		reply.email = info.User.getEmail();
		reply.session_id = info.User.getSessionId();
		reply.admin_level = info.User.AdminLevel;
		reply.country = info.User.Country;
		reply.language = info.User.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	public static ServerTime GetTime(EmptyClientRequest request, ClientMessageInfo info)
	{
		ServerTime time = new ServerTime();
		time.time = System.currentTimeMillis();
		
		return time;
	}
	
	public static UserAccountInfo GetUserInfo(GetUserInfo request, ClientMessageInfo info)
	{
		UserAccountInfo reply = new UserAccountInfo();
		
		reply.user_id = info.User.getId();
		reply.username = info.User.getUsername();
		reply.email = info.User.getEmail();
		reply.session_id = info.User.getSessionId();
		
		return reply;
	}
	
	public static EmptyClientReply SetLocale(SetLocaleRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		info.User.setLocale(request.country, request.language);
		
		return new EmptyClientReply();
	}
	
	public static SetDataReply SetUserKey(SetUserDataRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		if(!KeyDataItem.isValidUserWriteKeyname(request.key))
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
			if(!KeyDataItem.isValidUserWriteKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(info.User.getId(), itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	
	
	public static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item)
	{
		DataItemReply data = new DataItemReply();
		
		if(item.isDeleted())
		{
			data.deleted = true;
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
		if(!KeyDataItem.isValidUserReadKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		KeyDataItem item = KeyDataManager.loadKey(info.User.getId(), request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		return KeyDataItemToDataItemReply(info.User.getId(), item);
	}
	
	public static UserDataReply GetUserKeys(KeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		for(String key : request.keys)
		{
			if(!KeyDataItem.isValidUserReadKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		}
		
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
		List<KeyDataItem> items = KeyDataManager.loadAllUserVisibleKeysSince(info.User.getId(), (long)request.since);
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

		if(!KeyDataItem.isValidUserReadKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		// If it's not our data, we can't see it
		if(!request.id.equals(info.User.getId()) && !KeyDataItem.isPublicKeyname(request.key))
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
			if(!KeyDataItem.isValidUserReadKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
			
			if(!isMe && !KeyDataItem.isPublicKeyname(keyname))
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
		
		List<KeyDataItem> items = KeyDataManager.loadAllUserVisibleKeysSince(request.id, (long)request.since, request.include_deleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		boolean isMe = request.id.equals(info.User.getId());
		
		for(KeyDataItem item : items)
		{
			if(!isMe && !KeyDataItem.isPublicKeyname(item.key))
			{
				continue;
			}
			
			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	public static KeyDataInfo GetKeyDataRecord(KeyDataRecordRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		KeyDataRecord record = KeyDataRecord.load(request.id);
		if(record == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		KeyDataInfo keyInfo = new KeyDataInfo();
		
		keyInfo.id = record.Id;
		keyInfo.type = record.Type;
		keyInfo.owner = record.Owner;
		keyInfo.parent = record.Parent;
		keyInfo.version = record.Version;
		keyInfo.created = record.Created.getTime();
		keyInfo.modified = record.Modified.getTime();

		return keyInfo;
	}

	public static SetDataReply SetDataKeys(SetGlobalDataRequest request, ClientMessageInfo info) throws SQLException, JsonApiException
	{
		KeyDataRecord record = KeyDataRecord.load(request.id);
		if(record == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		if(!record.Owner.equals(info.User.getId()))
			throw new JsonApiException(ApiErrors.FORBIDDEN);
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetUserDataRequest data : request.values)
		{
			if(!KeyDataItem.isValidUserWriteKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(record.Id, itemList);
		
		
		SetDataReply reply = new SetDataReply();
		reply.updated_at = updateTime;
		return reply;
	}
	
	public static EmptyClientReply SetTransientValue(SetTransientValueRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		if(!KeyDataItem.isValidUserWriteKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);

		Resident resident = info.UserPresence.getOrCreateAlias(request.alias);
		resident.setTransientValue(request.key, request.value);
		
		EmptyClientReply reply = new EmptyClientReply();
		return reply;
	}
	
	public static EmptyClientReply SetTransientValues(SetTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		for(String keyname : request.values.keySet())
		{
			if(!KeyDataItem.isValidUserWriteKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
		}
		
		Resident resident = info.UserPresence.getOrCreateAlias(request.alias);
		resident.setTransientValues(request.values);
		
		EmptyClientReply reply = new EmptyClientReply();
		return reply;
	}
	
	
	public static TransientDataItemReply GetTransientValue(GetTransientValueRequest request, ClientMessageInfo info) throws JsonApiException
	{
		BaseResident resident = null;
		
		if(!KeyDataItem.isValidUserReadKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		if(request.id != null)
		{
			resident = ResidentManager.getResident(request.id);
		}
		else if(info.UserPresence != null)
		{
			resident = info.UserPresence.getAlias(request.alias);
		}
		
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, request.id+" / "+request.alias);
		}
		
		if(!info.User.getId().equals(resident.getUserId()) && !KeyDataItem.isPublicKeyname(request.key))
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		
		if(request.key.startsWith("$$"))
				throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		TransientDataItem item = resident.getTransientValue(request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		TransientDataItemReply reply = new TransientDataItemReply();
		reply.value = item.value;
		return reply;
	}
	

	public static TransientDataItemsReply GetTransientValues(GetTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		BaseResident resident = null;
		
		if(request.id != null)
		{
			resident = ResidentManager.getResident(request.id);
		}
		else if(info.UserPresence != null)
		{
			resident = info.UserPresence.getAlias(request.alias);
		}
		
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, request.id+" / "+request.alias);
		}
		
		Map<String,Object> values = new HashMap<String,Object>();
		
		boolean isMe = info.User.getId().equals(resident.getUserId());
		
		for(String key : request.keys)
		{
			if(!KeyDataItem.isValidUserReadKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
			
			if(!isMe && !KeyDataItem.isPublicKeyname(key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, key);
			
			TransientDataItem item = resident.getTransientValue(key);
			if(item != null)
			{
				values.put(item.key, item.value);
			}
		}

		TransientDataItemsReply reply = new TransientDataItemsReply();
		reply.values = values;
		return reply;
	}
	
	public static TransientDataItemsReply GetAllTransientValues(GetAllTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		BaseResident resident = null;
		
		if(request.id != null)
		{
			resident = ResidentManager.getResident(request.id);
		}
		else if(info.UserPresence != null)
		{
			resident = info.UserPresence.getAlias(request.alias);
		}
		
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, request.id+" / "+request.alias);
		}
		
		Map<String,Object> values = new HashMap<String,Object>();
		
		boolean isMe = info.User.getId().equals(resident.getUserId());
		
		for(TransientDataItem item : resident.getAllTransientValues())
		{
			if(!KeyDataItem.isValidUserReadKeyname(item.key))
				continue;
			
			if(!isMe && !KeyDataItem.isPublicKeyname(item.key))
				continue;
			
			values.put(item.key, item);
		}
		
		TransientDataItemsReply reply = new TransientDataItemsReply();
		reply.values = values;
		return reply;
	}
	
	public static ChannelInfo JoinChannel(JoinChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		}
		Channel channel = (Channel)source;
		
		Resident alias = info.UserPresence.getOrCreateAlias(request.alias);
		
		if(request.values != null)
		{
			alias.setTransientValues(request.values, true);
		}
		
		channel.addResident(alias);
		channel.addListener(info.UserPresence);
		

		return channel.getChannelInfo(0);
	}
	
	public static EmptyClientReply LeaveChannel(LeaveChannelRequest request, ClientMessageInfo info) throws JsonApiException
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
		Channel channel = (Channel)source;
		
		Resident alias = info.UserPresence.getAlias(request.alias);
		if(alias == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.alias);
		}
		
		channel.removeResident(alias, request.final_values);
		channel.removeListener(info.UserPresence);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply AddAliasToChannel(JoinChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		}
		Channel channel = (Channel)source;
		
		Resident alias = info.UserPresence.getOrCreateAlias(request.alias);
		
		if(request.values != null)
		{
			alias.setTransientValues(request.values, true);
		}
		
		channel.addResident(alias);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply RemoveAliasFromChannel(LeaveChannelRequest request, ClientMessageInfo info) throws JsonApiException
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
		Channel channel = (Channel)source;
		
		Resident alias = info.UserPresence.getAlias(request.alias);
		if(alias == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.alias);
		}
		
		channel.removeResident(alias, request.final_values);

		return new EmptyClientReply();
	}
	
	public static ChannelInfo ListenToChannel(ListenToResidentRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		}
		Channel channel = (Channel)source;
		
		channel.addListener(info.UserPresence);
		
		return channel.getChannelInfo(0);
	}
	
	public static EmptyClientReply StopListenToChannel(StopListenToResidentRequest request, ClientMessageInfo info) throws JsonApiException
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
		Channel channel = (Channel)source;
		
		channel.removeListener(info.UserPresence);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply SendClientMessage(TransientMessageRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(request.message_type == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "message_type");
		if(request.value == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "value");
		
		Resident alias = info.UserPresence.getAlias(request.alias);
		if(alias == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.alias);
		}
		
		TransientClientMessage message = new TransientClientMessage();
		message.message_type = request.message_type;
		message.value = request.value;
		
		if(request.to != null)
		{
			BaseResident listener = ResidentManager.getResident(request.to);
			if(listener == null)
			{
				throw new JsonApiException(ApiErrors.NOT_FOUND, request.to);
			}
			
			listener.sendMessageFrom("clientMessage", message, alias);
		}
		else
		{
			alias.sendMessage("clientMessage", message);
		}
		
		return new EmptyClientReply();
	}
	
	public static CurrencyBalanceReply GetCurrencyBalance(CurrencyBalanceRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.currency_id == null || request.currency_id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "currency_id");
		
		CurrencyInfo currency = CurrencyInfoManager.getCurrencyInfo(request.currency_id);
		if(currency == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "currency not found");
		
		CurrencyBalanceReply reply = new CurrencyBalanceReply();
		reply.balance = CurrencyInfoManager.getCurrencyBalance(info.User, currency);
		
		return reply;
	}
	
	public static CurrencyBalancesReply GetCurrencyBalances(EmptyClientRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		CurrencyBalancesReply reply = new CurrencyBalancesReply();
		
		reply.balances = CurrencyInfoManager.getCurrencyBalances(info.User);
		
		return reply;
	}
	
	public static ProductInfoList GetProducts(GetProductsRequest request, ClientMessageInfo info) throws JsonApiException
	{
		String currencyCode = CurrencyUtil.getCurrency(info.User.Country);
		
		ProductInfoList reply = new ProductInfoList();
		
		Collection<Product> products = ProductManager.getProductList();
		
		reply.products = new ArrayList<ProductInfo>(products.size());
		
		String lang = info.User.getLanguage();
		Locale loc = info.User.getLocale();
		
		for(Product prod : products)
		{
			Integer price = prod.Price.get(currencyCode);
			if(price == null)
			{
				if(!CurrencyUtil.DefaultCurrency.equals(currencyCode))
				{
					currencyCode = CurrencyUtil.DefaultCurrency;
					price = prod.Price.get(currencyCode);
				}
				
				if(price == null)
					throw new JsonApiException(ApiErrors.NOT_FOUND, "Products didn't have prices in "+CurrencyUtil.DefaultCurrency);
			}
			
			Product.ProductText text = LocaleUtil.getLocalized(lang, prod.Text);
			if(text == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "Products didn't have text in "+lang);
			
			ProductInfo prodInfo = new ProductInfo();
			prodInfo.id = prod.ProductId;
			prodInfo.name = text.name;
			prodInfo.description = text.desc;
			prodInfo.image_url = text.image_url;
			prodInfo.price = price;
			prodInfo.display_price = CurrencyUtil.getDisplayPrice(loc, currencyCode, price);
			
			reply.products.add(prodInfo);
		}
		
		return reply;
	}
	
	public static ProductInfo GetProduct(GetProductRequest request, ClientMessageInfo info) throws JsonApiException
	{
		String currencyCode = CurrencyUtil.getCurrency(info.User.Country);
		
		if(request.product_id == null || request.product_id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "product_id");
		
		Product prod = ProductManager.getProduct(request.product_id);
		
		Integer price = prod.Price.get(currencyCode);
		if(price == null)
		{
			if(!CurrencyUtil.DefaultCurrency.equals(currencyCode))
			{
				currencyCode = CurrencyUtil.DefaultCurrency;
				price = prod.Price.get(currencyCode);
			}
			
			if(price == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "Products didn't have prices in "+CurrencyUtil.DefaultCurrency);
		}
		
		String lang = info.User.getLanguage();
		Product.ProductText text = LocaleUtil.getLocalized(lang, prod.Text);
		if(text == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "Products didn't have text in "+lang);
		
		ProductInfo prodInfo = new ProductInfo();
		prodInfo.id = prod.ProductId;
		prodInfo.name = text.name;
		prodInfo.description = text.desc;
		prodInfo.image_url = text.image_url;
		prodInfo.price = price;
		prodInfo.display_price = CurrencyUtil.getDisplayPrice(info.User.getLocale(), currencyCode, price);
		
		return prodInfo;
	}
	
	public static ProductPurchasedReply stripeCheckout(StripeCheckoutRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(request.product_id == null || request.product_id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "product_id");
		
		if(request.stripe_token == null || request.stripe_token.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "stripe_token");
		
		String currencyCode = CurrencyUtil.getCurrency(info.User.Country);
		
		Product prod = ProductManager.getProduct(request.product_id);
		if(prod == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, "product "+request.product_id+" not found");
		}
		
		if(!prod.Price.containsKey(currencyCode))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, "product doesn't have a price in currency "+currencyCode);
		}
		
		StripeInterface.makePurchase(info.User, prod, currencyCode, request.stripe_token);
		
		ProductPurchasedReply reply = new ProductPurchasedReply();
		
		reply.product_id = request.product_id;
		reply.price = prod.Price.get(currencyCode);
		reply.currencies = prod.Currencies;
		
		return reply;
	}
	
}
