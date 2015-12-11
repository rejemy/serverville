package com.dreamwing.serverville.agent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.HttpUtil.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.SVID;

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
	
	public UserInfoReply getUserInfo(Map<String,Object> request) throws JsonApiException, SQLException
	{
		String id = (String)request.getOrDefault("id", null);
		String username = (String)request.getOrDefault("username", null);
		
		return AgentShared.getUserInfo(id, username);
	}

	public double setDataKey(String id, String key, Object value) throws Exception
	{
		return setDataKey(id, key, value, null);
	}
	
	public double setDataKey(String id, String key, Object value, String data_type) throws Exception
	{
		if(id == null)
			throw new JsonApiException("Invalid id: "+id);
		
		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException("Invalid key name: "+key);
		
		JsonDataType valueType = JsonDataType.fromString(data_type);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, valueType, value);
		long updateTime = KeyDataManager.saveKey(id, item);
		
		return updateTime;
	}
	
	public double setDataKeys(String id, List<Map<String,Object>> items) throws JsonApiException, SQLException, Exception
	{

		if(id == null)
			throw new JsonApiException("Invalid id: "+id);
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(items.size());
		
		for(Map<String,Object> data : items)
		{
			String key = (String)data.getOrDefault("key", null);
			Object value = data.getOrDefault("value", null);
			JsonDataType valueType = JsonDataType.fromString((String)data.getOrDefault("data_type", null));
			
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException("Invalid key name: "+key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, valueType, value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(id, itemList);
		
		return updateTime;
	}
	
	public DataItemReply getDataKey(String id, String key) throws JsonApiException, SQLException
	{
		if(id == null || id.length() == 0)
			throw new JsonApiException("Missing id");

		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException("Invalid key name: "+key);
		
		KeyDataItem item = KeyDataManager.loadKey(id, key);
		if(item == null)
			throw new JsonApiException("Key not found");
		
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
		if(id == null || id.length() == 0)
			throw new JsonApiException("Missing id");
		
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
		if(id == null || id.length() == 0)
			throw new JsonApiException("Missing id");
		
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
	
	
}
