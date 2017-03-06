package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.client.ClientMessages.UserMessageNotification;
import com.dreamwing.serverville.cluster.ClusterManager;
import com.dreamwing.serverville.cluster.ClusterMessages.DeliverUserNotificationMessage;
import com.dreamwing.serverville.cluster.DistributedData.OnlineUserLocator;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.SVID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "user_message")
public class UserMessage
{
	private static final Logger l = LogManager.getLogger(UserMessage.class);
	
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String MessageId;
	
	@DatabaseField(columnName="to", canBeNull=false)
	public String ToUser;
	
	@DatabaseField(columnName="from", canBeNull=false)
	public String From;
	
	@DatabaseField(columnName="from_user", canBeNull=false)
	public boolean FromUser;
	
	@DatabaseField(columnName="message_type", canBeNull=false)
	public String MessageType;
	
	@DatabaseField(columnName="content", canBeNull=true)
	public String Content;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	public static UserMessage load(String id) throws SQLException
	{
		return DatabaseManager.UserMessageDao.queryForId(id);
	}
	
	public static List<UserMessage> loadAllToUser(String userId) throws SQLException
	{
		return DatabaseManager.UserMessageDao.queryForEq("to", userId);
	}
	
	public void delete() throws SQLException
	{
		DatabaseManager.UserMessageDao.delete(this);
	}
	
	public static void deleteById(String id) throws SQLException
	{
		DatabaseManager.UserMessageDao.deleteById(id);
	}
	
	public void save() throws SQLException
	{
		DatabaseManager.UserMessageDao.create(this);
	}
	
	public UserMessageNotification toNotification()
	{
		UserMessageNotification notification = new UserMessageNotification();
		notification.id = MessageId;
		notification.message_type = MessageType;
		notification.message = Content;
		notification.from_id = From;
		notification.sender_is_user = FromUser;
		
		return notification;
	}
	
	public void deliver(boolean guaranteed) throws SQLException
	{
		if(Created == null)
			Created = new Date();
		
		if(guaranteed)
		{
			if(MessageId == null)
				MessageId = SVID.makeSVID();
			
			save();
		}
		
		OnlineUserLocator userLocator = ClusterManager.locateOnlineUser(ToUser);
		if(userLocator == null)
		{
			// User not online
			return;
		}
		
		UserMessageNotification notification = toNotification();
		String serializedNotification;
		try
		{
			serializedNotification = JSON.serializeToString(notification);
		}
		catch(JsonProcessingException e)
		{
			l.error("Error encoding message:", e);
			return;
		}
		
		DeliverUserNotificationMessage clusterMessage = new DeliverUserNotificationMessage();
		clusterMessage.UserId = ToUser;
		clusterMessage.NotificationType = "msg";
		clusterMessage.SerializedNotification = serializedNotification;
		
		for(Map.Entry<String,String> entry : userLocator.SessionToMember.entrySet())
		{
			clusterMessage.SessionId = entry.getKey();
			ClusterManager.sendToMember(clusterMessage, entry.getValue());
		}
		
		

	}
}
