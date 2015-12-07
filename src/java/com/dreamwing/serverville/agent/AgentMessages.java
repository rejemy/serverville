package com.dreamwing.serverville.agent;

import java.util.List;

import com.dreamwing.serverville.data.JsonDataType;

public class AgentMessages
{
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
		public double admin_level;
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
}
