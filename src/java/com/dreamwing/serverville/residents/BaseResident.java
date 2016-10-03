package com.dreamwing.serverville.residents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.TransientValuesChangeMessage;
import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public abstract class BaseResident
{
	protected static final Logger l = LogManager.getLogger(BaseResident.class);
	
	protected String Id;
	protected String UserId;
	
	protected ConcurrentMap<String,TransientDataItem> TransientValues;
	protected TransientDataItem MostRecentValue;
	
	protected ConcurrentMap<String,MessageListener> Listeners;
	
	
	public BaseResident(String id, String userId)
	{
		Id = id;
		UserId = userId;
		TransientValues = new ConcurrentHashMap<String,TransientDataItem>();
		Listeners = new ConcurrentHashMap<String,MessageListener>();
		
	}
	
	public String getId() { return Id; }
	public String getUserId() { return UserId; }
	
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
		sendMessageFrom(messageType, messageObject, this);
	}
	
	public void sendMessage(String messageType, String messageBody)
	{
		sendMessageFrom(messageType, messageBody, this);
	}
	
	public void sendMessageFrom(String messageType, Object messageObject, BaseResident sender)
	{
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(messageObject);
		} catch (JsonProcessingException e) {
			l.error("Error encoding directed message", e);
			return;
		}
		
		sendMessageFrom(messageType, messageBody, sender);
	}
	
	public void sendMessageFrom(String messageType, String messageBody, BaseResident sender)
	{
		if(messageType == null || messageType.length() == 0 || messageType.charAt(0) == '_' || messageType.indexOf(':') >= 0)
			throw new IllegalArgumentException("Invalid message type: "+messageType);
		
		Channel via = this instanceof Channel ? (Channel)this : null;
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onMessage(messageType, messageBody, sender.Id, via);
		}
	}
	
	public void setTransientValue(String key, Object value)
	{
		TransientDataItem item = TransientValues.computeIfAbsent(key, k -> { return new TransientDataItem(key, value);});
		if(item.value != value || item.deleted)
		{
			item.value = ScriptObjectMirror.wrapAsJSONCompatible(value, null);
			item.modified = System.currentTimeMillis();
			item.deleted = false;
		}

		updateTransientValueInList(item);
		
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		
		changeMessage.values = new HashMap<String,Object>();
		changeMessage.values.put(item.key, item.value);
		
		onStateChanged(changeMessage, item.modified);
	}
	
	public void setTransientValues(Map<String,Object> values)
	{
		setTransientValues(values, false);
	}
	
	public void setTransientValues(Map<String,Object> values, boolean forceUpdate)
	{
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		changeMessage.values = new HashMap<String,Object>();
		
		long currTime = System.currentTimeMillis();
		
		for(Map.Entry<String,Object> itemSet : values.entrySet())
		{
			String key = itemSet.getKey();
			Object value = itemSet.getValue();
			
			TransientDataItem item = TransientValues.computeIfAbsent(key, k -> { return new TransientDataItem(key, value);});
			if(item.value != value || item.deleted || forceUpdate)
			{
				item.value = ScriptObjectMirror.wrapAsJSONCompatible(value, null);
				item.modified = System.currentTimeMillis();
				item.deleted = false;
			}

			updateTransientValueInList(item);
			
			changeMessage.values.put(key, value);

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
	
	protected synchronized void updateTransientValueInList(TransientDataItem newItem)
	{
		if(newItem == MostRecentValue)
			return;
		
		// Remove from existing place in line
		if(newItem.prevItem != null)
			newItem.prevItem.nextItem = newItem.nextItem;
		if(newItem.nextItem != null)
			newItem.nextItem.prevItem = newItem.prevItem;
		
		// Add to head of line
		newItem.nextItem = MostRecentValue;
		if(MostRecentValue != null)
			MostRecentValue.prevItem = newItem;
		MostRecentValue = newItem;
	}
	
	public TransientDataItem getTransientValue(String key)
	{
		TransientDataItem item = TransientValues.get(key);
		if(item == null || item.deleted)
			return null;
		return item;
	}
	
	// Note - includes deleted items
	public Collection<TransientDataItem> getAllTransientValues()
	{
		return TransientValues.values();
	}
	
	public void deleteTransientValue(String key)
	{
		TransientDataItem item = TransientValues.get(key);
		if(item == null)
		{
			return;
		}
		
		item.deleted = true;
		item.modified = System.currentTimeMillis();
		
		updateTransientValueInList(item);
		
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		
		changeMessage.deleted = new ArrayList<String>(1);
		changeMessage.deleted.add(key);
		
		onStateChanged(changeMessage, item.modified);
	}
	
	public void deleteTransientValues(List<String> keys)
	{
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		changeMessage.deleted = new ArrayList<String>(keys.size());
		
		long currTime = System.currentTimeMillis();
		
		for(String key : keys)
		{
			TransientDataItem item = TransientValues.get(key);
			if(item == null)
			{
				continue;
			}
			
			item.deleted = true;
			item.modified = currTime;

			updateTransientValueInList(item);
			
			changeMessage.deleted.add(key);
		}
		
		onStateChanged(changeMessage, currTime);
	}
	
	public synchronized void deleteAllTransientValues()
	{
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		changeMessage.deleted = new ArrayList<String>(TransientValues.size());
		
		long currTime = System.currentTimeMillis();
		
		for(Map.Entry<String,TransientDataItem> itemSet : TransientValues.entrySet())
		{
			TransientDataItem item = itemSet.getValue();

			item.deleted = true;
			item.modified = currTime;

			changeMessage.deleted.add(itemSet.getKey());
		}
		
		onStateChanged(changeMessage, currTime);
	}
	
	public void addListener(MessageListener listener)
	{
		if(Listeners.put(listener.getId(), listener) == null)
			listener.onListeningTo(this);
	}
	
	
	public void removeListener(MessageListener listener)
	{
		if(Listeners.remove(listener.getId()) != null)
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

		TransientDataItem value = MostRecentValue;
		while(value != null && value.modified >= since)
		{
			if(!value.deleted)
				values.put(value.key, value.value);
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
