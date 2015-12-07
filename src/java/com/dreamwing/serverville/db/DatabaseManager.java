package com.dreamwing.serverville.db;


import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.data.AdminActionLog;
import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.data.ServervilleUser;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DatabaseManager {
	
	private static final Logger l = LogManager.getLogger(DatabaseManager.class);
	
	private static ComboPooledDataSource DataSource;
	
	private static ServervilleQueryRunner SqlServer;
	
	public static Dao<ServervilleUser, String> ServervilleUserDao;
	public static Dao<ServervilleUser.SessionIdLookup, String> ServervilleUser_SessionIdDao;
	public static Dao<ServervilleUser.UsernameLookup, String> ServervilleUser_UsernameDao;
	public static Dao<ServervilleUser.EmailLookup, String> ServervilleUser_EmailDao;
	
	public static Dao<AdminUserSession, String> AdminUserSessionDao;
	public static Dao<AdminUserSession.UserIdLookup, String> AdminUserSession_UserIdDao;
	
	public static Dao<AdminActionLog, String> AdminActionLogDao;
	
	public static Dao<ScriptData, String> ScriptDataDao;
	
	public static Dao<AgentKey, String> AgentKeyDao;
	
	public static void init() throws Exception
	{
		String driverClass = ServervilleMain.ServerProperties.getProperty("jdbc_driver");
		String url = ServervilleMain.ServerProperties.getProperty("jdbc_url");
		String username = ServervilleMain.ServerProperties.getProperty("jdbc_user");
		String password = ServervilleMain.ServerProperties.getProperty("jdbc_password");

		if(driverClass.length() == 0 || url.length() == 0)
		{
			throw new Exception("No JDB driver class or URL given, aborting");
		}
		
		if(username.length() == 0)
			username = null;
		if(password.length() == 0)
			password = null;
		
		String credentials = "";
		if(username != null)
		{
			credentials = " as "+username;
			if(password != null)
				credentials += " with password XXXXXXXXXX";
		}
		l.info("Connecting to database "+driverClass+" at "+url+credentials);
		
		DataSource = new ComboPooledDataSource();
		DataSource.setDriverClass(driverClass);
		DataSource.setJdbcUrl(url);
		if(username != null)
		{
			DataSource.setUser(username);
			if(password != null)
				DataSource.setPassword(password);
		}
		
		SqlServer = new ServervilleQueryRunner(DataSource);
		
		DataSourceConnectionSource cs = new DataSourceConnectionSource(DataSource, url);
		
		ServervilleUserDao = DaoManager.createDao(cs, ServervilleUser.class);
		ServervilleUser_SessionIdDao = DaoManager.createDao(cs, ServervilleUser.SessionIdLookup.class);
		ServervilleUser_UsernameDao = DaoManager.createDao(cs, ServervilleUser.UsernameLookup.class);
		ServervilleUser_EmailDao = DaoManager.createDao(cs, ServervilleUser.EmailLookup.class);
		
		AdminUserSessionDao = DaoManager.createDao(cs, AdminUserSession.class);
		AdminUserSession_UserIdDao = DaoManager.createDao(cs, AdminUserSession.UserIdLookup.class);
		
		AdminActionLogDao = DaoManager.createDao(cs, AdminActionLog.class);

		ScriptDataDao = DaoManager.createDao(cs, ScriptData.class);
		
		AgentKeyDao = DaoManager.createDao(cs, AgentKey.class);
		
		initAgentKeyPurger();
	}

	
	public static ServervilleQueryRunner getServer()
	{
		return SqlServer;
	}
	
	private static void initAgentKeyPurger()
	{
		final Runnable agentPurger = new Runnable()
		{
			public void run()
			{
				AgentKeyManager.purgeExpiredKeys();
			}
		};
		
		ServervilleMain.ServiceScheduler.scheduleAtFixedRate(agentPurger, 1, 1, TimeUnit.HOURS);
	}
	
}
