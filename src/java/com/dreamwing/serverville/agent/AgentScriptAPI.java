package com.dreamwing.serverville.agent;

import java.sql.SQLException;

import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoRequest;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.HttpUtil.JsonApiException;
import com.dreamwing.serverville.serialize.JsonDataDecoder;

public class AgentScriptAPI
{

	public UserInfoReply GetUserInfo(UserInfoRequest request) throws JsonApiException, SQLException
	{
		ServervilleUser user = null;
		
		if(request.id != null)
			user = ServervilleUser.findById(request.id);
		else if(request.username != null)
			user = ServervilleUser.findByUsername(request.username);
		else
			throw new JsonApiException("Must speicify either id or username");
		
		if(user == null)
			return null;
		
		UserInfoReply info = new UserInfoReply();
		
		info.id = user.getId();
		info.username = user.getUsername();
		info.created = user.Created.getTime();
		info.modified = user.Modified.getTime();
		info.admin_level = user.AdminLevel;
		
		return info;
	}
	
	public static double SetDataKey(String id, String key, Object value, JsonDataType data_type) throws Exception
	{
		if(id == null)
			throw new JsonApiException("Invalid id: "+id);
		
		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException("Invalid key name: "+key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, data_type, value);
		long updateTime = KeyDataManager.saveKey(id, item);
		
		return updateTime;
	}
}
