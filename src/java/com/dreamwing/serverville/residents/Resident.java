package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;

public class Resident extends BaseResident
{
	protected ConcurrentMap<String,Channel> Channels;
	
	public Resident(String id)
	{
		super(id);
		
		Channels = new ConcurrentHashMap<String,Channel>();
	}

	@Override
	public void sendMessage(String messageType, String messageBody)
	{
		super.sendMessage(messageType, messageBody);
		
		for(Channel channel : Channels.values())
		{
			channel.relayMessage(messageType, messageBody, Id);
		}
	}
	
	@Override
	protected void onStateChanged(String changeMessage, long when)
	{
		for(Channel channel : Channels.values())
		{
			channel.relayStateChangeMessage(changeMessage, when, getId());
		}
	}
	
	@Override
	public void destroy()
	{
		super.destroy();

		for(Channel channel : Channels.values())
		{
			channel.Members.remove(getId());
		}
		
		Channels.clear();
	}
	
	public ChannelMemberInfo getInfo(long since)
	{
		ChannelMemberInfo info = new ChannelMemberInfo();
		info.id = Id;
		info.values = getState(since);
		
		return info;
	}
}
