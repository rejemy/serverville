package com.dreamwing.serverville.util;

public class PasswordUtil {
	
	public static boolean validatePassword(String password)
	{
		if(password == null || password.length() == 0)
			return false;
		
		if(password.length() < 6)
			return false;
		
		return true;
	}
}
