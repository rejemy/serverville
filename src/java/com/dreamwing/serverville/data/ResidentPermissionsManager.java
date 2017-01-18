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

public class ResidentPermissionsManager
{
	private static final Logger l = LogManager.getLogger(ResidentPermissionsManager.class);
	
	static ConcurrentMap<String,PropertyPermissions> PropertyPermissionsDb;
	static PropertyPermissions DefaultPermissions;
	public static PropertyInfo DefaultPermission;
	
	@DatabaseTable(tableName = "resident_permissions")
	public static class ResidentPermissionsRecord
	{
		@DatabaseField(columnName="residenttype", id=true, canBeNull=false)
		public String ResidentType;
		
		@DatabaseField(columnName="properties", canBeNull=false)
		public String Properties;
		
		@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
		public Date Created;
		
		@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
		public Date Modified;
		
		public static List<ResidentPermissionsRecord> loadAllPermissionRecords() throws SQLException
		{
			return DatabaseManager.ResidentPermissionsRecordDao.queryForAll();
		}
		
		public static ResidentPermissionsRecord loadPermissionRecord(String recordType) throws SQLException
		{
			return DatabaseManager.ResidentPermissionsRecordDao.queryForId(recordType);
		}
		
		public void save() throws SQLException
		{
			DatabaseManager.ResidentPermissionsRecordDao.createOrUpdate(this);
		}
		
		public static void deleteByRecordType(String recordType) throws SQLException
		{
			DatabaseManager.ResidentPermissionsRecordDao.deleteById(recordType);
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
		
		reloadPermissions();
	}
	
	
	public static Collection<PropertyPermissions> reloadPermissions() throws SQLException
	{
		ConcurrentMap<String,PropertyPermissions> newDb = new ConcurrentHashMap<String,PropertyPermissions>();
		
		List<ResidentPermissionsRecord> residentPermissions = ResidentPermissionsRecord.loadAllPermissionRecords();
		for(ResidentPermissionsRecord resident : residentPermissions)
		{
			try {
				PropertyPermissions permissions = new PropertyPermissions(resident);
				newDb.put(resident.ResidentType, permissions);
				
			} catch (IOException | JsonApiException e) {
				l.error("Invalid property permissions in database for residentType "+resident.ResidentType, e);
			}
			
		}
		
		PropertyPermissionsDb = newDb;
		
		return newDb.values();
	}
	
	public static PropertyPermissions getPermissions(String residentType)
	{
		if(residentType == null)
			return DefaultPermissions;
		PropertyPermissions perms = PropertyPermissionsDb.get(residentType);
		if(perms == null)
			return DefaultPermissions;
		return perms;
	}

	public static PropertyPermissions reloadPermissions(String residentType) throws SQLException
	{
		ResidentPermissionsRecord record = ResidentPermissionsRecord.loadPermissionRecord(residentType);
		if(record == null)
		{
			PropertyPermissionsDb.remove(residentType);
		}
		else
		{
			try {
				PropertyPermissions permissions = new PropertyPermissions(record);
				PropertyPermissionsDb.put(record.ResidentType, permissions);
				
				return permissions;
			} catch (IOException | JsonApiException e) {
				l.error("Invalid property permissions in database for residentType "+record.ResidentType, e);
			}
		}
		
		return null;
	}
	
	public static void addPermissions(PropertyPermissions permissions) throws SQLException, JsonProcessingException
	{
		ResidentPermissionsRecord record = new ResidentPermissionsRecord();
		record.ResidentType = permissions.DataType;
		record.Created = permissions.Created;
		record.Modified = permissions.Modified;
		
		AdminPropertyPermissionsInfo info = permissions.getPermissionsInfo();
		
		record.Properties = JSON.serializeToString(info.properties);
		
		record.save();
		
		permissions.Modified = record.Modified;
		
		PropertyPermissionsDb.put(permissions.DataType, permissions);
		
	}
	
	public static void removePermissions(String residentType) throws SQLException
	{
		PropertyPermissionsDb.remove(residentType);
		ResidentPermissionsRecord.deleteByRecordType(residentType);
	}
}
