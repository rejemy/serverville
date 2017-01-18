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
		public String record_type;
		public String owner;
		public String parent;
		public double version;
		public double created;
		public double modified;
	}
	
	public static class KeyDataRecordsRequest
	{
		public String record_type;
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
	
	public static class GetHostWithResidentRequest
	{
		public String resident_id;
	}
	
	public static class GetHostWithResidentReply
	{
		public String host;
	}
	
	public static class CreateResidentRequest
	{
		public String resident_type;
		public Map<String,Object> values;
	}
	
	public static class CreateResidentReply
	{
		public String resident_id;
	}
	
	public static class DeleteResidentRequest
	{
		public String resident_id;
		public Map<String,Object> final_values;
	}
	
	public static class RemoveResidentFromAllChannelsRequest
	{
		public String resident_id;
		public Map<String,Object> final_values;
	}
	
	public static class JoinChannelRequest
	{
		public String channel_id;
		public String resident_id;
		public Map<String,Object> values;
	}
	
	public static class LeaveChannelRequest
	{
		public String channel_id;
		public String resident_id;
		public Map<String,Object> final_values;
	}
	
	public static class ListenToChannelRequest
	{
		public String channel_id;
	}
	
	public static class StopListenToChannelRequest
	{
		public String channel_id;
	}
	
	public static class ChannelMemberInfo
	{
		public String resident_id;
		public Map<String,Object> values;
	}
	
	public static class TriggerResidentEventRequest
	{
		public String resident_id;
		public String event_type;
		public String event_data;
	}
	
	public static class ResidentJoinedNotification
	{
		public String resident_id;
		public String via_channel;
		public Map<String,Object> values;
	}
	
	public static class ResidentStateUpdateNotification
	{
		public String resident_id;
		public String via_channel;
		public Map<String,Object> values;
		public List<String> deleted;
	}
	
	public static class ResidentLeftNotification
	{
		public String resident_id;
		public String via_channel;
		public Map<String,Object> final_values;
	}
	
	public static class ResidentEventNotification
	{
		public String resident_id;
		public String via_channel;
		public String event_type;
		public String event_data;
	}
	
	public static class ChannelInfo
	{
		public String channel_id;
		public Map<String,Object> values;
		public Map<String,ChannelMemberInfo> members;
	}
	
	public static class SetTransientValueRequest
	{
		public String resident_id;
		public String key;
		public Object value;
	}
	
	public static class SetTransientValuesRequest
	{
		public String resident_id;
		public Map<String,Object> values;
	}
	
	public static class DeleteTransientValueRequest
	{
		public String resident_id;
		public String key;
	}
	
	public static class DeleteTransientValuesRequest
	{
		public String resident_id;
		public List<String> values;
	}

	public static class GetTransientValueRequest
	{
		public String resident_id;
		public String key;
	}
	
	public static class GetTransientValuesRequest
	{
		public String resident_id;
		public List<String> keys;
	}
	
	public static class GetAllTransientValuesRequest
	{
		public String resident_id;
	}
	
	public static class SendUserMessageRequest
	{
		public String to;
		public String message_type;
		public String message;
		public boolean guaranteed;
	}
	
	public static class ClearMessageRequest
	{
		public String id;
	}
	
	public static class UserMessageNotification
	{
		public String id;
		public String message_type;
		public String message;
		public String from_id;
		public boolean sender_is_user;
	}
	
	public static class UserMessageList
	{
		public List<UserMessageNotification> messages;
	}
	
	public static class PendingNotification
	{
		public String notification_type;
		public String body;
	}
	
	public static class PendingNotificationList
	{
		public List<PendingNotification> notifications;
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
		public String currency;
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
	
	public static Class<?>[] NotificationRegistry = new Class[]
	{
		ResidentJoinedNotification.class,
		ResidentStateUpdateNotification.class,
		ResidentLeftNotification.class,
		ResidentEventNotification.class,
		UserMessageNotification.class,
		PendingNotification.class,
		PendingNotificationList.class,
	};
	
}
