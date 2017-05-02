package com.dreamwing.serverville.residents;


import java.util.Map;

import com.dreamwing.serverville.client.ClientMessages.ResidentLeftNotification;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

public class Zone extends Channel
{
	public Zone(String id)
	{
		this(id, null);
	}
	
	public Zone(String id, String residentType)
	{
		super(id, residentType == null ? "zone" : residentType);
	}
	
	public boolean moveResident(Resident resident, Zone newZone, Map<String,Object> stateUpdate)
	{
		if(Members.remove(resident.getId()) == null)
		{
			// wasn't in
			return false;
		}
		
		ResidentLeftNotification notification = new ResidentLeftNotification();
		notification.resident_id = resident.Id;
		notification.via_channel = Id;
		notification.final_values = stateUpdate;
		
		String messageBody = null;
		try {
			messageBody = JSON.serializeToString(notification);
		} catch (JsonProcessingException e) {
			l.error("Error encoding state change message", e);
			return true;
		}

		for(OnlineUser listener : Listeners.values())
		{
			// Don't send remove if the listener is listening to the new zone the resident is moving to
			if(listener.isListeningTo(newZone))
				continue;
			
			listener.onResidentLeft(resident, this, messageBody);
		}
		
		return true;
	}
	
}
