package com.dreamwing.serverville.data;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.dreamwing.serverville.admin.AdminAPI.AdminPropertyPermissionsInfo;
import com.dreamwing.serverville.data.PropertyPermissionsManager.PropertyPermissionsRecord;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class PropertyPermissions
{
	public static class PropertyInfo
	{
		public boolean IsPrefix;
		public boolean OwnerWritable;
		public boolean OwnerReadable;
		public boolean GlobalReadable;
	}
	
	public String RecordType;
	NavigableMap<String,PropertyInfo> Properties;
	public Date Created;
	public Date Modified;
	
	PropertyInfo DefaultPermission;
	
	public PropertyPermissions(String recordType, String jsonProperties) throws JsonParseException, JsonMappingException, IOException, JsonApiException
	{
		RecordType = recordType;
		Created = new Date();
		
		initProperties(jsonProperties);
	}
	
	public PropertyPermissions(PropertyPermissionsRecord record) throws JsonParseException, JsonMappingException, IOException, JsonApiException
	{
		RecordType = record.RecordType;
		Created = record.Created;
		Modified = record.Modified;
		
		initProperties(record.Properties);
	}
	
	void initProperties(String jsonProperties) throws JsonParseException, JsonMappingException, IOException, JsonApiException
	{
		
		Properties = new TreeMap<String,PropertyInfo>();
		
		if(jsonProperties != null)
		{
			Map<String,String> permMap = JSON.deserialize(jsonProperties, JSON.StringStringMapType);
			for(Map.Entry<String,String> entry : permMap.entrySet())
			{
				String propertyPrefix = entry.getKey();
				String permissionType = entry.getValue();
				
				PropertyInfo info = null;
				try
				{
					info = parsePropertyPermission(permissionType);
				}
				catch(JsonApiException e)
				{
					throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid permission "+permissionType+" for property "+propertyPrefix);
				}
				
				if(propertyPrefix.equals("*"))
				{
					DefaultPermission = info;
					continue;
				}
				
				if(propertyPrefix.endsWith("*"))
				{
					propertyPrefix = propertyPrefix.substring(0, propertyPrefix.length()-1);
					info.IsPrefix = true;
				}
				
				if(!KeyDataItem.isValidKeyname(propertyPrefix))
					throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid property name: "+propertyPrefix);
				
				Properties.put(propertyPrefix, info);
			}
		}
		
		if(DefaultPermission == null)
		{
			DefaultPermission = PropertyPermissionsManager.DefaultPermission;
		}
	}
	
	public static PropertyInfo parsePropertyPermission(String permissionType) throws JsonApiException
	{
		PropertyInfo info = new PropertyInfo();
		
		switch(permissionType.toLowerCase())
		{
		case "s":
			break;
		case "r":
			info.OwnerReadable = true;
			break;
		case "w":
			info.OwnerReadable = true;
			info.OwnerWritable = true;
			break;
		case "rr":
			info.OwnerReadable = true;
			info.GlobalReadable = true;
			break;
		case "wr":
			info.OwnerReadable = true;
			info.OwnerWritable = true;
			info.GlobalReadable = true;
			break;
		default:
			throw new JsonApiException(ApiErrors.INVALID_INPUT, "Invalid permission "+permissionType);
		}
		
		return info;
	}
	
	public PropertyInfo getPropertyPermission(String propname)
	{
		Map.Entry<String,PropertyInfo> entry = Properties.floorEntry(propname);
		if(entry == null)
			return DefaultPermission;
	
		PropertyInfo permissions = entry.getValue();
		String propPrefix = entry.getKey();
		
		if(permissions.IsPrefix)
		{
			if(propname.startsWith(propPrefix))
				return permissions;
			else
				return DefaultPermission;
				
		}
		else
		{
			if(propPrefix.equals(propname))
				return permissions;
			else
				return DefaultPermission;
		}
	}
	
	public boolean isOwnerWritable(String propname)
	{
		return getPropertyPermission(propname).OwnerWritable;
	}
	
	public boolean isOwnerReadable(String propname)
	{
		return getPropertyPermission(propname).OwnerReadable;
	}
	
	public boolean isGlobalReadable(String propname)
	{
		return getPropertyPermission(propname).GlobalReadable;
	}
	
	private static String encodePermissionInfo(PropertyInfo info)
	{
		if(info.GlobalReadable)
		{
			if(info.OwnerWritable)
				return "wr";
			else
				return "rr";
		}
		else
		{
			if(info.OwnerWritable)
				return "w";
			else if(info.OwnerReadable)
				return "r";
			else
				return "s";
		}
	}
	
	public AdminPropertyPermissionsInfo getPermissionsInfo()
	{
		AdminPropertyPermissionsInfo permsinfo = new AdminPropertyPermissionsInfo();
		
		permsinfo.record_type = RecordType;
		permsinfo.created = Created.getTime();
		if(Modified != null)
			permsinfo.modified = Modified.getTime();
		permsinfo.properties = new HashMap<String,String>();
		
		for(Map.Entry<String,PropertyInfo> entry : Properties.entrySet())
		{
			String key = entry.getKey();
			PropertyInfo entryInfo = entry.getValue();
			if(entryInfo.IsPrefix)
				key += "*";
			
			permsinfo.properties.put(key, encodePermissionInfo(entryInfo));
		}
		
		permsinfo.properties.put("*", encodePermissionInfo(DefaultPermission));
		
		return permsinfo;
	}
}
