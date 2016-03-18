package com.dreamwing.serverville.serialize;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.db.KeyDataManager.StringFlavor;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonDataDecoder {

	public static KeyDataItem MakeKeyDataFromJson(String keyname, JsonDataType userSuppliedType, Object data) throws JsonApiException
	{
		return MakeKeyDataFromJson(keyname, userSuppliedType, data, null);
	}
	
	public static KeyDataItem MakeKeyDataFromJson(String keyname, JsonDataType userSuppliedType, Object data, ScriptEngineContext ctx) throws JsonApiException
	{
		KeyDataItem item = new KeyDataItem(keyname);
		
		if(userSuppliedType != null)
		{
			switch(userSuppliedType)
			{
			case NULL:
				if(data != null)
					throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Tried to set non-null data with a null datatype");
				item.setNull();
				return item;
			case BOOLEAN:
				setBoolean(item, data);
				return item;
			case NUMBER:
				setNumber(item, data);
				return item;
			case BYTES:
				setBytes(item, data);
				return item;
			case DATETIME:
				setDatetime(item, data);
				return item;
			case JSON:
				setString(item, data, StringFlavor.JSON);
				return item;
			case STRING:
				setString(item, data, StringFlavor.TEXT);
				return item;
			case XML:
				setString(item, data, StringFlavor.XML);
				return item;
			case OBJECT:
				setObject(item, data, ctx);
				return item;
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
		else if(data instanceof Map || data instanceof List)
		{
			try {
				item.setJsonObject(data, ctx);
			} catch (JsonProcessingException | ScriptException e) {
				throw new JsonApiException(ApiErrors.DATA_CONVERSION, e.getMessage());
			}
		}
		else
		{
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Unknown data type from JSON decoder: "+data);
		}
		
		return item;
	}
	
	static void setBoolean(KeyDataItem item, Object data) throws JsonApiException
	{
		Class<?> objType = data.getClass();
		
		if(objType == Boolean.class)
			item.set((boolean)data);
		else if(objType == String.class)
			item.set(Boolean.parseBoolean((String)data));
		else
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Couldn't turn "+data+" into a boolean");
	}
	
	static void setBytes(KeyDataItem item, Object data) throws JsonApiException
	{
		Class<?> objType = data.getClass();
		if(objType != String.class)
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Couldn't turn "+data+" into bytes");
		
		byte[] binary = Base64.getDecoder().decode((String)data);
		item.set(binary);
	}
	
	static void setDatetime(KeyDataItem item, Object data) throws JsonApiException
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
			try {
				item.set(JsonDateFormat.parse((String)data));
			} catch (ParseException e) {
				throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Couldn't turn "+data+" into a date");
			}
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
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Couldn't turn "+data+" into a date");
		}
	}
	
	static void setString(KeyDataItem item, Object data, StringFlavor flavor)
	{
		item.set(data.toString(), flavor);
	}
	
	static void setNumber(KeyDataItem item, Object data) throws JsonApiException
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
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, "Couldn't turn "+data+" into a number");
		}
	}
	
	static void setObject(KeyDataItem item, Object data, ScriptEngineContext ctx) throws JsonApiException
	{
		try {
			item.setJsonObject(data, ctx);
		} catch (JsonProcessingException | ScriptException e) {
			throw new JsonApiException(ApiErrors.DATA_CONVERSION, e.getMessage());
		}
	}
	
}
