package com.dreamwing.serverville.agent;

import java.util.List;

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
	}
	
	public static class SetGlobalDataRequest
	{
		public String id;
		public String key;
		public Object value;
		public JsonDataType data_type;
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
	
	public static class ListenerRequest
	{
		public String source;
		public String listener;
		boolean two_way;
	}
	
	public static class EndListenerRequest
	{
		public String source;
		public String listener;
	}
	
	public static class GetTransientStateRequest
	{
		public String id;
		public String key;
	}
	
	public static class GetTransientStatesRequest
	{
		public String id;
		public List<String> keys;
	}
	
	public static class GetAllTransientStatesRequest
	{
		public String id;
	}

}
