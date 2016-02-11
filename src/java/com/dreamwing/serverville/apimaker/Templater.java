package com.dreamwing.serverville.apimaker;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dreamwing.serverville.util.FileUtil;

public class Templater {

	private String Template;
	private Map<String,String> Replacements;
	
	private static Pattern TokenPattern = Pattern.compile("\\{\\{([A-Za-z0-9]+)\\}\\}");

	public Templater(String resourcePath) throws IOException
	{
		Replacements = new HashMap<String,String>();
		
		Template = FileUtil.readFileToString(resourcePath, StandardCharsets.UTF_8);
	}
	
	public void clear()
	{
		Replacements.clear();
	}
	
	public void set(String token, String value)
	{
		Replacements.put(token, value);
	}
	
	public void set(String token, int value)
	{
		Replacements.put(token, Integer.toString(value));
	}
	
	public void set(String token, long value)
	{
		Replacements.put(token, Long.toString(value));
	}
	
	
	public String toString()
	{
		Matcher matcher = TokenPattern.matcher(Template);
		StringBuilder builder = new StringBuilder();
		
		int pos = 0;
		while (matcher.find())
		{
			builder.append(Template.substring(pos, matcher.start()));
			
			String replacement = Replacements.get(matcher.group(1));
			if (replacement == null)
			{
				System.err.println("Missing template token: "+matcher.group(1));
				builder.append("{{ MISSING TOKEN: "+matcher.group(1)+" }}");
			}
			else
			{
				builder.append(replacement);
			}
			
			pos = matcher.end();
		}
		builder.append(Template.substring(pos, Template.length()));
		
		return builder.toString();
	}
	
	public void writeToFile(String filename, Charset encoding) throws IOException
	{
		String result = toString();
		FileUtil.writeStringToFile(filename, result, encoding);
	}
}
