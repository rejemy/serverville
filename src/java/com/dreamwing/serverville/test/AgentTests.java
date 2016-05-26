package com.dreamwing.serverville.test;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Assert;

import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoRequest;
import com.dreamwing.serverville.agent.AgentServerSocketInitializer;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.JsonApiException;
import com.fasterxml.jackson.core.type.TypeReference;

public class AgentTests
{
	@SuppressWarnings({ "rawtypes" })
	private <S> S makeAgentCall(String auth, String api, Object request, TypeReference valueTypeRef) throws IOException, JsonApiException
	{
		return HttpHelpers.postClientApi(AgentServerSocketInitializer.URL+api, auth, request, valueTypeRef);
	}
	
	private AgentKey Key;
	
	
	@Test(order=1)
	public void InitAgentKey() throws SQLException
	{
		Key = AgentKeyManager.createAgentKey("Agent API test key", null, null);
		Assert.assertNotNull(Key);
	}
	
	@Test(order=2)
	public void InvalidAPI() throws IOException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.id = "dgfgf";
		try {
			makeAgentCall(Key.Key, "api/klsjdklg", request, new TypeReference<UserInfoReply>(){});
		} catch (JsonApiException e) {
			// Expecting an api exception
			Assert.assertEquals(ApiErrors.UNKNOWN_API.getCode(), e.Error.errorCode);
			return;
		}
		
		Assert.fail("Invalid api call didn't produce error");
	}
	
	@Test(order=3)
	public void InvalidAuth() throws IOException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.id = "dgfgf";
		try {
			makeAgentCall("fdgdghgjg", "api/GetUserInfo", request, new TypeReference<UserInfoReply>(){});
		} catch (JsonApiException e) {
			// Expecting an api exception
			Assert.assertEquals(ApiErrors.BAD_AUTH.getCode(), e.Error.errorCode);
			return;
		}
		
		Assert.fail("Invalid auth call didn't produce error");
	}
	
	@Test(order=4)
	public void GetUserInfo() throws IOException, JsonApiException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.username = "admin";
		
		UserInfoReply reply = makeAgentCall(Key.Key, "api/GetUserInfo", request, new TypeReference<UserInfoReply>(){});
		Assert.assertNotNull(reply);
	}
	
	@Test(order=1000)
	public void Cleanup() throws SQLException
	{
		Key.delete();
	}
}
