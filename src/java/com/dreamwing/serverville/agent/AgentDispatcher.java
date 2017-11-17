package com.dreamwing.serverville.agent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.JSON;

import io.netty.buffer.ByteBuf;


public class AgentDispatcher
{	
	private class DispatchMethod
	{
		public Method ClassMethod;
		public Class<?> RequestClass;
	}
	
	private Map<String, DispatchMethod> Methods;
	
	public AgentDispatcher()
	{
		Methods = new HashMap<String, DispatchMethod>();
	}
	
	public void addAllMethods(Class<?> classType) throws Exception
	{

		Method methods[] = classType.getMethods();
		for(Method method : methods)
		{
			int mods = method.getModifiers();
			if(!Modifier.isStatic(mods) || !Modifier.isPublic(mods))
				continue;
			
			Class<?> params[] = method.getParameterTypes();
			if(params.length != 1)
				continue;
			
			addMethodInternal(method);
		}
	}
	
	private void addMethodInternal(Method method) throws Exception
	{

		DispatchMethod action = new DispatchMethod();
		action.ClassMethod = method;
		
		Class<?> params[] = method.getParameterTypes();
		
		action.RequestClass = params[0];

		
		Methods.put(method.getName(), action);
	}
	
	public String dispatch(String messageType, String messageNum, String messageData) throws Exception
	{
		Object reply = invokeMethod(messageType, messageData);
		
		if(reply == null)
			return null;
		
		String messageStr = ":"+messageNum+":"+JSON.serializeToString(reply);
		
		return messageStr;
	}
	
	public ByteBuf dispatch(String messageType, String messageData) throws Exception
	{
		Object reply = invokeMethod(messageType, messageData);

		return JSON.serializeToByteBuf(reply);
	}
	
	private Object invokeMethod(String messageType, String messageData) throws Exception
	{
		DispatchMethod method = Methods.get(messageType);
		if(method == null)
		{
			if(ScriptManager.hasAgentHandler(messageType))
			{
				ScriptEngineContext context = ScriptManager.getEngine();
				try
				{
					return context.invokeAgentHandler(messageType, messageData);
				}
				finally
				{
					ScriptManager.returnEngine(context);
				}
			}
			else
			{
				throw new JsonApiException(ApiErrors.UNKNOWN_API);
			}
		}
		
		Object requestObj = JSON.deserialize(messageData, method.RequestClass);
		
		Object reply = null;
		
		try
		{
			reply = method.ClassMethod.invoke(null, requestObj);
		}
		catch(InvocationTargetException e)
		{
			if(e.getCause() != null && e.getCause() instanceof Exception)
			{
				throw (Exception)e.getCause();
			}
			else
			{
				throw e;
			}
		}
		
		return reply;
	}
}
