package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.db.KeyDataManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
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
	
	public static KeyDataRecord load(String id) throws SQLException
	{
		return DatabaseManager.KeyDataRecordDao.queryForId(id);
	}
	
	public static List<KeyDataRecord> loadChildren(String parentId) throws SQLException
	{
		QueryBuilder<KeyDataRecord, String> queryBuilder = DatabaseManager.KeyDataRecordDao.queryBuilder();
		return queryBuilder.where().eq("parent", parentId).query();
	}
	
	public static List<KeyDataRecord> loadByOwner(String ownerId) throws SQLException
	{
		QueryBuilder<KeyDataRecord, String> queryBuilder = DatabaseManager.KeyDataRecordDao.queryBuilder();
		return queryBuilder.where().eq("owner", ownerId).query();
	}
	
	public static List<KeyDataRecord> loadByType(String typeId) throws SQLException
	{
		QueryBuilder<KeyDataRecord, String> queryBuilder = DatabaseManager.KeyDataRecordDao.queryBuilder();
		return queryBuilder.where().eq("type", typeId).query();
	}
	
	public static List<KeyDataRecord> query(String ownerId, String typeId, String parentId) throws SQLException
	{
		Where<KeyDataRecord, String> queryBuilder = DatabaseManager.KeyDataRecordDao.queryBuilder().where();
		boolean and=false;
		if(ownerId != null)
		{
			queryBuilder.eq("owner", ownerId);
			and = true;
		}
		if(typeId != null)
		{
			if(and)
				queryBuilder.and();
			queryBuilder.eq("type", typeId);
			and = true;
		}
		if(parentId != null)
		{
			if(and)
				queryBuilder.and();
			queryBuilder.eq("parent", parentId);
			and = true;
		}
		
		return queryBuilder.query();
	}
	
	public static void delete(String id) throws SQLException
	{
		List<KeyDataRecord> children = KeyDataRecord.loadChildren(id);
		if(children != null)
		{
			for(KeyDataRecord child : children)
			{
				delete(child.Id);
			}
		}
		
		KeyDataManager.deleteAndPurgeAllKeys(id);
		DatabaseManager.KeyDataRecordDao.deleteById(id);
	}
}
