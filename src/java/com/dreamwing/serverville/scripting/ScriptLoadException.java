package com.dreamwing.serverville.scripting;


public class ScriptLoadException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public String ScriptId;
	
	public ScriptLoadException(String id, Exception cause)
	{
		super("Error loading script "+id);
		ScriptId = id;
		initCause(cause);
	}
}
