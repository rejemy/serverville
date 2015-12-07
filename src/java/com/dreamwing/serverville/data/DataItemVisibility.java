package com.dreamwing.serverville.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataItemVisibility {

	PRIVATE("private", 0),
	PUBLIC("public", 1);
	//WRITABLE("writable", 2);
	
	private final byte dbId;
	private final String value;
	
	DataItemVisibility(String v, int id)
	{
		value = v;
		dbId = (byte)id;
	}
	
	public byte getDbId() { return dbId;}
	
	@JsonValue
	public String value()
	{
	    return value;
	} 
	
	@JsonCreator
	public static DataItemVisibility fromString(String v)
	{
		return v == null ? null : DataItemVisibility.valueOf(v.toUpperCase());

	}
	
	
	private static DataItemVisibility[] ValueLookup;
	
	static
	{
		ValueLookup = new DataItemVisibility[3];
		DataItemVisibility[] values = DataItemVisibility.values();
		for(DataItemVisibility val : values)
		{
			ValueLookup[val.dbId] = val;
		}
	}
	
	public static DataItemVisibility fromByte(byte id)
	{
		return ValueLookup[id];
	}
}
