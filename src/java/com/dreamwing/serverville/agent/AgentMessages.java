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
	
	public static class AddResidentRequest
	{
		public String channel_id;
		public String resident_id;
	}
	
	public static class RemoveResidentRequest
	{
		public String channel_id;
		public String resident_id;
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

}
