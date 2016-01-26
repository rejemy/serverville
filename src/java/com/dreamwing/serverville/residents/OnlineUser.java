package com.dreamwing.serverville.residents;

import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.data.ServervilleUser;

public class OnlineUser extends BaseResident
{
	public ServervilleUser User;
	public ClientConnectionHandler Connection;
	
	
	public OnlineUser(ClientConnectionHandler connection)
	{
		super(connection.getUser().getId());
		User = connection.getUser();
		Connection = connection;
	}
	
	
	
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
	
}
