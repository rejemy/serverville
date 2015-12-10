package com.dreamwing.serverville.db;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataTypes;
import com.dreamwing.serverville.db.KeyDataResultHandlers.*;
import com.dreamwing.serverville.net.HttpUtil.JsonApiException;
import com.dreamwing.serverville.serialize.ByteEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;

public class KeyDataManager {

	private static final Logger l = LogManager.getLogger(KeyDataManager.class);
	
	private static ItemResultSetHandler ItemHandler;
	private static ItemsResultSetHandler ItemListHandler;
	
	
	public static final long DeleteRetentionPeriod = 1000 * 60 * 60 * 24; // 24 hours in milliseconds
	
	public static final int MaxItemBytes = 62000;
	
	private static final String UpsertStatement = 
			"INSERT INTO `keydata_item` (`id`,`key`,`data`,`datatype`,`created`,`modified`) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `data`=VALUES(`data`), `datatype`=VALUES(`datatype`), `modified`=VALUES(`modified`), `deleted`=NULL;";
	private static final String UpsertWithDeleteStatement = 
			"INSERT INTO `keydata_item` (`id`,`key`,`data`,`datatype`,`created`,`modified`,`deleted`) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `data`=VALUES(`data`), `datatype`=VALUES(`datatype`), `modified`=VALUES(`modified`), `deleted`=VALUES(`deleted`);";
	
	
	
	public enum StringFlavor
	{
		TEXT,
		JSON,
		XML
	}
	
	public static void init() throws Exception
	{
		ItemHandler = new ItemResultSetHandler();
		ItemListHandler = new ItemsResultSetHandler();
		
		
		
		
		final Runnable deletePurger = new Runnable()
		{
			public void run()
			{
				try {
					purgeStaleDeletedKeys();
				} catch (SQLException e) {
					// Not much to do, I think, it's already been logged
				}
			}
		};
		
		ServervilleMain.ServiceScheduler.scheduleAtFixedRate(deletePurger, 1, 1, TimeUnit.HOURS);
	}

	
	public static File getLogDir()
	{
		return ServervilleMain.DataRoot.resolve("logs").toFile();
	}
	
	public static long saveKeyNull(String id, String key) throws SQLException
	{
		return saveKey(id, key, null, KeyDataTypes.NULL);
	}
	
	public static long saveKeyValue(String id, String key, boolean value) throws SQLException
	{
		KeyDataTypes t = value ? KeyDataTypes.TRUE : KeyDataTypes.FALSE;
		return saveKey(id, key, null, t);
	}
	
	public static long saveKeyValue(String id, String key, byte value) throws SQLException
	{
		if(value == 0)
			return saveKey(id, key, null, KeyDataTypes.BYTE_ZERO);
		else if(value == 1)
			return saveKey(id, key, null, KeyDataTypes.BYTE_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.BYTE);
	}
	
	public static long saveKeyValue(String id, String key, short value) throws SQLException
	{
		if(value == 0)
			return saveKey(id, key, null, KeyDataTypes.SHORT_ZERO);
		else if(value == 1)
			return saveKey(id, key, null, KeyDataTypes.SHORT_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.SHORT);
	}
	
	public static long saveKeyValue(String id, String key, int value) throws SQLException
	{
		if(value == 0)
			return saveKey(id, key, null, KeyDataTypes.INT_ZERO);
		else if(value == 1)
			return saveKey(id, key, null, KeyDataTypes.INT_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.INT);
	}
	
	public static long saveKeyValue(String id, String key, long value) throws SQLException
	{
		if(value == 0)
			return saveKey(id, key, null, KeyDataTypes.LONG_ZERO);
		else if(value == 1)
			return saveKey(id, key, null, KeyDataTypes.LONG_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.LONG);
	}
	
