package com.dreamwing.serverville.serialize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Date;

import com.dreamwing.serverville.data.KeyDataTypes;

public class ByteEncoder {

	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	public static byte[] encode(byte value)
	{
		return new byte[] { value };
	}
	
	public static byte[] encode(short value)
	{
		byte[] encoded = new byte[Short.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort(value);
		return encoded;
	}
	
	public static byte[] encode(int value)
	{
		byte[] encoded = new byte[Integer.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(value);
		return encoded;
	}
	
	public static byte[] encode(long value)
	{
		byte[] encoded = new byte[Long.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putLong(value);
		return encoded;
	}
	
	public static byte[] encode(float value)
	{
		byte[] encoded = new byte[Float.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putFloat(value);
		return encoded;
	}
	
	public static byte[] encode(double value)
	{
		byte[] encoded = new byte[Double.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putDouble(value);
		return encoded;
	}
	
	public static byte[] encode(String value)
	{
		return value.getBytes(UTF8_CHARSET);
	}
	
	public static byte[] encode(Date value)
	{
		long timeValue = value.getTime();
		byte[] encoded = new byte[Long.BYTES];
		ByteBuffer buf = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
		buf.putLong(timeValue);
		return encoded;
	}
	
	public static byte[] encode(Object valueObj, KeyDataTypes valueType)
	{
		switch(valueType)
		{
		case BYTE:
			return encode((byte)valueObj);
		case BYTES:
			return (byte[])valueObj;
		case BYTE_ONE:
			return null;
		case BYTE_ZERO:
			return null;
		case DATETIME:
			return encode((Date)valueObj);
		case DICT:
			break;
		case DOUBLE:
			return encode((double)valueObj);
		case DOUBLE_ONE:
			return null;
		case DOUBLE_ZERO:
			return null;
		case FALSE:
			return null;
		case FLOAT:
			return encode((float)valueObj);
		case FLOAT_ONE:
			return null;
		case FLOAT_ZERO:
			return null;
		case INT:
			return encode((int)valueObj);
		case INT_ONE:
			return null;
		case INT_ZERO:
			return null;
		case JAVA_SERIALIZED:
			return null;
		case LIST:
			break;
		case LONG:
			return encode((long)valueObj);
		case LONG_ONE:
			return null;
		case LONG_ZERO:
			return null;
		case NULL:
			return null;
		case SHORT:
			return encode((short)valueObj);
		case SHORT_ONE:
			return null;
		case SHORT_ZERO:
			return null;
		case STRING:
		case STRING_JSON:
		case STRING_XML:
			return encode((String)valueObj);
		case TRUE:
			return null;
		
		}
		
		return null;
	}
}
