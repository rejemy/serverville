package com.dreamwing.serverville.ext.amplitude;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.client.ClientConnectionHandler;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

import okhttp3.RequestBody;

public class AmplitudeInterface
{
	private static final Logger l = LogManager.getLogger(AmplitudeInterface.class);
	
	private static String ApiKey;
	private static String URL = "https://api.amplitude.com/httpapi";
	
	public static void init()
	{
		ApiKey = ServervilleMain.ServerProperties.getProperty("amplitude_api_key");
		if(ApiKey == null || ApiKey.length() == 0)
		{
			ApiKey = null;
			return;
		}
		
		l.info("Amplitude interface initialized");
	}
	
	public static class AmplitudeEvent
	{
		public long session_id;
		public String user_id;
		public String event_type;
		public long time;
		public Object event_properties;
		public Object user_properties;
	}
	
	public static void createEvent(ClientConnectionHandler userConnection, String eventType, Object eventProperties, Object userProperties)
	{
		if(ApiKey == null)
			return;
		
		AmplitudeEvent event = new AmplitudeEvent();
		event.session_id = userConnection.getSessionStartedAt();
		event.user_id = userConnection.getUser().getId();
		event.event_type = eventType;
		event.time = System.currentTimeMillis();
		event.event_properties = eventProperties;
		event.user_properties = userProperties;
		
		String urlEncoded = null;
		try
		{
			String jsonEvent = JSON.serializeToString(event);
			urlEncoded = URLEncoder.encode(jsonEvent, "UTF-8");
		}
		catch (JsonProcessingException | UnsupportedEncodingException e1)
		{
			l.error("Error encoding Amplitude event", e1);
		}
		
		String content = "api_key="+ApiKey+"&event="+urlEncoded;
		
		RequestBody body = RequestBody.create(HttpHelpers.FORM_URLENCODED_CONTENT_TYPE, content);
		
		try
		{
			HttpHelpers.postData(URL, body);
		}
		catch (IOException e)
		{
			l.warn("Exception creating amplitude event", e);
		}
	}
}
