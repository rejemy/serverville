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
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.InviteCode;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataRecord;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.PropertyPermissions;
import com.dreamwing.serverville.data.RecordPermissionsManager;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.data.UserMessage;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.Resident;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.residents.World;
import com.dreamwing.serverville.residents.Zone;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.stripe.StripeInterface;
import com.dreamwing.serverville.util.CurrencyUtil;
import com.dreamwing.serverville.util.LocaleUtil;
import com.dreamwing.serverville.util.PasswordUtil;
import com.dreamwing.serverville.util.SVID;
import com.dreamwing.serverville.util.StringUtil;


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
		reply.session_id = info.ConnectionHandler.getSession().getId();
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
		reply.session_id = info.ConnectionHandler.getSession().getId();
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
		reply.session_id = info.ConnectionHandler.getSession().getId();
		reply.admin_level = user.AdminLevel;
		reply.country = user.Country;
		reply.language = user.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static SignInReply CreateAccount(CreateAccount request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.username) || StringUtil.isNullOrEmpty(request.email))
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
		reply.session_id = info.ConnectionHandler.getSession().getId();
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
		
		if(StringUtil.isNullOrEmpty(request.username) || StringUtil.isNullOrEmpty(request.email))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must set a username and email to create an account");
		
		if(!PasswordUtil.validatePassword(request.password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
		
		info.User.register(request.password, request.username, request.email);
		
		SignInReply reply = new SignInReply();
		
		reply.user_id = info.User.getId();
		reply.username = info.User.getUsername();
		reply.email = info.User.getEmail();
		reply.session_id = info.ConnectionHandler.getSession().getId();
		reply.admin_level = info.User.AdminLevel;
		reply.country = info.User.Country;
		reply.language = info.User.Language;
		
		reply.time = System.currentTimeMillis();
		
		return reply;
	}
	
	public static ChangePasswordReply ChangePassword(ChangePasswordRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(info.User.isAnonymous())
		{
			throw new JsonApiException(ApiErrors.ANON_NOT_ALLOWED, "Can't change a password for an anonymous user");
		}
		
		if(!PasswordUtil.validatePassword(request.new_password))
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid password");
	
		if(!info.User.checkPassword(request.old_password))
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Password does not match");
		
		
		info.User.setPassword(request.new_password);
		UserSession.deleteAllUserSessions(info.User.getId());
		
		UserSession session = UserSession.startNewSession(info.User.getId());
		info.ConnectionHandler.switchSession(session);
		
		ChangePasswordReply reply = new ChangePasswordReply();
		reply.session_id = info.ConnectionHandler.getSession().getId();
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
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
		reply.session_id = info.ConnectionHandler.getSession().getId();
		
		return reply;
	}
	
	public static EmptyClientReply SetLocale(SetLocaleRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		info.User.setLocale(request.country, request.language);
		
		return new EmptyClientReply();
	}
	
	public static GetUserDataComboReply GetUserDataCombo(GetUserDataComboRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(info.User.getId(), (long)request.since);
		if(items == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		GetUserDataComboReply reply = new GetUserDataComboReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			if(!RecordPermissionsManager.UserPermissions.isOwnerReadable(item.key))
				continue;
			
			DataItemReply data = KeyDataItemToDataItemReply(info.User.getId(), item);
			reply.values.put(data.key, data);
		}
		
		reply.balances = CurrencyInfoManager.getCurrencyBalances(info.User);
		
		return reply;
	}
	
	public static SetDataReply SetUserKey(SetUserDataRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		if(!RecordPermissionsManager.UserPermissions.isOwnerWritable(request.key))
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		
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
			if(!RecordPermissionsManager.UserPermissions.isOwnerWritable(data.key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(info.User.getId(), itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	public static SetDataReply SetAndDeleteUserKeys(UserDataSetAndDeleteRequestList request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		long updateTime = System.currentTimeMillis();
		
		if(request.values != null)
		{
			List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
			
			for(SetUserDataRequest data : request.values)
			{
				if(!KeyDataItem.isValidKeyname(data.key))
					throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
				if(!RecordPermissionsManager.UserPermissions.isOwnerWritable(data.key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, data.key);
				
				KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
				itemList.add(item);
			}

			KeyDataManager.saveKeys(info.User.getId(), itemList, updateTime);
		}
		
		if(request.delete_keys != null)
		{
			for(String key : request.delete_keys)
			{
				if(!KeyDataItem.isValidKeyname(key))
					throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
				if(!RecordPermissionsManager.UserPermissions.isOwnerWritable(key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, key);
			}
			
			KeyDataManager.deleteKeys(info.User.getId(), request.delete_keys, updateTime);
		}
		
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
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		if(!RecordPermissionsManager.UserPermissions.isOwnerReadable(request.key))
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		
		KeyDataItem item = KeyDataManager.loadKey(info.User.getId(), request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		return KeyDataItemToDataItemReply(info.User.getId(), item);
	}
	
	public static UserDataReply GetUserKeys(KeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		for(String key : request.keys)
		{
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
			if(!RecordPermissionsManager.UserPermissions.isOwnerReadable(key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, key);
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
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(info.User.getId(), (long)request.since);
		if(items == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			if(!RecordPermissionsManager.UserPermissions.isOwnerReadable(item.key))
				continue;
			
			DataItemReply data = KeyDataItemToDataItemReply(info.User.getId(), item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	public static SetDataReply DeleteUserKey(DeleteKeyRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		long updatedAt = KeyDataManager.deleteKey(info.User.getId(), request.key);
		
		SetDataReply reply = new SetDataReply();
		reply.updated_at = updatedAt;
		return reply;
	}
	
	public static SetDataReply DeleteUserKeys(DeleteKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		for(String key : request.keys)
		{
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		}
		
		long updatedAt = KeyDataManager.deleteKeys(info.User.getId(), request.keys);
		
		SetDataReply reply = new SetDataReply();
		reply.updated_at = updatedAt;
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static DataItemReply GetDataKey(GlobalKeyRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		String userId = info.User != null ? info.User.getId() : null;
		
		if(request.id.equals(userId))
		{
			if(!RecordPermissionsManager.UserPermissions.isOwnerReadable(request.key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		}
		else
		{
			// If it's not our data, load the keydata record to see if we can get it
			KeyDataRecord record = KeyDataRecord.load(request.id);
			PropertyPermissions perms = RecordPermissionsManager.getPermissions(record);
			if(record != null && record.Owner.equals(userId))
			{
				if(!perms.isOwnerReadable(request.key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
			}
			else
			{
				if(!perms.isGlobalReadable(request.key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
			}
		}
		
		
		KeyDataItem item = KeyDataManager.loadKey(request.id, request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		return KeyDataItemToDataItemReply(request.id, item);
	}
	
	@ClientHandlerOptions(auth=false)
	public static UserDataReply GetDataKeys(GlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		String userId = info.User != null ? info.User.getId() : null;
		
		PropertyPermissions perms = RecordPermissionsManager.UserPermissions;
		boolean isMe = request.id.equals(userId);
		if(!isMe)
		{
			KeyDataRecord record = KeyDataRecord.load(request.id);
			if(record != null && record.Owner.equals(userId))
				isMe = true;
			perms = RecordPermissionsManager.getPermissions(record);
		}
		
		for(String keyname : request.keys)
		{
			if(!KeyDataItem.isValidKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
			
			if(isMe && !perms.isOwnerReadable(keyname))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, keyname);
			else if(!isMe && !perms.isGlobalReadable(keyname))
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
	
	@ClientHandlerOptions(auth=false)
	public static UserDataReply GetDataKeysStartingWith(KeysStartingWithRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		if(StringUtil.isNullOrEmpty(request.prefix))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "prefix");
		
		List<KeyDataItem> items = KeyDataManager.loadKeysStartingWithSince(request.id, request.prefix, (long)request.since, request.include_deleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		String userId = info.User != null ? info.User.getId() : null;
		
		PropertyPermissions perms = RecordPermissionsManager.UserPermissions;
		boolean isMe = request.id.equals(userId);
		if(!isMe)
		{
			KeyDataRecord record = KeyDataRecord.load(request.id);
			if(record != null && record.Owner.equals(userId))
				isMe = true;
			perms = RecordPermissionsManager.getPermissions(record);
		}
		
		
		for(KeyDataItem item : items)
		{
			if(isMe && !perms.isOwnerReadable(item.key))
				continue;
			else if(!isMe && !perms.isGlobalReadable(item.key))
				continue;
			
			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static UserDataReply GetAllDataKeys(AllGlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(request.id, (long)request.since, request.include_deleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		UserDataReply reply = new UserDataReply();
		
		reply.values = new HashMap<String,DataItemReply>();
		
		String userId = info.User != null ? info.User.getId() : null;
		
		PropertyPermissions perms = RecordPermissionsManager.UserPermissions;
		boolean isMe = request.id.equals(userId);
		if(!isMe)
		{
			KeyDataRecord record = KeyDataRecord.load(request.id);
			if(record != null && record.Owner.equals(userId))
				isMe = true;
			perms = RecordPermissionsManager.getPermissions(record);
		}
		
		
		for(KeyDataItem item : items)
		{
			if(isMe && !perms.isOwnerReadable(item.key))
				continue;
			else if(!isMe && !perms.isGlobalReadable(item.key))
				continue;
			
			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.put(data.key, data);
		}
		
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static OrderedDataReply PageAllDataKeys(PageGlobalKeysRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> items = KeyDataManager.pageAllKeys(request.id, (int)request.page_size, request.start_after, request.descending);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		OrderedDataReply reply = new OrderedDataReply();
		
		reply.values = new ArrayList<DataItemReply>(items.size());
		
		String userId = info.User != null ? info.User.getId() : null;
		
		PropertyPermissions perms = RecordPermissionsManager.UserPermissions;
		boolean isMe = request.id.equals(userId);
		if(!isMe)
		{
			KeyDataRecord record = KeyDataRecord.load(request.id);
			if(record != null && record.Owner.equals(userId))
				isMe = true;
			perms = RecordPermissionsManager.getPermissions(record);
		}
		
		for(KeyDataItem item : items)
		{
			if(isMe && !perms.isOwnerReadable(item.key))
				continue;
			else if(!isMe && !perms.isGlobalReadable(item.key))
				continue;
			
			DataItemReply data = KeyDataItemToDataItemReply(request.id, item);
			reply.values.add(data);
		}
		
		return reply;
	}

	
	static KeyDataInfo keyDataRecordToInfo(KeyDataRecord record)
	{
		KeyDataInfo keyInfo = new KeyDataInfo();
		
		keyInfo.id = record.Id;
		keyInfo.record_type = record.Type;
		keyInfo.owner = record.Owner;
		keyInfo.parent = record.Parent;
		keyInfo.version = record.Version;
		keyInfo.created = record.Created.getTime();
		keyInfo.modified = record.Modified.getTime();
		
		return keyInfo;
	}
	
	@ClientHandlerOptions(auth=false)
	public static KeyDataInfo GetKeyDataRecord(KeyDataRecordRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		KeyDataRecord record = KeyDataRecord.load(request.id);
		if(record == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		KeyDataInfo keyInfo = keyDataRecordToInfo(record);

		return keyInfo;
	}
	
	public static KeyDataRecords GetKeyDataRecords(KeyDataRecordsRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		List<KeyDataRecord> records = KeyDataRecord.query(info.User.getId(), request.record_type, request.parent);
		
		KeyDataRecords reply = new KeyDataRecords();
	
		reply.records = new ArrayList<KeyDataInfo>(records.size());
		for(KeyDataRecord record : records)
		{
			reply.records.add(keyDataRecordToInfo(record));
		}
		
		return reply;
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
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			if(!RecordPermissionsManager.UserPermissions.isOwnerWritable(data.key))
				throw new JsonApiException(ApiErrors.FORBIDDEN, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(record.Id, itemList);
		
		
		SetDataReply reply = new SetDataReply();
		reply.updated_at = updateTime;
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static GetHostWithResidentReply GetHostWithResident(GetHostWithResidentRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		GetHostWithResidentReply reply = new GetHostWithResidentReply();
		reply.host = ClusterManager.getHostnameForResident(request.resident_id);
		return reply;
	}
	
	public static CreateResidentReply CreateResident(CreateResidentRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_type))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_type);
		
		String id = SVID.makeSVID();
		
		Resident resident = info.UserPresence.getOrCreateOwnedResident(id, request.resident_type);
		
		if(request.values != null)
		{
			resident.setTransientValues(request.values, true);
		}
		
		CreateResidentReply reply = new CreateResidentReply();
		reply.resident_id = id;
		
		return reply;
	}
	
	public static EmptyClientReply DeleteResident(DeleteResidentRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		info.UserPresence.deleteResident(request.resident_id, request.final_values);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply RemoveResidentFromAllChannels(RemoveResidentFromAllChannelsRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		resident.removeFromAllChannels(request.final_values);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply SetTransientValue(SetTransientValueRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);

		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		if(!perms.isOwnerWritable(request.key))
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		
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
		
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		for(String keyname : request.values.keySet())
		{
			if(!KeyDataItem.isValidKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
			if(!perms.isOwnerWritable(keyname))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, keyname);
		}
		
		resident.setTransientValues(request.values);
		
		EmptyClientReply reply = new EmptyClientReply();
		return reply;
	}
	

	public static EmptyClientReply DeleteTransientValue(DeleteTransientValueRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);

		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		if(!perms.isOwnerWritable(request.key))
			throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		
		resident.deleteTransientValue(request.key);
		
		EmptyClientReply reply = new EmptyClientReply();
		return reply;
	}
	
	public static EmptyClientReply DeleteTransientValues(DeleteTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		Resident resident = info.UserPresence.getOwnedResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		for(String keyname : request.values)
		{
			if(!KeyDataItem.isValidKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
			if(!perms.isOwnerWritable(keyname))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, keyname);
		}
		
		resident.deleteTransientValues(request.values);
		
		EmptyClientReply reply = new EmptyClientReply();
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static TransientDataItemReply GetTransientValue(GetTransientValueRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		BaseResident resident = ResidentManager.getResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		String userId = info.User != null ? info.User.getId() : null;
		
		if(userId != null && userId.equals(resident.getOwnerId()))
		{
			if(!perms.isOwnerReadable(request.key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		}
		else
		{
			if(!perms.isGlobalReadable(request.key))
				throw new JsonApiException(ApiErrors.PRIVATE_DATA, request.key);
		}
		
		TransientDataItem item = resident.getTransientValue(request.key);
		if(item == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		TransientDataItemReply reply = new TransientDataItemReply();
		reply.value = item.value;
		return reply;
	}
	
	@ClientHandlerOptions(auth=false)
	public static TransientDataItemsReply GetTransientValues(GetTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		BaseResident resident = ResidentManager.getResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		Map<String,Object> values = new HashMap<String,Object>();
		
		String userId = info.User != null ? info.User.getId() : null;
		
		boolean isMe = userId != null && userId.equals(resident.getOwnerId());
		
		for(String key : request.keys)
		{
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
			
			if(isMe)
			{
				if(!perms.isOwnerReadable(key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, key);
			}
			else
			{
				if(!perms.isGlobalReadable(key))
					throw new JsonApiException(ApiErrors.PRIVATE_DATA, key);
			}
			
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
	
	@ClientHandlerOptions(auth=false)
	public static TransientDataItemsReply GetAllTransientValues(GetAllTransientValuesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		BaseResident resident = ResidentManager.getResident(request.resident_id);
		if(resident == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		PropertyPermissions perms = resident.getPermissions();
		
		Map<String,Object> values = new HashMap<String,Object>();
		
		String userId = info.User != null ? info.User.getId() : null;
		
		boolean isMe = userId != null && userId.equals(resident.getOwnerId());
		
		for(TransientDataItem item : resident.getAllTransientValues())
		{
			if(isMe)
			{
				if(!perms.isOwnerReadable(item.key))
					continue;
			}
			else
			{
				if(!perms.isGlobalReadable(item.key))
					continue;
			}
			
			values.put(item.key, item);
		}
		
		TransientDataItemsReply reply = new TransientDataItemsReply();
		reply.values = values;
		return reply;
	}
	
	public static ChannelInfo JoinChannel(JoinChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		Resident res = info.UserPresence.getOwnedResident(request.resident_id);
		if(res == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		if(request.values != null)
		{
			res.setTransientValues(request.values, true);
		}
		
		channel.addResident(res);
		channel.addListener(info.UserPresence);
		

		return channel.getChannelInfo(0);
	}
	
	public static EmptyClientReply LeaveChannel(LeaveChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		Resident res = info.UserPresence.getOwnedResident(request.resident_id);
		if(res == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		channel.removeResident(res, request.final_values);
		channel.removeListener(info.UserPresence);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply AddResidentToChannel(JoinChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		Resident res = info.UserPresence.getOwnedResident(request.resident_id);
		if(res == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		if(request.values != null)
		{
			res.setTransientValues(request.values, true);
		}
		
		channel.addResident(res);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply RemoveResidentFromChannel(LeaveChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		Resident res = info.UserPresence.getOwnedResident(request.resident_id);
		if(res == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		channel.removeResident(res, request.final_values);

		return new EmptyClientReply();
	}
	
	public static ChannelInfo ListenToChannel(ListenToChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		channel.addListener(info.UserPresence);
		
		return channel.getChannelInfo(0);
	}
	
	public static EmptyClientReply StopListenToChannel(StopListenToChannelRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.channel_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.channel_id);
		
		BaseResident listener = ResidentManager.getResident(info.User.getId());
		if(listener == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.channel_id);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.channel_id);
		}
		Channel channel = (Channel)source;
		
		channel.removeListener(info.UserPresence);
		
		return new EmptyClientReply();
	}
	
	public static WorldZonesInfo UpdateWorldListeningZones(UpdateWorldListeningZonesRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.world_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.world_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		BaseResident source = ResidentManager.getResident(request.world_id);
		if(source == null || !(source instanceof World))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.world_id);
		}
		World world = (World)source;
		
		if(request.stop_listen_to != null)
		{
			for(String zoneId : request.stop_listen_to)
			{
				Zone zone = world.getZone(zoneId);
				if(zone != null)
					zone.removeListener(info.UserPresence);
			}
		}
		
		WorldZonesInfo reply = new WorldZonesInfo();
		reply.zones = new HashMap<String,ChannelInfo>();
		
		if(request.listen_to != null)
		{
			for(String zoneId : request.listen_to)
			{
				Zone zone = world.getOrCreateZone(zoneId);
				zone.addListener(info.UserPresence);
				
				reply.zones.put(zoneId, zone.getChannelInfo(0));
			}
		}
		
		
		return reply;
	}
	
	public static EmptyClientReply TriggerResidentEvent(TriggerResidentEventRequest request, ClientMessageInfo info) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		if(StringUtil.isNullOrEmpty(request.event_type))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, request.resident_id);
		
		if(info.UserPresence == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, info.User.getId());
		}
		
		Resident res = info.UserPresence.getOwnedResident(request.resident_id);
		if(res == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.resident_id);
		}
		
		res.triggerEvent(request.event_type, request.event_data);
		
		return new EmptyClientReply();
	}
	
	public static EmptyClientReply SendUserMessage(SendUserMessageRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.message_type))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "message_type");
		if(request.message == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "value");
		if(StringUtil.isNullOrEmpty(request.to))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "to");
		
		if(ServervilleUser.findById(request.to) == null)
			return new EmptyClientReply(); // Don't throw error on invalid user to help prevent scanning for valid user ids
		
		UserMessage msg = new UserMessage();
		msg.ToUser = request.to;
		msg.From = info.User.getId();
		msg.FromUser = true;
		msg.MessageType = request.message_type;
		msg.setContent(request.message);
		
		msg.deliver(request.guaranteed);
		
		return new EmptyClientReply();
	}
	
	public static UserMessageList GetPendingMessages(EmptyClientRequest request, ClientMessageInfo info) throws SQLException
	{
		List<UserMessage> messages = UserMessage.loadAllToUser(info.User.getId());
		
		UserMessageList list = new UserMessageList();
		list.messages = new ArrayList<UserMessageNotification>(messages.size());
		
		for(UserMessage msg : messages)
		{
			UserMessageNotification messageNotification = new UserMessageNotification();
			
			messageNotification.id = msg.MessageId;
			messageNotification.message_type = msg.MessageType;
			messageNotification.message = msg.getContent();
			messageNotification.from_id = msg.From;
			messageNotification.sender_is_user = msg.FromUser;
			
			list.messages.add(messageNotification);
		}
		
		return list;
	}
	
	public static EmptyClientReply ClearPendingMessage(ClearMessageRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		UserMessage msg = UserMessage.load(request.id);
		if(msg == null || !info.User.getId().equals(msg.ToUser))
			throw new JsonApiException(ApiErrors.NOT_FOUND, "message "+request.id+" not found");
		
		msg.delete();
		
		return new EmptyClientReply();
	}
	
	public static CurrencyBalanceReply GetCurrencyBalance(CurrencyBalanceRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.currency_id))
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
			prodInfo.currency = currencyCode;
			
			reply.products.add(prodInfo);
		}
		
		return reply;
	}
	
	public static ProductInfo GetProduct(GetProductRequest request, ClientMessageInfo info) throws JsonApiException
	{
		String currencyCode = CurrencyUtil.getCurrency(info.User.Country);
		
		if(StringUtil.isNullOrEmpty(request.product_id))
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
		prodInfo.currency = currencyCode;
		
		return prodInfo;
	}
	
	public static ProductPurchasedReply stripeCheckout(StripeCheckoutRequest request, ClientMessageInfo info) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(request.product_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "product_id");
		
		if(StringUtil.isNullOrEmpty(request.stripe_token))
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
	
	public static BatchRequestReply batchRequest(BatchRequest requestBatch, ClientMessageInfo info) throws JsonApiException
	{
		if(requestBatch.requests == null || requestBatch.requests.size() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "requests");
		
		BatchRequestReply reply = new BatchRequestReply();
		reply.replies = new ArrayList<Object>(requestBatch.requests.size());
		
		for(BatchRequestItem request : requestBatch.requests)
		{
			reply.replies.add(info.ConnectionHandler.dispatchJsonApi(request.api, request.request, info));
		}
		
		return reply;
	}
	
}
