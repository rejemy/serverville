package com.dreamwing.serverville.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
	
	public static String readFileToString(String filename, Charset encoding) throws IOException
	{
		return readStreamToString(new FileInputStream(filename), encoding);
	}
	
	public static String readFileToString(File file, Charset encoding) throws IOException
	{
		return readStreamToString(new FileInputStream(file), encoding);
	}
	
	public static void writeStringToFile(String filename, String contents, Charset encoding) throws IOException
	{
		writeStringToFile(new File(filename), contents, encoding);
	}
	
	public static void writeStringToFile(File file, String contents, Charset encoding) throws IOException
	{
		File parent = file.getParentFile();
		
		if (!parent.exists() && !parent.mkdirs()) {
			throw new IOException("Couldn't create dir: " + parent);
		}
		
		try(Writer out = new OutputStreamWriter(new FileOutputStream(file), encoding))
		{
			out.write(contents);
		}
	}
	
	public static void writeBytesToFile(String filename, byte[] contents) throws IOException
	{
		writeBytesToFile(new File(filename), contents);
	}
	
	public static void writeBytesToFile(File file, byte[] contents) throws IOException
	{
		File parent = file.getParentFile();
		
		if (!parent.exists() && !parent.mkdirs()) {
			throw new IOException("Couldn't create dir: " + parent);
		}
		
		try(FileOutputStream out = new FileOutputStream(file))
		{
			out.write(contents);
		}
	}
}