	public static long saveKeyValue(String id, String key, float value) throws SQLException
	{
		if(value == 0.0f)
			return saveKey(id, key, null, KeyDataTypes.FLOAT_ZERO);
		else if(value == 1.0f)
			return saveKey(id, key, null, KeyDataTypes.FLOAT_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.FLOAT);
	}
	
	public static long saveKeyValue(String id, String key, double value) throws SQLException
	{
		if(value == 0.0)
			return saveKey(id, key, null, KeyDataTypes.DOUBLE_ZERO);
		else if(value == 1.0)
			return saveKey(id, key, null, KeyDataTypes.DOUBLE_ONE);
		else
			return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.DOUBLE);
	}
	
	public static long saveKeyValue(String id, String key, String value) throws SQLException
	{
		return saveKeyValue(id, key, value, StringFlavor.TEXT);
	}
	
	public static long saveKeyValue(String id, String key, String value, StringFlavor flavor) throws SQLException
	{
		if(value == null)
			return saveKey(id, key, null, KeyDataTypes.NULL);
		
		KeyDataTypes t = KeyDataTypes.STRING;
		switch(flavor)
		{
		case TEXT:
			t = KeyDataTypes.STRING;
			break;
		case JSON:
			t = KeyDataTypes.STRING_JSON;
			break;
		case XML:
			t = KeyDataTypes.STRING_XML;
			break;
		}
		return saveKey(id, key, ByteEncoder.encode(value), t);
	}
	
	public static long saveKeyValue(String id, String key, Date value) throws SQLException
	{
		if(value == null)
			return saveKey(id, key, null, KeyDataTypes.NULL);
		
		return saveKey(id, key, ByteEncoder.encode(value), KeyDataTypes.DATETIME);
	}
	
	public static long saveKeyValue(String id, String key, byte[] data) throws SQLException
	{
		return saveKey(id, key, data, KeyDataTypes.BYTES);
	}
	
	public static long saveKey(String id, String key, byte[] data, KeyDataTypes datatype) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(key == null || key.length() == 0)
		{
			l.error("Data item has invalid key: "+key);
			throw new IllegalArgumentException("Invalid key");
		}
		
		if(data != null && data.length > MaxItemBytes)
		{
			l.error("Data item "+id+"."+key+" is too big, size: "+data.length+"/"+MaxItemBytes);
			throw new IllegalArgumentException("Data item too big");
		}
		
		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().update(UpsertStatement, id, key, data, datatype.toInt(), time, time);
		} catch (SQLException e) {
			l.error("Error saving item "+id+" to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static long saveKey(String id, KeyDataItem key) throws SQLException, JsonProcessingException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(key == null)
		{
			l.error("Null key passed into saveKey");
			throw new IllegalArgumentException("Null keys passed into saveKeys");
		}
		
		key.encode();

		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().update(UpsertStatement, id, key.key, key.data, key.datatype.toInt(), time, time);
		} catch (SQLException e) {
			l.error("Error saving item "+id+" to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static long saveKeys(String id, Collection<KeyDataItem> keys) throws SQLException, JsonProcessingException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(keys == null)
		{
			l.error("Null keys passed into saveKeys");
			throw new IllegalArgumentException("Null keys passed into saveKeys");
		}
		
		if(keys.size() == 0)
		{
			// Warning?
			return 0;
		}
		
		long time = System.currentTimeMillis();
		
		Object[][] params = new Object[keys.size()][];
		
		int i = 0;
		for(KeyDataItem data : keys)
		{
			if(data.key == null || data.key.length() == 0)
			{
				l.error("Data item has invalid key: "+data.key);
				throw new IllegalArgumentException("Invalid key");
			}
			
			data.encode();
			
			params[i++] = new Object[] {id, data.key, data.data, data.datatype.toInt(), time, time};
		}
		
		try {
			DatabaseManager.getServer().batch(UpsertStatement, params);
		} catch (SQLException e) {
			l.error("Error saving item batch to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static long saveOnlyDirtyKeys(String id, Collection<KeyDataItem> keys) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(keys == null)
		{
			l.error("Null keys passed into saveKeys");
			throw new IllegalArgumentException("Null keys passed into saveKeys");
		}
		
		if(keys.size() == 0)
		{
			// Warning?
			return 0;
		}
		
		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().batch(UpsertWithDeleteStatement, id, time, keys);
		} catch (SQLException e) {
			l.error("Error saving item batch to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static KeyDataItem loadKey(String id, String key) throws SQLException
	{
		return loadKey(id, key, false);
	}
	
	public static KeyDataItem loadKey(String id, String key, boolean includeDeleted) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(key == null || key.length() == 0)
		{
			l.error("Data item has invalid key: "+key);
			throw new IllegalArgumentException("Invalid key");
		}
		
		try {
			KeyDataItem result;
			if(includeDeleted)
				result = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=? AND `key`=?", ItemHandler, id, key);
			else
				result = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=? AND `key`=? AND `deleted` IS NULL;", ItemHandler, id, key);
			return result;
		} catch (SQLException e) {
			l.error("Error loading item "+id+" to database ", e);
			throw e;
		}

	}
	
	private static String collectionToList(Collection<String> keys) throws JsonApiException
	{
		StringBuilder str = new StringBuilder();
		
		str.append("(");
		
		boolean first = true;
		for(String key : keys)
		{
			// The keyname validator should not let anything through that might have damaging SQL escape sequences
			if(!KeyDataItem.isValidKeyname(key))
				throw new JsonApiException("Invalid key name: "+key);
			
			if(!first)
				str.append(",");
			else
				first = false;
			str.append('"'+key+'"');
		}
		
		str.append(")");
		
		return str.toString();
	}
	
	public static List<KeyDataItem> loadKeys(String id, Collection<String> keys) throws SQLException, JsonApiException
	{
		return loadKeys(id, keys, false);
	}
	
	public static List<KeyDataItem> loadKeys(String id, Collection<String> keys, boolean includeDeleted) throws SQLException, JsonApiException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(keys == null || keys.size() == 0)
		{
			l.error("Didn't supply any keys");
			throw new IllegalArgumentException("Invalid key");
		}
		
		try {
			List<KeyDataItem> results = null;

			String queryStr = null;
			
			// Key names are not SQL escaped here, so we have to be really dang sure they've been validated
			if(includeDeleted)
				queryStr = "SELECT * FROM `keydata_item` WHERE `id`=? AND `key` IN "+collectionToList(keys)+";";
			else
				queryStr = "SELECT * FROM `keydata_item` WHERE `id`=? AND `key` IN "+collectionToList(keys)+" AND `deleted` IS NULL;";
			
			results = DatabaseManager.getServer().query(queryStr, ItemListHandler, id);
			return results;
		} catch (SQLException e) {
			l.error("Error loading item "+id+" to database ", e);
			throw e;
		}
	}
	
	public static List<KeyDataItem> loadKeysSince(String id, Collection<String> keys, long time) throws SQLException, JsonApiException
	{
		return loadKeysSince(id, keys, time, false);
	}
	
	public static List<KeyDataItem> loadKeysSince(String id, Collection<String> keys, long time, boolean includeDeleted) throws SQLException, JsonApiException
	{
		if(time <= 0)
			return loadKeys(id, keys);
		
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(keys == null || keys.size() == 0)
		{
			l.error("Didn't supply any keys");
			throw new IllegalArgumentException("Invalid key");
		}
		
		try {
			List<KeyDataItem> results = null;
			
			String queryStr = null;
			
			// Key names are not SQL escaped here, so we have to be really dang sure they've been validated
			if(includeDeleted)
				queryStr = "SELECT * FROM `keydata_item` WHERE `id`=? AND `key` IN "+collectionToList(keys)+" AND `modified`>?;";
			else
				queryStr = "SELECT * FROM `keydata_item` WHERE `id`=? AND `key` IN "+collectionToList(keys)+" AND `modified`>? AND `deleted` IS NULL;";
			
			results = DatabaseManager.getServer().query(queryStr, ItemListHandler, id, time);
			return results;
		} catch (SQLException e) {
			l.error("Error loading item "+id+" to database ", e);
			throw e;
		}
	}
	
	public static List<KeyDataItem> loadAllKeys(String id) throws SQLException
	{
		return loadAllKeys(id, false);
	}
	
	public static List<KeyDataItem> loadAllKeys(String id, boolean includeDeleted) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		try {
			List<KeyDataItem> results = null;
			if(includeDeleted)
				results = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=?", ItemListHandler, id);
			else
				results = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=? AND `deleted` IS NULL;", ItemListHandler, id);
			return results;
		} catch (SQLException e) {
			l.error("Error loading item "+id+" to database ", e);
			throw e;
		}

	}
	
	public static List<KeyDataItem> loadAllKeysSince(String id, long time) throws SQLException
	{
		return loadAllKeysSince(id, time, false);
	}
	
	public static List<KeyDataItem> loadAllKeysSince(String id, long time, boolean includeDeleted) throws SQLException
	{
		if(time <= 0)
			return loadAllKeys(id, includeDeleted);
		
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}

		try {
			List<KeyDataItem> results = null;
			if(includeDeleted)
				results = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=? AND `modified`>?", ItemListHandler, id, time);
			else
				results = DatabaseManager.getServer().query("SELECT * FROM `keydata_item` WHERE `id`=? AND `modified`>? AND `deleted` IS NULL;", ItemListHandler, id, time);
			return results;
		} catch (SQLException e) {
			l.error("Error loading item "+id+" to database ", e);
			throw e;
		}
	}
	
	public static long deleteKey(String id, String key) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(key == null || key.length() == 0)
		{
			l.error("Data item has invalid key: "+key);
			throw new IllegalArgumentException("Invalid key");
		}
		
		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().update("UPDATE `keydata_item` SET `modified`=?, `deleted`=1 WHERE `id`=? AND `key`=?;", time, id, key);
		} catch (SQLException e) {
			l.error("Error deleting item "+id+" to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static long deleteAllKeys(String id) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().update("UPDATE `keydata_item` SET `modified`=?, `deleted`=1 WHERE `id`=?", time, id);
		} catch (SQLException e) {
			l.error("Error deleting item "+id+" to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static long undeleteKey(String id, String key) throws SQLException
	{
		if(id == null || id.length() == 0)
		{
			l.error("Data item has invalid id: "+id);
			throw new IllegalArgumentException("Invalid id");
		}
		
		if(key == null || key.length() == 0)
		{
			l.error("Data item has invalid key: "+key);
			throw new IllegalArgumentException("Invalid key");
		}
		
		long time = System.currentTimeMillis();
		
		try {
			DatabaseManager.getServer().update("UPDATE `keydata_item` SET `modified`=?, `deleted`=NULL WHERE `id`=? AND `key`=?;", time, id, key);
		} catch (SQLException e) {
			l.error("Error deleting item "+id+" to database ", e);
			throw e;
		}
		
		return time;
	}
	
	public static void purgeStaleDeletedKeys() throws SQLException
	{
		long since = System.currentTimeMillis() - DeleteRetentionPeriod;
		
		try {
			DatabaseManager.getServer().update("DELETE FROM `keydata_item` WHERE `modified`<? AND `deleted` IS NOT NULL;", since);
		} catch (SQLException e) {
			l.error("Error purging deleted items", e);
			throw e;
		}
	}
	
	public static void purgeDeletedKeysFor(String id) throws SQLException
	{
		try {
			DatabaseManager.getServer().update("DELETE FROM `keydata_item` WHERE `id`=? AND `deleted` IS NOT NULL;", id);
		} catch (SQLException e) {
			l.error("Error purging deleted items", e);
			throw e;
		}
	}
	

}
