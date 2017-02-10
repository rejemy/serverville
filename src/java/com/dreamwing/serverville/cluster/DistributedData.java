package com.dreamwing.serverville.cluster;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.TransientDataItem;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.Resident;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.core.PartitionAware;
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
		public static final int RESIDENT_LOCATOR = 2;
		public static final int RESIDENT_CLUSTER_DATA = 3;
		public static final int CHANNEL_CLUSTER_DATA = 4;
		public static final int TRANSIENT_DATA = 5;
		
		@Override
		public IdentifiedDataSerializable create(int typeId)
		{
			switch(typeId)
			{
			case ONLINE_USER_LOCATOR:
				return new OnlineUserLocator();
			case RESIDENT_LOCATOR:
				return new ResidentLocator();
			case RESIDENT_CLUSTER_DATA:
				return new ResidentClusterData();
			case CHANNEL_CLUSTER_DATA:
				return new ChannelClusterData();
			case TRANSIENT_DATA:
				return new TransientDataItem();
			default:
				l.error("Tried to deserialize cluster data with unknown type: "+typeId);
			}
			
			return null;
		}
		
	}
	
	public static class OnlineUserLocator implements IdentifiedDataSerializable
	{
		public String SessionId;
		public String MemberUUID;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(SessionId);
			out.writeUTF(MemberUUID);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
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
	
	
	public static class ResidentLocator implements IdentifiedDataSerializable, PartitionAware<String>
	{
		public String ResidentId;
		public String RouteToId;
		
		public ResidentLocator() {}
		
		public ResidentLocator(String resId, String routeId)
		{
			ResidentId = resId;
			RouteToId = routeId;
		}
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(ResidentId);
			StringUtil.writeUTFNullSafe(out, RouteToId);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			ResidentId = in.readUTF();
			RouteToId = StringUtil.readUTFNullSafe(in);
		}

		@Override
		public int getFactoryId() {
			return DistributedDataFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return DistributedDataFactory.RESIDENT_LOCATOR;
		}

		@Override
		public String getPartitionKey() {
			if(RouteToId != null)
				return RouteToId;
			return ResidentId;
		}
		
	}
	
	public static abstract class BaseResidentClusterData implements IdentifiedDataSerializable
	{
		boolean IsTempObject;
		
		@Override
		public int getFactoryId() {
			return DistributedDataFactory.FACTORY_ID;
		}

	}
	
	public static class ResidentClusterData extends BaseResidentClusterData
	{
		Resident LiveResident;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			//l.info("Serializing resident");
			
			out.writeUTF(LiveResident.getId());
			out.writeUTF(LiveResident.getType());
			StringUtil.writeUTFNullSafe(out, LiveResident.getOwnerId());
			
			LiveResident.write(out);
			
			if(!IsTempObject)
				ResidentManager.removeResident(LiveResident);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			//l.info("Deserializing resident");
			
			String id = in.readUTF();
			String type = in.readUTF();
			String owner = StringUtil.readUTFNullSafe(in);
			
			LiveResident = new Resident(id, owner, type);
			LiveResident.read(in);
			
			if(!IsTempObject)
				ResidentManager.addResident(LiveResident);
		}

		@Override
		public int getId() {
			return DistributedDataFactory.RESIDENT_CLUSTER_DATA;
		}
	}
	
	public static class ChannelClusterData extends BaseResidentClusterData
	{
		Channel LiveChannel;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			//l.info("Serializing channel");
			
			out.writeUTF(LiveChannel.getId());
			out.writeUTF(LiveChannel.getType());
			LiveChannel.write(out);
			
			if(!IsTempObject)
				ResidentManager.removeResident(LiveChannel);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			//l.info("Deserializing channel");
			
			String id = in.readUTF();
			String type = in.readUTF();
			
			LiveChannel = new Channel(id, type);
			LiveChannel.read(in);
			
			if(!IsTempObject)
				ResidentManager.addResident(LiveChannel);
		}


		@Override
		public int getId() {
			return DistributedDataFactory.CHANNEL_CLUSTER_DATA;
		}
	}

}
