package com.dreamwing.serverville;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.db.KeyDataResultHandlers.IntResultSetHandler;
import com.dreamwing.serverville.net.JsonApiException;

public class UserManager
{

	private static final Logger l = LogManager.getLogger(UserManager.class);
	
	public static final long AdminLogRetentionPeriod = 1000 * 60 * 60 * 24 * 30; // 30 days
	public static final long AdminSessionInactivePeriod = 1000 * 60 * 60 * 24 * 30; // 30 days
	public static final long InactiveAnonymousAccountPeriod = 1000 * 60 * 60 * 24 * 30; // 30 days
	
	public static void init()
	{
		bootstrapAdminAccount();
		
		final Runnable adminLogPurger = new Runnable()
		{
			public void run()
			{
				try {
					purgeOldAdminLogs();
				} catch (SQLException e) {
					l.error("Error purging old admin logs", e);
				}
			}
		};
		
		ServervilleMain.ServiceScheduler.scheduleAtFixedRate(adminLogPurger, 1, 1, TimeUnit.DAYS);
		
		final Runnable anonymousAccountPurger = new Runnable()
		{
			public void run()
			{
				try {
					purgeOldAdminSessions();
				} catch (SQLException e) {
					l.error("Error purging old admin sessions", e);
				}
				
				try {
					purgeOldUserSessions();
				} catch (SQLException e) {
					l.error("Error purging old user sessions and anonymous users", e);
				}
			}
		};
		
		ServervilleMain.ServiceScheduler.scheduleAtFixedRate(anonymousAccountPurger, 1, 1, TimeUnit.DAYS);
	}
	

	
	private static void bootstrapAdminAccount()
	{
		Integer numAdmins = 0;
		try {
			numAdmins = DatabaseManager.getServer().query("SELECT count(`id`) FROM `user` WHERE `admin` = 15;", new IntResultSetHandler());
		} catch (SQLException e) {
			l.error("SQL error trying to count admin accounts", e);
			return;
		}
		
		if(numAdmins > 0)
			return;
		
		l.info("Creating admin account with password \"admin\", please log in and change ASAP");

		try {
			ServervilleUser.create("admin", "admin", null, ServervilleUser.AdminLevel_Admin, null, null);
		} catch (SQLException e) {
			l.error("SQL error trying create bootstap admin account", e);
			return;
		} catch (JsonApiException e) {
			l.error("API error trying create bootstap admin account", e);
			return;
		}
	}
	
	private static void purgeOldAdminLogs() throws SQLException
	{
		long since = System.currentTimeMillis() - AdminLogRetentionPeriod;
		DatabaseManager.getServer().update("DELETE FROM `admin_log` WHERE `created`<?;", since);
	}
	
	private static void purgeOldAdminSessions() throws SQLException
	{
		long since = System.currentTimeMillis() - InactiveAnonymousAccountPeriod;
		
		AdminUserSession.deleteSessionsInactiveSince(since);
	}
	
	private static void purgeOldUserSessions() throws SQLException
	{
		long since = System.currentTimeMillis() - InactiveAnonymousAccountPeriod;
		
		AdminUserSession.deleteSessionsInactiveSince(since);
	}

}
