package com.dreamwing.serverville.residents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class World extends Channel
{
	private ConcurrentMap<String,Zone> Zones;
	private ConcurrentMap<String,Zone> ResidentLocation;
	
	public World(String id)
	{
		this(id, null);
	}
	
	public World(String id, String residentType)
	{
		super(id, residentType == null ? "world" : residentType);
		
		Zones = new ConcurrentHashMap<String,Zone>();
		ResidentLocation = new ConcurrentHashMap<String,Zone>();
	}
	
	@Override
	public void destroy()
	{
		super.destroy();

		for(Channel zone : Zones.values())
		{
			zone.destroy();
		}
		
		Zones.clear();
	}
	
	public Zone getZone(String id)
	{
		return Zones.get(id);
	}
	
	public Zone getOrCreateZone(String id)
	{
		return Zones.computeIfAbsent(id, k ->
		{
			return new Zone(Id+"_"+id);
		});
	}
	
	public void addResidentToZone(Resident resident, String zoneId, Map<String,Object> stateUpdate)
	{
		Zone previousZone = ResidentLocation.get(resident.getId());
		Zone zone = getZone(zoneId);
		
		if(previousZone != null)
		{
			if(previousZone == zone)
				return;
			
			previousZone.moveResident(resident, zone, stateUpdate);
		}
		
		ResidentLocation.put(resident.getId(), zone);
		resident.setTransientValues(stateUpdate);
		zone.addResident(resident);
	}
	
	public void removeResidentFromZones(Resident resident, Map<String,Object> finalValues)
	{
		Zone previousZone = ResidentLocation.get(resident.getId());
		if(previousZone != null)
		{
			previousZone.removeResident(resident, finalValues);
		}
	}
}
