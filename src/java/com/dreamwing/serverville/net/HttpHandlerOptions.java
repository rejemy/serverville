package com.dreamwing.serverville.net;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HttpHandlerOptions
{	
	public enum Method {
		GET,
		POST,
		PUT,
		DELETE
	}

	public Method method() default Method.GET;
	public boolean auth() default true;
}
