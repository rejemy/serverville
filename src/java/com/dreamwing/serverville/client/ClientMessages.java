package com.dreamwing.serverville.client;

import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.data.JsonDataType;


public class ClientMessages {

	public static class EmptyClientReply
	{

	}
	
	public static class CreateAnonymousAccount
	{

	}
	
	public static class GetUserInfo
	{

	}
	
	public static class CreateAccount
	{
		public String username;
		public String email;
		public String password;
	}
	
	public static class CreateAccountReply
	{
		public String user_id;
		public String username;
		public String email;
		public String session_id;
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
	}
	
	public static class DataItemExtendedReply extends DataItemReply
	{
		public boolean deleted;
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
	
	public static class TransientValuesChangeMessage
	{
		public Map<String,Object> values;
	}
	
	public static class JoinChannelRequest
	{
		String id;
		boolean listen_only;
	}
	
	public static class LeaveChannelRequest
	{
		String id;
	}
	
	
	public static class ChannelInfo
	{
		String id;
		public List<String> members;
	}
	
	public static class SetTransientValueRequest
	{
		public String key;
		public Object value;
		public JsonDataType data_type;
	}
	
	public static class SetTransientValuesRequest
	{
		public List<SetTransientValueRequest> values;
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
	
	public static class TransientMessageRequest
	{
		public String to;
		public String message_type;
		public Object value;
		public JsonDataType data_type;
	}
	
	public static class TransientClientMessage
	{
		public String message_type;
		public Object value;
		public JsonDataType data_type;
	}
}
