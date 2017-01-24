package com.dreamwing.serverville.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;

public final class SVID {

	private static final Logger l = LogManager.getLogger(SVID.class);
	
	private static final long EpochOffset = 1420000000000L;
	private static final Random CounterGenerator = new Random();
	
	private static short ServerNum=0;
	private static short CounterStart=(short)CounterGenerator.nextInt();
	private static short Counter=CounterStart;
	private static long LastTime=0;
	
	public static void init()
	{
		ServerNum = ServervilleMain.getServerNumber();
	}
	
	public static String makeSVID()
	{
		short c;
		long epoch = ((System.currentTimeMillis()-EpochOffset) & 0xffffffffffffL);
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
					
					l.warn("SVID generator is running "+(LastTime-epoch)+" milliseconds behind");
				}
			}
		}
		
		ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
		epoch = LastTime << 16;
		buf.putLong(epoch);
		buf.position(6);
		buf.putShort(ServerNum);
		buf.putShort(c);
		
		return SVIDCodec.encode(buf.array());
	}
	
	public static String engineerSVID(long time, short serverNum, short counter)
	{
		long epoch = (time-EpochOffset) & 0xffffffffffffL;
		ByteBuffer buf = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN);
		epoch = epoch << 16;
		buf.putLong(epoch);
		buf.position(6);
		buf.putShort(serverNum);
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
