package com.dreamwing.serverville.residents;

public abstract class BaseResident
{
	protected String Id;
	
	public BaseResident(String id)
	{
		Id = id;
	}
	
	public String getId() { return Id; }
	
	public abstract void destroy();
	
}
