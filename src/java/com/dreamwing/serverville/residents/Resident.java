package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;

public class Resident extends BaseResident
{
	protected ConcurrentMap<String,Channel> Channels;
	
	public Resident(String id, String userId)
	{
		super(id, userId);
		
		Channels = new ConcurrentHashMap<String,Channel>();
	}

	@Override
	public void sendMessageFrom(String messageType, String messageBody, BaseResident sender)
	{
		super.sendMessageFrom(messageType, messageBody, sender);
		
		for(Channel channel : Channels.values())
		{
			channel.relayMessage(messageType, messageBody, sender.Id);
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
			channel.onResidentRemoved(this, null);
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
