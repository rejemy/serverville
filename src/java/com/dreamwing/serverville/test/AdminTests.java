package com.dreamwing.serverville.test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.admin.AdminAPI.AgentKeyInfo;
import com.dreamwing.serverville.admin.AdminAPI.AgentKeyInfoList;
import com.dreamwing.serverville.admin.AdminAPI.LogFileListing;
import com.dreamwing.serverville.admin.AdminAPI.ScriptDataInfo;
import com.dreamwing.serverville.admin.AdminAPI.ScriptDataInfoList;
import com.dreamwing.serverville.admin.AdminAPI.SelfTestStatus;
import com.dreamwing.serverville.admin.AdminAPI.ServerInfo;
import com.dreamwing.serverville.admin.AdminAPI.SignInReply;
import com.dreamwing.serverville.admin.AdminAPI.UserInfo;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.admin.AdminServerSocketInitializer;
import com.dreamwing.serverville.log.IndexedFileManager.LogSearchHit;
import com.dreamwing.serverville.log.IndexedFileManager.LogSearchHits;
import com.dreamwing.serverville.net.ApiError;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpUtil;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.SVID;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import io.netty.handler.codec.http.HttpResponseStatus;

public class AdminTests {

	private static final Logger l = LogManager.getLogger(AdminTests.class);
	
	private String LogSearchTerm;
	
	private String AdminUsername;
	private String AdminPassword;
	private String AdminUserId;
	private String AdminSessionId;
	
	private String TestUserId;
	private String TestUserName;
	private String TestUserEmail;
	
	private Random Rand;
	
	private <S> S getAdminApi(String api, Class<S> successClass) throws IOException, JsonApiException
	{
		return HttpUtil.getJsonApi(AdminServerSocketInitializer.URL+api, AdminSessionId, successClass);
	}
	
	private String getAdminApiBody(String api) throws IOException, JsonApiException
	{
		return HttpUtil.getString(AdminServerSocketInitializer.URL+api, AdminSessionId);
	}
	
	private void postAdminApi(String api, RequestBody body) throws IOException, JsonApiException
	{
		HttpUtil.postJsonApi(AdminServerSocketInitializer.URL+api, AdminSessionId, body);
	}
	
	private <S> S postAdminApi(String api, RequestBody body, Class<S> successClass) throws IOException, JsonApiException
	{
		return HttpUtil.postJsonApi(AdminServerSocketInitializer.URL+api, AdminSessionId, body, successClass);
	}
	
	@Test(order=1)
	public void LogTestInfo() throws SQLException
	{
		HttpUtil.resetHttpClient();
		
		LogSearchTerm = SVID.makeSVID();
		AdminSessionId = null;
		TestUserId = null;
		TestUserName = null;
		TestUserEmail = null;
		
		TestAgentKey = null;
		
		Rand = new Random();
		
		AdminUsername = "SelftestAdmin_"+SVID.makeSVID();
		AdminPassword = Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong());
		
		ServervilleUser tempAdmin = ServervilleUser.create(AdminPassword, AdminUsername, null, ServervilleUser.AdminLevel_Admin);
		Assert.assertNotNull(tempAdmin);
		AdminUserId = tempAdmin.getId();
		
