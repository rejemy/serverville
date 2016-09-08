package com.dreamwing.serverville.scripting;

import java.sql.SQLException;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.agent.AgentMessages.UserInfoReply;
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
	
	private ScriptObjectMirror JsonApi;
	
	private ScriptObjectMirror ClientHandlers;
	private ScriptObjectMirror AgentHandlers;
	private ScriptObjectMirror CallbackHandlers;
	
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
		
		JsonApi = (ScriptObjectMirror)Engine.get("JSON");
		
		Engine.put("api", new AgentScriptAPI(this));
		addEnum("JsonDataType", JsonDataType.class);
		
		try
		{
			Engine.eval(ScriptManager.EngineBaseSource);
		} catch (Exception e)
		{
			l.error("Couldn't setup javascript engine due to exception in engine.js", e);
			throw new ScriptLoadException("engine.js", e);
		}
		
		for(ScriptData script : userScripts)
		{
			try
			{
				Engine.eval(script.ScriptSource);
			} catch (Exception e)
			{
				l.error("Couldn't setup javascript engine due to exception in "+script.Id, e);
				throw new ScriptLoadException(script.Id, e);
			}
		}
		
		
		ClientHandlers = (ScriptObjectMirror) Engine.get("client");
		AgentHandlers = (ScriptObjectMirror) Engine.get("agent");
		CallbackHandlers = (ScriptObjectMirror) Engine.get("callbacks");
		
		try {
			Engine.invokeFunction("localInit");
		} catch (NoSuchMethodException e) {
			// No function, no problem
		} catch (Exception e) {
			l.error("Error executing script localInit: ", e);
			throw new ScriptLoadException("<main>", e);
		}
	}
	
	private void addEnum(String javaName, Class<? extends Enum<?>> enumClass)
	{
		ScriptObjectMirror enumHolder = null;
		try {
			enumHolder = (ScriptObjectMirror)Engine.eval("var "+javaName+" = {}; "+javaName+";\n");
		} catch (Exception e) {
			l.error("Error creating enum "+javaName, e);
			return;
		}
		Enum<?>[] consts = enumClass.getEnumConstants();
		for(Enum<?> enumMember : consts)
		{
			String name = enumMember.toString();
			enumHolder.put(name, enumMember);
		}
	}
	
	private void makeNashornEngine()
	{
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
	
	public Object getClientHandler(String api)
	{
		return ClientHandlers.get(api);
	}
	
	public Object getCallbackHandler(String api)
	{
		return CallbackHandlers.get(api);
	}
	
	public Object invokeFunction(String name, Object... args)
	        throws ScriptException, NoSuchMethodException
    {
		return Engine.invokeFunction(name, args);
    }
	
	public Object decodeJSON(String json) throws ScriptException
	{
		try
		{
			return Engine.invokeMethod(JsonApi, "parse", json);
		}
		catch(NoSuchMethodException e)
		{
			l.error("No parse method on JSON object, what the what?");
			return null;
		}
	}
	
	public String encodeJSON(Object value) throws ScriptException
	{
		try
		{
			return (String)Engine.invokeMethod(JsonApi, "stringify", value);
		}
		catch(NoSuchMethodException e)
		{
			l.error("No stringify method on JSON object, what the what?");
			return null;
		}
	}

	public String[] getClientHandlerList()
	{
		if(ClientHandlers == null)
			return null;
		
		return ClientHandlers.getOwnKeys(false);
	}
	

	public Object invokeClientHandler(String apiId, String jsonInput, UserInfoReply user) throws NoSuchMethodException, ScriptException, JsonApiException
	{
		Object decodedInput = decodeJSON(jsonInput);
		
		try
		{
			Object result = Engine.invokeMethod(ClientHandlers, apiId, decodedInput, user);
			return ScriptObjectMirror.wrapAsJSONCompatible(result, null);
		}
		catch(ScriptException e)
		{
			l.error("Error executing client handler "+apiId, e);
			throw new JsonApiException(ApiErrors.JAVASCRIPT_ERROR, e.getMessage());
		}
		
	}

	
	public String[] getAgentHandlerList()
	{
		if(AgentHandlers == null)
			return null;
		
		return AgentHandlers.getOwnKeys(false);
	}
	

	public Object invokeAgentHandler(String apiId, String jsonInput) throws NoSuchMethodException, ScriptException, JsonApiException
	{
		Object decodedInput = decodeJSON(jsonInput);
		
		try
		{
			Object result = Engine.invokeMethod(AgentHandlers, apiId, decodedInput);
			return ScriptObjectMirror.wrapAsJSONCompatible(result, null);
		}
		catch(ScriptException e)
		{
			l.error("Error executing agent handler "+apiId, e);
			throw new JsonApiException(ApiErrors.JAVASCRIPT_ERROR, e.getMessage());
		}
	}
	
	public String[] getCallbackHandlerList()
	{
		if(CallbackHandlers == null)
			return null;
		
		return CallbackHandlers.getOwnKeys(false);
	}
	
	public Object invokeCallbackHandler(String handlerName, final Object... args) throws NoSuchMethodException, ScriptException
	{
		return Engine.invokeMethod(CallbackHandlers, handlerName, args);
	}
	
	public Object onListenToChannelHandler(String channelId, String listenerId) throws NoSuchMethodException, ScriptException
	{
		return Engine.invokeMethod(CallbackHandlers, "onListenToChannel", channelId, listenerId);
	}
	
	public Object onStopListenToChannelHandler(String channelId, String listenerId) throws NoSuchMethodException, ScriptException
	{
		return Engine.invokeMethod(CallbackHandlers, "onStopListenToChannel", channelId, listenerId);
	}

}
