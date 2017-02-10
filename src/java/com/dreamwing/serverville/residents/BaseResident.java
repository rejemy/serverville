package com.dreamwing.serverville.residents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.client.ClientSessionManager;
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.cluster.DistributedData.ResidentLocator;
import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;
import com.dreamwing.serverville.client.ClientMessages.ResidentEventNotification;
import com.dreamwing.serverville.client.ClientMessages.ResidentStateUpdateNotification;
import com.dreamwing.serverville.data.PropertyPermissions;
import com.dreamwing.serverville.data.ResidentPermissionsManager;
import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public abstract class BaseResident
{
	protected static final Logger l = LogManager.getLogger(BaseResident.class);
	
	protected String Id;
	protected String ResidentType;
	protected PropertyPermissions Permissions;
	
	protected ConcurrentMap<String,TransientDataItem> TransientValues;
	protected TransientDataItem MostRecentValue;
	
	protected ConcurrentMap<String,OnlineUser> Listeners;
	
	protected String ColocatedWith;
	
	public BaseResident(String id, String residentType)
	{
		Id = id;
		ResidentType = residentType;
		Permissions = ResidentPermissionsManager.getPermissions(ResidentType);
		
		TransientValues = new ConcurrentHashMap<String,TransientDataItem>();
		Listeners = new ConcurrentHashMap<String,OnlineUser>();
	}
	
	public String getId() { return Id; }
	
	public String getType() { return ResidentType; }
	
	public String getOwnerId() { return null; }
	
	public String getColocation() { return ColocatedWith; }
	
	public ResidentLocator getLocator()
	{
		return new ResidentLocator(Id, ColocatedWith);
	}
	
	public void destroy()
	{
		ClusterManager.unregisterLocalResident(this);
		ResidentManager.removeResident(this);
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onStoppedListeningTo(this);
		}
		
		Listeners.clear();
	}
	
	public PropertyPermissions getPermissions()
	{
		return Permissions;
	}
	
	public void triggerEvent(String eventType, String eventBody)
	{
		ResidentEventNotification notification = new ResidentEventNotification();
		notification.resident_id = Id;
		notification.via_channel = null;
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
			listener.onResidentEvent(this, null, messageBody);
		}
	}
	
	
	public void setTransientValue(String key, Object value)
	{
		TransientDataItem item = TransientValues.computeIfAbsent(key, k -> { return new TransientDataItem(key, value);});
		if(item.value != value || item.deleted)
		{
			item.value = ScriptObjectMirror.wrapAsJSONCompatible(value, null);
			item.modified = System.currentTimeMillis();
			item.deleted = false;
		}

		updateTransientValueInList(item);
		
		// Don't send updates for private data
		if(!Permissions.isGlobalReadable(key))
			return;
		
		ResidentStateUpdateNotification changeMessage = new ResidentStateUpdateNotification();
		
		changeMessage.resident_id = Id;
		changeMessage.values = new HashMap<String,Object>();
		changeMessage.values.put(item.key, item.value);
		
		onStateChanged(changeMessage, item.modified);
	}
	
	public void setTransientValues(Map<String,Object> values)
	{
		setTransientValues(values, false);
	}
	
	public void setTransientValues(Map<String,Object> values, boolean forceUpdate)
	{
		ResidentStateUpdateNotification changeMessage = new ResidentStateUpdateNotification();
		changeMessage.resident_id = Id;
		changeMessage.values = new HashMap<String,Object>();
		
		long currTime = System.currentTimeMillis();
		
		for(Map.Entry<String,Object> itemSet : values.entrySet())
		{
			String key = itemSet.getKey();
			Object value = itemSet.getValue();
			
			TransientDataItem item = TransientValues.computeIfAbsent(key, k -> { return new TransientDataItem(key, value, currTime);});
			if(item.value != value || item.deleted || forceUpdate)
			{
				item.value = ScriptObjectMirror.wrapAsJSONCompatible(value, null);
				item.modified = currTime;
				item.deleted = false;
			}

			updateTransientValueInList(item);
			
			// Don't send updates for private data
			if(Permissions.isGlobalReadable(key))
				changeMessage.values.put(key, value);

		}
		
		if(!changeMessage.values.isEmpty())
			onStateChanged(changeMessage, currTime);
	}
	
	public void deleteTransientValue(String key)
	{
		TransientDataItem item = TransientValues.get(key);
		if(item == null)
		{
			return;
		}
		
		item.value = null;
		item.deleted = true;
		item.modified = System.currentTimeMillis();
		
		updateTransientValueInList(item);
		
		// Don't send updates for private data
		if(!Permissions.isGlobalReadable(key))
			return;
				
		ResidentStateUpdateNotification changeMessage = new ResidentStateUpdateNotification();
		changeMessage.resident_id = Id;
		
		changeMessage.deleted = new ArrayList<String>(1);
		changeMessage.deleted.add(key);
		
		onStateChanged(changeMessage, item.modified);
	}
	
	public void deleteTransientValues(List<String> keys)
	{
		ResidentStateUpdateNotification changeMessage = new ResidentStateUpdateNotification();
		changeMessage.resident_id = Id;
		changeMessage.deleted = new ArrayList<String>(keys.size());
		
		long currTime = System.currentTimeMillis();
		
		for(String key : keys)
		{
			TransientDataItem item = TransientValues.get(key);
			if(item == null)
			{
				continue;
			}
			
			item.deleted = true;
			item.modified = currTime;

			updateTransientValueInList(item);
			
			// Don't send updates for private data
			if(Permissions.isGlobalReadable(key))
				changeMessage.deleted.add(key);
		}
		
		if(!changeMessage.deleted.isEmpty())
			onStateChanged(changeMessage, currTime);
	}
	
	public void deleteAllTransientValues()
	{
		ResidentStateUpdateNotification changeMessage = new ResidentStateUpdateNotification();
		changeMessage.resident_id = Id;
		changeMessage.deleted = new ArrayList<String>(TransientValues.size());
		
		long currTime = System.currentTimeMillis();
		
		synchronized(this)
		{
			for(Map.Entry<String,TransientDataItem> itemSet : TransientValues.entrySet())
			{
				TransientDataItem item = itemSet.getValue();
	
				item.deleted = true;
				item.modified = currTime;
	
				changeMessage.deleted.add(itemSet.getKey());
			}
		}
		
		if(!changeMessage.deleted.isEmpty())
			onStateChanged(changeMessage, currTime);
	}
	
	
	
	protected void onStateChanged(ResidentStateUpdateNotification changeMessage, long when)
	{
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(changeMessage);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return;
		}
		
		for(OnlineUser listener : Listeners.values())
		{
			listener.onStateChange(this, null, messageBody, when);
		}
		
	}

	
	protected synchronized void updateTransientValueInList(TransientDataItem newItem)
	{
		if(newItem == MostRecentValue)
			return;
		
		// Remove from existing place in line
		if(newItem.prevItem != null)
			newItem.prevItem.nextItem = newItem.nextItem;
		if(newItem.nextItem != null)
			newItem.nextItem.prevItem = newItem.prevItem;
		
		// Add to head of line
		newItem.nextItem = MostRecentValue;
		if(MostRecentValue != null)
			MostRecentValue.prevItem = newItem;
		MostRecentValue = newItem;
	}
	
	public TransientDataItem getTransientValue(String key)
	{
		TransientDataItem item = TransientValues.get(key);
		if(item == null || item.deleted)
			return null;
		return item;
	}
	
	// Note - includes deleted items, and they will be unordered
	public Collection<TransientDataItem> getAllTransientValues()
	{
		return TransientValues.values();
	}
	
	
	
	public void addListener(OnlineUser listener)
	{
		if(Listeners.put(listener.getId(), listener) == null)
			listener.onListeningTo(this);
	}
	
	
	public void removeListener(OnlineUser listener)
	{
		if(Listeners.remove(listener.getId()) != null)
			listener.onStoppedListeningTo(this);
	}
	

	public Collection<String> getListeners()
	{
		return Listeners.keySet();
	}
	
	public boolean hasListener(String id)
	{
		return Listeners.containsKey(id);
	}
	
	protected Map<String,Object> getState(long since)
	{
		Map<String,Object> values = new HashMap<String,Object>();

		TransientDataItem value = MostRecentValue;
		while(value != null && value.modified >= since)
		{
			if(!value.deleted)
				values.put(value.key, value.value);
			value = value.nextItem;
		}
		
		return values;
	}

	public long getLastModifiedTime()
	{
		if(MostRecentValue == null)
			return 0;
		return MostRecentValue.modified;
	}
	
	public ChannelMemberInfo getInfo(long since)
	{
		ChannelMemberInfo info = new ChannelMemberInfo();
		info.resident_id = Id;
		info.values = getState(since);
		
		return info;
	}
	
	public List<TransientDataItem> getValues()
	{
		synchronized(this)
		{
			int numValues = TransientValues.size();
			if(numValues == 0)
				return null;
			List<TransientDataItem> values = new ArrayList<TransientDataItem>(numValues);
			
			TransientDataItem item = MostRecentValue;
			while(item != null)
			{
				values.add(item);
				item = item.nextItem;
			}
			
			return values;
		}
	}
	
	public void setValues(List<TransientDataItem> values)
	{
		if(values == null)
			return;
		
		synchronized(this)
		{
			TransientValues.clear();
			
			TransientDataItem lastItem = null;
			for(TransientDataItem item : values)
			{
				if(lastItem == null)
				{
					MostRecentValue = item;
				}
				else
				{
					lastItem.nextItem = item;
					item.prevItem = lastItem;
				}
				
				lastItem = item;
			}
		}
	}
	
	public void write(ObjectDataOutput out) throws IOException
	{
		StringUtil.writeUTFNullSafe(out, ColocatedWith);
		
		List<TransientDataItem> values = new ArrayList<TransientDataItem>(TransientValues.size());
		TransientDataItem value = MostRecentValue;
		while(value != null)
		{
			values.add(value);
			value = value.nextItem;
		}
		
		String encodedValues = JSON.serializeToString(values);
		out.writeUTF(encodedValues);
		
		out.writeInt(Listeners.size());
		for(OnlineUser listener : Listeners.values())
		{
			out.writeUTF(listener.User.getId());
		}
	}

	public void read(ObjectDataInput in) throws IOException
	{
		ColocatedWith = StringUtil.readUTFNullSafe(in);
		
		String encodedValues = in.readUTF();
		
		MostRecentValue = null;
		TransientValues.clear();
		
		TransientDataItem[] dataItems = JSON.deserialize(encodedValues, JSON.TransientDataItemArrayType);
		for(int i=dataItems.length-1; i >= 0; i--)
		{
			TransientDataItem item = dataItems[i];
			 
			TransientValues.put(item.key, item);
			item.nextItem = MostRecentValue;
			if(MostRecentValue != null)
				MostRecentValue.prevItem = item;
			MostRecentValue = item;
		}
		
		int numListeners = in.readInt();
		for(int l=0; l<numListeners; l++)
		{
			String userId = in.readUTF();
			
			ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(userId);
			if(client == null)
				continue;
			
			OnlineUser user = client.getPresence();
			if(user == null)
				continue;
			
			Listeners.put(user.getId(), user);
		}
	}
}
