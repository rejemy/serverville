package com.dreamwing.serverville.residents;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientMessages.ResidentEventNotification;
import com.dreamwing.serverville.client.ClientMessages.ResidentStateUpdateNotification;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

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
	
	public void write(ObjectDataOutput out) throws IOException
	{
		super.write(out);
		
		StringUtil.writeUTFNullSafe(out, OwnerId);
		
		out.writeInt(Channels.size());
		for(Channel channel : Channels.values())
		{
			out.writeUTF(channel.getId());
		}
	}

	public void read(ObjectDataInput in) throws IOException
	{
		super.read(in);
		
		OwnerId = StringUtil.readUTFNullSafe(in);
		
		int numChannels = in.readInt();
		for(int i=0; i<numChannels; i++)
		{
			String channelId = in.readUTF();
			BaseResident res = ResidentManager.getResident(channelId);
			if(res == null || !(res instanceof Channel))
				continue;
			
			Channel channel = (Channel)res;
			Channels.put(channel.getId(), channel);
		}
	}
	
}
