package com.dreamwing.serverville.residents;

import java.util.Collection;
import java.util.HashMap;
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
	
	private ConcurrentMap<String,KeyDataItem> TransientValues;
	private ConcurrentMap<String,BaseResident> Listeners;
	private ConcurrentMap<String,BaseResident> ListeningTo;
	
	public BaseResident(String id)
	{
		Id = id;
		TransientValues = new ConcurrentHashMap<String,KeyDataItem>();
		Listeners = new ConcurrentHashMap<String,BaseResident>();
		ListeningTo = new ConcurrentHashMap<String,BaseResident>();
	}
	
	public String getId() { return Id; }
	
	public void destroy()
	{
		ResidentManager.removeResident(this);
		
		for(BaseResident source : ListeningTo.values())
		{
			source.Listeners.remove(Id);
		}
		
		for(BaseResident listener : Listeners.values())
		{
			listener.ListeningTo.remove(Id);
		}
		
		Listeners.clear();
		ListeningTo.clear();
	}
	
	public void sendMessage(String messageType, Object messageObject)
	{
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(messageObject);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return;
		}
		
		sendMessage(messageType, messageBody);
	}
	
	public void sendMessage(String messageType, String messageBody)
	{
		for(BaseResident listener : Listeners.values())
		{
			listener.relayMessage(messageType, messageBody, Id);
		}
	}
	
	protected void relayMessage(String messageType, String messageBody, String fromId)
	{
		for(BaseResident listener : Listeners.values())
		{
			listener.relayMessage(messageType, messageBody, fromId);
		}
	}
	
	public void setTransientValue(KeyDataItem value)
	{
		TransientValues.put(value.key, value);
		
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		
		changeMessage.values = new HashMap<String,Object>();
		try {
			changeMessage.values.put(value.key, value.asDecodedObject());
		} catch (Exception e) {
			l.error("Error decoding json object", e);
		}
		sendMessage("stateChange", changeMessage);
	}
	
	public void setTransientValues(Collection<KeyDataItem> values)
	{
		TransientValuesChangeMessage changeMessage = new TransientValuesChangeMessage();
		changeMessage.values = new HashMap<String,Object>();
		
		for(KeyDataItem value : values)
		{
			TransientValues.put(value.key, value);
			
			try {
				changeMessage.values.put(value.key, value.asDecodedObject());
			} catch (Exception e) {
				l.error("Error decoding json object", e);
			}
		}
		
		sendMessage("stateChange", changeMessage);
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
	
	public void addListener(BaseResident resident)
	{
		Listeners.put(resident.getId(), resident);
		resident.listenTo(this);
	}
	
	protected void listenTo(BaseResident resident)
	{
		ListeningTo.put(resident.getId(), resident);
	}
	
	public void removeListener(BaseResident resident)
	{
		Listeners.remove(resident.getId());
		resident.stopListenTo(this);
	}
	
	protected void stopListenTo(BaseResident resident)
	{
		ListeningTo.remove(resident.getId());
	}
	
	public Collection<String> getListeners()
	{
		return Listeners.keySet();
	}
	
	public Collection<String> getListeningTo()
	{
		return ListeningTo.keySet();
	}
}
