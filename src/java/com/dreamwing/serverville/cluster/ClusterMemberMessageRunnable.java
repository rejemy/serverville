package com.dreamwing.serverville.cluster;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.cluster.ClusterMessages.ClusterMessageFactory;
import com.dreamwing.serverville.cluster.ClusterMessages.DeliverUserNotificationMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.DisconnectUserMessage;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class ClusterMemberMessageRunnable implements Runnable, IdentifiedDataSerializable
{
	private static final Logger l = LogManager.getLogger(ClusterMemberMessageRunnable.class);
	
	IdentifiedDataSerializable Message;
	
	@Override
	public void run()
	{
		int messageType = Message.getId();
		switch(messageType)
		{
		case ClusterMessageFactory.DISCONNECT_USER:
			ClusterMessageHandler.onDisconnectUser((DisconnectUserMessage)Message);
			break;
		case ClusterMessageFactory.DELIVER_USER_NOTIFICATION:
			ClusterMessageHandler.onDeliverUserNotification((DeliverUserNotificationMessage)Message);
			break;
		default:
			l.error("Tried to run cluster message of unknown type: "+messageType);
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
