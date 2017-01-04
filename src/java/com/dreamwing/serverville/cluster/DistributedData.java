package com.dreamwing.serverville.cluster;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class DistributedData
{
	private static final Logger l = LogManager.getLogger(DistributedData.class);
	
	public static class DistributedDataFactory implements DataSerializableFactory
	{
		public static final int FACTORY_ID = 2;
		
		public static final int ONLINE_USER_LOCATOR = 1;
		
		@Override
		public IdentifiedDataSerializable create(int typeId)
		{
			switch(typeId)
			{
			case ONLINE_USER_LOCATOR:
				return new OnlineUserLocator();
			default:
				l.error("Tried to deserialize cluster data with unknown type: "+typeId);
			}
			
			return null;
		}
		
	}
	
	public static class OnlineUserLocator implements IdentifiedDataSerializable
	{
		public String UserId;
		public String SessionId;
		public String MemberUUID;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(UserId);
			out.writeUTF(SessionId);
			out.writeUTF(MemberUUID);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			UserId = in.readUTF();
			SessionId = in.readUTF();
			MemberUUID = in.readUTF();
		}

		@Override
		public int getFactoryId() {
			return DistributedDataFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return DistributedDataFactory.ONLINE_USER_LOCATOR;
		}
		
	}
}
