package com.dreamwing.serverville.data;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.db.KeyDataManager;

public class KeyData {
	
	private String Id;
	private Map<String,KeyDataItem> Keys;
	private long MostRecent=0;
	
	public KeyData(String id)
	{
		Id = id;
		Keys = new HashMap<String,KeyDataItem>();
	}
	
	public String getId() { return Id; }
	
	public void loadAll() throws Exception
	{
		List<KeyDataItem> data = KeyDataManager.loadAllKeys(Id);
		Keys.clear();
		if(data == null)
			return;
		
		for(KeyDataItem item : data)
		{
			Keys.put(item.key, item);
			if(item.modified > MostRecent)
				MostRecent = item.modified;
		}
	}
	
	public void refresh() throws Exception
	{
		List<KeyDataItem> data = KeyDataManager.loadAllKeysSince(Id, MostRecent, true);

		if(data == null)
			return;
		
		for(KeyDataItem item : data)
		{
			Keys.put(item.key, item);
			if(item.modified > MostRecent)
				MostRecent = item.modified;
		}
	}
	
	public void save() throws Exception
	{
		KeyDataManager.saveOnlyDirtyKeys(Id, Keys.values());
	}
	
	public KeyDataItem getKeyData(String key)
	{
		KeyDataItem data = Keys.get(key);
		if(data == null || data.isDeleted())
			return null;
		
		return data;
	}
	
	public KeyDataItem getOrCreateKeyData(String key)
	{
		if(key == null)
			throw new IllegalArgumentException("Null keys not allowd");
		KeyDataItem data = Keys.get(key);
		if(data != null)
		{
			data.setDeleted(false);
			return data;
		}
		data = new KeyDataItem(key);
		Keys.put(key, data);
		return data;
	}
	
	
	public void setNull(String key)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.setNull();
	}
	
	public void set(String key, boolean val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Boolean getAsBoolean(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asBoolean();
	}
	
	public void set(String key, byte val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Byte getAsByte(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asByte();
	}
	
	public void set(String key, short val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Short getAsShort(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asShort();
	}
	
	public void set(String key, int val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Integer getAsInt(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asInt();
	}
	
	public void set(String key, long val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Long getAsLong(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return Keys.get(key).asLong();
	}
	
	public void set(String key, float val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Float getAsFloat(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asFloat();
	}
	
	public void set(String key, double val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Double getAsDouble(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asDouble();
	}
	
	public void set(String key, String val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public String getAsString(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asString();
	}
	
	public void set(String key, Date val)
	{
		KeyDataItem data = getOrCreateKeyData(key);
		data.set(val);
	}
	
	public Date getAsDate(String key)
	{
		KeyDataItem data = getKeyData(key);
		if(data == null)
			return null;
		return data.asDate();
	}
	
	public void deleteKey(String key)
	{
		KeyDataItem data = Keys.get(key);
		if(data == null)
			return;
		
		if(!data.isDeleted())
		{
			data.setDeleted(true);
			data.dirty = true;
		}
	}
	
	public void deleteAllKeys()
	{
		for(KeyDataItem item : Keys.values())
		{
			if(!item.isDeleted())
			{
				item.setDeleted(true);
				item.dirty = true;
			}
		}
	}
}
