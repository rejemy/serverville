package com.dreamwing.serverville.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.agent.AgentShared;
import com.dreamwing.serverville.net.ApiError;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ClientDispatcher {
	
	private static final Logger l = LogManager.getLogger(ClientDispatcher.class);
	
	private class DispatchMethod
	{
		public boolean Authenticate;
		public Method ClassMethod;
		public Class<?> RequestClass;
	}
	
	private Map<String, DispatchMethod> Methods;
	
	public ClientDispatcher()
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
			if(params.length != 2)
				continue;
			
			if(!params[1].equals(ClientMessageInfo.class))
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
		action.Authenticate = true;
		
		ClientHandlerOptions options = method.getAnnotation(ClientHandlerOptions.class);
		if(options != null)
		{
			action.Authenticate = options.auth();
		}
		
		Methods.put(method.getName(), action);
	}
	

	
	public Object dispatch(String messageType, String messageData, ClientMessageInfo info)
	{
		try
		{
			return invokeMethod(messageType, messageData, info);
		}
		catch(JsonProcessingException e)
		{
			return new ApiError(ApiErrors.JSON_ERROR, e.getMessage());
		}
		catch(SQLException e)
		{
			l.warn("Database error:", e);
			return new ApiError(ApiErrors.DB_ERROR, e.getMessage());
		}
		catch(JsonApiException e)
		{
			return e.Error;
		}
		catch(Exception e)
		{
			l.error("Exception in client api method: ", e);
			return new ApiError(ApiErrors.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	private Object invokeMethod(String messageType, String messageData, ClientMessageInfo info) throws Exception
	{
		// Check if we have an override for it in a script
		Boolean hasScript = ScriptManager.hasClientHandler(messageType);
		if(hasScript == null)
		{
			// Not defined in script
			DispatchMethod method = Methods.get(messageType);
			if(method == null)
				throw new JsonApiException(ApiErrors.UNKNOWN_API);
			
			if(method.Authenticate && info.User == null)
				throw new JsonApiException(ApiErrors.NOT_AUTHED);
			
			Object requestObj = JSON.deserialize(messageData, method.RequestClass);
			
			Object reply = null;
			
			try
			{
				reply = method.ClassMethod.invoke(null, requestObj, info);
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
		else if(hasScript == true)
		{
			// Defined and there is a script
			// All script methods require authentication
			if(info.User == null)
				throw new JsonApiException(ApiErrors.NOT_AUTHED);
			
			ScriptEngineContext context = ScriptManager.getEngine();
			try
			{
				return context.invokeClientHandler(messageType, messageData, AgentShared.userToUserInfo(info.User));
			}
			finally
			{
				ScriptManager.returnEngine(context);
			}
		}
		else
		{
			// Defined but it's a null script
			throw new JsonApiException(ApiErrors.UNKNOWN_API);
		}
	}
}