		l.info("Starting selftest "+LogSearchTerm);
		ServervilleMain.LogSearcher.flush();
	}

	@Test(order=2)
	public void InvalidSignin() throws IOException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("username", AdminUsername);
		body.add("password", "wrongpasswordfool!");
		
		
		try {
			postAdminApi("api/signIn", body.build(), SignInReply.class);
		} catch (JsonApiException e) {
			Assert.assertTrue(e.Error.isError);
			Assert.assertEquals(HttpResponseStatus.FORBIDDEN, e.HttpStatus);
			Assert.assertEquals(ApiErrors.BAD_AUTH.getCode(), e.Error.errorCode);
			return;
		}
		
		Assert.fail("Didn't get an auth exception");
	}
	
	@Test(order=3)
	public void InvalidAuth() throws IOException
	{
		boolean gotError=false;
		try {
			getAdminApi("api/info", ServerInfo.class);
		} catch (JsonApiException e) {
			gotError = true;
		}
		
		Assert.assertTrue(gotError);
	}
	
	@Test(order=4)
	public void Signin() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("username", AdminUsername);
		body.add("password", AdminPassword);
		
		SignInReply reply = postAdminApi("api/signIn", body.build(), SignInReply.class);
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.session_id);
		
		AdminSessionId = reply.session_id;
	}
	
	@Test(order=5)
	public void Info() throws IOException, JsonApiException
	{
		ServerInfo info = getAdminApi("api/info", ServerInfo.class);
		Assert.assertNotNull(info);
		Assert.assertEquals(ServervilleMain.Hostname, info.hostname);
		Assert.assertTrue(info.uptime_seconds > 0);
		Assert.assertNotNull(info.java_version);
		Assert.assertNotNull(info.os);
	}
	
	@Test(order=6)
	public void SelftestResults() throws IOException, JsonApiException
	{
		SelfTestStatus result = getAdminApi("api/selftest", SelfTestStatus.class);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.started_at > 0);
		Assert.assertEquals(SelfTest.getNumTests(), result.total_tests);
		Assert.assertNotNull(result.results);
		Assert.assertEquals(SelfTest.getNumTests(), result.results.size());
	}
	
	@Test(order=7)
	public void SelftestPartialResults() throws IOException, JsonApiException
	{
		String url = "api/selftest?first=1";
		SelfTestStatus result = getAdminApi(url, SelfTestStatus.class);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.started_at > 0);
		Assert.assertEquals(SelfTest.getNumTests(), result.total_tests);
		Assert.assertNotNull(result.results);
		Assert.assertEquals(2, result.results.get(0).index);
	}
	
	@Test(order=8)
	public void LogFilesList() throws IOException, JsonApiException
	{
		LogFileListing result = getAdminApi("api/logfiles", LogFileListing.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.files);
		Assert.assertTrue(result.files.size() > 0);
	}
	
	@Test(order=9)
	public void LogFile() throws IOException, JsonApiException
	{
		LogFileListing result = getAdminApi("api/logfiles", LogFileListing.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.files);
		Assert.assertTrue(result.files.size() > 0);
		String log = HttpUtil.getString(AdminServerSocketInitializer.URL+"logs/"+result.files.get(0).filename);
		Assert.assertNotNull(log);
		Assert.assertTrue(log.length() > 0);
	}
	
	// 10 missing
	
	@Test(order=11)
	public void LogSearchFuture() throws IOException, JsonApiException
	{
		long futureTime = System.currentTimeMillis()+1000*60*60;
		String url = "api/searchLogs?q=*:*&from="+futureTime;
		LogSearchHits result = getAdminApi(url, LogSearchHits.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.hits);
		Assert.assertTrue(result.hits.size() == 0);
	}
	
	@Test(order=12)
	public void LogFileMissing() throws IOException, JsonApiException
	{
		ApiError result = HttpUtil.getJson(AdminServerSocketInitializer.URL+"logs/sdgadhsfghghg.fgf", ApiError.class);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isError);
	}
	
	@Test(order=13)
	public void InvalidAPI() throws IOException
	{
		String url = AdminServerSocketInitializer.URL+"api/lskdjioif";
		ApiError result = HttpUtil.getJson(url, ApiError.class);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isError);
	}
	

	@Test(order=14)
	public void CreateUser() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		
		TestUserName = "test_"+SVID.makeSVID();
		TestUserEmail = SVID.makeSVID()+"@serverville.com";
		
		body.add("username", TestUserName);
		body.add("password", Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong()));
		body.add("email", TestUserEmail);
		
		UserInfo reply = postAdminApi("api/createUser", body.build(), UserInfo.class);
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.id);
		Assert.assertEquals(reply.username, TestUserName);
		Assert.assertEquals(reply.email, TestUserEmail);
		Assert.assertTrue(reply.created > 0);
		Assert.assertTrue(reply.modified > 0);
		Assert.assertTrue(reply.admin_level == 0);
		
		TestUserId = reply.id;
		
	}
	
	@Test(order=15)
	public void UserById() throws IOException, JsonApiException
	{
		String url = "api/user?id="+TestUserId;
		UserInfo reply = getAdminApi(url, UserInfo.class);
		Assert.assertNotNull(reply);
		Assert.assertEquals(reply.id, TestUserId);
		Assert.assertEquals(reply.username, TestUserName);
		Assert.assertEquals(reply.email, TestUserEmail);
		Assert.assertTrue(reply.created > 0);
		Assert.assertTrue(reply.modified > 0);
		Assert.assertTrue(reply.admin_level == 0);
	}
	
	@Test(order=16)
	public void UserByEmail() throws IOException, JsonApiException
	{
		String url = "api/user?email="+TestUserEmail;
		UserInfo reply = getAdminApi(url, UserInfo.class);
		Assert.assertNotNull(reply);
		Assert.assertEquals(reply.id, TestUserId);
		Assert.assertEquals(reply.username, TestUserName);
		Assert.assertEquals(reply.email, TestUserEmail);
		Assert.assertTrue(reply.created > 0);
		Assert.assertTrue(reply.modified > 0);
		Assert.assertTrue(reply.admin_level == 0);
	}
	
	@Test(order=17)
	public void UserByUsername() throws IOException, JsonApiException
	{
		String url = "api/user?username="+TestUserName;
		UserInfo reply = getAdminApi(url, UserInfo.class);
		Assert.assertNotNull(reply);
		Assert.assertEquals(reply.id, TestUserId);
		Assert.assertEquals(reply.username, TestUserName);
		Assert.assertEquals(reply.email, TestUserEmail);
		Assert.assertTrue(reply.created > 0);
		Assert.assertTrue(reply.modified > 0);
		Assert.assertTrue(reply.admin_level == 0);
	}
	
	@Test(order=18)
	public void SetUserPassword() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("password", Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong()));
		
		postAdminApi("api/setUserPassword", body.build());
	}
	
	@Test(order=19)
	public void SetUserUsername() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("username", "test_"+SVID.makeSVID());
		
		postAdminApi("api/setUserUsername", body.build());
	}
	
	@Test(order=20)
	public void SetUserEmail() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("email", "test_"+SVID.makeSVID()+"@serverville.com");
		
		postAdminApi("api/setUserEmail", body.build());
	}
	
	@Test(order=21)
	public void SetUserAdminLevel() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("admin_level", "readOnlyAdmin");
		
		postAdminApi("api/setUserAdminLevel", body.build());
	}
	
	@Test(order=50)
	public void DeleteUser() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		
		postAdminApi("api/deleteUser", body.build());
	}
	
	

	// Search after we added the marker, to increase odds it was flushed normally
	@Test(order=51)
	public void LogSearch() throws IOException, JsonApiException
	{
		ServervilleMain.LogSearcher.flush();
		String url = "api/searchLogs?q="+LogSearchTerm;
		LogSearchHits result = getAdminApi(url, LogSearchHits.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.hits);
		Assert.assertTrue(result.hits.size() > 0);
		
		for(LogSearchHit hit : result.hits)
		{
			if(hit.message.indexOf(LogSearchTerm) < 0)
			{
				Assert.fail(LogSearchTerm+" not found in result "+hit.message);
			}
		}
		
	}
	
	String TestAgentKey;
	
	@Test(order=100)
	public void CreateAgentKey() throws IOException, JsonApiException
	{
		Date expirationDate = new Date();
		long expiration = expirationDate.getTime() + 1000*60;
		
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("comment", "Test agent key for selftest");
		body.add("expiration", Long.toString(expiration));
		body.add("iprange", "127.0.0.1/8");
		
		AgentKeyInfo result = postAdminApi("api/createAgentKey", body.build(), AgentKeyInfo.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.key);
		
		TestAgentKey = result.key;
	}
	
	@Test(order=101)
	public void AgentKeys() throws IOException, JsonApiException
	{
		String url = "api/agentKeys";
		AgentKeyInfoList reply = getAdminApi(url, AgentKeyInfoList.class);
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.keys);
		
		AgentKeyInfo found = null;
		for(AgentKeyInfo keyInfo : reply.keys)
		{
			if(keyInfo.key.equals(TestAgentKey))
			{
				found = keyInfo;
				break;
			}
		}
		
		if(found == null)
			Assert.fail("Didn't find expected agent key in list");
		
		Assert.assertNotNull(found.comment);
		Assert.assertNotNull(found.iprange);
		Assert.assertTrue(found.expiration > 0);
	}
	
	
	@Test(order=102)
	public void EditAgentKey() throws IOException, JsonApiException
	{

		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("key", TestAgentKey);
		body.add("comment", "Test agent key for selftest w/ new comment");
		body.add("expiration", "0");
		
		AgentKeyInfo result = postAdminApi("api/editAgentKey", body.build(), AgentKeyInfo.class);
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.key);
		
	}
	
	@Test(order=103)
	public void DeleteAgentKey() throws IOException, JsonApiException
	{

		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("key", TestAgentKey);
		
		postAdminApi("api/deleteAgentKey", body.build());
	}
	
	private String TestScriptId;
	private static String TestScriptBody = "var _selftest_test_script_object = {};";
	
	@Test(order=104)
	public void AddScript() throws IOException, JsonApiException
	{
		TestScriptId = "_test_script_"+SVID.makeSVID();
		
		RequestBody body = RequestBody.create(HttpUtil.JAVASCRIPT_CONTENT_TYPE, TestScriptBody);
		
		postAdminApi("api/addScript?id="+TestScriptId, body);
	}
	
	@Test(order=105)
	public void ScriptInfo() throws IOException, JsonApiException
	{
		String url = "api/scriptInfo?id="+TestScriptId;
		ScriptDataInfo reply = getAdminApi(url, ScriptDataInfo.class);
		Assert.assertNotNull(reply);
		Assert.assertEquals(TestScriptId, reply.id);
		Assert.assertEquals(TestScriptBody, reply.source);
		Assert.assertTrue(reply.created > 0);
		Assert.assertTrue(reply.modified > 0);
	}
	
	@Test(order=106)
	public void Script() throws IOException, JsonApiException
	{
		String url = "api/script?id="+TestScriptId;
		String reply = getAdminApiBody(url);
		Assert.assertNotNull(reply);
		Assert.assertEquals(TestScriptBody, reply);
	}
	
	@Test(order=107)
	public void Scripts() throws IOException, JsonApiException
	{
		String url = "api/scripts";
		ScriptDataInfoList reply = getAdminApi(url, ScriptDataInfoList.class);
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.scripts);
		
		ScriptDataInfo scriptInfo = null;
		for(ScriptDataInfo script : reply.scripts)
		{
			if(script.id.equals(TestScriptId))
			{
				scriptInfo = script;
				break;
			}
		}
		
		if(scriptInfo == null)
			Assert.fail("Didn't find expected test script in list");
		
		Assert.assertEquals(TestScriptBody, scriptInfo.source);
		Assert.assertTrue(scriptInfo.created > 0);
		Assert.assertTrue(scriptInfo.modified > 0);
	}
	
	@Test(order=108)
	public void DeleteScript() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("id", TestScriptId);
		
		postAdminApi("api/deleteScript", body.build());
		
		TestScriptId = null;
		TestScriptBody = null;
	}
	
	
	@Test(order=1000)
	public void Cleanup() throws SQLException
	{
		ServervilleUser tempAdmin = ServervilleUser.findById(AdminUserId);
		Assert.assertNotNull(tempAdmin);
		tempAdmin.delete();
	}
	
	
	
}
