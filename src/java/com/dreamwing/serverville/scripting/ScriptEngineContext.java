package com.dreamwing.serverville.scripting;

import java.sql.SQLException;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.data.ServervilleUser;

import com.dreamwing.serverville.agent.AgentScriptAPI;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.NashornScriptEngine;

@SuppressWarnings("restriction")
public class ScriptEngineContext {

	private static final Logger l = LogManager.getLogger(ScriptEngineContext.class);
	
	private NashornScriptEngine Engine;
	public volatile boolean InUse=false;
	private int Version;
	
	private ScriptObjectMirror JsonDecoder;
	
	private ScriptObjectMirror ClientHandlers;
	private ScriptObjectMirror AdminHandlers;
	private ScriptObjectMirror AgentHandlers;
	
	public ScriptEngineContext()
	{

	}
	
	// Inits by loading scripts from database if required
	public void init(int version) throws SQLException, ScriptLoadException
	{
		if(version == Version && Engine != null)
			return;
		
		Version = version;
		
		List<ScriptData> userScripts = null;
		try {
			userScripts = ScriptData.loadAll();
		} catch (SQLException e) {
			l.error("Couldn't create javascript engine due to error loading scripts:", e);
			throw e;
		}
		
		init(userScripts);
	}
	
	// Init with the list of provided scripts
	public void init(List<ScriptData> userScripts) throws ScriptLoadException
	{
		makeNashornEngine();
		
		ScriptObjectMirror jsonAPI = (ScriptObjectMirror)Engine.get("JSON");
		JsonDecoder = (ScriptObjectMirror)jsonAPI.get("parse");
		
		try
		{
			Engine.eval(ScriptManager.EngineBaseSource);
		} catch (ScriptException e)
		{
			l.error("Couldn't setup javascript engine due to exception in engine.js", e);
			throw new ScriptLoadException("engine.js", e);
		}
		
		Engine.put("api", new AgentScriptAPI());
		
		for(ScriptData script : userScripts)
		{
			try
			{
				Engine.eval(script.ScriptSource);
			} catch (ScriptException e)
			{
				l.error("Couldn't setup javascript engine due to exception in "+script.Id, e);
				throw new ScriptLoadException(script.Id, e);
			}
		}
		
		
		ClientHandlers = (ScriptObjectMirror) Engine.get("client");
		AdminHandlers = (ScriptObjectMirror) Engine.get("admin");
		AgentHandlers = (ScriptObjectMirror) Engine.get("agent");
		
		
		try {
			Engine.invokeFunction("localInit");
		} catch (NoSuchMethodException e) {
			// No function, no problem
		} catch (ScriptException e) {
			l.error("Error executing script localInit: ", e);
			throw new ScriptLoadException("<main>", e);
		}
	}
	
	private void makeNashornEngine()
	{
		if(Engine != null)
			return;
		
		ClassLoader loader = ScriptEngineContext.class.getClassLoader();
		
		ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine(new String[] {"--no-syntax-extensions", "--no-java"}, loader, new ScriptClassFilter());
		
		Engine = (NashornScriptEngine)engine;
	}
	

	// Resets engine to pristine state
	public void reset() throws SQLException, ScriptLoadException
	{
		Engine = null;
		init(Version);
	}
	
	public Object eval(String script) throws ScriptException
	{
		return Engine.eval(script);
	}
	
	public Object getValue(String key)
	{
		return Engine.get(key);
	}
	
	public Object invokeFunction(String name, Object... args)
	        throws ScriptException, NoSuchMethodException
    {
		return Engine.invokeFunction(name, args);
    }

	public String[] getClientHandlerList()
	{
		if(ClientHandlers == null)
			return null;
		
		return ClientHandlers.getOwnKeys(false);
	}
	

	public Object invokeClientHandler(String apiId, String jsonInput, ServervilleUser user) throws NoSuchMethodException, ScriptException
	{
		Object decodedInput = JsonDecoder.call(null, jsonInput);
		
		try
		{
			Object result = Engine.invokeMethod(ClientHandlers, apiId, decodedInput, user);
			return ScriptObjectMirror.wrapAsJSONCompatible(result, null);
		}
		catch(Exception e)
		{
			l.error("Script error calling client API: "+apiId, e);
			throw e;
		}
		
	}
	
	public String[] getAdminHandlerList()
	{
		if(AdminHandlers == null)
			return null;
		
		return AdminHandlers.getOwnKeys(false);
	}
	

	public Object invokeAdminHandler(String apiId, String jsonInput, ServervilleUser user) throws Exception
	{
		Object decodedInput = JsonDecoder.call(null, jsonInput);
		
		try
		{
			Object result = Engine.invokeMethod(AdminHandlers, apiId, decodedInput, user);
			return ScriptObjectMirror.wrapAsJSONCompatible(result, null);
		}
		catch(Exception e)
		{
			l.error("Script error calling client API: "+apiId, e);
			throw e;
		}
	}
	
	
	public String[] getAgentHandlerList()
	{
		if(AgentHandlers == null)
			return null;
		
		return AgentHandlers.getOwnKeys(false);
	}
	

	public Object invokeAgentHandler(String apiId, String jsonInput) throws Exception
	{
		Object decodedInput = JsonDecoder.call(null, jsonInput);
		
		try
		{
			Object result = Engine.invokeMethod(AgentHandlers, apiId, decodedInput);
			return ScriptObjectMirror.wrapAsJSONCompatible(result, null);
		}
		catch(Exception e)
		{
			l.error("Script error calling client API: "+apiId, e);
			throw e;
		}
	}
	

}
