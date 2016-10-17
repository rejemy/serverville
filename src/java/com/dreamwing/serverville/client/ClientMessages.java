package com.dreamwing.serverville.client;


import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.data.JsonDataType;


public class ClientMessages {

	public static class EmptyClientRequest
	{

	}
	
	public static class EmptyClientReply
	{

	}
	
	public static class CreateAnonymousAccount
	{
		public String invite_code;
		public String language;
		public String country;
	}
	
	public static class GetUserInfo
	{

	}
	
	public static class CreateAccount
	{
		public String username;
		public String email;
		public String password;
		public String invite_code;
		public String language;
		public String country;
	}

	public static class ValidateSessionRequest
	{
		public String session_id;
	}
	
	public static class SignIn
	{
		public String username;
		public String email;
		public String password;
	}
	
	public static class SignInReply
	{
		public String user_id;
		public String username;
		public String email;
		public String session_id;
		public double admin_level;
		public String language;
		public String country;
		public double time;
	}
	
	public static class UserAccountInfo
	{
		public String user_id;
		public String username;
		public String email;
		public String session_id;
		public double admin_level;
	}
	
	public static class ServerTime
	{
		public double time;
	}
	
	public static class SetLocaleRequest
	{
		public String country;
		public String language;
	}
	
	public static class GetUserDataComboRequest
	{
		public double since;
	}
	
	public static class GetUserDataComboReply
	{
		public Map<String,DataItemReply> values;
		public Map<String,Integer> balances;
	}
	
	public static class SetUserDataRequest
	{
		public String key;
		public Object value;
		public JsonDataType data_type;
	}
	
	public static class UserDataRequestList
	{
		public List<SetUserDataRequest> values;
	}
	
	public static class SetDataReply
	{
		public double updated_at;
	}
	
	public static class KeyRequest
	{
		public String key;
	}
	
	public static class DataItemReply
	{
		public String id;
		public String key;
		public Object value;
		public JsonDataType data_type;
		public double created;
		public double modified;
		public boolean deleted;
	}
	
	public static class TransientDataItemReply
	{
		public Object value;
	}
	
	public static class TransientDataItemsReply
	{
		public Map<String,Object> values;
	}
	
	public static class KeysRequest
	{
		public List<String> keys;
		public double since;
	}
	
	public static class UserDataReply
	{
		public Map<String,DataItemReply> values;
	}
	
	public static class AllKeysRequest
	{
		public double since;
	}
	
	
	public static class GlobalKeyRequest
	{
		public String id;
		public String key;
	}
	
	public static class GlobalKeysRequest
	{
		public String id;
		public List<String> keys;
		public double since;
		public boolean include_deleted;
	}
	
	public static class AllGlobalKeysRequest
	{
		public String id;
		public double since;
		public boolean include_deleted;
	}
	
	public static class KeyDataRecordRequest
	{
		public String id;
	}
	
	public static class KeyDataInfo
	{
		public String id;
		public String type;
		public String owner;
		public String parent;
		public double version;
		public double created;
		public double modified;
	}
	
	public static class KeyDataRecordsRequest
	{
		public String type;
		public String parent;
	}
	
	public static class KeyDataRecords
	{
		public List<KeyDataInfo> records;
	}
	
	public static class SetGlobalDataRequest
	{
		public String id;
		public List<SetUserDataRequest> values;
	}
	
	public static class TransientValuesChangeMessage
	{
		public Map<String,Object> values;
		public List<String> deleted;
	}
	
	public static class JoinChannelRequest
	{
		public String alias;
		public String id;
		public Map<String,Object> values;
	}
	
	public static class LeaveChannelRequest
	{
		public String alias;
		public String id;
		public Map<String,Object> final_values;
	}
	
	public static class ListenToResidentRequest
	{
		public String id;
	}
	
	public static class StopListenToResidentRequest
	{
		public String id;
	}
	
	public static class ChannelMemberInfo
	{
		public String id;
		public Map<String,Object> values;
	}
	
	public static class ChannelInfo
	{
		public String id;
		public Map<String,Object> values;
		public Map<String,ChannelMemberInfo> members;
	}
	
	public static class SetTransientValueRequest
	{
		public String alias;
		public String key;
		public Object value;
	}
	
	public static class SetTransientValuesRequest
	{
		public String alias;
		public Map<String,Object> values;
	}

	public static class GetTransientValueRequest
	{
		public String id;
		public String alias;
		public String key;
	}
	
	public static class GetTransientValuesRequest
	{
		public String id;
		public String alias;
		public List<String> keys;
	}
	
	public static class GetAllTransientValuesRequest
	{
		public String id;
		public String alias;
	}
	
	public static class TransientMessageRequest
	{
		public String to;
		public String alias;
		public String message_type;
		public Object value;
	}
	
	public static class TransientClientMessage
	{
		public String message_type;
		public Object value;
	}
	
	public static class CurrencyBalanceRequest
	{
		public String currency_id;
	}
	
	public static class CurrencyBalanceReply
	{
		public String currency_id;
		public int balance;
	}
	
	public static class CurrencyBalancesReply
	{
		public Map<String,Integer> balances;
	}
	
	public static class GetProductsRequest
	{

	}
	
	public static class GetProductRequest
	{
		public String product_id;
	}
	
	public static class ProductInfo
	{
		public String id;
		public String name;
		public String description;
		public String image_url;
		public double price;
		public String display_price;
	}
	
	public static class ProductInfoList
	{
		public List<ProductInfo> products;
	}
	
	public static class StripeCheckoutRequest
	{
		public String stripe_token;
		public String product_id;
	}
	
	public static class ProductPurchasedReply
	{
		public String product_id;
		public double price;
		public Map<String,Integer> currencies;
	}
	
}
