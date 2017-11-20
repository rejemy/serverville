package com.dreamwing.serverville.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LocaleUtil
{
	public static Locale DefaultLocale;
	public static String DefaultLanguage;
	public static String DefaultCountry;
	
	private static Set<String> ISO_LANGUAGES;
	private static Set<String> ISO_COUNTRIES;
	
	public static void init()
	{
		ISO_LANGUAGES = new HashSet<String>(Arrays.asList(Locale.getISOLanguages()));
		ISO_COUNTRIES = new HashSet<String>(Arrays.asList(Locale.getISOCountries()));
	}
	
	public static boolean isKnownCountryCode(String code)
	{
		return ISO_COUNTRIES.contains(code);
	}
	
	public static boolean isKnownLanguageCode(String code)
	{
		return ISO_LANGUAGES.contains(code);
	}
	
	public static String normalizeCountryCode(String code)
	{
		code = code.toUpperCase();
		if(ISO_COUNTRIES.contains(code))
			return code;
		else
			return null;
	}
	
	public static String normalizeLanguageCode(String code)
	{
		code = code.toLowerCase();
		if(ISO_LANGUAGES.contains(code))
			return code;
		else
			return null;
	}
	
	public static <T> T getLocalized(Locale loc, Map<String,T> text)
	{
		if(loc == null)
			loc = DefaultLocale;
		
		String language = loc.getLanguage();
		
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
