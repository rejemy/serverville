package com.dreamwing.serverville.data;


import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "agent_key")
public class AgentKey {

	@DatabaseField(columnName="key", id=true, canBeNull=false)
	public String Key;
	
	@DatabaseField(columnName="comment", canBeNull=true)
	public String Comment;
	
	@DatabaseField(columnName="iprange", canBeNull=true)
	public String IPRange;
	
	@DatabaseField(columnName="expiration", dataType=DataType.DATE_LONG, canBeNull=true)
	public Date Expiration;
	
	public void update() throws SQLException
	{
		DatabaseManager.AgentKeyDao.update(this);
	}
	
	public void delete() throws SQLException
	{
		DatabaseManager.AgentKeyDao.delete(this);
	}
	
	public static List<AgentKey> loadAll() throws SQLException
	{
		return DatabaseManager.AgentKeyDao.queryForAll();
	}
	
	public static AgentKey load(String key) throws SQLException
	{
		return DatabaseManager.AgentKeyDao.queryForId(key);
	}
	
	

}