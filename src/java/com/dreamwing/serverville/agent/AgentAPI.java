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
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.ResidentManager;
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
	
	public static SetDataReply DeleteAllDataKeys(DeleteGlobalKeyDataRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		reply.updated_at = ApiInst.deleteDataKey(request.id, request.key);
		return reply;
	}
	
	public static SetDataReply DeleteAllDataKeys(DeleteGlobalDataAllRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		reply.updated_at = ApiInst.deleteAllDataKeys(request.id);
		return reply;
	}
	
	public static CreateChannelReply CreateChannel(CreateChannelRequest request) throws JsonApiException
	{
		CreateChannelReply reply = new CreateChannelReply();
		reply.id = ApiInst.createChannel(request.id);
		return reply;
	}
	
	public static EmptyReply DeleteChannel(DeleteChannelRequest request) throws JsonApiException
	{
		ApiInst.deleteChannel(request.id);
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	
	public static EmptyReply AddListener(ListenerRequest request) throws JsonApiException
	{
		ApiInst.addListener(request.source, request.listener, request.two_way);
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	
	public static EmptyReply RemoveListener(EndListenerRequest request) throws JsonApiException
	{
		ApiInst.removeListener(request.source, request.listener);
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	

	public static EmptyReply SetTransientValue(SetGlobalDataRequest request) throws JsonApiException
	{
		ApiInst.setTransientValue(request.id, request.key, request.value, request.data_type.value());
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	
	public static EmptyReply SetTransientValues(SetGlobalDataListRequest request) throws JsonApiException
	{
		EmptyReply reply = new EmptyReply();
		
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(request.id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		
		List<KeyDataItem> stateValues = new ArrayList<KeyDataItem>(request.values.size());
		
		for(SetGlobalDataItemRequest data : request.values)
		{
			if(!KeyDataItem.isValidKeyname(data.key))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, data.key);
			
			KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(data.key, data.data_type, data.value);
			stateValues.add(item);
		}
		
		res.setTransientValues(stateValues);
		
		return reply;
	}
	
	public static DataItemReply GetTransientValue(GetTransientValueRequest request) throws JsonApiException
	{
		return ApiInst.getTransientValue(request.id, request.key);
	}
	

	public static UserDataReply GetTransientValues(GetTransientValuesRequest request) throws JsonApiException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getTransientValues(request.id, request.keys);
		return reply;
	}
	
	public static UserDataReply getAllTransientValues(GetAllTransientValuesRequest request) throws JsonApiException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getAllTransientValues(request.id);
		return reply;
	}
}
