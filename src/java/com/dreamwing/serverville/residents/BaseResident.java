package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.data.KeyDataItem;

public abstract class BaseResident
{
	protected String Id;
	private ConcurrentMap<String,KeyDataItem> TransientState;
	
	public BaseResident(String id)
	{
		Id = id;
		TransientState = new ConcurrentHashMap<String,KeyDataItem>();
	}
	
	public String getId() { return Id; }
	
	public void destroy() {}
	
	public void setTransientState(String key, KeyDataItem value)
	{
		TransientState.put(key, value);
	}
	
	public KeyDataItem getTransientState(String key)
	{
		return TransientState.get(key);
	}
	
	public void clearTransientState(String key)
	{
		TransientState.remove(key);
	}
	
}
