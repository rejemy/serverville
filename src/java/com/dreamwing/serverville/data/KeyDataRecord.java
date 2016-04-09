package com.dreamwing.serverville.data;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "keydata")
public class KeyDataRecord {

	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String Id;
	
	@DatabaseField(columnName="type", canBeNull=false)
	public String Type;
	
	@DatabaseField(columnName="owner", canBeNull=false)
	public String Owner;
	
	@DatabaseField(columnName="parent", canBeNull=true)
	public String Parent;
	
	@DatabaseField(columnName="version", canBeNull=false)
	public int Version;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
}
