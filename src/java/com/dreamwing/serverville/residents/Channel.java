package com.dreamwing.serverville.residents;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ChannelInfo;
import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Channel extends BaseResident
{
	protected ConcurrentMap<String,Resident> Members;
	
	public Channel(String id)
	{
		super(id);
		
		Members = new ConcurrentHashMap<String,Resident>();
	}
	
	@Override
	public void destroy()
	{
		super.destroy();

		for(Resident source : Members.values())
		{
			source.Listeners.remove(Id);
		}
		
		Members.clear();
	}
	
	public boolean addResident(Resident resident)
	{
		resident.Channels.put(getId(), this);
		
		if(Members.put(resident.getId(), resident) != null)
		{
			// already in
			return false;
		}
		
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onResidentJoined(resident, this);
		}
		
		return true;
	}
	
	public boolean hasResident(Resident resident)
	{
		return Members.containsKey(resident.getId());
	}
	
	public boolean removeResident(Resident resident, Map<String,Object> finalValues)
	{
		resident.Channels.remove(getId());
		
		if(Members.remove(resident.getId()) == null)
		{
			// wasn't in
			return false;
		}
		
		String messageBody = null;
		if(finalValues != null)
		{
			try {
				messageBody = JSON.serializeToString(finalValues);
			} catch (JsonProcessingException e) {
				l.error("Error encoding state change message", e);
			}
		}
		else
		{
			messageBody = "{}";
		}

		onResidentRemoved(resident, messageBody);
		
		return true;
	}
	
	public void onResidentRemoved(Resident resident, String messageBody)
	{
		for(MessageListener listener : Listeners.values())
		{
			listener.onResidentLeft(resident, messageBody, this);
		}
	}

	protected void relayMessage(String messageType, String messageBody, String fromId)
	{
		for(MessageListener listener : Listeners.values())
		{
			listener.onMessage(messageType, messageBody, Id, this);
		}
	}
	
	protected void relayStateChangeMessage(String messageBody, long when, String fromId)
	{
		for(MessageListener listener : Listeners.values())
		{
			listener.onStateChange(messageBody, when, fromId, this);
		}
	}
	
	public ChannelInfo getChannelInfo(long since)
	{
		ChannelInfo info = new ChannelInfo();
		
		info.id = Id;
		info.members = new HashMap<String,ChannelMemberInfo>();
		info.values = getState(since);
		
		for(Resident source : Members.values())
		{
			info.members.put(source.Id, source.getInfo(since));
		}
		
		return info;
	}
	
	public void addListener(MessageListener listener)
	{
		super.addListener(listener);
		
		ScriptManager.onListenToChannel(this, listener);
	}
	
	
	public void removeListener(MessageListener listener)
	{
		super.removeListener(listener);
		
		ScriptManager.onStopListenToChannel(this, listener);
	}
}
