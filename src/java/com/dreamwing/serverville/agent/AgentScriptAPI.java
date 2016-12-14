package com.dreamwing.serverville.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.CurrencyInfoManager;
import com.dreamwing.serverville.WritableDirectories;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.client.ClientMessages.ChannelInfo;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.client.ClientSessionManager;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyData;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataRecord;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.data.UserMessage;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.OnlineUser;
import com.dreamwing.serverville.residents.Resident;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.FileUtil;
import com.dreamwing.serverville.util.SVID;
import com.dreamwing.serverville.util.StringUtil;



public class AgentScriptAPI
{
	private static final Logger l = LogManager.getLogger(AgentScriptAPI.class);
	
	// Can be null for the case of web API
	private ScriptEngineContext Context;
	
	public AgentScriptAPI(ScriptEngineContext context)
	{
		Context = context;
	}
	
	public void log_debug(String msg)
	{
		l.debug(msg);
	}
	
	public void log_info(String msg)
	{
		l.info(msg);
	}
	
	public void log_warning(String msg)
	{
		l.warn(msg);
	}
	
	public void log_error(String msg)
	{
		l.error(msg);
	}
	
	public String makeSVID()
	{
		return SVID.makeSVID();
	}
	
	public double time()
	{
		return System.currentTimeMillis();
	}
	
	public UserInfoReply getUserInfo(Map<String,Object> request) throws JsonApiException, SQLException
	{
		String id = (String)request.getOrDefault("id", null);
		String username = (String)request.getOrDefault("username", null);
		
		return AgentShared.getUserInfo(id, username);
	}

	public KeyDataRecord findKeyDataRecord(String id) throws SQLException
	{
		return DatabaseManager.KeyDataRecordDao.queryForId(id);
	}
	
	public KeyDataRecord findOrCreateKeyDataRecord(String id, String type, String owner, String parent) throws SQLException
	{
		return KeyData.findOrCreate(id, type, owner, parent).GetDBRecord();
	}
	
	public void setKeyDataVersion(String id, int version) throws SQLException, JsonApiException
	{
		KeyData keyData = KeyData.find(id);
		keyData.setVersion(version);
	}
	
	public void deleteKeyData(String id) throws SQLException
	{
		KeyDataRecord.delete(id);
	}
	
	public double setDataKey(String id, String key, Object value) throws Exception
	{
		return setDataKey(id, key, value, null);
	}
	
