package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.cluster.ClusterManager;

public class ResidentManager {
	
	private static ConcurrentMap<String,BaseResident> ActiveResidents;
	
	public static void init()
	{
		ActiveResidents = new ConcurrentHashMap<String,BaseResident>();
	}
	
	public static void shutdown()
	{
		for(BaseResident resident : ActiveResidents.values())
		{
			ClusterManager.unregisterLocalResident(resident);
		}
	}
	
	public static void addResident(BaseResident resident)
	{
		ActiveResidents.putIfAbsent(resident.getId(), resident);
	}
	
	public static boolean hasResident(String resId)
	{
		return ActiveResidents.containsKey(resId);
	}
	
	public static BaseResident getResident(String resId)
	{
		return ActiveResidents.get(resId);
	}
	
	public static void removeResident(BaseResident resident)
	{
		ActiveResidents.remove(resident.getId());
	}
	
}
