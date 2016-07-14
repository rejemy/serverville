package com.dreamwing.serverville.data;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

@SuppressWarnings("restriction")
public class TransientDataItem
{
	public String key;
	public Object value;
	public long created;
	public long modified;
	public boolean deleted;
	
	// For in-memory resident transient state
	public TransientDataItem nextItem;
	public TransientDataItem prevItem;
		
	
	public TransientDataItem(String key, Object val)
	{
		this.key = key;
		value = ScriptObjectMirror.wrapAsJSONCompatible(val, null);;
		created = System.currentTimeMillis();
		modified = created;
	}

}
