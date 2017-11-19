package com.dreamwing.serverville.net;

import com.dreamwing.serverville.data.ServervilleUser;

import io.netty.channel.ChannelHandlerContext;

public class HttpConnectionInfo
{
	public ServervilleUser User; // May be null if this is an agent connection
	public HttpSession Session;
	
	public ChannelHandlerContext Ctx;
	public String ConnectionId;
	public long ConnectionStartedAt;
	
}
