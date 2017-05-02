package com.dreamwing.serverville.cluster;

import java.sql.SQLException;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.CurrencyInfoManager;
import com.dreamwing.serverville.ProductManager;
import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.client.ClientSessionManager;
import com.dreamwing.serverville.cluster.ClusterMessages.CachedDataUpdateMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.CreateChannelRequestMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.CreateWorldRequestMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.DeliverUserNotificationMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.DisconnectUserMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.GlobalChannelDataUpdateMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.GlobalChannelEventMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.MemberShuttingDownMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.RemoveGlobalChannelMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.ReplicateGlobalChannelMessage;
import com.dreamwing.serverville.cluster.ClusterMessages.StartupCompleteMessage;
import com.dreamwing.serverville.data.RecordPermissionsManager;
import com.dreamwing.serverville.data.ResidentPermissionsManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.residents.BaseResident;
import com.dreamwing.serverville.residents.Channel;
import com.dreamwing.serverville.residents.GlobalChannel;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.residents.World;
import com.dreamwing.serverville.scripting.ScriptManager;

public class ClusterMessageHandler
{
	private static final Logger l = LogManager.getLogger(ClusterManager.class);
	
	public static void onStartupCompleteMessage(StartupCompleteMessage message)
	{
		ClusterManager.onClusterReady();
	}
	
	public static void onCachedDataUpdateMessage(CachedDataUpdateMessage message)
	{
		switch(message.DataType)
		{
			case CachedDataUpdateMessage.Scripts:
				handleScriptUpdate();
				break;
			case CachedDataUpdateMessage.Currency:
				handleCurrencyUpdate(message.DataId);
				break;
			case CachedDataUpdateMessage.Product:
				handleProductUpdate(message.DataId);
				break;
			case CachedDataUpdateMessage.RecordPerms:
				handleRecordPermsUpdate(message.DataId);
				break;
			case CachedDataUpdateMessage.ResidentPerms:
				handleResidentPermsUpdate(message.DataId);
				break;
			default:
				l.warn("Unknown cached update message type: "+message.DataType);
		}
	}

	private static void handleScriptUpdate()
	{
		try {
			ScriptManager.scriptsUpdated();
		} catch (ScriptException e) {
			
			String errorMessage = e.getCause().getMessage()+" at line "+e.getLineNumber();
			l.error("Error reloading javascript:", errorMessage);
		} catch (SQLException e) {
			l.error("Error loading javascript:", e);
		}
	}
	
	private static void handleCurrencyUpdate(String currencyId)
	{
		try {
			CurrencyInfoManager.reloadCurrencyInfo(currencyId);
		} catch (SQLException e) {
			l.error("Error loading currency info:", e);
		}
	}
	
	private static void handleProductUpdate(String productId)
	{
		try {
			ProductManager.reloadProduct(productId);
		} catch (SQLException e) {
			l.error("Error loading product info:", e);
		}
	}
	
	private static void handleRecordPermsUpdate(String recordType)
	{
		try {
			RecordPermissionsManager.reloadPermissions(recordType);
		} catch (SQLException e) {
			l.error("Error loading record permissions:", e);
		}
	}
	
	private static void handleResidentPermsUpdate(String residentType)
	{
		try {
			ResidentPermissionsManager.reloadPermissions(residentType);
		} catch (SQLException e) {
			l.error("Error loading resident permissions:", e);
		}
	}
	
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
	
	public static void onMemberShuttingDown(MemberShuttingDownMessage message)
	{
		ClusterManager.onMemberGracefulExit(message.MemberId);
	}
	
	public static void onCreateChannelRequest(CreateChannelRequestMessage request) throws JsonApiException
	{
		
		BaseResident res = ResidentManager.getResident(request.ChannelId);
		if(res != null)
		{
			if(res instanceof Channel && request.ResidentType.equals(res.getType()))
			{
				if(request.Values != null)
					res.setTransientValues(request.Values, true);
				
				return;
			}
			
			throw new JsonApiException(ApiErrors.CHANNEL_ID_TAKEN, request.ChannelId);
		}	
		
		Channel chan = new Channel(request.ChannelId, request.ResidentType);
		
		if(request.Values != null)
			chan.setTransientValues(request.Values, true);
		
		ClusterManager.createChannelInCluster(chan);
		
		/*
		ResidentManager.addResident(chan);
		
		
		
		try
		{
			ClusterManager.registerLocalResident(chan);
		}
		catch(JsonApiException e)
		{
			// Oops, we have to clean it up if we couldn't register it on the cluster
			ResidentManager.removeResident(chan);
			throw e;
		}*/
		
	}
	
	public static void onCreateWorldRequest(CreateWorldRequestMessage request) throws JsonApiException
	{
		
		BaseResident res = ResidentManager.getResident(request.WorldId);
		if(res != null)
		{
			if(res instanceof Channel && request.ResidentType.equals(res.getType()))
			{
				if(request.Values != null)
					res.setTransientValues(request.Values, true);
				
				return;
			}
			
			throw new JsonApiException(ApiErrors.CHANNEL_ID_TAKEN, request.WorldId);
		}	
		
		World world = new World(request.WorldId, request.ResidentType);
		
		if(request.Values != null)
			world.setTransientValues(request.Values, true);
		
		ClusterManager.createChannelInCluster(world);
		
		/*
		ResidentManager.addResident(chan);
		
		
		
		try
		{
			ClusterManager.registerLocalResident(chan);
		}
		catch(JsonApiException e)
		{
			// Oops, we have to clean it up if we couldn't register it on the cluster
			ResidentManager.removeResident(chan);
			throw e;
		}*/
		
	}
	
	public static void onReplicateGlobalChannel(ReplicateGlobalChannelMessage message)
	{
		GlobalChannel globalChan = new GlobalChannel(message.ChannelId, message.ResidentType);
		
		
		ResidentManager.addResident(globalChan);
	}
	
	public static void onGlobalChannelEvent(GlobalChannelEventMessage message)
	{
		BaseResident res = ResidentManager.getResident(message.ChannelId);
		if(res == null || !(res instanceof GlobalChannel))
		{
			l.error("Got global event for channel that hasn't been created yet: "+message.ChannelId);
			return;
		}
		
		GlobalChannel channel = (GlobalChannel)res;
		channel.onEventTriggered(message.MessageBody);
	}
	
	public static void onGlobalChannelDataUpdate(GlobalChannelDataUpdateMessage message)
	{
		BaseResident res = ResidentManager.getResident(message.ChannelId);
		if(res == null || !(res instanceof GlobalChannel))
		{
			l.error("Got global update for channel that hasn't been created yet: "+message.ChannelId);
			return;
		}
		
		
		GlobalChannel channel = (GlobalChannel)res;
		
		if(message.Updates != null)
		{
			channel.setTransientValues(message.Updates, message.ForceUpdate);
		}
		else if(message.Deleted != null)
		{
			channel.deleteTransientValues(message.Deleted);
		}
		else if(message.DeleteAll)
		{
			channel.deleteAllTransientValues();
		}
	}
	
	public static void onRemoveGlobalChannel(RemoveGlobalChannelMessage message)
	{
		BaseResident res = ResidentManager.getResident(message.ChannelId);
		if(!(res instanceof GlobalChannel))
			return;
		
		GlobalChannel channel = (GlobalChannel)res;
		channel.destroy();
	}
}
