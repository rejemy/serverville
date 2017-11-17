package com.dreamwing.serverville.net;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class HttpDispatcher
{

	private abstract class DispatchAction
	{
		public abstract ChannelFuture dispatch(String uriMatched, HttpRequestInfo req) throws Exception;
	}

	private class InvokeDispatchAction extends DispatchAction
	{
		public Object Instance;
		public Method InvokeMethod;
		public boolean HasInput;
		public boolean Authenticate;
		
		@Override
		public ChannelFuture dispatch(String uriMatched, HttpRequestInfo req) throws Exception
		{
			if(Authenticate)
			{
				if(!Authenticator.isAuthenticated(req))
				{
					return HttpHelpers.sendError(req, ApiErrors.BAD_AUTH);
				}
			}
			
			Object result = null;
			
			req.PathRemainder = req.RequestURI.getPath().substring(uriMatched.length());
			
			try
			{
				if(HasInput)
				{
					result = InvokeMethod.invoke(Instance, req, null);
				}
				else
				{
					result = InvokeMethod.invoke(Instance, req);
				}
			} catch (InvocationTargetException e) {
				if(e.getCause() != null && e.getCause() instanceof Exception)
				{
					throw (Exception)e.getCause();
				}
				else
				{
					throw e;
				}
			}
			
			return (ChannelFuture)result;
		}
	}
	
	private class RedirectDispatchAction extends DispatchAction
	{
		public String RedirectTo;
		
		@Override
		public ChannelFuture dispatch(String uriMatched, HttpRequestInfo req)
		{
			return HttpHelpers.sendRedirect(req, RedirectTo);
		}
		
	}
	
	private class DispatchTable
	{
		public Map<String, DispatchAction> ExactMatches;
		public LinkedHashMap<String, DispatchAction> PrefixMatches;
		
		public DispatchTable()
		{
			ExactMatches = new HashMap<String, DispatchAction>();
			PrefixMatches = new LinkedHashMap<String, DispatchAction>();
		}
		
		public void add(String uri, DispatchAction action)
		{
			if(uri.endsWith("*"))
			{
				uri = uri.substring(0, uri.length()-1);
				PrefixMatches.put(uri, action);
			}
			else
			{
				ExactMatches.put(uri, action);
			}
		}
	}
	
	private APIAuthenticator Authenticator;
	private DispatchTable GetTable;
	private DispatchTable PostTable;
	private DispatchTable PutTable;
	private DispatchTable DeleteTable;
	
	public HttpDispatcher(APIAuthenticator authenticator)
	{
		Authenticator = authenticator;
		GetTable = new DispatchTable();
		PostTable = new DispatchTable();
		PutTable = new DispatchTable();
		DeleteTable = new DispatchTable();
	}
	
	private DispatchTable getDispatchTable(HttpHandlerOptions.Method method)
	{
		switch(method)
		{
			case GET:
				return GetTable;
			case POST:
				return PostTable;
			case PUT:
				return PutTable;
			case DELETE:
				return DeleteTable;
		}
		return null;
	}
	
	public void addRedirect(String from, String to)
	{
		RedirectDispatchAction action = new RedirectDispatchAction();
		action.RedirectTo = to;
		GetTable.add(from, action);
		PostTable.add(from, action);
		PutTable.add(from, action);
		DeleteTable.add(from, action);
	}
	
	private Method getMethod(Class<?> classType, String methodName)
	{

		try {
			return classType.getMethod(methodName, HttpRequestInfo.class);
		} catch (NoSuchMethodException | SecurityException e) {}
		
		try {
			return classType.getMethod(methodName, HttpRequestInfo.class, String.class);
		} catch (NoSuchMethodException | SecurityException e) {}
		
		return null;
	}
	
	public void addMethod(String uri, Object instance, String methodName) throws Exception
	{
		Class<?> classType = instance.getClass();
		
		Method method = getMethod(classType, methodName);
		
		if(Modifier.isStatic(method.getModifiers()))
		{
			throw new Exception("Method "+methodName+" is static");
		}
		
		if(!Modifier.isPublic(method.getModifiers()))
		{
			throw new Exception("Method "+method.getName()+" isn't public");
		}
		
		HttpHandlerOptions options = method.getAnnotation(HttpHandlerOptions.class);
		if(options == null)
		{
			throw new Exception("Method "+method.getName()+" doesn't have HttpHandlerOptions");
		}
		
		addMethodInternal(method, instance, uri);
	}
	
	public void addStaticMethod(String uri, Class<?> classType, String methodName) throws Exception
	{
		Method method = getMethod(classType, methodName);
		
		if(!Modifier.isStatic(method.getModifiers()))
		{
			throw new Exception("Method "+methodName+" isn't static");
		}
		
		if(!Modifier.isPublic(method.getModifiers()))
		{
			throw new Exception("Method "+method.getName()+" isn't public");
		}
		
		HttpHandlerOptions options = method.getAnnotation(HttpHandlerOptions.class);
		if(options == null)
		{
			throw new Exception("Method "+method.getName()+" doesn't have HttpHandlerOptions");
		}
		
		addMethodInternal(method, null, uri);
	}
	
	private void addMethodInternal(Method method, Object instance, String uri) throws Exception
	{
		HttpHandlerOptions options = method.getAnnotation(HttpHandlerOptions.class);
		if(options == null)
			return;
		
		InvokeDispatchAction action = new InvokeDispatchAction();
		
		Class<?> params[] = method.getParameterTypes();
		if(params.length == 1 && params[0] == HttpRequestInfo.class)
		{
			action.HasInput = false;
		}
		else if(params.length == 2 && params[0] == HttpRequestInfo.class && !params[1].isPrimitive())
		{
			action.HasInput = true;
		}
		else
		{
			throw new Exception("Handler parameters for "+method.getName()+" aren't recognized");
		}
		
		
		action.Instance = instance;
		action.InvokeMethod = method;
		action.Authenticate = options.auth();
		
		if(action.Authenticate && Authenticator == null)
			throw new Exception("API method "+method.getName()+" requries authentication, but dispatcher has not auth handler");
		
		DispatchTable table = getDispatchTable(options.method());
		table.add(uri, action);

	}
	
	public void addAllMethods(String uriPrefix, Object instance) throws Exception
	{
		Class<?> classType = instance.getClass();
		
		Method methods[] = classType.getMethods();
		for(Method method : methods)
		{
			int mods = method.getModifiers();
			if(Modifier.isStatic(mods) || !Modifier.isPublic(mods))
				continue;
			
			addMethodInternal(method, instance, uriPrefix+method.getName());
		}
	}
	
	public void addAllStaticMethods(String uriPrefix, Class<?> classType) throws Exception
	{
		Method methods[] = classType.getMethods();
		for(Method method : methods)
		{
			int mods = method.getModifiers();
			if(!Modifier.isStatic(mods) || !Modifier.isPublic(mods))
				continue;
			
			addMethodInternal(method, null, uriPrefix+method.getName());
		}
	}
	
	public ChannelFuture dispatch(HttpRequestInfo req) throws Exception
	{
		HttpMethod reqMethod = req.Request.method();
		
		DispatchTable table = null;
		
		if(reqMethod == HttpMethod.GET)
		{
			table = GetTable;
		}
		else if(reqMethod == HttpMethod.POST)
		{
			table = PostTable;
		}
		else if(reqMethod == HttpMethod.PUT)
		{
			table = PutTable;
		}
		else if(reqMethod == HttpMethod.DELETE)
		{
			table = DeleteTable;
		}
		
		if(table == null)
			return HttpHelpers.sendError(req, ApiErrors.BAD_HTTP_METHOD);
		
		String path = req.RequestURI.getPath();
		
		DispatchAction action = table.ExactMatches.get(path);
		if(action != null)
		{
			return action.dispatch(path, req);
		}
		
		for(Map.Entry<String, DispatchAction> entry : table.PrefixMatches.entrySet())
		{
			if(path.startsWith(entry.getKey()))
			{
				return entry.getValue().dispatch(entry.getKey(), req);
			}
		}
		return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
	}
}
