package com.dreamwing.serverville.agent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.dreamwing.serverville.agent.AgentMessages.*;
import com.dreamwing.serverville.client.ClientMessages.AllGlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeyRequest;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.SetDataReply;
import com.dreamwing.serverville.client.ClientMessages.UserDataReply;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.HttpUtil.JsonApiException;
import com.dreamwing.serverville.serialize.JsonDataDecoder;



public class AgentAPI
{
	
	private static AgentScriptAPI ApiInst = new AgentScriptAPI();
	
	public static UserInfoReply GetUserInfo(UserInfoRequest request) throws JsonApiException, SQLException, Exception
	{
		UserInfoReply info = ApiInst.GetUserInfo(request);
		if(info == null)
			throw new JsonApiException("User not found");

		
		return info;
	}
	
	public static SetDataReply SetDataKey(SetGlobalDataRequest request) throws JsonApiException, SQLException, Exception
	{
		SetDataReply reply = new SetDataReply();
		
		if(request.id == null)
			throw new JsonApiException("Invalid id: "+request.id);
		
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(request.key, request.data_type, request.value);
		long updateTime = KeyDataManager.saveKey(request.id, item);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	public static SetDataReply SetDataKeys(SetGlobalDataListRequest request) throws JsonApiException, SQLException, Exception
	{
		SetDataReply reply = new SetDataReply();
		
		if(request.id == null)
			throw new JsonApiException("Invalid id: "+request.id);
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetGlobalDataItemRequest data : request.values)
		{
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException("Invalid key name: "+data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(request.id, itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	private static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item)
	{
		DataItemReply data = new DataItemReply();
		data.id = id;
		data.key = item.key;
		data.value = item.asObject();
		data.data_type = JsonDataType.fromKeyDataType(item.datatype);
		data.created = item.created;
		data.modified = item.modified;
		return data;
	}
	
	public static DataItemReply GetDataKey(GlobalKeyRequest request) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");

		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		KeyDataItem item = KeyDataManager.loadKey(request.id, request.key);
		if(item == null)
			throw new JsonApiException("Key not found");
		
		return KeyDataItemToDataItemReply(request.id, item);
	}
	
	
	public static UserDataReply GetDataKeys(GlobalKeysRequest request) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");
		
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
	
	public static UserDataReply GetAllDataKeys(AllGlobalKeysRequest request) throws JsonApiException, SQLException
	{
		if(request.id == null || request.id.length() == 0)
			throw new JsonApiException("Missing id");
		
		List<KeyDataItem> items = KeyDataManager.loadAllKeysSince(request.id, (long)request.since);
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
}
