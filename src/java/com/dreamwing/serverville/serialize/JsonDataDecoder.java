package com.dreamwing.serverville.serialize;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager.StringFlavor;

public class JsonDataDecoder {

	public static KeyDataItem MakeKeyDataFromJson(String keyname, JsonDataType userSuppliedType, Object data) throws Exception
	{
		KeyDataItem item = new KeyDataItem(keyname);
		
		if(userSuppliedType != null)
		{
			switch(userSuppliedType)
			{
			case NULL:
				if(data != null)
					throw new Exception("Tried to set non-null data with a null datatype");
				item.setNull();
				break;
			case BOOLEAN:
				setBoolean(item, data);
				break;
			case NUMBER:
				setNumber(item, data);
				break;
			case BYTES:
				setBytes(item, data);
				break;
			case DATETIME:
				setDatetime(item, data);
				break;
			case JSON:
				setString(item, data, StringFlavor.JSON);
				break;
			case STRING:
				setString(item, data, StringFlavor.TEXT);
				break;
			case XML:
				setString(item, data, StringFlavor.XML);
				break;
			}
		}
		
		if(data == null)
		{
			item.setNull();
			return item;
		}
		
		Class<?> objType = data.getClass();
		if(objType == Boolean.class)
		{
			item.set((boolean)data);
		}
		else if(objType == String.class)
		{
			item.set((String)data);
		}
		else if(objType == Integer.class)
		{
			item.set((int)data);
		}
		else if(objType == Double.class)
		{
			item.set((double)data);
		}
		else if(objType == Float.class)
		{
			item.set((float)data);
		}
		else if(objType == Long.class)
		{
			item.set((long)data);
		}
		else if(objType == Short.class)
		{
			item.set((short)data);
		}
		else if(objType == Byte.class)
		{
			item.set((byte)data);
		}
		else if(data instanceof Date)
		{
			item.set((Date)data);
		}
		else
		{
			throw new Exception("Unknown data type from JSON decoder: "+data);
		}
		
		return item;
	}
	
	static void setBoolean(KeyDataItem item, Object data) throws Exception
	{
		Class<?> objType = data.getClass();
		
		if(objType == Boolean.class)
			item.set((boolean)data);
		else if(objType == String.class)
			item.set(Boolean.parseBoolean((String)data));
		else
			throw new Exception("Couldn't turn "+data+" into a boolean");
	}
	
	static void setBytes(KeyDataItem item, Object data) throws Exception
	{
		Class<?> objType = data.getClass();
		if(objType != String.class)
			throw new Exception("Couldn't turn "+data+" into bytes");
		
		byte[] binary = Base64.getDecoder().decode((String)data);
		item.set(binary);
	}
	
	static void setDatetime(KeyDataItem item, Object data) throws Exception
	{
		Class<?> objType = data.getClass();
		
		if(data instanceof Date)
		{
			item.set((Date)data);
			return;
		}
		else if(objType == String.class)
		{
			DateFormat JsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			item.set(JsonDateFormat.parse((String)data));
		}
		else if(objType == Double.class)
		{
			item.set(new Date((long)(double)data));
		}
		else if(objType == Float.class)
		{
			item.set(new Date((long)(float)data));
		}
		else if(objType == Long.class)
		{
			item.set(new Date((long)data));
		}
		else
		{
			throw new Exception("Couldn't turn "+data+" into a date");
		}
	}
	
	static void setString(KeyDataItem item, Object data, StringFlavor flavor)
	{
		item.set(data.toString(), flavor);
	}
	
	static void setNumber(KeyDataItem item, Object data) throws Exception
	{
		Class<?> objType = data.getClass();
		
		if(objType == String.class)
		{
			item.set(Double.parseDouble((String)data));
		}
		else if(objType == Integer.class)
		{
			item.set((int)data);
		}
		else if(objType == Double.class)
		{
			item.set((double)data);
		}
		else if(objType == Float.class)
		{
			item.set((float)data);
		}
		else if(objType == Long.class)
		{
			item.set((long)data);
		}
		else if(objType == Short.class)
		{
			item.set((short)data);
		}
		else if(objType == Byte.class)
		{
			item.set((byte)data);
		}
		else
		{
			throw new Exception("Couldn't turn "+data+" into a number");
		}
	}
}
