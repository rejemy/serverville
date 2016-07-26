package com.dreamwing.serverville.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SubnetMask {

	private String AddrMaskString;
	
	private int Ipv4Addr = 0;
	private int IPv4Mask = 0;
	
	private long Ipv6UpperAddr = 0;
	private long IPv6UpperMask = 0;
	
	private long Ipv6LowerAddr = 0;
	private long IPv6LowerMask = 0;
	
	public SubnetMask(String addrMask) throws UnknownHostException
	{
		AddrMaskString = addrMask;
		
		String[] parts = addrMask.split("/");
		if(parts.length < 1 || parts.length > 2)
			throw new IllegalArgumentException("Invalid address mask format: "+addrMask);
		
		InetAddress addr = InetAddress.getByName(parts[0]);
		if(addr instanceof Inet4Address)
		{
			int bits = 32;
			if(parts.length == 2)
				bits = Integer.parseInt(parts[1]);
			
			if(bits > 32 || bits < 0)
				throw new IllegalArgumentException("Invalid address mask format: "+addrMask);
			
			Inet4Address addr4 = (Inet4Address)addr;
			byte[] b = addr4.getAddress();
			Ipv4Addr =	 ((b[0] & 0xFF) << 24) |
	                     ((b[1] & 0xFF) << 16) |
	                     ((b[2] & 0xFF) << 8)  |
	                     ((b[3] & 0xFF) << 0);
			
			IPv4Mask = (0xffffffff << (32-bits));
			Ipv4Addr = Ipv4Addr & IPv4Mask;
		}
		else if(addr instanceof Inet6Address)
		{
			int bits = 128;
			if(parts.length == 2)
				bits = Integer.parseInt(parts[1]);
			
			if(bits > 128 || bits < 0)
				throw new IllegalArgumentException("Invalid address mask format: "+addrMask);
			
			Inet6Address addr6 = (Inet6Address)addr;
			byte[] b = addr6.getAddress();
			
			Ipv6UpperAddr = ((b[0] & 0xFF) << 56) |
		                    ((b[1] & 0xFF) << 48) |
		                    ((b[2] & 0xFF) << 40) |
		                    ((b[3] & 0xFF) << 32) |
		                    ((b[4] & 0xFF) << 24) |
		                    ((b[5] & 0xFF) << 16) |
		                    ((b[6] & 0xFF) << 8)  |
		                    ((b[7] & 0xFF) << 0);
			
			Ipv6LowerAddr = ((b[8]  & 0xFF) << 56) |
		                    ((b[9]  & 0xFF) << 48) |
		                    ((b[10] & 0xFF) << 40) |
		                    ((b[11] & 0xFF) << 32) |
		                    ((b[12] & 0xFF) << 24) |
		                    ((b[13] & 0xFF) << 16) |
		                    ((b[14] & 0xFF) << 8)  |
		                    ((b[15] & 0xFF) << 0);
			
			bits = 128 - bits;
			
			if(bits > 64)
				IPv6UpperMask = (0xffffffffffffffffL << (bits-64));
			else
				IPv6UpperMask = 0xffffffffffffffffL;
			
			if(bits >= 64)
				IPv6LowerMask = 0L;
			else
				IPv6LowerMask = (0xffffffffffffffffL << bits);
			
			Ipv6UpperAddr = Ipv6UpperAddr & IPv6UpperMask;
			Ipv6LowerAddr = Ipv6LowerAddr & IPv6LowerMask;
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
		else if(addr instanceof Inet6Address)
			return match((Inet6Address)addr);
		
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
	
	public boolean match(Inet6Address addr)
	{
		byte[] b = addr.getAddress();
		
		long addrUpper = ((b[0] & 0xFF) << 56) |
                ((b[1] & 0xFF) << 48) |
                ((b[2] & 0xFF) << 40) |
                ((b[3] & 0xFF) << 32) |
                ((b[4] & 0xFF) << 24) |
                ((b[5] & 0xFF) << 16) |
                ((b[6] & 0xFF) << 8)  |
                ((b[7] & 0xFF) << 0);

		long addrLower = ((b[8]  & 0xFF) << 56) |
                ((b[9]  & 0xFF) << 48) |
                ((b[10] & 0xFF) << 40) |
                ((b[11] & 0xFF) << 32) |
                ((b[12] & 0xFF) << 24) |
                ((b[13] & 0xFF) << 16) |
                ((b[14] & 0xFF) << 8)  |
                ((b[15] & 0xFF) << 0);
		
		return Ipv6UpperAddr == (addrUpper & IPv6UpperMask) && Ipv6LowerAddr == (addrLower & IPv6LowerMask);
	}
}
