package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class BaseListener implements MessageListener
{
	private static final Logger l = LogManager.getLogger(BaseListener.class);
	
	static class ResidentStateView
	{
		long lastTimestamp;
	}
	
	protected String Id;
	protected ConcurrentMap<String,BaseResident> ListeningTo;
	protected ConcurrentMap<String,ResidentStateView> KnownResidents;
	
	public BaseListener(String id)
	{
		Id = id;
		ListeningTo = new ConcurrentHashMap<String,BaseResident>();
		KnownResidents = new ConcurrentHashMap<String,ResidentStateView>();
	}
	
	@Override
	public String getId()
	{
		return Id;
	}
	
	@Override
	public void onListeningTo(BaseResident resident)
	{
		ListeningTo.put(resident.getId(), resident);
	}
	
	@Override
	public void onStoppedListeningTo(BaseResident resident)
	{
		ListeningTo.remove(resident.getId(), resident);
	}
	
	@Override
	public void onResidentJoined(Resident resident, Channel viaChannel)
	{
		long knownTime = lastTimestampForResident(resident.Id);
		
		ChannelMemberInfo info = resident.getInfo(knownTime);
		
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(info);
		} catch (JsonProcessingException e) {
			l.error("Error encoding channel joined message", e);
			return;
		}

		onMessage("memberJoined", messageBody, resident.Id, viaChannel);
		
		long time = resident.getLastModifiedTime();
		setTimestampForResident(resident.Id, time);
	}
	
	@Override
	public void onResidentLeft(Resident resident, String messageBody, Channel viaChannel)
	{
		onMessage("memberLeft", messageBody, resident.Id, viaChannel);
	}
	
	@Override
	public void onStateChange(String messageBody, long when, String fromId, Channel viaChannel)
	{
		onMessage("stateChange", messageBody, fromId, viaChannel);
		
		setTimestampForResident(fromId, when);
	}
	

	long lastTimestampForResident(String residentId)
	{
		ResidentStateView view = KnownResidents.get(residentId);
		if(view == null)
			return 0;
		return view.lastTimestamp;
	}
	
	void setTimestampForResident(String residentId, long timestamp)
	{
		ResidentStateView view = KnownResidents.get(residentId);
		if(view == null)
		{
			view = new ResidentStateView();
			ResidentStateView prev = KnownResidents.putIfAbsent(residentId, view);
			if(prev != null)
				view = prev;
		}
		
		if(timestamp > view.lastTimestamp)
			view.lastTimestamp = timestamp;
	}
	
	/*
	@Override
	public void onResidentJoinedChannel(String channelId, Resident resident)
	{
		ChannelMemberInfo info = resident.getInfo(0);
		
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(info);
		} catch (JsonProcessingException e) {
			l.error("Error encoding channel joined message", e);
			return;
		}
		
		onMessage("memberJoined", messageBody, channelId);
	}
	*/
	
	@Override
	public void destroy()
	{
		for(BaseResident source : ListeningTo.values())
		{
			source.Listeners.remove(Id);
		}
		
		ListeningTo.clear();
	}


}
