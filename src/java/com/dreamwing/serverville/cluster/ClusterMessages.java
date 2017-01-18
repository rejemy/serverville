package com.dreamwing.serverville.cluster;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.JSON;
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
		public static final int MEMBER_SHUTTING_DOWN = 5;
		public static final int CREATE_CHANNEL_REQUEST = 6;
		
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
			case MEMBER_SHUTTING_DOWN:
				return new MemberShuttingDownMessage();
			case CREATE_CHANNEL_REQUEST:
				return new CreateChannelRequestMessage();
			default:
				l.error("Tried to deserialize cluster message with unknown type: "+typeId);
			}
			
			return null;
		}
		
	}
	
	public static class ClusterMemberMessageRunnable implements Runnable, Callable<IdentifiedDataSerializable>, IdentifiedDataSerializable
	{
		IdentifiedDataSerializable Message;
		
		@Override
		public IdentifiedDataSerializable call() throws JsonApiException
		{
			IdentifiedDataSerializable returnValue = null;
			
			int messageType = Message.getId();
			switch(messageType)
			{
			case ClusterMessageFactory.CACHED_DATA_UPDATE:
				ClusterMessageHandler.onCachedDataUpdateMessage((CachedDataUpdateMessage)Message);
				break;
			case ClusterMessageFactory.DISCONNECT_USER:
				ClusterMessageHandler.onDisconnectUser((DisconnectUserMessage)Message);
				break;
			case ClusterMessageFactory.DELIVER_USER_NOTIFICATION:
				ClusterMessageHandler.onDeliverUserNotification((DeliverUserNotificationMessage)Message);
				break;
			case ClusterMessageFactory.MEMBER_SHUTTING_DOWN:
				ClusterMessageHandler.onMemberShuttingDown((MemberShuttingDownMessage)Message);
				break;
			case ClusterMessageFactory.CREATE_CHANNEL_REQUEST:
				ClusterMessageHandler.onCreateChannelRequest((CreateChannelRequestMessage)Message);
				break;
			default:
				l.error("Tried to run cluster message of unknown type: "+messageType);
			}
			
			return returnValue;
		}
		
		@Override
		public void run()
		{
			try {
				call();
			} catch (JsonApiException e) {
				l.error("Exception in cluster message handler:", e);
			}
		}

		@Override
		public void writeData(ObjectDataOutput out) throws IOException {
			out.writeObject(Message);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException {
			Message = in.readObject();
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.CLUSTER_MESSAGE_RUNNABLE;
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
	
	public static class MemberShuttingDownMessage implements IdentifiedDataSerializable
	{

		public String MemberId;

		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(MemberId);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			MemberId = in.readUTF();
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.MEMBER_SHUTTING_DOWN;
		}
		
	}
	
	public static class CreateChannelRequestMessage implements IdentifiedDataSerializable
	{
		public String ChannelId;
		public String ResidentType;
		public Map<String,Object> Values;
		
		@Override
		public void writeData(ObjectDataOutput out) throws IOException
		{
			out.writeUTF(ChannelId);
			out.writeUTF(ResidentType);
			String encodedValues = null;
			if(Values != null)
				encodedValues = JSON.serializeToString(Values);
			StringUtil.writeUTFNullSafe(out, encodedValues);
		}

		@Override
		public void readData(ObjectDataInput in) throws IOException
		{
			ChannelId = in.readUTF();
			ResidentType = in.readUTF();
			String encodedValues = StringUtil.readUTFNullSafe(in);
			if(encodedValues != null)
				Values = JSON.deserialize(encodedValues, JSON.StringObjectMapType);
		}

		@Override
		public int getFactoryId() {
			return ClusterMessageFactory.FACTORY_ID;
		}

		@Override
		public int getId() {
			return ClusterMessageFactory.CREATE_CHANNEL_REQUEST;
		}
	}
}
