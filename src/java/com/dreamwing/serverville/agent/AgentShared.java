package com.dreamwing.serverville.agent;

import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataTypes;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;

// Stuff that can be shared between the network and local script agent APIs
public class AgentShared
{
	private static final Logger l = LogManager.getLogger(AgentShared.class);
	
	public static UserInfoReply getUserInfo(String id, String username) throws JsonApiException, SQLException
	{
		ServervilleUser user = null;

		if(id != null)
			user = ServervilleUser.findById(id);
		else if(username != null)
			user = ServervilleUser.findByUsername(username);
		else
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply either a username or email");
		
		return userToUserInfo(user);
	}
	
	public static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item)
	{
		return KeyDataItemToDataItemReply(id, item, null);
	}
	
	public static DataItemReply KeyDataItemToDataItemReply(String id, KeyDataItem item, ScriptEngineContext ctx)
	{
		DataItemReply data = new DataItemReply();
		if(item.isDeleted())
		{
			data.deleted = true;
		}
		
		data.id = id;
		data.key = item.key;
		
		try
		{
			if(item.datatype == KeyDataTypes.JSON && ctx != null)
			{
				data.value = ctx.decodeJSON(item.asString());
			}
			else
			{
				data.value = item.asDecodedObject();
			}
		}
		catch(Exception e)
		{
			l.error("Exception decoding json value", e);
			data.value = "<error>";
		}
		
		data.data_type = JsonDataType.fromKeyDataType(item.datatype);
		data.created = item.created;
		data.modified = item.modified;
		return data;
	}
	
	public static UserInfoReply userToUserInfo(ServervilleUser user)
	{
		if(user == null)
			return null;
		
		UserInfoReply info = new UserInfoReply();
		
		info.id = user.getId();
		info.username = user.getUsername();
		info.created = user.Created.getTime();
		info.modified = user.Modified.getTime();
		info.admin_level = ServervilleUser.adminLevelToString(user.AdminLevel);
		
		return info;
	}
}
