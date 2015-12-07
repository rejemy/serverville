package com.dreamwing.serverville.residents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ResidentManager {
	
	private static ConcurrentMap<String,BaseResident> ActiveResidents;
	
	public static void init()
	{
		ActiveResidents = new ConcurrentHashMap<String,BaseResident>();
	}
	
	public static BaseResident getResident(String resId)
	{
		return ActiveResidents.get(resId);
	}
	
}
