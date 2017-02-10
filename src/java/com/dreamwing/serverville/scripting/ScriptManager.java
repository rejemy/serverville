package com.dreamwing.serverville.scripting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.agent.AgentShared;
import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.OnlineUser;
import com.dreamwing.serverville.scripting.ScriptEngineContext.ClientMethodInfo;
import com.dreamwing.serverville.util.FileUtil;


public class ScriptManager
{
	public static final int MAX_ENGINES=4;
	
	private static final Logger l = LogManager.getLogger(ScriptManager.class);
	
	public static String EngineBaseSource;
	
	private static Semaphore EngineLock;
	private static ScriptEngineContext[] Engines;
	
	private static List<ScriptData> GlobalScriptCache;
	private static volatile int ScriptVersion=0;
	
	private static Map<String,ClientMethodInfo> ClientHandlers;
	private static Set<String> AgentHandlers;
	private static Set<String> CallbackHandlers;
	
	public static void init() throws ScriptException, SQLException, IOException
	{
		ClassLoader loader = ScriptManager.class.getClassLoader();
		EngineBaseSource = FileUtil.readStreamToString(loader.getResourceAsStream("javascript/engine.js"), StandardCharsets.UTF_8);
		
		Engines = new ScriptEngineContext[MAX_ENGINES];
		EngineLock = new Semaphore(MAX_ENGINES, true);

		scriptsUpdated();
	}
	
	/*
	public static void start()
	{
		if(ClusterManager.isSeniorMember())
		{
			// Should only happen on one server
			doGlobalInit();
		}
	}
	*/
	
	public static ScriptEngineContext getEngine()
	{
		ScriptEngineContext engine = getEngineContext();
		
		try {
			engine.init(ScriptVersion);
		} catch (Exception e) {
			l.error("Error creating new engine", e);
			EngineLock.release();
			return null;
		}
		
		return engine;
	}
	
	private static ScriptEngineContext getEngineContext()
	{
		try {
			EngineLock.acquire();
		} catch (InterruptedException e) {
			l.error("Interrupted waiting for engine context", e);
			return null;
		}
		try
		{
			synchronized(ScriptManager.class)
			{
				for(int i = 0; i<MAX_ENGINES; i++)
				{
					ScriptEngineContext info = Engines[i];
					
					if(info == null)
					{
						info = new ScriptEngineContext();
						Engines[i] = info;
					}
					else if(info.InUse)
					{
						continue;
					}
					
					info.InUse = true;
					return info;
				}
			}
		}
		finally
		{
			EngineLock.release();
		}
		
		l.error("No engine available, something is out of sync");
		
		return null;
	}
	
	public static void returnEngine(ScriptEngineContext engineInfo)
	{
		if(engineInfo == null)
			return;
		
		engineInfo.InUse = false;
		EngineLock.release();
	}
	
	public static synchronized void scriptsUpdated() throws ScriptException, SQLException
	{
		GlobalScriptCache = ScriptData.loadAll();
		ScriptVersion++;
		
		updateHandlerSets();
	}
	
	public synchronized static void doGlobalInit()
	{
		ScriptEngineContext eng = null;
		try
		{
			eng = getEngine();
			eng.invokeFunction("globalInit");
			l.info("Ran globalInit javascript");
		} catch (NoSuchMethodException e) {
			// No function, no problem
		} catch (ScriptException e) {
			l.error("Error in global init javascript:", e);
		}
		finally
		{
			returnEngine(eng);
		}
	}
	
	public static List<ScriptData> getUserScripts()
	{
		return GlobalScriptCache;
	}
	
	private static void updateHandlerSets()
	{
		ScriptEngineContext ctx = new ScriptEngineContext();
		
		try {
			ctx.init(ScriptVersion);
		} catch (Exception e) {
			l.error("Couldn't update script handler sets due exception", e);
			return;
		}
		

		String[] agentHandlerList = ctx.getAgentHandlerList();
		String[] callbackHandlerList = ctx.getCallbackHandlerList();

		ClientHandlers = ctx.getClientHandlerList();
		
		
		Set<String> agentHandlers = new HashSet<String>();
		if(agentHandlerList != null)
		{
			for(String handlerName : agentHandlerList)
			{
				agentHandlers.add(handlerName);
			}
		}
		
		AgentHandlers = agentHandlers;
		
		
		Set<String> callbackHandlers = new HashSet<String>();
		if(callbackHandlerList != null)
		{
			for(String handlerName : callbackHandlerList)
			{
				callbackHandlers.add(handlerName);
			}
		}
		
		CallbackHandlers = callbackHandlers;
		
	}
	
	public static ClientMethodInfo getClientHandlerInfo(String apiType)
	{
		return ClientHandlers.get(apiType);
	}
	
	public static boolean hasAgentHandler(String apiType)
	{
		return AgentHandlers.contains(apiType);
	}
	
	public static boolean hasCallbackHandler(String apiType)
	{
		return CallbackHandlers.contains(apiType);
	}
	
	public static void onListenToChannel(Channel channel, OnlineUser listener)
	{
		if(!hasCallbackHandler("onListenToChannel"))
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onListenToChannel", channel.getId(), listener.getId());
		} catch (Exception e) {
			l.error("Error executing onListenToChannel handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}
	
	public static void onStopListenToChannel(Channel channel, OnlineUser listener)
	{
		if(!hasCallbackHandler("onStopListenToChannel"))
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onStopListenToChannel", channel.getId(), listener.getId());
		} catch (Exception e) {
			l.error("Error executing onListenToChannel handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}
	
	public static void onUserSignIn(ClientConnectionHandler connection)
	{
		if(!hasCallbackHandler("onUserSignIn"))
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onUserSignIn", AgentShared.userToUserInfo(connection.getUser()), connection.getPresence() != null);
		} catch (Exception e) {
			l.error("Error executing onUserSignIn handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}
	
	public static void onUserSignOut(ClientConnectionHandler connection)
	{
		if(!hasCallbackHandler("onUserSignOut"))
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onUserSignOut", AgentShared.userToUserInfo(connection.getUser()), connection.getPresence() != null);
		} catch (Exception e) {
			l.error("Error executing onUserSignIn handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}

}
