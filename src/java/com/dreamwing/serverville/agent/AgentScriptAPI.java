package com.dreamwing.serverville.agent;

import java.sql.SQLException;

import com.dreamwing.serverville.agent.AgentMessages.SetGlobalDataRequest;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoRequest;
import com.dreamwing.serverville.client.ClientMessages.SetDataReply;
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
	
	public static SetDataReply SetDataKey(SetGlobalDataRequest request) throws JsonApiException, SQLException, Exception
	{
		SetDataReply reply = new SetDataReply();
		
		if(request.id == null)
			throw new JsonApiException("Invalid id: "+request.id);
		
		if(!KeyDataItem.isValidKeyname(request.key))
			throw new JsonApiException("Invalid key name: "+request.key);
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(request.key, request.data_type, request.value, request.visibility);
		long updateTime = KeyDataManager.saveKey(request.id, item);
		
		reply.updated_at = updateTime;
		return reply;
	}
}