	public double setDataKey(String id, String key, Object value, Object data_type) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		
		JsonDataType valueType = JsonDataType.fromObject(data_type);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, valueType, value, Context);
		long updateTime = KeyDataManager.saveKey(id, item);
		
		return updateTime;
	}
	
	public double setDataKeys(String id, List<Map<String,Object>> items) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(items.size());
		
		for(Map<String,Object> data : items)
		{
			String key = (String)data.getOrDefault("key", null);
			Object value = data.getOrDefault("value", null);
			JsonDataType valueType = JsonDataType.fromObject(data.getOrDefault("data_type", null));
			
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, valueType, value, Context);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(id, itemList);
		
		return updateTime;
	}
	
	public DataItemReply getDataKey(String id, String key) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		
		KeyDataItem item = KeyDataManager.loadKey(id, key);
		if(item == null)
			return null;
		
		return AgentShared.KeyDataItemToDataItemReply(id, item, Context);
	}
	
	public Map<String,DataItemReply> getDataKeys(String id, List<String> keys) throws JsonApiException, SQLException
	{
		return getDataKeys(id, keys, 0.0, false);
	}
	
	public Map<String,DataItemReply> getDataKeys(String id, List<String> keys, double since) throws JsonApiException, SQLException
	{
		return getDataKeys(id, keys, since, false);
	}
	
	public Map<String,DataItemReply> getDataKeys(String id, List<String> keys, double since, boolean includeDeleted) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		for(String key : keys)
		{
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		}
		
		List<KeyDataItem> items = KeyDataManager.loadKeysSince(id, keys, (long)since, includeDeleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		Map<String,DataItemReply> reply = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			DataItemReply data = AgentShared.KeyDataItemToDataItemReply(id, item, Context);
			reply.put(data.key, data);
		}
		
		return reply;
	}
	
	public Map<String,DataItemReply> getAllDataKeys(String id) throws JsonApiException, SQLException
	{
		return getAllDataKeys(id, 0.0, false);
	}
	
	public Map<String,DataItemReply> getAllDataKeys(String id, double since) throws JsonApiException, SQLException
	{
		return getAllDataKeys(id, since, false);
	}
	
	public Map<String,DataItemReply> getAllDataKeys(String id, double since, boolean includeDeleted) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(id, (long)since, includeDeleted);
		if(items == null)
			items = new ArrayList<KeyDataItem>();
		
		Map<String,DataItemReply> reply = new HashMap<String,DataItemReply>();
		
		for(KeyDataItem item : items)
		{
			DataItemReply data = AgentShared.KeyDataItemToDataItemReply(id, item, Context);
			reply.put(data.key, data);
		}
		
		return reply;
	}

	public double deleteDataKey(String id, String key) throws SQLException, JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		
		return KeyDataManager.deleteKey(id, key);
	}
	
	public double deleteAllDataKeys(String id) throws SQLException, JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		return KeyDataManager.deleteAllKeys(id);
	}

	public String createChannel(String id) throws JsonApiException
	{
		return createChannel(id, null, null);
	}
	
	public String createChannel(String id, String residentType) throws JsonApiException
	{
		return createChannel(id, residentType, null);
	}
	
	public String createChannel(String id, String residentType, Map<String,Object> values) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			id = SVID.makeSVID();
		
		BaseResident res = ResidentManager.getResident(id);
		if(res != null)
		{
			if(res instanceof Channel)
			{
				return res.getId();
			}
			
			throw new JsonApiException(ApiErrors.CHANNEL_ID_TAKEN, id);
		}
		
		Channel chan = new Channel(id, residentType);
		
		if(values != null)
			chan.setTransientValues(values, true);
		
		ResidentManager.addResident(chan);
		
		return chan.getId();
	}
	
	public void deleteChannel(String id) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, id);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null || !(res instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		}
		
		res.destroy();
	}

	public String createResident(String id, String residentType) throws JsonApiException
	{
		return createResident(id, residentType, null, null);
	}
	
	public String createResident(String id, String residentType, String owner) throws JsonApiException
	{
		return createResident(id, residentType, owner, null);
	}
	
	public String createResident(String id, String residentType, String owner, Map<String,Object> values) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(residentType))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, residentType);
		
		if(id == null)
			id = SVID.makeSVID();
		
		if(StringUtil.isNullOrEmpty(owner))
		{
			Resident res = new Resident(id, null, residentType);
			
			if(values != null)
				res.setTransientValues(values, true);
			
			ResidentManager.addResident(res);
		}
		else
		{
			ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(owner);
			if(client == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
			
			OnlineUser user = client.getPresence();
			if(user == null)
				throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
			
			Resident res = user.getOrCreateOwnedResident(id, residentType);
			
			if(values != null)
				res.setTransientValues(values, true);
		}
		
		
		return id;
	}
	
	public void deleteResident(String id) throws JsonApiException
	{
		deleteResident(id, null);
	}
	
	public void deleteResident(String id, Map<String,Object> finalValues) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, id);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null || !(res instanceof Resident))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		}
		Resident resident = (Resident)res;
		
		if(resident.getOwnerId() != null)
		{
			ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(resident.getOwnerId());
			if(client == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
			
			OnlineUser user = client.getPresence();
			if(user == null)
				throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
			
			user.deleteResident(id, finalValues);
		}
		else
		{
			resident.removeFromAllChannels(finalValues);
			resident.destroy();
		}
		
	}
	
	public void removeResidentFromAllChannels(String id) throws JsonApiException
	{
		removeResidentFromAllChannels(id, null);
	}
	
	public void removeResidentFromAllChannels(String id, Map<String,Object> finalValues) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, id);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null || !(res instanceof Resident))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		}
		Resident resident = (Resident)res;
		
		resident.removeFromAllChannels(finalValues);
	}
	
	// TODO: handle residents and owners on different nodes
	public void setResidentOwner(String id, String owner) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, id);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null || !(res instanceof Resident))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		}
		Resident resident = (Resident)res;
		
		OnlineUser newOwner = null;
		if(owner != null)
		{
			ClientConnectionHandler newOwnerClient = ClientSessionManager.getSessionByUserId(owner);
			if(newOwnerClient == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "new owner not found");
			
			newOwner = newOwnerClient.getPresence();
			if(newOwner == null)
				throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "new owner not online");
		
		}
		
		if(resident.getOwnerId() != null)
		{
			ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(resident.getOwnerId());
			if(client == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
			
			OnlineUser user = client.getPresence();
			if(user == null)
				throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
			
			user.removeResident(id);
			
			resident.setOwnerId(null);
		}
		
		if(newOwner != null)
		{
			newOwner.addResident(resident);
		}
		
	}
	
	public void addResidentToChannel(String channelId, String residentId) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		if(StringUtil.isNullOrEmpty(residentId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, residentId);
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		Channel channel = (Channel)source;
		
		BaseResident listener = ResidentManager.getResident(residentId);
		if(listener == null || !(listener instanceof Resident))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, residentId);
		}
		Resident resident = (Resident)listener;
		
		channel.addResident(resident);
	}
	
	public void removeResidentFromChannel(String channelId, String residentId, Map<String,Object> finalValues) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		if(StringUtil.isNullOrEmpty(residentId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, residentId);
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		Channel channel = (Channel)source;
		
		BaseResident listener = ResidentManager.getResident(residentId);
		if(listener == null || !(listener instanceof Resident))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, residentId);
		}
		Resident resident = (Resident)listener;
		
		channel.removeResident(resident, finalValues);

		if(finalValues != null)
			resident.setTransientValues(finalValues);
	}
	
	
	public void addChannelListener(String channelId, String userId) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(userId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, userId);
		
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		
		ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(userId);
		if(client == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		OnlineUser user = client.getPresence();
		if(user == null)
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		
		Channel channel = (Channel)source;
		
		channel.addListener(user);
	}
	
	public void removeChannelListener(String channelId, String userId) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(userId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, userId);
		
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		
		ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(userId);
		if(client == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		OnlineUser user = client.getPresence();
		if(user == null)
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		
		Channel channel = (Channel)source;
		
		channel.removeListener(user);
	}
	
	public ChannelInfo userJoinChannel(String userId, String channelId, String residentId, Map<String,Object> values) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(userId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, userId);
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		if(StringUtil.isNullOrEmpty(residentId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, residentId);
		
		
		ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(userId);
		if(client == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		OnlineUser user = client.getPresence();
		if(user == null)
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		Channel channel = (Channel)source;
		
		Resident userRes = user.getOwnedResident(residentId);
		if(userRes == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, residentId);
		}
		
		if(values != null)
		{
			userRes.setTransientValues(values, true);
		}
		
		channel.addResident(userRes);
		channel.addListener(user);
		

		return channel.getChannelInfo(0);
	}
	
	public void userLeaveChannel(String userId, String channelId, String residentId, Map<String,Object> finalValues) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(userId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, userId);
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		if(StringUtil.isNullOrEmpty(residentId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, residentId);
		
		BaseResident listener = ResidentManager.getResident(userId);
		if(listener == null)
		{
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, userId);
		}
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		Channel channel = (Channel)source;
		
		ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(userId);
		if(client == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		OnlineUser user = client.getPresence();
		if(user == null)
			throw new JsonApiException(ApiErrors.USER_NOT_PRESENT, "user not online");
		
		Resident userRes = user.getOwnedResident(residentId);
		if(userRes == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, residentId);
		}
		
		channel.removeResident(userRes, finalValues);
		channel.removeListener(user);
	}
	
	public ChannelInfo getChannelInfo(String channelId, double since) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(channelId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, channelId);
		
		BaseResident source = ResidentManager.getResident(channelId);
		if(source == null || !(source instanceof Channel))
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, channelId);
		}
		Channel channel = (Channel)source;
		
		return channel.getChannelInfo((long)since);
	}
	
	public void setTransientValue(String id, String key, Object value) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		res.setTransientValue(key, value);
	
	}
	
	public void setTransientValues(String id, Map<String,Object> items) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		for(String keyname : items.keySet())
		{
			if(!KeyDataItem.isValidKeyname(keyname))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, keyname);
		}
		
		res.setTransientValues(items);
	}
	

	public Object getTransientValue(String id, String key) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		TransientDataItem item = res.getTransientValue(key);
		if(item == null)
			return null;
		
		return item.value;
	}
	

	public Map<String,Object> getTransientValues(String id, List<String> keys) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		Map<String,Object> reply = new HashMap<String,Object>();
		
		for(String key : keys)
		{
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);
			
			TransientDataItem item = res.getTransientValue(key);
			if(item != null)
			{
				reply.put(item.key, item.value);
			}
		}
		
		return reply;
	}
	
	public Map<String,Object> getAllTransientValues(String id) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		Map<String,Object> reply = new HashMap<String,Object>();
		
		for(TransientDataItem item : res.getAllTransientValues())
		{
			if(item.deleted)
				continue;
			reply.put(item.key, item.value);
		}
		
		return reply;
	}
	
	public void deleteTransientValue(String id, String key) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		res.deleteTransientValue(key);
		
	}
	
	public void deleteTransientValues(String id, List<String> keys) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		res.deleteTransientValues(keys);
	}
	
	public void deleteAllTransientValues(String id) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, id);
		
		res.deleteAllTransientValues();
	}
	
	public void triggerResidentEvent(String residentId, String eventType, String event) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(residentId))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "residentId");
		
		if(StringUtil.isNullOrEmpty(eventType))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "eventType");
		
		BaseResident res = ResidentManager.getResident(residentId);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, residentId);
		
		res.triggerEvent(eventType, event);
	}
	
	public void sendUserMessage(String to, String from, boolean fromUser, boolean guaranteed, String messageType, String message) throws JsonApiException, SQLException
	{
		if(StringUtil.isNullOrEmpty(to))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "to");
		
		if(StringUtil.isNullOrEmpty(from))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "from");
		
		if(StringUtil.isNullOrEmpty(messageType))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "messageType");
		
		if(ServervilleUser.findById(to) == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, to);
		
		if(fromUser)
		{
			if(ServervilleUser.findById(from) == null)
				throw new JsonApiException(ApiErrors.NOT_FOUND, from);
		}
		
		UserMessage msg = new UserMessage();
		msg.ToUser = to;
		msg.From = from;
		msg.FromUser = fromUser;
		msg.MessageType = messageType;
		msg.Content = message;
		
		UserMessage.deliverUserMessage(msg, guaranteed);
	}
	
	
	public int getCurrencyBalance(String userid, String currencyId) throws JsonApiException, SQLException
	{
		if(userid == null || userid.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "userid");
		
		if(currencyId == null || currencyId.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "currencyid");
		
		ServervilleUser user = ServervilleUser.findById(userid);
		if(user == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		CurrencyInfo currency = CurrencyInfoManager.getCurrencyInfo(currencyId);
		if(currency == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "currency not found");
		
		return CurrencyInfoManager.getCurrencyBalance(user, currency);
	}
	
	public Map<String,Integer> getCurrencyBalances(String userid) throws JsonApiException, SQLException
	{
		if(userid == null || userid.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "userid");
		
		ServervilleUser user = ServervilleUser.findById(userid);
		if(user == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		return CurrencyInfoManager.getCurrencyBalances(user);
	}
	
	public int addCurrency(String userid, String currencyId, int amount, String reason) throws JsonApiException, SQLException
	{
		if(userid == null || userid.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "userid");
		
		if(currencyId == null || currencyId.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "currencyid");
		
		if(reason == null || reason.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "reason");
		
		if(amount <= 0)
		{
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "can only add positive values to currency balances");
		}
		
		ServervilleUser user = ServervilleUser.findById(userid);
		if(user == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		CurrencyInfo currency = CurrencyInfoManager.getCurrencyInfo(currencyId);
		if(currency == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "currency not found");
		
		return CurrencyInfoManager.changeCurrencyBalance(user, currency, amount, reason);
	}
	
	public int subtractCurrency(String userid, String currencyId, int amount, String reason) throws JsonApiException, SQLException
	{
		if(userid == null || userid.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "userid");
		
		if(currencyId == null || currencyId.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "currencyid");
		
		if(reason == null || reason.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "reason");
		
		if(amount <= 0)
		{
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "can only subtract positive values from currency balances");
		}
		
		ServervilleUser user = ServervilleUser.findById(userid);
		if(user == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "user not found");
		
		CurrencyInfo currency = CurrencyInfoManager.getCurrencyInfo(currencyId);
		if(currency == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "currency not found");
		
		return CurrencyInfoManager.changeCurrencyBalance(user, currency, -amount, reason);
	}
	
	public byte[] base64decode(String data)
	{
		if(data == null)
			return null;
		
		byte[] bytes = Base64.getDecoder().decode(data);
		
		return bytes;
	}
	
	public String base64encode(byte[] data)
	{
		if(data == null)
			return null;
		
		return Base64.getEncoder().encodeToString(data);
	}
	
	File getWritableFile(String location, String filename) throws Exception
	{
		if(filename == null || filename.length() == 0)
			throw new Exception("Must provide a filename");
		
		Path f = WritableDirectories.getDirectory(location);
		if(f == null)
			throw new Exception("Writable file location "+location+" not found");
		
		if(filename.startsWith("/"))
			filename = filename.substring(1);
		
		Path p = f.resolve(filename).normalize();
		if(!p.startsWith(f))
			throw new Exception("filename is outside of the writable directory");
		
		return p.toFile();
	}
	
	public void writeFile(String location, String filename, String contents) throws Exception
	{
		File f = getWritableFile(location, filename);
		
		FileUtil.writeStringToFile(f, contents, StandardCharsets.UTF_8);
	}
	
	public void writeFile(String location, String filename, byte[] contents) throws Exception
	{
		File f = getWritableFile(location, filename);
		
		FileOutputStream writer = new FileOutputStream(f);
		writer.write(contents);
		writer.close();
	}
	
	public String readTextFile(String location, String filename) throws Exception
	{
		File f = getWritableFile(location, filename);
		
		if(!f.exists() || !f.canRead())
			throw new Exception("Can't read file "+filename);
		
		String data = FileUtil.readFileToString(f, StandardCharsets.UTF_8);
		return data;
	}
	
	public byte[] readBinaryFile(String location, String filename) throws Exception
	{
		File f = getWritableFile(location, filename);
		
		if(!f.exists() || !f.canRead())
			throw new Exception("Can't read file "+filename);
		
		byte[] data = new byte[(int)f.length()];
		
		FileInputStream reader = new FileInputStream(f);
		reader.read(data);
		reader.close();
		
		return null;
	}
}
