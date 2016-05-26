package com.dreamwing.serverville.test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import com.dreamwing.serverville.client.ClientMessages.*;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.client.ClientSocketInitializer;
import com.dreamwing.serverville.net.ApiError;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.Assert;

public class ClientTests {

	private static class TestUser
	{
		public String UserId=null;
		public String Username=null;
		public String Email=null;
		public String Password=null;
		public String SessionId=null;
	}
	
	private TestUser User1 = new TestUser();
	
	@SuppressWarnings({ "rawtypes" })
	private <S> S makeClientCall(TestUser user, String api, Object request, TypeReference valueTypeRef) throws IOException, JsonApiException
	{
		return HttpHelpers.postClientApi(ClientSocketInitializer.URL+api, user.SessionId, request, valueTypeRef);
	}
	
	private Random Rand = new Random();
	
	@Test(order=1)
	public void InvalidAPI() throws IOException
	{
		String url = ClientSocketInitializer.URL+"api/lskdjioif";
		ApiError result = HttpHelpers.getJson(url, ApiError.class);
		Assert.assertNotNull(result);
		Assert.assertTrue(result.isError);
	}
	
	@Test(order=2)
	public void CreateUserAccount() throws IOException, JsonApiException
	{
		CreateAccount request = new CreateAccount();
		request.username = "testUser_"+SVID.makeSVID();
		request.email = request.username+"@serverville.com";
		request.password = Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong());
		
		UserAccountInfo reply = makeClientCall(User1, "api/CreateAccount", request, new TypeReference<UserAccountInfo>(){});
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.user_id);
		Assert.assertNotNull(reply.session_id);
		
		User1.UserId = reply.user_id;
		User1.Username = request.username;
		User1.Email = request.email;
		User1.SessionId = reply.session_id;
		User1.Password = request.password;
	}
	
	@Test(order=3)
	public void InvalidSignIn() throws IOException, JsonApiException
	{
		User1.SessionId = null;
		
		SignIn request = new SignIn();
		request.username = "testUser_"+SVID.makeSVID();
		request.password = Long.toHexString(Rand.nextLong())+Long.toHexString(Rand.nextLong());
		
		boolean gotError = false;
		try
		{
			makeClientCall(User1, "api/SignIn", request, new TypeReference<UserAccountInfo>(){});
		} catch (JsonApiException e) {
			gotError = true;
		}
		Assert.assertTrue(gotError);
	}
	
	@Test(order=4)
	public void UsernameSignIn() throws IOException, JsonApiException
	{
		User1.SessionId = null;
		
		SignIn request = new SignIn();
		request.username = User1.Username;
		request.password = User1.Password;
		
		UserAccountInfo reply = makeClientCall(User1, "api/SignIn", request, new TypeReference<UserAccountInfo>(){});
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.user_id);
		Assert.assertNotNull(reply.session_id);
	}
	
	@Test(order=5)
	public void EmailSignIn() throws IOException, JsonApiException
	{
		User1.SessionId = null;
		
		SignIn request = new SignIn();
		request.email = User1.Email;
		request.password = User1.Password;
		
		UserAccountInfo reply = makeClientCall(User1, "api/SignIn", request, new TypeReference<UserAccountInfo>(){});
		Assert.assertNotNull(reply);
		Assert.assertNotNull(reply.user_id);
		Assert.assertNotNull(reply.session_id);
	}
	
	@Test(order=100)
	public void Cleanup() throws SQLException
	{
		ServervilleUser tempUser1 = ServervilleUser.findById(User1.UserId);
		Assert.assertNotNull(tempUser1);
		tempUser1.delete();
	}
}
