package com.dreamwing.serverville.net;

import com.dreamwing.serverville.data.ServervilleUser;

import io.netty.channel.ChannelHandlerContext;

public class HttpConnectionInfo {
	
	// May be null if this is an agent connection
	public ServervilleUser User;
	public HttpSession Session;
	
	public ChannelHandlerContext Ctx;
	public String ConnectionId;
	
}
