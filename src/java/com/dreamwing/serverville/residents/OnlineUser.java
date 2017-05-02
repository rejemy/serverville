package com.dreamwing.serverville.residents;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.client.ClientMessages.ChannelMemberInfo;
import com.dreamwing.serverville.client.ClientMessages.ResidentJoinedNotification;
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;


public class OnlineUser
{

	static class ResidentStateView
	{
		long lastTimestamp;
	}
	
	protected ConcurrentMap<String,BaseResident> ListeningTo;
	protected ConcurrentMap<String,ResidentStateView> KnownResidents;
	
	public ServervilleUser User;
	public ClientConnectionHandler Connection;
	
	private Map<String,Resident> OwnedResidents;

	public OnlineUser(ClientConnectionHandler connection)
	{
		ListeningTo = new ConcurrentHashMap<String,BaseResident>();
		KnownResidents = new ConcurrentHashMap<String,ResidentStateView>();
		
		User = connection.getUser();
		Connection = connection;
		
		OwnedResidents = new HashMap<String,Resident>();
	}
	
	
	public String getId()
	{
		return User.getId();
	}
	
	public boolean isListeningTo(BaseResident resident)
	{
		return ListeningTo.containsKey(resident.getId());
	}
	
	public void onListeningTo(BaseResident resident)
	{
		ListeningTo.put(resident.getId(), resident);
	}
	
	public void onStoppedListeningTo(BaseResident resident)
	{
		ListeningTo.remove(resident.getId(), resident);
	}
	
	public void onResidentJoined(BaseResident resident, Channel viaChannel)
	{
		long knownTime = lastTimestampForResident(resident.Id);
		
		ChannelMemberInfo info = resident.getInfo(knownTime);
		
		ResidentJoinedNotification notification = new ResidentJoinedNotification();
		notification.resident_id = info.resident_id;
		notification.via_channel = viaChannel.getId();
		notification.values = info.values;
		
		Connection.sendNotification("resJoined", notification);

		long time = resident.getLastModifiedTime();
		setTimestampForResident(resident.Id, time);
	}
	
	public void onResidentLeft(BaseResident resident, Channel viaChannel, String notificationBody)
	{
		Connection.sendNotification("resLeft", notificationBody);
	}
	
	public void onResidentEvent(BaseResident resident, Channel viaChannel, String notificationBody)
	{
		Connection.sendNotification("resEvent", notificationBody);
	}
	
	public void onStateChange(BaseResident resident, Channel viaChannel, String notificationBody, long when)
	{
		Connection.sendNotification("resUpdate", notificationBody);
		
		setTimestampForResident(resident.Id, when);
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
	

	public void destroy()
	{
		for(BaseResident source : ListeningTo.values())
		{
			source.Listeners.remove(User.getId());
		}
		
		ListeningTo.clear();

		for(Resident res : OwnedResidents.values())
		{
			res.destroy();
		}
		
		OwnedResidents.clear();
	}

	public synchronized Resident getOwnedResident(String id)
	{
		if(id == null || id.length() == 0)
		{
			return null;
		}
		
		Resident alias = OwnedResidents.get(id);

		return alias;
	}
	
	public synchronized Resident getOrCreateOwnedResident(String id, String residentType) throws JsonApiException
	{
		
		Resident res = OwnedResidents.get(id);
		if(res == null)
		{
			if(ResidentManager.hasResident(id))
				throw new JsonApiException(ApiErrors.RESIDENT_ID_TAKEN, id);
			
			res = new Resident(id, User.getId(), residentType);
			OwnedResidents.put(id, res);
			ResidentManager.addResident(res);
			
			
			try
			{
				ClusterManager.registerLocalResident(res);
			}
			catch(Exception e)
			{
				// Oops, we have to clean it up if we couldn't register it on the cluster
				OwnedResidents.remove(id);
				ResidentManager.removeResident(res);
				throw e;
			}
		}
		
		return res;
	}
	
	public void removeResident(String id)
	{
		OwnedResidents.remove(id);
	}
	
	public void addResident(Resident resident)
	{
		resident.setOwnerId(User.getId());
		OwnedResidents.put(resident.Id, resident);
	}
	
	public void deleteResident(String id)
	{
		deleteResident(id, null);
	}
	
	public void deleteResident(String id, Map<String,Object> finalValues)
	{
		Resident res = OwnedResidents.remove(id);
		if(res == null)
			return;
		
		res.removeFromAllChannels(finalValues);
		res.destroy();
	}
	
	

}
