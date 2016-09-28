package com.dreamwing.serverville;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WritableDirectories
{
	private static Map<String,Path> Directories;
	
	public static void init() throws Exception
	{
		Directories = new HashMap<String,Path>();
		
		String directories = ServervilleMain.ServerProperties.getProperty("writeable_directories").trim();
		if(directories == null || directories.length() == 0)
			return;
		
		int pos=0;
		while(pos < directories.length())
		{
			int aliasEnd = directories.indexOf(':', pos);
			if(aliasEnd == -1)
			{
				throw new Exception("Invalid writeable_directories: "+directories);
			}
			
			String alias = directories.substring(pos, aliasEnd);
			pos = aliasEnd+1;
			
			String dir;
			int dirEnd = directories.indexOf(',', pos);
			if(dirEnd == -1)
			{
				dir = directories.substring(pos);
				pos = directories.length();
			}
			else
			{
				dir = directories.substring(pos, dirEnd);
				pos = dirEnd+1;
			}
			
			Path p = ServervilleMain.WorkingPath.resolve(dir).normalize();
			File f = p.toFile();
			
			if(!f.exists() || !f.isDirectory())
			{
				throw new Exception("Writable directory "+dir+" doesn't exist or isn't a directory");
			}
			
			if(!f.canWrite() || !f.canRead())
			{
				throw new Exception("Writable directory "+dir+" can't be accessed");
			}
			
			Directories.put(alias, p);
		}
	}
	
	public static Path getDirectory(String alias)
	{
		return Directories.get(alias);
	}
}
