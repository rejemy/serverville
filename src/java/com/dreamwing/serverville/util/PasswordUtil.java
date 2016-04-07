package com.dreamwing.serverville.util;

import java.util.Random;

public class PasswordUtil {
	
	public static boolean validatePassword(String password)
	{
		if(password == null || password.length() == 0)
			return false;
		
		if(password.length() < 6)
			return false;
		
		return true;
	}
	
	public static String makeRandomString(int bytes)
	{
		Random rand = new Random();
		byte[] buf = new byte[bytes];
		rand.nextBytes(buf);
		return SVIDCodec.encode(buf);
	}
}
