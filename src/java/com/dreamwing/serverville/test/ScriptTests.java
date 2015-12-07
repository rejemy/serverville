package com.dreamwing.serverville.test;

import org.junit.Assert;


import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptManager;

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
}
