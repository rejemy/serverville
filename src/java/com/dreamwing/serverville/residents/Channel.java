package com.dreamwing.serverville.residents;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ChannelInfo;
import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;
import com.dreamwing.serverville.client.ClientMessages.ResidentEventNotification;
import com.dreamwing.serverville.client.ClientMessages.ResidentLeftNotification;
import com.dreamwing.serverville.client.ClientMessages.ResidentStateUpdateNotification;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Channel extends BaseResident
{
	protected ConcurrentMap<String,Resident> Members;
	
	public Channel(String id)
	{
		this(id, null);
	}
	
	public Channel(String id, String residentType)
	{
		super(id, residentType == null ? "channel" : residentType);
		
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
		
		
		for(OnlineUser listener : Listeners.values())
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
		return removeResident(resident, finalValues, false);
	}
	
	public boolean removeResident(Resident resident, Map<String,Object> finalValues, boolean dontUpdateResident)
	{
		if(!dontUpdateResident)
			resident.Channels.remove(getId());
		
		if(Members.remove(resident.getId()) == null)
		{
			// wasn't in
			return false;
		}
		
		ResidentLeftNotification notification = new ResidentLeftNotification();
		notification.resident_id = resident.Id;
		notification.via_channel = Id;
		notification.final_values = finalValues;
		
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(notification);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return true;
		}

		onResidentRemoved(resident, messageBody);
		
		return true;
	}
	
	public void onResidentRemoved(Resident resident, String messageBody)
	{
		for(OnlineUser listener : Listeners.values())
		{
			listener.onResidentLeft(resident, this, messageBody);
		}
	}
	
	@Override
	public void triggerEvent(String eventType, String eventBody)
	{
		ResidentEventNotification notification = new ResidentEventNotification();
		notification.resident_id = Id;
		notification.via_channel = Id;
		notification.event_type = eventType;
		notification.event_data = eventBody;
		
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(notification);
		} catch (JsonProcessingException e) {
			l.error("Error encoding resident event message", e);
			return;
		}
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onResidentEvent(this, this, messageBody);
		}
	}
	
	@Override
	protected void onStateChanged(ResidentStateUpdateNotification changeMessage, long when)
	{
		String messageBody = null;
		changeMessage.via_channel = Id;
		try {
			messageBody = JSON.serializeToString(changeMessage);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return;
		}
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onStateChange(this, this, messageBody, when);
		}
		
	}

	protected void relayResidentEvent(Resident resident, ResidentEventNotification eventMessage)
	{
		eventMessage.via_channel = Id;
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(eventMessage);
		} catch (JsonProcessingException e) {
			l.error("Error encoding resident event message", e);
			return;
		}
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onResidentEvent(resident, this, messageBody);
		}
	}
	
	protected void relayStateChangeMessage(Resident resident, ResidentStateUpdateNotification changeMessage, long when)
	{
		changeMessage.via_channel = Id;
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(changeMessage);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return;
		}
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onStateChange(resident, this, messageBody, when);
		}
	}
	
	public ChannelInfo getChannelInfo(long since)
	{
		ChannelInfo info = new ChannelInfo();
		
		info.channel_id = Id;
		info.members = new HashMap<String,ChannelMemberInfo>();
		info.values = getState(since);
		
		for(Resident source : Members.values())
		{
			info.members.put(source.Id, source.getInfo(since));
		}
		
		return info;
	}
	
	public void addListener(OnlineUser listener)
	{
		super.addListener(listener);
		
		ScriptManager.onListenToChannel(this, listener);
	}
	
	
	public void removeListener(OnlineUser listener)
	{
		super.removeListener(listener);
		
		ScriptManager.onStopListenToChannel(this, listener);
	}
}
