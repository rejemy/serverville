package com.dreamwing.serverville.client;

import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.residents.OnlineUser;

public class ClientMessageInfo
{
	public String MessageId;
	public String MessageNum;
	public ClientConnectionHandler ConnectionHandler;
	public ServervilleUser User;
	public OnlineUser UserPresence;
}
