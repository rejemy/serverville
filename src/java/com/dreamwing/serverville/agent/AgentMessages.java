package com.dreamwing.serverville.agent;

import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.data.JsonDataType;

public class AgentMessages
{
	public static class EmptyReply
	{
	}
	
	public static class UserInfoRequest
	{
		public String id;
		public String username;
	}
	
	public static class UserInfoReply
	{
		public String id;
		public String username;
		public double created;
		public double modified;
		public String admin_level;
		public double admin_privs;
	}
	
	public static class SetGlobalDataRequest
	{
		public String id;
		public String key;
		public Object value;
		public JsonDataType data_type;
	}
	
	public static class SetTransientDataRequest
	{
		public String id;
		public String key;
		public Object value;
	}
	
	public static class DeleteTransientDataRequest
	{
		public String id;
		public String key;
	}
	
	public static class DeleteTransientDatasRequest
	{
		public String id;
		public List<String> keys;
	}
	
	public static class DeleteAllTransientDataRequest
	{
		public String id;
	}
	
	public static class SetGlobalDataItemRequest
	{
		public String key;
		public Object value;
		public JsonDataType data_type;
	}
	
	public static class SetGlobalDataListRequest
	{
		public String id;
		public List<SetGlobalDataItemRequest> values;
	}
	
	public static class SetTransientDataListRequest
	{
		public String id;
		public Map<String,Object> values;
	}
	
	public static class DeleteGlobalKeyDataRequest
	{
		public String id;
		public String key;
	}
	
	public static class DeleteGlobalDataAllRequest
	{
		public String id;
	}
	
	public static class CreateChannelRequest
	{
		public String id;
	}
	
	public static class CreateChannelReply
	{
		public String id;
	}
	
	public static class DeleteChannelRequest
	{
		public String id;
	}
	
	public static class UserAliasRequest
	{
		public String user_id;
		public String alias;
	}
	
	public static class UserAliasReply
	{
		public String alias_id;
	}
	
	public static class AddResidentRequest
	{
		public String channel_id;
		public String resident_id;
	}
	
	public static class RemoveResidentRequest
	{
		public String channel_id;
		public String resident_id;
		public Map<String,Object> final_values;
	}
	
	public static class AddListenerRequest
	{
		public String user_id;
		public String channel_id;
	}
	
	public static class JoinChannelRequest
	{
		public String user_id;
		public String channel_id;
		public String alias;
		public Map<String,Object> values;
	}
	
	public static class LeaveChannelRequest
	{
		public String user_id;
		public String channel_id;
		public String alias;
		public Map<String,Object> final_values;
	}
	
	public static class GetChannelInfoRequest
	{
		public String channel_id;
		public double since;
	}
	
	public static class GetTransientValueRequest
	{
		public String id;
		public String key;
	}
	
	public static class GetTransientValuesRequest
	{
		public String id;
		public List<String> keys;
	}
	
	public static class GetAllTransientValuesRequest
	{
		public String id;
	}
	
	public static class ServerMessageRequest
	{
		public String to;
		public String from;
		public String alias;
		public String messageType;
		public Object value;
	}
	
	public static class CurrencyBalanceRequest
	{
		public String user_id;
		public String currency_id;
	}
	
	public static class CurrencyBalancesRequest
	{
		public String user_id;
	}
	
	public static class CurrencyChangeRequest
	{
		public String user_id;
		public String currency_id;
		public int amount;
		public String reason;
	}
	
	public static class CurrencyBalanceReply
	{
		public String user_id;
		public String currency_id;
		public int balance;
	}
	
	public static class CurrencyBalancesReply
	{
		public String user_id;
		public Map<String,Integer> balances;
	}

}
