package com.dreamwing.serverville.cluster;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.util.StringUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ClusterMessages
{
	private static final Logger l = LogManager.getLogger(ClusterMessages.class);
	
	public static class ClusterMessageFactory implements DataSerializableFactory
	{
		public static final int FACTORY_ID = 1;
		
		public static final int CLUSTER_MESSAGE_RUNNABLE = 1;
		public static final int CACHED_DATA_UPDATE = 2;
		public static final int DISCONNECT_USER = 3;
		public static final int DELIVER_USER_NOTIFICATION = 4;
		
		@Override
		public IdentifiedDataSerializable create(int typeId)
		{
			switch(typeId)
			{
			case CLUSTER_MESSAGE_RUNNABLE:
				return new ClusterMemberMessageRunnable();
			case CACHED_DATA_UPDATE:
				return new CachedDataUpdateMessage();
			case DISCONNECT_USER:
				return new DisconnectUserMessage();
			case DELIVER_USER_NOTIFICATION:
				return new DeliverUserNotificationMessage();
			default:
				l.error("Tried to deserialize cluster message with unknown type: "+typeId);
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
	
	public static class DisconnectUserMessage implements IdentifiedDataSerializable
	{
		public String SessionId;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(SessionId);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			SessionId = in.readUTF();
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.DISCONNECT_USER;
		}
	}
	
	public static class DeliverUserNotificationMessage implements IdentifiedDataSerializable
	{
		public String UserId;
		public String NotificationType;
		public String SerializedNotification;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			//out.writeUTF(SessionId);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			//SessionId = in.readUTF();
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.DELIVER_USER_NOTIFICATION;
		}
	}
}
