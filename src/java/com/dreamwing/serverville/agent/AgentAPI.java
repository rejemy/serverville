package com.dreamwing.serverville.agent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.dreamwing.serverville.agent.AgentMessages.*;
import com.dreamwing.serverville.client.ClientMessages.AllGlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeyRequest;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.SetDataReply;
import com.dreamwing.serverville.client.ClientMessages.UserDataReply;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.serialize.JsonDataDecoder;



public class AgentAPI
{
	private static AgentScriptAPI ApiInst = new AgentScriptAPI(null);
	
	public static UserInfoReply GetUserInfo(UserInfoRequest request) throws JsonApiException, SQLException
	{
		return AgentShared.getUserInfo(request.id, request.username);
	}
	
	public static SetDataReply SetDataKey(SetGlobalDataRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");

		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, request.key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(request.key, request.data_type, request.value);
		long updateTime = KeyDataManager.saveKey(request.id, item);
		
		reply.updated_at = updateTime;
		
		return reply;
	}
	
	public static SetDataReply SetDataKeys(SetGlobalDataListRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetGlobalDataItemRequest data : request.values)
		{
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			itemList.add(item);
		}
		
		long updateTime = KeyDataManager.saveKeys(request.id, itemList);
		
		reply.updated_at = updateTime;
		return reply;
	}
	
	
	
	public static DataItemReply GetDataKey(GlobalKeyRequest request) throws JsonApiException, SQLException
	{
		return ApiInst.getDataKey(request.id, request.key);
	}
	
	
	public static UserDataReply GetDataKeys(GlobalKeysRequest request) throws JsonApiException, SQLException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getDataKeys(request.id, request.keys, request.since, request.include_deleted);
		return reply;
	}
	
	public static UserDataReply GetAllDataKeys(AllGlobalKeysRequest request) throws JsonApiException, SQLException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getAllDataKeys(request.id, request.since, request.include_deleted);
		return reply;
	}
}
