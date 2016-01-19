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
	
	public static class TransientStateChangeMessage
	{
		public Map<String,Object> values;
	}
	
	public static class ResidentIntroductionMessage
	{
		public List<String> ids;
	}
	
	public static class ListenToChannelRequest
	{
		String id;
		boolean two_way;
	}
	
	public static class EndListenToChannelRequest
	{
		String id;
	}
	
	public static class ChannelInfo
	{
		String id;
		public List<String> members;
	}

}
