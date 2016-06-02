package com.dreamwing.serverville.residents;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.TransientValuesChangeMessage;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class BaseResident
{
	private static final Logger l = LogManager.getLogger(BaseResident.class);
	
	protected String Id;
	
	protected ConcurrentMap<String,KeyDataItem> TransientValues;
	protected KeyDataItem MostRecentValue;
	
	protected ConcurrentMap<String,MessageListener> Listeners;
	
	
	public BaseResident(String id)
	{
		Id = id;
		TransientValues = new ConcurrentHashMap<String,KeyDataItem>();
		Listeners = new ConcurrentHashMap<String,MessageListener>();
		
	}
	
	public String getId() { return Id; }
	
	public void destroy()
	{
		ResidentManager.removeResident(this);
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onStoppedListeningTo(this);
		}
		
		Listeners.clear();
	}
	
	public void sendMessage(String messageType, Object messageObject)
	{
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(messageObject);
		} catch (JsonProcessingException e) {
			l.error("Error encoding directed message", e);
			return;
		}
		
		sendMessage(messageType, messageBody);
	}
	
	public void sendMessage(String messageType, String messageBody)
	{
		if(messageType == null || messageType.length() == 0 || messageType.charAt(0) == '_' || messageType.indexOf(':') >= 0)
			throw new IllegalArgumentException("Invalid message type: "+messageType);
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onMessage(messageType, messageBody, Id, null);
		}
	}
	
	
	public void setTransientValue(KeyDataItem value)
	{
		long currTime = System.currentTimeMillis();
		value.created = currTime;
		value.modified = currTime;
		
		KeyDataItem prev = TransientValues.put(value.key, value);
		updateTransientValueInList(prev, value);
		
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		
		changeMessage.values = new HashMap<String,Object>();
		try {
			changeMessage.values.put(value.key, value.asDecodedObject());
		} catch (Exception e) {
			l.error("Error decoding json object", e);
		}
		
		onStateChanged(changeMessage, currTime);
	}
	
	public void setTransientValues(Collection<KeyDataItem> values)
	{
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		changeMessage.values = new HashMap<String,Object>();
		
		long currTime = System.currentTimeMillis();
		
		for(KeyDataItem value : values)
		{
			value.created = currTime;
			value.modified = currTime;
			
			KeyDataItem prev = TransientValues.put(value.key, value);
			updateTransientValueInList(prev, value);
			
			try {
				changeMessage.values.put(value.key, value.asDecodedObject());
			} catch (Exception e) {
				l.error("Error decoding json object", e);
			}
		}
		
		onStateChanged(changeMessage, currTime);
	}
	
	protected void onStateChanged(TransientValuesChangeMessage changeMessage, long when)
	{
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(changeMessage);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return;
		}
		
		onStateChanged(messageBody, when);

	}
	
	protected void onStateChanged(String changeMessage, long when)
	{
		for(MessageListener listener : Listeners.values())
		{
			listener.onStateChange(changeMessage, when, getId(), null);
		}
	}
	
	protected synchronized void updateTransientValueInList(KeyDataItem prevItem, KeyDataItem newItem)
	{
		if(prevItem != null)
		{
			if(prevItem.prevItem != null)
				prevItem.prevItem.nextItem = prevItem.nextItem;
			if(prevItem.nextItem != null)
				prevItem.nextItem.prevItem = prevItem.prevItem;
			
			newItem.created = prevItem.created;
		}
		
		MostRecentValue.nextItem = MostRecentValue;
		MostRecentValue = newItem;
	}
	
	public KeyDataItem getTransientValue(String key)
	{
		return TransientValues.get(key);
	}
	
	public Collection<KeyDataItem> getAllTransientValues()
	{
		return TransientValues.values();
	}
	
	public void clearTransientValue(String key)
	{
		TransientValues.remove(key);
	}
	
	public void addListener(MessageListener listener)
	{
		Listeners.put(listener.getId(), listener);
		listener.onListeningTo(this);
	}
	
	
	public void removeListener(MessageListener listener)
	{
		Listeners.remove(listener.getId());
		listener.onStoppedListeningTo(this);
	}
	

	public Collection<String> getListeners()
	{
		return Listeners.keySet();
	}
	
	public boolean hasListener(String id)
	{
		return Listeners.containsKey(id);
	}
	
	protected Map<String,Object> getState(long since)
	{
		Map<String,Object> values = new HashMap<String,Object>();

		KeyDataItem value = MostRecentValue;
		while(value != null && value.modified >= since)
		{
			Object val;
			try {
				val = value.asDecodedObject();
			} catch (Exception e)
			{
				l.error("Exception decoding JSON value: ",e);
				val = "<error>";
			}
			values.put(value.key, val);
			
			value = value.nextItem;
		}
		
		return values;
	}

	public long getLastModifiedTime()
	{
		if(MostRecentValue == null)
			return 0;
		return MostRecentValue.modified;
	}
	
}
