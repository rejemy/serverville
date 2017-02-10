package com.dreamwing.serverville.data;

import java.io.IOException;

import com.dreamwing.serverville.cluster.DistributedData.DistributedDataFactory;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class TransientDataItem implements IdentifiedDataSerializable
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
		this(key, val, System.currentTimeMillis());
	}
	
	public TransientDataItem(String key, Object val, long when)
	{
		this.key = key;
		value = ScriptObjectMirror.wrapAsJSONCompatible(val, null);
		created = when;
		modified = created;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(key);
		out.writeUTF(JSON.serializeToString(value));
		out.writeLong(created);
		out.writeLong(modified);
		out.writeBoolean(deleted);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		key = in.readUTF();
		value = JSON.deserialize(in.readUTF());
		created = in.readLong();
		modified = in.readLong();
		deleted = in.readBoolean();
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return DistributedDataFactory.FACTORY_ID;
	}

	@JsonIgnore
	@Override
	public int getId() {
		return DistributedDataFactory.TRANSIENT_DATA;
	}

	public TransientDataItem clone()
	{
		TransientDataItem copy = new TransientDataItem();
		copy.key = key;
		copy.value = value;
		copy.created = created;
		copy.modified = modified;
		copy.deleted = deleted;
		return copy;
	}

}
