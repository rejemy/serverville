package com.dreamwing.serverville.cluster;

import java.io.IOException;

import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ClusterMessages
{
	public static class ClusterMessageFactory implements DataSerializableFactory
	{
		public static final int FACTORY_ID = 1;
		
		public static final int CACHED_DATA_UPDATE = 1;
		
		@Override
		public IdentifiedDataSerializable create(int typeId)
		{
			switch(typeId)
			{
			case CACHED_DATA_UPDATE:
				return new CachedDataUpdateMessage();
			}
			
			return null;
		}
		
	}
	
	public static class CachedDataUpdateMessage implements IdentifiedDataSerializable
	{
		public static final String Scripts = "scripts";
		public static final String Currency = "currency";
		public static final String Product = "product";
		public static final String RecordPerms = "record_perms";
		public static final String ResidentPerms = "resident_perms";
		
		public String DataType;
		public String DataId;
		
		
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(DataType);
			StringUtil.writeUTFNullSafe(out, DataId);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			DataType = in.readUTF();
			DataId = StringUtil.readUTFNullSafe(in);
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.CACHED_DATA_UPDATE;
		}
		
	}
}
