package com.dreamwing.serverville.data;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.db.KeyDataManager.StringFlavor;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.serialize.ByteDecoder;
import com.dreamwing.serverville.serialize.ByteEncoder;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;


public class KeyDataItem
{
	
	public String key;
	public Object value;
	public byte[] data;
	public KeyDataTypes datatype;
	public long created;
	public long modified;
	public boolean dirty;
	public Boolean deleted;
	
	public KeyDataItem() {}
	
	public KeyDataItem(String key)
	{
		this.key = key;
	}
	
	public KeyDataItem(String key, byte[] data, KeyDataTypes datatype)
	{
		this.key = key;
		this.data = data;
		this.datatype = datatype;
		
		decode();
	}
	
	public KeyDataItem(String key, Object value, KeyDataTypes datatype)
	{
		this.key = key;
		this.value = value;
		this.datatype = datatype;
	}
	
	public KeyDataItem(ResultSet rs) throws SQLException
	{
		key = rs.getString(2);
		data = rs.getBytes(3);
		datatype = KeyDataTypes.fromInt(rs.getInt(4));
		created = rs.getLong(5);
		modified = rs.getLong(6);
		deleted = (Boolean)rs.getObject(7);
		
		decode();
	}
	
	private void decode()
	{
		value = ByteDecoder.decode(data, datatype);
	}
	
	public void encode()
	{
		if(!dirty)
			return;
		data = ByteEncoder.encode(value, datatype);
		dirty = false;
		
		if(data != null && data.length > KeyDataManager.MaxItemBytes)
		{
			throw new IndexOutOfBoundsException("Data item "+key+" is too big, size: "+data.length+"/"+KeyDataManager.MaxItemBytes);
		}
	}
	
	public void setNull()
	{
		value = null;
		datatype = KeyDataTypes.NULL;
		dirty = true;
	}
	
	public Object asObject()
	{
		return value;
	}
	
	public Object asDecodedObject() throws JsonProcessingException, IOException
	{
		if(datatype == KeyDataTypes.JSON)
		{
			return asJsonObject();
		}
		return value;
	}
	
	public void setDeleted(boolean del)
	{
		deleted = del ? true : null;
	}
	
	public boolean isDeleted()
	{
		return deleted != null;
	}
	
	public void set(boolean val)
	{
		value = val;
		if(val)
			datatype = KeyDataTypes.TRUE;
		else
			datatype = KeyDataTypes.FALSE;
		dirty = true;
	}
	
	public boolean asBoolean()
	{
		if(datatype == KeyDataTypes.TRUE || datatype == KeyDataTypes.FALSE)
			return (boolean)value;
		return value != null;
	}
	
	public void set(byte val)
	{
		value = val;
		if(val == 0)
			datatype = KeyDataTypes.BYTE_ZERO;
		else if(val == 1)
			datatype = KeyDataTypes.BYTE_ONE;
		else
			datatype = KeyDataTypes.BYTE;
		dirty = true;
	}
	
	public byte asByte()
	{
		return (byte)value;
	}
	
	public void set(short val)
	{
		value = val;
		if(val == 0)
			datatype = KeyDataTypes.SHORT_ZERO;
		else if(val == 1)
			datatype = KeyDataTypes.SHORT_ONE;
		else
			datatype = KeyDataTypes.SHORT;
		dirty = true;
	}
	
	public short asShort()
	{
		return (short)value;
	}
	
	public void set(int val)
	{
		value = val;
		if(val == 0)
			datatype = KeyDataTypes.INT_ZERO;
		else if(val == 1)
			datatype = KeyDataTypes.INT_ONE;
		else
			datatype = KeyDataTypes.INT;
		dirty = true;
	}
	
	public int asInt()
	{
		return (int)value;
	}
	
	public void set(long val)
	{
		value = val;
		if(val == 0)
			datatype = KeyDataTypes.LONG_ZERO;
		else if(val == 1)
			datatype = KeyDataTypes.LONG_ONE;
		else
			datatype = KeyDataTypes.LONG;
		dirty = true;
	}
	
	public long asLong()
	{
		return (long)value;
	}
	
	public void set(float val)
	{
		value = val;
		if(val == 0.0f)
			datatype = KeyDataTypes.FLOAT_ZERO;
		else if(val == 1.0f)
			datatype = KeyDataTypes.FLOAT_ONE;
		else
			datatype = KeyDataTypes.FLOAT;
		dirty = true;
	}
	
	public float asFloat()
	{
		return (float)value;
	}
	
	public void set(double val)
	{
		value = val;
		if(val == 0.0)
			datatype = KeyDataTypes.DOUBLE_ZERO;
		else if(val == 1.0)
			datatype = KeyDataTypes.DOUBLE_ONE;
		else
			datatype = KeyDataTypes.DOUBLE;
		dirty = true;
	}
	
	public double asDouble()
	{
		return (double)value;
	}
	
	public void set(String val)
	{
		set(val, StringFlavor.TEXT);
	}
	
	public void set(String val, StringFlavor flavor)
	{
		value = val;
		if(val == null)
		{
			datatype = KeyDataTypes.NULL;
		}
		else
		{
			switch(flavor)
			{
			case TEXT:
				datatype = KeyDataTypes.STRING;
				break;
			case JSON:
				datatype = KeyDataTypes.STRING_JSON;
				break;
			case XML:
				datatype = KeyDataTypes.STRING_XML;
				break;
			}
		}
		dirty = true;
	}
	
	public String asString()
	{
		if(datatype == KeyDataTypes.STRING || datatype == KeyDataTypes.STRING_JSON || datatype == KeyDataTypes.STRING_XML)
			return (String)value;
		
		if(value == null)
			return null;
		
		return value.toString();
	}
	
	public void set(Date val)
	{
		value = val;
		if(val == null)
			datatype = KeyDataTypes.NULL;
		else
			datatype = KeyDataTypes.DATETIME;
		dirty = true;
	}
	
	public Date asDate()
	{
		return (Date)value;
	}
	
	public void set(byte[] val)
	{
		value = val;
		if(val == null)
			datatype = KeyDataTypes.NULL;
		else
			datatype = KeyDataTypes.BYTES;
		dirty = true;
	}
	
	public void setJsonObject(Object val, ScriptEngineContext ctx) throws JsonProcessingException, ScriptException
	{
		if(val == null)
		{
			value = null;
			datatype = KeyDataTypes.NULL;
		}
		else
		{
			if(ctx != null)
			{
				value = ctx.encodeJSON(val);
			}
			else
			{
				value = JSON.serializeToString(val);
			}
			datatype = KeyDataTypes.JSON;
		}
		dirty = true;
	}
	
	public Object asJsonObject() throws JsonProcessingException, IOException
	{
		if(value == null)
			return null;
		
		return JSON.deserialize((String)value);
	}
	
	public byte[] asBytes()
	{
		return (byte[])value;
	}
	
	private static final Predicate<String> ValidKeynameRegex = Pattern.compile("^[a-zA-Z_$][0-9a-zA-Z_$]*$").asPredicate();
	
	// The keyname validator should not let anything through that might have damaging SQL escape sequences
	public static boolean isValidKeyname(String key)
	{
		if(key == null)
			return false;
		return ValidKeynameRegex.test(key);
	}
	
	public static boolean isPrivateKeyname(String key)
	{
		if(key == null)
			return false;
		return key.charAt(0) == '_';
	}
}