package com.dreamwing.serverville.serialize;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import com.dreamwing.serverville.data.KeyDataTypes;


public final class ByteDecoder
{
	public static Object decode(byte[] data, KeyDataTypes dbType)
	{
		switch(dbType)
		{
		case NULL:
			return null;
		case FALSE:
			return false;
		case TRUE:
			return true;
		case BYTE:
			return decodeByte(data);
		case BYTES:
			return data;
		case BYTE_ONE:
			return (byte)1;
		case BYTE_ZERO:
			return (byte)0;
		case DATETIME:
			return decodeDate(data);
		case DICT:
			break;
		case DOUBLE:
			return decodeDouble(data);
		case DOUBLE_ONE:
			return 1.0;
		case DOUBLE_ZERO:
			return 0.0;
		case FLOAT:
			return decodeFloat(data);
		case FLOAT_ONE:
			return 1.0f;
		case FLOAT_ZERO:
			return 0.0f;
		case INT:
			return decodeInt(data);
		case INT_ONE:
			return 1;
		case INT_ZERO:
			return 0;
		case JAVA_SERIALIZED:
			break;
		case LIST:
			break;
		case LONG:
			return decodeLong(data);
		case LONG_ONE:
			return 1L;
		case LONG_ZERO:
			return 0L;
		case SHORT:
			return decodeShort(data);
		case SHORT_ONE:
			return (short)1;
		case SHORT_ZERO:
			return (short)0;
		case STRING:
		case STRING_JSON:
		case STRING_XML:
		case JSON:
			return decodeString(data);
		}
		
		return null;
	}
	
	public static byte decodeByte(byte[] data)
	{
		if(data.length < Byte.BYTES)
			throw new BufferUnderflowException();
		return data[0];
	}
	
	public static String decodeString(byte[] data)
	{
		return new String(data, ByteEncoder.UTF8_CHARSET);
	}
	
	public static short decodeShort(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		return buf.getShort();
	}
	
	public static int decodeInt(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		return buf.getInt();
	}
	
	public static long decodeLong(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		return buf.getLong();
	}
	
	public static float decodeFloat(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		return buf.getFloat();
	}
	
	public static double decodeDouble(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		return buf.getDouble();
	}
	
	public static Date decodeDate(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
		long datetime = buf.getLong();
		return new Date(datetime);
	}
	
}
