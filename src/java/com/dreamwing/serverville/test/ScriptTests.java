package com.dreamwing.serverville.test;

import java.util.Map;

import org.junit.Assert;

import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
import com.dreamwing.serverville.client.ClientMessages.DataItemReply;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.SVID;

public class ScriptTests {
	
	@Test(order=1)
	public void EngineCreate() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		ScriptManager.returnEngine(ctx);
	}
	
	@Test(order=2)
	public void EvalScript() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			ctx.eval("var a = 1;");
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=3)
	public void InvokeMethod() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			ctx.eval("var func1 = function(thing) { return thing+'.js'; };");
			String result = (String)ctx.invokeFunction("func1", "nose");
			Assert.assertEquals("nose.js", result);
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	private static String TestItemID = SVID.makeSVID();
	
	@Test(order=100)
	public void GetUserInfo() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			UserInfoReply result = (UserInfoReply)ctx.eval("api.getUserInfo({\"username\":\"admin\"});");
			Assert.assertNotNull(result);
			Assert.assertEquals(result.username, "admin");
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=101)
	public void setDataKey() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			Double result = (Double)ctx.eval("api.setDataKey(\""+TestItemID+"\", \"testkey1\", 1024);");
			Assert.assertNotNull(result);
			Assert.assertTrue(result > 0.0);
			
			result = (Double)ctx.eval("api.setDataKey(\""+TestItemID+"\", \"testkey1\", \"999\", \"number\");");
			Assert.assertNotNull(result);
			Assert.assertTrue(result > 0.0);
			
			result = (Double)ctx.eval("api.setDataKey(\""+TestItemID+"\", \"complex\", [{\"stuff\":1},{\"stiff\":2}]);");
			Assert.assertNotNull(result);
			Assert.assertTrue(result > 0.0);
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=102)
	public void setDataKeys() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			Double result = (Double)ctx.eval("api.setDataKeys(\""+TestItemID+"\", [{\"key\":\"testkey1\", \"value\":700},{\"key\":\"testkey2\", \"value\":\"stuff\", \"data_type\":\"string\"}]);");
			Assert.assertNotNull(result);
			Assert.assertTrue(result > 0.0);
			
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=103)
	public void getDataKey() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			DataItemReply result = (DataItemReply)ctx.eval("api.getDataKey(\""+TestItemID+"\", \"testkey1\");");
			Assert.assertNotNull(result);
			Assert.assertEquals(TestItemID, result.id);
			Assert.assertEquals("testkey1", result.key);
			Assert.assertEquals(700, result.value);
			
			ctx.eval("var result = api.getDataKey(\""+TestItemID+"\", \"complex\");");
			result = (DataItemReply)ctx.eval("result;");
			Assert.assertNotNull(result);
			Assert.assertEquals(TestItemID, result.id);
			Assert.assertEquals("complex", result.key);
			Object innerValue = ctx.eval("result.value[1].stiff;");
			Assert.assertEquals(2, innerValue);
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=104)
	public void getDataKeys() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			@SuppressWarnings("unchecked")
			Map<String,DataItemReply> result = (Map<String,DataItemReply>)ctx.eval("api.getDataKeys(\""+TestItemID+"\", [\"testkey1\",\"testkey2\"]);");
			Assert.assertNotNull(result);
			Assert.assertEquals(2, result.size());
			
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	
	@Test(order=105)
	public void newData() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			ctx.eval("var testData = new KeyData(\""+TestItemID+"\");");
			ctx.eval("testData.set(\"key1\", \"Noooo\");");
			ctx.eval("testData.save();");

		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	
	@Test(order=106)
	public void loadData() throws Exception
	{
		ScriptEngineContext ctx = ScriptManager.getEngine();
		Assert.assertNotNull(ctx);
		
		try
		{
			ctx.eval("var testData = KeyData.load(\""+TestItemID+"\");");
			Object result = ctx.eval("testData.data.key1");
			Assert.assertEquals("Noooo", result);
		}
		finally
		{
			ctx.reset();
			ScriptManager.returnEngine(ctx);
		}
	}
	

	
}
