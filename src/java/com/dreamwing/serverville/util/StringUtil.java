package com.dreamwing.serverville.util;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

public final class StringUtil
{
	public static boolean isNullOrEmpty(String str)
	{
		return str == null || str.length() == 0;
	}
	
	public static void writeUTFNullSafe(ObjectDataOutput out, String str) throws IOException
	{
		if(str != null)
		{
			out.writeBoolean(true);
			out.writeUTF(str);
		}
		else
		{
			out.writeBoolean(false);
		}
	}
	
	public static String readUTFNullSafe(ObjectDataInput in) throws IOException
	{
		if(in.readBoolean())
			return in.readUTF();
		else
			return null;
	}
}
