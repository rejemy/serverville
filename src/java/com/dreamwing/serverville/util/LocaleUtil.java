package com.dreamwing.serverville.util;

import java.util.HashMap;
import java.util.Map;

public class LocaleUtil
{
	public static String DefaultLanguage;
	
	public static <T> T getLocalized(String language, Map<String,T> text)
	{
		if(language == null)
			language = DefaultLanguage;
		
		T val = text.get(language);
		if(val != null)
			return val;
		
		if(language.length() > 2)
			language = language.substring(0, 2);
		
		val = text.get(language);
		if(val != null)
			return val;
		
		return text.get(DefaultLanguage);
	}
	
	public static <T> void setMapDefaults(Map<String,T> text)
	{
		Map<String,T> fallbacks = new HashMap<String,T>();
		
		for(String key : text.keySet())
		{
			if(key.length() <= 2)
				continue;
			
			String baseKey = key.substring(0, 2);
			if(!text.containsKey(baseKey))
			{
				fallbacks.putIfAbsent(baseKey, text.get(key));
			}
		}
		
		text.putAll(fallbacks);
		
	}
}
