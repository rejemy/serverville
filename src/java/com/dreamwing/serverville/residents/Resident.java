package com.dreamwing.serverville.residents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ResidentEventNotification;
import com.dreamwing.serverville.client.ClientMessages.ResidentStateUpdateNotification;

public class Resident extends BaseResident
{
	protected ConcurrentMap<String,Channel> Channels;
	
	String OwnerId;
	
	public Resident(String id, String ownerId, String residentType)
	{
		super(id, residentType);
		
		OwnerId = ownerId;
		
		Channels = new ConcurrentHashMap<String,Channel>();
	}

	@Override
	public String getOwnerId() { return OwnerId; }
	
	public void setOwnerId(String owner)
	{
		OwnerId = owner;
	}
	
	@Override
	public void triggerEvent(String eventType, String eventBody)
	{
		super.triggerEvent(eventType, eventBody);
		
		ResidentEventNotification notification = new ResidentEventNotification();
		notification.resident_id = Id;
		notification.via_channel = null;
		notification.event_type = eventType;
		notification.event_data = eventBody;
		
		for(Channel channel : Channels.values())
		{
			channel.relayResidentEvent(this, notification);
		}
	}
	
	@Override
	protected void onStateChanged(ResidentStateUpdateNotification changeMessage, long when)
	{
		super.onStateChanged(changeMessage, when);
		
		for(Channel channel : Channels.values())
		{
			channel.relayStateChangeMessage(this, changeMessage, when);
		}
	}
	
	public void removeFromAllChannels()
	{
		removeFromAllChannels(null);
	}
	
	public void removeFromAllChannels(Map<String,Object> finalValues)
	{
		for(Channel channel : Channels.values())
		{
			channel.removeResident(this, finalValues, true);
		}
		
		Channels.clear();
	}
	
	@Override
	public void destroy()
	{
		super.destroy();
		
		removeFromAllChannels(null);
	}
	
	
}
