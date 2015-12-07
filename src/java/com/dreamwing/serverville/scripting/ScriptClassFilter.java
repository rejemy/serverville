package com.dreamwing.serverville.scripting;

import jdk.nashorn.api.scripting.ClassFilter;

@SuppressWarnings("restriction")
public class ScriptClassFilter implements ClassFilter {

	@Override
	public boolean exposeToScripts(String className)
	{
		
		return false;
	}

}
