package com.dreamwing.serverville.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.data.ServervilleUser;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;

public class HttpRequestInfo
{
	public HttpConnectionInfo Connection;
	public URI RequestURI;
	public FullHttpRequest Request;
	public String RequestId;
	public String PathRemainder;
	public Map<String,List<String>> QueryParams;
	public String BodyString;
	public Map<String,List<String>> FormBody;
	
	public void init(HttpConnectionInfo conn, FullHttpRequest request, String requestId) throws URISyntaxException
	{
		URI uri = new URI(request.uri());
		
		Connection = conn;
		Request = request;
		RequestURI = uri;
		RequestId = requestId;
		
		if(uri.getQuery() != null)
		{
			QueryStringDecoder decoder = new QueryStringDecoder(uri);
			QueryParams = decoder.parameters();
		}
		
		String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
		if(contentType != null && contentType.equals("application/x-www-form-urlencoded") && request.content() != null)
		{
			String formBody = getBody();
			QueryStringDecoder decoder = new QueryStringDecoder(formBody, false);
			FormBody = decoder.parameters();
		}
	}
	
	public ServervilleUser getUser()
	{
		return Connection.User;
	}
	
	public String getOneHeader(String header, String defValue)
	{
		String headerVal = Request.headers().get(header);
		if(headerVal != null)
			return headerVal;
		return defValue;
	}
	
	public String getOneQuery(String query) throws JsonApiException
	{
		if(QueryParams == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, query);
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, query);
		
		return queries.get(0);
	}
	
	public String getOneQuery(String query, String defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		return queries.get(0);
	}
	
	public int getOneQueryAsInt(String query, int defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Integer.parseInt(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public long getOneQueryAsLong(String query, long defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Long.parseLong(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public Long getOneQueryAsLong(String query, Long defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Long.parseLong(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public double getOneQueryAsDouble(String query, double defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Double.parseDouble(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public boolean getOneQueryAsBoolean(String query, boolean defValue)
	{
		if(QueryParams == null)
			return defValue;
		
		List<String> queries = QueryParams.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0).toLowerCase();
		if(val.equals("false"))
			return false;
		
		return true;
	}
	
	public String getBody()
	{
		if(BodyString == null)
			BodyString = Request.content().toString(StandardCharsets.UTF_8);
		
		return BodyString;
	}
	
	public String getOneBody(String query) throws JsonApiException
	{
		if(FormBody == null)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, query);
		
		List<String> queries = FormBody.get(query);
		if(queries == null || queries.size() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, query);
		
		return queries.get(0);
	}
	
	public String getOneBody(String query, String defValue)
	{
		if(FormBody == null)
			return defValue;
		
		List<String> queries = FormBody.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		return queries.get(0);
	}
	
	public int getOneBodyAsInt(String query, int defValue)
	{
		if(FormBody == null)
			return defValue;
		
		List<String> queries = FormBody.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Integer.parseInt(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public long getOneBodyAsLong(String query, long defValue)
	{
		if(FormBody == null)
			return defValue;
		
		List<String> queries = FormBody.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Long.parseLong(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
	
	public double getOneBodyAsDouble(String query, double defValue)
	{
		if(FormBody == null)
			return defValue;
		
		List<String> queries = FormBody.get(query);
		if(queries == null || queries.size() == 0)
			return defValue;
		
		String val = queries.get(0);
		
		try
		{
			return Double.parseDouble(val);
		}
		catch(Exception e)
		{
			return defValue;
		}
	}
}
