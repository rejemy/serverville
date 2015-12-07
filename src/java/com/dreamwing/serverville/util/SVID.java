package com.dreamwing.serverville.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Random;

public final class SVID {

	private static final long EpochOffset = 1420000000000L;
	private static final Random CounterGenerator = new Random();
	
	private static short ServerId=0;
	private static short CounterStart=(short)CounterGenerator.nextInt();
	private static short Counter=CounterStart;
	private static long LastTime=0;
	
	public static void init(short serverId)
	{
		ServerId = serverId;
		
		
	}
	
	public static String makeSVID()
	{
		short c;
		long epoch = (System.currentTimeMillis() & 0xffffffffffffL) - EpochOffset;
		synchronized(SVID.class)
		{
			if(epoch > LastTime)
			{
				LastTime = epoch;
				CounterStart=(short)CounterGenerator.nextInt();
				Counter = CounterStart;
				c = Counter++;
			}
			else
			{
				c = Counter++;
				if(Counter == CounterStart)
				{
					// Uh oh, wrapped around, increment epoch whether it's time yet or not
					LastTime++;
					CounterStart=(short)CounterGenerator.nextInt();
					Counter = CounterStart;
				}
			}
		}
		
		ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
		epoch = epoch << 16;
		buf.putLong(epoch);
		buf.position(6);
		buf.putShort(ServerId);
		buf.putShort(c);
		
		return SVIDCodec.encode(buf.array());
	}
	
	public static String engineerSVID(long time, short serverId, short counter)
	{
		long epoch = (time & 0xffffffffffffL) - EpochOffset;
		ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
		epoch = epoch << 16;
		buf.putLong(epoch);
		buf.position(6);
		buf.putShort(serverId);
		buf.putShort(counter);
		
		return SVIDCodec.encode(buf.array());
	}
	
	public static class SVIDInfo
	{
		public Date Timestamp;
		public short ServerId;
		public short Counter;
	}
	
	public static SVIDInfo getSVIDInfo(String svid)
	{
		SVIDInfo info = new SVIDInfo();
		
		byte[] data = SVIDCodec.decode(svid);
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
		
		long epoch = ((buf.getLong() >> 16) & 0xffffffffffffL) + EpochOffset;
		buf.position(6);
		info.ServerId = buf.getShort();
		info.Counter = buf.getShort();
		
		info.Timestamp = new Date(epoch);
		
		
		
		return info;
	}
}
