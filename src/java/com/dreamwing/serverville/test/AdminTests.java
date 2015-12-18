package com.dreamwing.serverville.test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.admin.AdminAPI.CreateUserReply;
import com.dreamwing.serverville.admin.AdminAPI.LogFileListing;
import com.dreamwing.serverville.admin.AdminAPI.SelfTestStatus;
import com.dreamwing.serverville.admin.AdminAPI.ServerInfo;
import com.dreamwing.serverville.admin.AdminAPI.SignInReply;
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
	
	private Random Rand;
	
	private <S> S getAdminApi(String api, Class<S> successClass) throws IOException, JsonApiException
	{
		return HttpUtil.getJsonApi(AdminServerSocketInitializer.URL+api, AdminSessionId, successClass);
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
		body.add("username", "test_"+SVID.makeSVID());
		body.add("password", Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong()));
		body.add("email", SVID.makeSVID()+"@serverville.com");
		
		CreateUserReply reply = postAdminApi("api/createUser", body.build(), CreateUserReply.class);
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.user_id);
		
		TestUserId = reply.user_id;
	}
	
	@Test(order=15)
	public void SetUserPassword() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("password", Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong()));
		
		postAdminApi("api/setUserPassword", body.build());
	}
	
	@Test(order=16)
	public void SetUserUsername() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("username", "test_"+SVID.makeSVID());
		
		postAdminApi("api/setUserUsername", body.build());
	}
	
	@Test(order=17)
	public void SetUserEmail() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("email", "test_"+SVID.makeSVID()+"@serverville.com");
		
		postAdminApi("api/setUserEmail", body.build());
	}
	
	@Test(order=18)
	public void SetUserAdminLevel() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		body.add("admin_level", "readOnlyAdmin");
		
		postAdminApi("api/setUserAdminLevel", body.build());
	}
	
	// Search after we added the marker, to increase odds it was flushed normally
	@Test(order=19)
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
	
	@Test(order=50)
	public void DeleteUser() throws IOException, JsonApiException
	{
		FormEncodingBuilder body = new FormEncodingBuilder();
		body.add("user_id", TestUserId);
		
		postAdminApi("api/deleteUser", body.build());
	}
	
	
	@Test(order=1000)
	public void Cleanup() throws SQLException
	{
		ServervilleUser tempAdmin = ServervilleUser.findById(AdminUserId);
		Assert.assertNotNull(tempAdmin);
		tempAdmin.delete();
	}
	
	
	
}
