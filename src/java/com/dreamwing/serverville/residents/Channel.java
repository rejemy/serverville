package com.dreamwing.serverville.residents;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ChannelInfo;
import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;

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
	
	public void addResident(Resident resident)
	{
		Members.put(resident.getId(), resident);
		resident.Channels.put(getId(), this);
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onResidentJoined(resident, this);
		}
	}
	
	public void removeResident(Resident resident)
	{
		Members.remove(resident.getId());
		resident.Channels.remove(getId());
		
		for(MessageListener listener : Listeners.values())
		{
			listener.onResidentLeft(resident, this);
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
	
	/*
	protected void listenTo(BaseResident resident)
	{
		super.listenTo(resident);
	}
	
	protected void stopListenTo(BaseResident resident)
	{
		super.stopListenTo(resident);
	}
	*/
}
