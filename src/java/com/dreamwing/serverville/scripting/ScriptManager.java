package com.dreamwing.serverville.scripting;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.MessageListener;
import com.dreamwing.serverville.util.FileUtil;


public class ScriptManager
{
	public static final int MAX_ENGINES=4;
	
	private static final Logger l = LogManager.getLogger(ScriptManager.class);
	
	public static String EngineBaseSource;
	
	private static Semaphore EngineLock;
	private static ScriptEngineContext[] Engines;
	
	private static volatile int ScriptVersion=0;
	
	private static Map<String,Boolean> ClientHandlers;
	private static Set<String> AgentHandlers;
	private static Set<String> CallbackHandlers;
	private static boolean HasListenToChannelHandler;
	private static boolean HasStopListenToChannelHandler;
	
	public static void init() throws Exception
	{
		ClassLoader loader = ScriptManager.class.getClassLoader();
		EngineBaseSource = FileUtil.readStreamToString(loader.getResourceAsStream("javascript/engine.js"), StandardCharsets.UTF_8);
		
		Engines = new ScriptEngineContext[MAX_ENGINES];
		EngineLock = new Semaphore(MAX_ENGINES, true);

		updateHandlerSets();
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeFunction("globalInit");
		} catch (NoSuchMethodException e) {
			// No function, no problem
		} catch (Exception e) {
			l.error("Error executing script globalInit: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}
	
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
		EngineLock.release();
		
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
	
	public static void scriptsUpdated() throws ScriptException
	{
		ScriptVersion++;
		
		ScriptEngineContext eng = null;
		try
		{
			eng = getEngine();
			eng.invokeFunction("globalInit");
		} catch (NoSuchMethodException e) {
			// No function, no problem
		}
		finally
		{
			returnEngine(eng);
		}
		
		updateHandlerSets();
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
		
		Map<String,Boolean> clientHandlers = new HashMap<String,Boolean>();

		String[] clientHandlerList = ctx.getClientHandlerList();
		if(clientHandlerList != null)
		{
			for(String handlerName : clientHandlerList)
			{
				Object handler = ctx.getClientHandler(handlerName);
				if(handler == null)
				{
					// Nulled out API, so it can't be called by clients
					clientHandlers.put(handlerName, false);
				}
				else
				{
					clientHandlers.put(handlerName, true);
				}
				
			}
		}
		
		ClientHandlers = clientHandlers;
		
		
		Set<String> agentHandlers = new HashSet<String>();
		
		String[] agentHandlerList = ctx.getAgentHandlerList();
		if(agentHandlerList != null)
		{
			for(String handlerName : agentHandlerList)
			{
				agentHandlers.add(handlerName);
			}
		}
		
		AgentHandlers = agentHandlers;
		
		
		Set<String> callbackHandlers = new HashSet<String>();
		
		String[] callbackHandlerList = ctx.getCallbackHandlerList();
		if(callbackHandlerList != null)
		{
			for(String handlerName : callbackHandlerList)
			{
				callbackHandlers.add(handlerName);
			}
		}
		
		CallbackHandlers = callbackHandlers;
		
		HasListenToChannelHandler = ctx.getCallbackHandler("onListenToChannel") != null;
		HasStopListenToChannelHandler = ctx.getCallbackHandler("onStopListenToChannel") != null;
	}
	
	public static Boolean hasClientHandler(String apiType)
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
	
	public static void onListenToChannel(Channel channel, MessageListener listener)
	{
		if(!HasListenToChannelHandler)
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onListenToChannel", channel.getId(), listener.getId());
			//engine.onListenToChannelHandler(channel.getId(), listener.getId());
		} catch (Exception e) {
			l.error("Error executing onListenToChannel handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}
	
	public static void onStopListenToChannel(Channel channel, MessageListener listener)
	{
		if(!HasStopListenToChannelHandler)
			return;
		
		ScriptEngineContext engine = getEngine();
		try
		{
			engine.invokeCallbackHandler("onStopListenToChannel", channel.getId(), listener.getId());
			//engine.onStopListenToChannelHandler(channel.getId(), listener.getId());
		} catch (Exception e) {
			l.error("Error executing onListenToChannel handler: ", e);
		}
		finally
		{
			returnEngine(engine);
		}
	}

}
