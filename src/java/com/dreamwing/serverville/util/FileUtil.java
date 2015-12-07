package com.dreamwing.serverville.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class FileUtil {

	public static boolean deleteRecursive(File path) throws FileNotFoundException
	{
        if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }
	
	private static final int BUFFER_SIZE = 4 * 1024;
	
	public static String readStreamToString(InputStream stream, Charset encoding) throws IOException
	{
		StringBuilder builder = new StringBuilder();
	    InputStreamReader reader = new InputStreamReader(stream, encoding);
	    char[] buffer = new char[BUFFER_SIZE];
	    int length;
	    while ((length = reader.read(buffer)) != -1) {
	        builder.append(buffer, 0, length);
	    }
	    reader.close();
	    return builder.toString();
	}
}
