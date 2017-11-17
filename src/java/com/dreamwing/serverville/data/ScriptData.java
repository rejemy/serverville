package com.dreamwing.serverville.data;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "script")
public class ScriptData
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String Id;
	
	@DatabaseField(columnName="source", dataType=DataType.BYTE_ARRAY, canBeNull=false)
	public byte[] ScriptSourceBytes;
	private String ScriptSource;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	public static ScriptData findById(String id) throws SQLException
	{
		return DatabaseManager.ScriptDataDao.queryForId(id);
	}
	
	public static List<ScriptData> loadAll() throws SQLException
	{
		List<ScriptData> scripts = DatabaseManager.ScriptDataDao.queryForAll();
		
		scripts.sort(new ScriptIdComparator());
		
		return scripts;
	}
	
	public static List<ScriptData> loadAllSince(long since) throws SQLException
	{
		QueryBuilder<ScriptData, String> queryBuilder = DatabaseManager.ScriptDataDao.queryBuilder();
		
		return queryBuilder.where().gt("modified", since).query();
	
	}
	
	public void setScriptSource(String source)
	{
		ScriptSource = source;
		ScriptSourceBytes = ScriptSource.getBytes(StandardCharsets.UTF_8);
	}
	
	public String getScriptSource()
	{
		if(ScriptSource == null && ScriptSourceBytes != null)
			ScriptSource = new String(ScriptSourceBytes, StandardCharsets.UTF_8);
	
		return ScriptSource;
	}
	
	public void create() throws SQLException
	{
		DatabaseManager.ScriptDataDao.create(this);
	}
	
	public void update() throws SQLException
	{
		DatabaseManager.ScriptDataDao.update(this);
	}
	
	public void delete() throws SQLException
	{
		DatabaseManager.ScriptDataDao.delete(this);
	}
	
	public static class ScriptIdComparator implements Comparator<ScriptData>
	{

		@Override
		public int compare(ScriptData o1, ScriptData o2)
		{
			return o1.Id.compareTo(o2.Id);
		}
		
	}
}
