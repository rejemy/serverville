package com.dreamwing.serverville.cluster;

import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.CurrencyInfoManager;
import com.dreamwing.serverville.ProductManager;
import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.cluster.ClusterMessages.CachedDataUpdateMessage;
import com.dreamwing.serverville.data.RecordPermissionsManager;
import com.dreamwing.serverville.data.ResidentPermissionsManager;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class CachedDataUpdateListener implements MessageListener<CachedDataUpdateMessage>
{
	private static final Logger l = LogManager.getLogger(CachedDataUpdateListener.class);
	
	@Override
	public void onMessage(Message<CachedDataUpdateMessage> message)
	{
		final Runnable updateRunner = new Runnable()
		{
			public void run()
			{
				handleCachedDataUpdateMessage(message.getMessageObject());
			}
		};
		
		// Random delay so everyone doesn't hit the DB at once
		Random rand = new Random();
		ServervilleMain.ServiceScheduler.schedule(updateRunner, rand.nextInt(1500), TimeUnit.MILLISECONDS);
	}

	private void handleCachedDataUpdateMessage(CachedDataUpdateMessage message)
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

	private void handleScriptUpdate()
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
	
	private void handleCurrencyUpdate(String currencyId)
	{
		try {
			CurrencyInfoManager.reloadCurrencyInfo(currencyId);
		} catch (SQLException e) {
			l.error("Error loading currency info:", e);
		}
	}
	
	private void handleProductUpdate(String productId)
	{
		try {
			ProductManager.reloadProduct(productId);
		} catch (SQLException e) {
			l.error("Error loading product info:", e);
		}
	}
	
	private void handleRecordPermsUpdate(String recordType)
	{
		try {
			RecordPermissionsManager.reloadPermissions(recordType);
		} catch (SQLException e) {
			l.error("Error loading record permissions:", e);
		}
	}
	
	private void handleResidentPermsUpdate(String residentType)
	{
		try {
			ResidentPermissionsManager.reloadPermissions(residentType);
		} catch (SQLException e) {
			l.error("Error loading resident permissions:", e);
		}
	}
}
