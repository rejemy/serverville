package com.dreamwing.serverville.cluster;

import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.client.ClientSessionManager;
import com.dreamwing.serverville.cluster.ClusterMessages.DeliverUserNotificationMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.DisconnectUserMessage;

public class ClusterMessageHandler {
	
	public static void onDisconnectUser(DisconnectUserMessage message)
	{
		ClientConnectionHandler connectionHandler = ClientSessionManager.getSession(message.SessionId);
		if(connectionHandler != null)
		{
			connectionHandler.expireSession();
		}
		
	}
	
	public static void onDeliverUserNotification(DeliverUserNotificationMessage message)
	{
		ClientConnectionHandler client = ClientSessionManager.getSessionByUserId(message.UserId);
		if(client == null)
		{
			// User not online
			return;
		}
		
		client.sendNotification(message.NotificationType, message.SerializedNotification);
	}
}
