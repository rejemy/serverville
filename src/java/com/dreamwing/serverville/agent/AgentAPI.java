package com.dreamwing.serverville.agent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.agent.AgentMessages.*;
import com.dreamwing.serverville.client.ClientMessages.AllGlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.ChannelInfo;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.client.ClientMessages.GetHostWithResidentReply;
import com.dreamwing.serverville.client.ClientMessages.GetHostWithResidentRequest;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeyRequest;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.KeysStartingWithRequest;
import com.dreamwing.serverville.client.ClientMessages.SetDataReply;
import com.dreamwing.serverville.client.ClientMessages.TransientDataItemReply;
import com.dreamwing.serverville.client.ClientMessages.TransientDataItemsReply;
import com.dreamwing.serverville.client.ClientMessages.UserDataReply;
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.StringUtil;



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
		
		if(StringUtil.isNullOrEmpty(request.id))
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
	
	public static UserDataReply GetDataKeysStartingWith(KeysStartingWithRequest request) throws JsonApiException, SQLException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getDataKeysStartingWith(request.id, request.prefix, request.since, request.include_deleted);
		return reply;
	}
	
	public static UserDataReply GetAllDataKeys(AllGlobalKeysRequest request) throws JsonApiException, SQLException
	{
		UserDataReply reply = new UserDataReply();
		reply.values = ApiInst.getAllDataKeys(request.id, request.since, request.include_deleted);
		return reply;
	}
	
	public static SetDataReply DeleteDataKey(DeleteGlobalKeyDataRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		reply.updated_at = ApiInst.deleteDataKey(request.id, request.key);
		return reply;
	}

	public static SetDataReply DeleteDataKeys(DeleteGlobalKeysDataRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		reply.updated_at = ApiInst.deleteDataKeys(request.id, request.keys);
		return reply;
	}
	
	
	public static SetDataReply DeleteAllDataKeys(DeleteGlobalDataAllRequest request) throws JsonApiException, SQLException
	{
		SetDataReply reply = new SetDataReply();
		reply.updated_at = ApiInst.deleteAllDataKeys(request.id);
		return reply;
	}
	
	public static GetHostWithResidentReply GetHostWithResident(GetHostWithResidentRequest request) throws JsonApiException
	{
		if(StringUtil.isNullOrEmpty(request.resident_id))
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "resident_id");
		
		GetHostWithResidentReply reply = new GetHostWithResidentReply();
		reply.host = ClusterManager.getHostnameForResident(request.resident_id);
		return reply;
	}
	
	public static CreateChannelReply CreateChannel(CreateChannelRequest request) throws JsonApiException
	{
		CreateChannelReply reply = new CreateChannelReply();
		reply.channel_id = ApiInst.createChannel(request.channel_id, request.resident_type, request.values);
		return reply;
	}
	
	public static EmptyReply DeleteChannel(DeleteChannelRequest request) throws JsonApiException
	{
		ApiInst.deleteChannel(request.channel_id);
		
		return new EmptyReply();
	}
	
	public static CreateResidentReply CreateResident(CreateResidentRequest request) throws JsonApiException
	{
		CreateResidentReply reply = new CreateResidentReply();
		reply.resident_id = ApiInst.createResident(request.resident_id, request.resident_type, request.owner, request.values);
		return reply;
	}
	
	public static EmptyReply DeleteResident(DeleteResidentRequest request) throws JsonApiException
	{
		ApiInst.deleteResident(request.resident_id, request.final_values);
		
		return new EmptyReply();
	}
	
	public static EmptyReply RemoveResidentFromAllChannels(RemoveResidentFromAllChannelsRequest request) throws JsonApiException
	{
		ApiInst.removeResidentFromAllChannels(request.resident_id, request.final_values);
		
		return new EmptyReply();
	}
	
	public static EmptyReply SetResidentOwner(SetResidentOwnerRequest request) throws JsonApiException
	{
		ApiInst.setResidentOwner(request.resident_id, request.user_id);
		
		return new EmptyReply();
	}
	
	public static EmptyReply AddResidentToChannel(AddResidentRequest request) throws JsonApiException
	{
		ApiInst.addResidentToChannel(request.channel_id, request.resident_id);
		
		return new EmptyReply();
	}
	
	public static EmptyReply RemoveResidentFromChannel(RemoveResidentRequest request) throws JsonApiException
	{
		ApiInst.removeResidentFromChannel(request.channel_id, request.resident_id, request.final_values);
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	
	public static EmptyReply AddChannelListener(AddListenerRequest request) throws JsonApiException
	{
		ApiInst.addChannelListener(request.channel_id, request.user_id);
		return new EmptyReply();
	}
	
	public static EmptyReply RemoveChannelListener(AddListenerRequest request) throws JsonApiException
	{
		ApiInst.removeChannelListener(request.channel_id, request.user_id);
		return new EmptyReply();
	}
	
	public static ChannelInfo UserJoinChannel(JoinChannelRequest request) throws JsonApiException
	{
		return ApiInst.userJoinChannel(request.user_id, request.channel_id, request.resident_id, request.values);
	}
	
	public static EmptyReply UserLeaveChannel(LeaveChannelRequest request) throws JsonApiException
	{
		ApiInst.userLeaveChannel(request.user_id, request.channel_id, request.resident_id, request.final_values);
		return new EmptyReply();
	}

	public static ChannelInfo GetChannelInfo(GetChannelInfoRequest request) throws JsonApiException
	{
		return ApiInst.getChannelInfo(request.channel_id, request.since);
	}
	
	public static EmptyReply SetTransientValue(SetTransientDataRequest request) throws JsonApiException
	{
		ApiInst.setTransientValue(request.id, request.key, request.value);
		
		EmptyReply reply = new EmptyReply();
		return reply;
	}
	
	public static EmptyReply SetTransientValues(SetTransientDataListRequest request) throws JsonApiException
	{
		EmptyReply reply = new EmptyReply();
		
		if(request.id == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "id");
		
		BaseResident res = ResidentManager.getResident(request.id);
		if(res == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, request.id);
		
		for(Map.Entry<String,Object> item : request.values.entrySet())
		{
			if(!KeyDataItem.isValidKeyname(item.getKey()))
				throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, item.getKey());
		}
		
		res.setTransientValues(request.values);
		
		return reply;
	}
	
	public static TransientDataItemReply GetTransientValue(GetTransientValueRequest request) throws JsonApiException
	{
		TransientDataItemReply reply = new TransientDataItemReply();
		Object value = ApiInst.getTransientValue(request.resident_id, request.key);
		
		reply.value = value;
		return reply;
	}
	

	public static TransientDataItemsReply GetTransientValues(GetTransientValuesRequest request) throws JsonApiException
	{
		TransientDataItemsReply reply = new TransientDataItemsReply();
		reply.values = ApiInst.getTransientValues(request.resident_id, request.keys);
		return reply;
	}
	
	public static TransientDataItemsReply GetAllTransientValues(GetAllTransientValuesRequest request) throws JsonApiException
	{
		TransientDataItemsReply reply = new TransientDataItemsReply();
		reply.values = ApiInst.getAllTransientValues(request.resident_id);
		return reply;
	}
	
	public static EmptyReply DeleteTransientValue(DeleteTransientDataRequest request) throws JsonApiException
	{
		ApiInst.deleteTransientValue(request.id, request.key);
		return new EmptyReply();
	}
	
	public static EmptyReply DeleteTransientValues(DeleteTransientDatasRequest request) throws JsonApiException
	{
		ApiInst.deleteTransientValues(request.id, request.keys);
		return new EmptyReply();
	}
	
	public static EmptyReply DeleteAllTransientValues(DeleteAllTransientDataRequest request) throws JsonApiException
	{
		ApiInst.deleteAllTransientValues(request.id);
		return new EmptyReply();
	}
	
	public static EmptyReply TriggerResidentEvent(TriggerResidentEventRequest request) throws JsonApiException, SQLException
	{
		ApiInst.triggerResidentEvent(request.resident_id, request.event_type, request.event);
		return new EmptyReply();
	}
	
	public static EmptyReply SendUserMessage(SendUserMessageRequest request) throws JsonApiException, SQLException
	{
		ApiInst.sendUserMessage(request.to, request.from, request.from_user, request.guaranteed, request.message_type, request.message);
		return new EmptyReply();
	}
	
	public static CurrencyBalanceReply GetCurrencyBalance(CurrencyBalanceRequest request) throws JsonApiException, SQLException
	{
		CurrencyBalanceReply reply = new CurrencyBalanceReply();
		reply.user_id = request.user_id;
		reply.currency_id = request.currency_id;
		
		reply.balance = ApiInst.getCurrencyBalance(request.user_id, request.currency_id);
		
		return reply;
	}
	
	public static CurrencyBalancesReply GetCurrencyBalances(CurrencyBalancesRequest request) throws JsonApiException, SQLException
	{
		CurrencyBalancesReply reply = new CurrencyBalancesReply();
		reply.user_id = request.user_id;
		
		reply.balances = ApiInst.getCurrencyBalances(request.user_id);
		
		return reply;
	}
	
	public static CurrencyBalanceReply AddCurrency(CurrencyChangeRequest request) throws JsonApiException, SQLException
	{
		CurrencyBalanceReply reply = new CurrencyBalanceReply();
		reply.user_id = request.user_id;
		reply.currency_id = request.currency_id;
		
		reply.balance = ApiInst.addCurrency(request.user_id, request.currency_id, request.amount, request.reason);
		
		return reply;
	}
	
	public static CurrencyBalanceReply SubtractCurrency(CurrencyChangeRequest request) throws JsonApiException, SQLException
	{
		CurrencyBalanceReply reply = new CurrencyBalanceReply();
		reply.user_id = request.user_id;
		reply.currency_id = request.currency_id;
		
		reply.balance = ApiInst.subtractCurrency(request.user_id, request.currency_id, request.amount, request.reason);
		
		return reply;
	}
	
}
