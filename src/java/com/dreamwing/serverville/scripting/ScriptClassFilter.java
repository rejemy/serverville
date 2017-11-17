package com.dreamwing.serverville.scripting;

import jdk.nashorn.api.scripting.ClassFilter;

public class ScriptClassFilter implements ClassFilter
{

	@Override
	public boolean exposeToScripts(String className)
	{
		
		return false;
	}

}
