package com.dreamwing.serverville.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class TransientDataItem
{
	public String key;
	public Object value;
	public long created;
	public long modified;
	public boolean deleted;
	
	// For in-memory resident transient state
	@JsonIgnore
	public TransientDataItem nextItem;
	@JsonIgnore
	public TransientDataItem prevItem;
	
	public TransientDataItem() {}
	
	public TransientDataItem(String key, Object val)
	{
		this.key = key;
		value = ScriptObjectMirror.wrapAsJSONCompatible(val, null);
		created = System.currentTimeMillis();
		modified = created;
	}

}
