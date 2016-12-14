package com.dreamwing.serverville.data;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.admin.AdminAPI.AdminPropertyPermissionsInfo;
import com.dreamwing.serverville.data.PropertyPermissions.PropertyInfo;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

public class RecordPermissionsManager
{
	private static final Logger l = LogManager.getLogger(RecordPermissionsManager.class);
	
	static ConcurrentMap<String,PropertyPermissions> PropertyPermissionsDb;
	static PropertyPermissions DefaultPermissions;
	public static PropertyInfo DefaultPermission;
	public static PropertyPermissions UserPermissions;
	
	@DatabaseTable(tableName = "record_permissions")
	public static class RecordPermissionsRecord
	{
		@DatabaseField(columnName="recordtype", id=true, canBeNull=false)
		public String RecordType;
		
		@DatabaseField(columnName="properties", canBeNull=false)
		public String Properties;
		
		@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
		public Date Created;
		
		@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
		public Date Modified;
		
		public static List<RecordPermissionsRecord> loadAllPermissionRecords() throws SQLException
		{
			return DatabaseManager.RecordPermissionsRecordDao.queryForAll();
		}
		
		public static RecordPermissionsRecord loadPermissionRecord(String recordType) throws SQLException
		{
			return DatabaseManager.RecordPermissionsRecordDao.queryForId(recordType);
		}
		
		public void save() throws SQLException
		{
			DatabaseManager.RecordPermissionsRecordDao.createOrUpdate(this);
		}
		
		public static void deleteByRecordType(String recordType) throws SQLException
		{
			DatabaseManager.RecordPermissionsRecordDao.deleteById(recordType);
		}
	}
	
	public static void init() throws SQLException
	{
		String defaultPerm = ServervilleMain.ServerProperties.getProperty("default_property_permission");
		try {
			DefaultPermission = PropertyPermissions.parsePropertyPermission(defaultPerm);
		} catch (JsonApiException e1)
		{
			DefaultPermission = new PropertyInfo();
			l.error("Invalid default_property_permission value: "+defaultPerm);
		}
		
		try {
			DefaultPermissions = new PropertyPermissions("default", null);
		} catch (IOException | JsonApiException e) {
			// Should not happen
			l.error("Exception creating default permissions: ", e);
		}
		
		try {
			UserPermissions = new PropertyPermissions("user", null);
		} catch (IOException | JsonApiException e) {
			// Should not happen
			l.error("Exception creating default user permissions: ", e);
		}
		
		reloadPermissions();
	}
	
	
	public static Collection<PropertyPermissions> reloadPermissions() throws SQLException
	{
		ConcurrentMap<String,PropertyPermissions> newDb = new ConcurrentHashMap<String,PropertyPermissions>();
		
		List<RecordPermissionsRecord> recordPermissions = RecordPermissionsRecord.loadAllPermissionRecords();
		for(RecordPermissionsRecord record : recordPermissions)
		{
			try {
				PropertyPermissions permissions = new PropertyPermissions(record);
				newDb.put(record.RecordType, permissions);
				
				if(record.RecordType.equals("user"))
					UserPermissions = permissions;
				
			} catch (IOException | JsonApiException e) {
				l.error("Invalid property permissions in database for recordType "+record.RecordType, e);
			}
			
		}
		
		PropertyPermissionsDb = newDb;
		
		return newDb.values();
	}
	
	public static PropertyPermissions getPermissions(String recordType)
	{
		if(recordType == null)
			return DefaultPermissions;
		PropertyPermissions perms = PropertyPermissionsDb.get(recordType);
		if(perms == null)
			return DefaultPermissions;
		return perms;
	}
	
	public static PropertyPermissions getPermissions(KeyDataRecord record)
	{
		if(record == null || record.Type == null)
			return DefaultPermissions;
		PropertyPermissions perms = PropertyPermissionsDb.get(record.Type);
		if(perms == null)
			return DefaultPermissions;
		return perms;
	}
	
	public static PropertyPermissions reloadPermissions(String recordType) throws SQLException
	{
		RecordPermissionsRecord record = RecordPermissionsRecord.loadPermissionRecord(recordType);
		if(record == null)
		{
			PropertyPermissionsDb.remove(recordType);
		}
		else
		{
			try {
				PropertyPermissions permissions = new PropertyPermissions(record);
				PropertyPermissionsDb.put(record.RecordType, permissions);
				
				if(record.RecordType.equals("user"))
					UserPermissions = permissions;
				
				return permissions;
			} catch (IOException | JsonApiException e) {
				l.error("Invalid property permissions in database for recordType "+record.RecordType, e);
			}
		}
		
		return null;
	}
	
	// TODO: send reload message to cluster
	public static void addPermissions(PropertyPermissions permissions) throws SQLException, JsonProcessingException
	{
		RecordPermissionsRecord record = new RecordPermissionsRecord();
		record.RecordType = permissions.DataType;
		record.Created = permissions.Created;
		record.Modified = permissions.Modified;
		
		AdminPropertyPermissionsInfo info = permissions.getPermissionsInfo();
		
		record.Properties = JSON.serializeToString(info.properties);
		
		record.save();
		
		permissions.Modified = record.Modified;
		
		PropertyPermissionsDb.put(permissions.DataType, permissions);
		
		if(record.RecordType.equals("user"))
			UserPermissions = permissions;
	}
	
	// TODO: send reload message to cluster
	public static void removePermissions(String recordType) throws SQLException
	{
		PropertyPermissionsDb.remove(recordType);
		RecordPermissionsRecord.deleteByRecordType(recordType);
	}
}
