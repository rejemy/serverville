package com.dreamwing.serverville.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SubnetMask {

	private String AddrMaskString;
	
	private int Ipv4Addr = 0;
	private int IPv4Mask = 0;
	
	public SubnetMask(String addrMask) throws UnknownHostException
	{
		AddrMaskString = addrMask;
		
		String[] parts = addrMask.split("/");
		if(parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("Invalid address mask format: "+addrMask);
		
		int bits = 0;
		if(parts.length == 2)
			bits = Integer.parseInt(parts[1]);
		
		if(bits > 32 || bits < 0)
			throw new IllegalArgumentException("Invalid address mask format: "+addrMask);
		
		InetAddress addr = InetAddress.getByName(parts[0]);
		if(addr instanceof Inet4Address)
		{
			Inet4Address addr4 = (Inet4Address)addr;
			byte[] b = addr4.getAddress();
			Ipv4Addr =	 ((b[0] & 0xFF) << 24) |
	                     ((b[1] & 0xFF) << 16) |
	                     ((b[2] & 0xFF) << 8)  |
	                     ((b[3] & 0xFF) << 0);
			
			IPv4Mask = (0xffffffff << bits);
			Ipv4Addr = Ipv4Addr & IPv4Mask;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported address type: "+addrMask);
		}
		
	}
	
	public String getAddrString()
	{
		return AddrMaskString;
	}
	
	public boolean match(InetAddress addr)
	{
		if(addr instanceof Inet4Address)
			return match((Inet4Address)addr);
		
		return false;
	}
	
	public boolean match(Inet4Address addr)
	{
		byte[] b = addr.getAddress();
		int addrInt =	((b[0] & 0xFF) << 24) |
						((b[1] & 0xFF) << 16) |
						((b[2] & 0xFF) << 8)  |
						((b[3] & 0xFF) << 0);
		
		return Ipv4Addr == (addrInt & IPv4Mask);
	}
}
