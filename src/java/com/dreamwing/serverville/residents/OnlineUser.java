package com.dreamwing.serverville.residents;

import java.util.HashMap;
import java.util.Map;

import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.data.ServervilleUser;

public class OnlineUser extends BaseListener
{
	public ServervilleUser User;
	public ClientConnectionHandler Connection;
	
	private Resident DefaultAlias;
	private Map<String,Resident> Aliases;
	
	public OnlineUser(String id, ClientConnectionHandler connection)
	{
		super(id);
		User = connection.getUser();
		Connection = connection;
		
		Aliases = new HashMap<String,Resident>();
		
		DefaultAlias = new Resident(User.getId());
		ResidentManager.addResident(DefaultAlias);
	}

	@Override
	public void onMessage(String messageType, String messageBody, String fromId, Channel viaChannel)
	{
		String viaChannelId = viaChannel != null ? viaChannel.Id : null;
		Connection.sendMessage(messageType, messageBody, fromId, viaChannelId);
	}

	public synchronized Resident getAlias(String name)
	{
		if(name == null || name.length() == 0)
		{
			return DefaultAlias;
		}
		
		Resident alias = Aliases.get(name);

		return alias;
	}
	
	public synchronized Resident getOrCreateAlias(String name)
	{
		if(name == null || name.length() == 0)
		{
			return DefaultAlias;
		}
		
		Resident alias = Aliases.get(name);
		if(alias == null)
		{
			alias = new Resident(User.getId()+"/"+name);
			Aliases.put(alias.getId(), alias);
			ResidentManager.addResident(alias);
		}
		
		return alias;
	}
	
	@Override
	public void destroy()
	{
		super.destroy();
		
		DefaultAlias.destroy();
		DefaultAlias = null;
		
		for(Resident alias : Aliases.values())
		{
			alias.destroy();
		}
		
		Aliases.clear();
	}
	
	/*
	@Override
	public void sendMessage(String messageType, String messageBody)
	{
		relayMessage(messageType, messageBody, Id);
		
		super.sendMessage(messageType, messageBody);
	}
	
	@Override
	protected void relayMessage(String messageType, String messageBody, String fromId)
	{
		Connection.sendMessage(messageType, messageBody, fromId);
		
	}
	*/
}
