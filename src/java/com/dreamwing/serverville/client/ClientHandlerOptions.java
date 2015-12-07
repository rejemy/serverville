package com.dreamwing.serverville.client;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ClientHandlerOptions {
	
	public boolean auth() default true;
}
