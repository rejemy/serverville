package com.dreamwing.serverville.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.LinkedList;

import org.junit.Assert;

import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.agent.AgentMessages.SetGlobalDataItemRequest;
import com.dreamwing.serverville.agent.AgentMessages.SetGlobalDataListRequest;
import com.dreamwing.serverville.agent.AgentMessages.SetGlobalDataRequest;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoRequest;
import com.dreamwing.serverville.client.ClientMessages.AllGlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeyRequest;
import com.dreamwing.serverville.client.ClientMessages.GlobalKeysRequest;
import com.dreamwing.serverville.client.ClientMessages.SetDataReply;
import com.dreamwing.serverville.client.ClientMessages.UserDataReply;
import com.dreamwing.serverville.agent.AgentServerSocketInitializer;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.type.TypeReference;

public class AgentTests
{
	@SuppressWarnings({ "rawtypes" })
	private <S> S makeAgentCall(String auth, String api, Object request, TypeReference valueTypeRef) throws IOException, JsonApiException
	{
		return HttpHelpers.postClientApi(AgentServerSocketInitializer.URL+api, auth, request, valueTypeRef);
	}
	
	private AgentKey Key;
	private AgentKey LocalhostLimitedKey;
	private AgentKey OtherLimitedKey;
	
	private String TestItemID = KeyDataManager.TestIdPrefix+SVID.makeSVID();
	
	@Test(order=1)
	public void InitAgentKey() throws SQLException, UnknownHostException
	{
		Key = AgentKeyManager.createAgentKey("Agent API test key", null, null);
		Assert.assertNotNull(Key);
		
		InetAddress localaddr = InetAddress.getLocalHost();
		String address = localaddr.getHostAddress();
		
		LocalhostLimitedKey = AgentKeyManager.createAgentKey("Agent API IP range test key", address+"/32", null);
		Assert.assertNotNull(LocalhostLimitedKey);
		
		OtherLimitedKey = AgentKeyManager.createAgentKey("Agent API IP range test key", "20.1.2.0/24", null);
		Assert.assertNotNull(OtherLimitedKey);
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
	public void LocalhostKey() throws IOException, JsonApiException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.username = "admin";
		UserInfoReply reply = makeAgentCall(LocalhostLimitedKey.Key, "api/GetUserInfo", request, new TypeReference<UserInfoReply>(){});
		Assert.assertNotNull(reply);
	}
	
	@Test(order=5)
	public void InvalidIpKey() throws IOException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.username = "admin";
		try {
			makeAgentCall(OtherLimitedKey.Key, "api/GetUserInfo", request, new TypeReference<UserInfoReply>(){});
		} catch (JsonApiException e) {
			// Expecting an api exception
			Assert.assertEquals(ApiErrors.BAD_AUTH.getCode(), e.Error.errorCode);
			return;
		}
		
		Assert.fail("Invalid ip source call didn't produce error");
	}
	
	@Test(order=10)
	public void GetUserInfo() throws IOException, JsonApiException
	{
		UserInfoRequest request = new UserInfoRequest();
		request.username = "admin";
		
		UserInfoReply reply = makeAgentCall(Key.Key, "api/GetUserInfo", request, new TypeReference<UserInfoReply>(){});
		Assert.assertNotNull(reply);
	}
	
	@Test(order=11)
	public void SetDataKey() throws IOException, JsonApiException
	{
		SetGlobalDataRequest request = new SetGlobalDataRequest();
		request.id = TestItemID;
		request.key = "agentKey1";
		request.value = "stuff";
		
		SetDataReply reply = makeAgentCall(Key.Key, "api/SetDataKey", request, new TypeReference<SetDataReply>(){});
		Assert.assertNotNull(reply);
		Assert.assertTrue(reply.updated_at > 0);
	}
	
	@Test(order=12)
	public void SetDataKeys() throws IOException, JsonApiException
	{
		SetGlobalDataListRequest request = new SetGlobalDataListRequest();
		request.id = TestItemID;
		request.values = new LinkedList<SetGlobalDataItemRequest>();
		
		SetGlobalDataItemRequest r = new SetGlobalDataItemRequest();
		r.key = "agentKeys1";
		r.value = "stuff1";
		request.values.add(r);
		
		r = new SetGlobalDataItemRequest();
		r.key = "agentKeys2";
		r.value = "stuff2";
		request.values.add(r);
		
		SetDataReply reply = makeAgentCall(Key.Key, "api/SetDataKeys", request, new TypeReference<SetDataReply>(){});
		Assert.assertNotNull(reply);
		Assert.assertTrue(reply.updated_at > 0);
	}
	
	@Test(order=13)
	public void GetDataKey() throws IOException, JsonApiException
	{
		GlobalKeyRequest request = new GlobalKeyRequest();
		request.id = TestItemID;
		request.key = "agentKey1";
		
		DataItemReply reply = makeAgentCall(Key.Key, "api/GetDataKey", request, new TypeReference<DataItemReply>(){});
		Assert.assertNotNull(reply);
		Assert.assertEquals("stuff", reply.value);
	}
	
	@Test(order=14)
	public void GetDataKeys() throws IOException, JsonApiException
	{
		GlobalKeysRequest request = new GlobalKeysRequest();
		request.id = TestItemID;
		request.keys = new LinkedList<String>();
		request.keys.add("agentKeys1");
		request.keys.add("agentKeys2");
		
		UserDataReply reply = makeAgentCall(Key.Key, "api/GetDataKeys", request, new TypeReference<UserDataReply>(){});
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.values);
		
		DataItemReply val1 = reply.values.get("agentKeys1");
		Assert.assertEquals("stuff1", val1.value);
		
		DataItemReply val2 = reply.values.get("agentKeys2");
		Assert.assertEquals("stuff2", val2.value);
	}
	
	@Test(order=15)
	public void GetAllDataKeys() throws IOException, JsonApiException
	{
		AllGlobalKeysRequest request = new AllGlobalKeysRequest();
		request.id = TestItemID;
		
		UserDataReply reply = makeAgentCall(Key.Key, "api/GetAllDataKeys", request, new TypeReference<UserDataReply>(){});
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.values);
		Assert.assertEquals(3, reply.values.size());
	}
	

	@Test(order=1000)
	public void Cleanup() throws SQLException
	{
		Key.delete();
		Key = null;
		
		LocalhostLimitedKey.delete();
		LocalhostLimitedKey = null;
		
		OtherLimitedKey.delete();
		OtherLimitedKey = null;
	}
}
