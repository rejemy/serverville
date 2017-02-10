package com.dreamwing.serverville.residents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.client.ClientMessages.ResidentEventNotification;
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.cluster.ClusterMessages.GlobalChannelDataUpdateMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.GlobalChannelEventMessage;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;


public class GlobalChannel extends Channel
{

	public GlobalChannel(String id)
	{
		this(id, null);
	}
	
	public GlobalChannel(String id, String residentType)
	{
		super(id, residentType);

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
		
		GlobalChannelEventMessage clusterMessage = new GlobalChannelEventMessage();
		clusterMessage.ChannelId = Id;
		clusterMessage.MessageBody = messageBody;
		
		ClusterManager.sendToAll(clusterMessage);
	}
	
	public void onEventTriggered(String messageBody)
	{
		for(OnlineUser listener : Listeners.values())
		{
			listener.onResidentEvent(this, this, messageBody);
		}
	}
	
	@Override
	public void setTransientValue(String key, Object value)
	{
		GlobalChannelDataUpdateMessage update = new GlobalChannelDataUpdateMessage();
		update.ChannelId = Id;
		update.When = System.currentTimeMillis();
		update.Updates = new HashMap<String,Object>();
		update.Updates.put(key, value);
		
		ClusterManager.sendToAll(update);
	}
	
	@Override
	public void setTransientValues(Map<String,Object> values, boolean forceUpdate)
	{

		GlobalChannelDataUpdateMessage update = new GlobalChannelDataUpdateMessage();
		update.ChannelId = Id;
		update.When = System.currentTimeMillis();
		update.Updates = values;
		update.ForceUpdate = forceUpdate;
		
		ClusterManager.sendToAll(update);
	}
	
	@Override
	public void deleteTransientValue(String key)
	{
		GlobalChannelDataUpdateMessage update = new GlobalChannelDataUpdateMessage();
		update.ChannelId = Id;
		update.When = System.currentTimeMillis();
		update.Deleted = new ArrayList<String>(1);
		update.Deleted.add(key);
		
		ClusterManager.sendToAll(update);
	}
	
	@Override
	public void deleteTransientValues(List<String> keys)
	{
		GlobalChannelDataUpdateMessage update = new GlobalChannelDataUpdateMessage();
		update.ChannelId = Id;
		update.When = System.currentTimeMillis();
		update.Deleted = keys;
		
		ClusterManager.sendToAll(update);
	}
	
	@Override
	public void deleteAllTransientValues()
	{
		GlobalChannelDataUpdateMessage update = new GlobalChannelDataUpdateMessage();
		update.ChannelId = Id;
		update.When = System.currentTimeMillis();
		update.DeleteAll = true;
		
		ClusterManager.sendToAll(update);
	}
	
	@Override
	public boolean addResident(Resident resident)
	{
		throw new UnsupportedOperationException("Not implemented!");
	}
	
	@Override
	public boolean hasResident(Resident resident)
	{
		return false;
	}
	
	@Override
	public boolean removeResident(Resident resident, Map<String,Object> finalValues)
	{
		return false;
	}
	
	@Override
	public boolean removeResident(Resident resident, Map<String,Object> finalValues, boolean dontUpdateResident)
	{
		return false;
	}

}
