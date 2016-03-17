package com.dreamwing.serverville.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonDataType {
	NULL("null"),
	BOOLEAN("boolean"),
	NUMBER("number"),
	STRING("string"),
	JSON("json"),
	XML("xml"),
	DATETIME("datetime"),
	BYTES("bytes"),
	OBJECT("object");
	
	private final String value;
	
	JsonDataType(String v)
	{
		value = v;
	}
	
	@JsonValue
	public String value()
	{
	    return value;
	} 
	
	@JsonCreator
	public static JsonDataType fromString(String v)
	{
		return v == null ? null : JsonDataType.valueOf(v.toUpperCase());

	}
	
	public static JsonDataType fromObject(Object v)
	{
		if(v == null)
		{
			return null;
		}
		else if(v instanceof JsonDataType)
		{
			return (JsonDataType)v;
		}
		else if(v instanceof String)
		{
			return JsonDataType.valueOf(((String)v).toUpperCase());
		}
		else
		{
			throw new RuntimeException("Can't case to JsonDataType: "+v);
		}
	}
	
	public static JsonDataType fromKeyDataType(KeyDataTypes kdType)
	{
		switch(kdType)
		{
			case NULL:
				return JsonDataType.NULL;
			case FALSE:
			case TRUE:
				return JsonDataType.BOOLEAN;
			case BYTE:
			case BYTE_ZERO:
			case BYTE_ONE:
			case SHORT:
			case SHORT_ZERO:
			case SHORT_ONE:
			case INT:
			case INT_ZERO:
			case INT_ONE:
			case LONG:
			case LONG_ZERO:
			case LONG_ONE:
			case FLOAT:
			case FLOAT_ZERO:
			case FLOAT_ONE:
			case DOUBLE:
			case DOUBLE_ZERO:
			case DOUBLE_ONE:
				return JsonDataType.NUMBER;
			case STRING:
				return JsonDataType.STRING;
			case STRING_JSON:
				return JsonDataType.JSON;
			case STRING_XML:
				return JsonDataType.XML;
			case DATETIME:
				return JsonDataType.DATETIME;
			case BYTES:
			case JAVA_SERIALIZED:
				return JsonDataType.BYTES;
			case JSON:
				return JsonDataType.OBJECT;
			default:
				return null;
		}
		
	}
	
	
}
