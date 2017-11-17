package com.dreamwing.serverville.db;


import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.data.AdminActionLog;
import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.data.ClusterMember;
import com.dreamwing.serverville.data.CurrencyHistory;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.CurrencyRecord;
import com.dreamwing.serverville.data.InviteCode;
import com.dreamwing.serverville.data.KeyDataRecord;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.RecordPermissionsManager;
import com.dreamwing.serverville.data.ResidentPermissionsManager;
import com.dreamwing.serverville.data.Purchase;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.UserMessage;
import com.dreamwing.serverville.data.UserSession;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DatabaseManager
{
	private static final Logger l = LogManager.getLogger(DatabaseManager.class);
	
	private static ComboPooledDataSource DataSource;
	
	private static ServervilleQueryRunner SqlServer;
	
	public static Dao<ClusterMember, String> ClusterMemberDao;
	
	public static Dao<ServervilleUser, String> ServervilleUserDao;
	public static Dao<UserSession, String> ServervilleUser_UserSessionDao;
	public static Dao<UserSession.UserSessionLookup, Void> ServervilleUser_UserSession_UserIdDao;
	public static Dao<ServervilleUser.UsernameLookup, String> ServervilleUser_UsernameDao;
	public static Dao<ServervilleUser.EmailLookup, String> ServervilleUser_EmailDao;
	
	public static Dao<UserMessage, String> UserMessageDao;
	
	public static Dao<AdminUserSession, String> AdminUserSessionDao;
	public static Dao<AdminUserSession.AdminUserSessionLookup, Void> AdminUserSession_UserIdDao;
	
	public static Dao<AdminActionLog, String> AdminActionLogDao;
	
	public static Dao<ScriptData, String> ScriptDataDao;
	
	public static Dao<AgentKey, String> AgentKeyDao;
	
	public static Dao<KeyDataRecord, String> KeyDataRecordDao;
	public static Dao<RecordPermissionsManager.RecordPermissionsRecord, String> RecordPermissionsRecordDao;
	public static Dao<ResidentPermissionsManager.ResidentPermissionsRecord, String> ResidentPermissionsRecordDao;
	public static Dao<InviteCode, String> InviteCodeDao;
	
	public static Dao<CurrencyRecord, Void> CurrencyDao;
	public static Dao<CurrencyHistory, Void> CurrencyHistoryDao;
	public static Dao<CurrencyInfo, String> CurrencyInfoDao;
	
	public static Dao<Product, String> ProductDao;
	public static Dao<Purchase, String> PurchaseDao;
	
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
		
		DataSource.setIdleConnectionTestPeriod(60);
		DataSource.setTestConnectionOnCheckout(true);
		
		SqlServer = new ServervilleQueryRunner(DataSource);
		
		DataSourceConnectionSource cs = new DataSourceConnectionSource(DataSource, url);
		
		ClusterMemberDao = DaoManager.createDao(cs, ClusterMember.class);
		
		ServervilleUserDao = DaoManager.createDao(cs, ServervilleUser.class);
		ServervilleUser_UserSessionDao = DaoManager.createDao(cs, UserSession.class);
		ServervilleUser_UserSession_UserIdDao = DaoManager.createDao(cs, UserSession.UserSessionLookup.class);
		ServervilleUser_UsernameDao = DaoManager.createDao(cs, ServervilleUser.UsernameLookup.class);
		ServervilleUser_EmailDao = DaoManager.createDao(cs, ServervilleUser.EmailLookup.class);
		
		UserMessageDao = DaoManager.createDao(cs, UserMessage.class);
		
		AdminUserSessionDao = DaoManager.createDao(cs, AdminUserSession.class);
		AdminUserSession_UserIdDao = DaoManager.createDao(cs, AdminUserSession.AdminUserSessionLookup.class);
		
		AdminActionLogDao = DaoManager.createDao(cs, AdminActionLog.class);

		ScriptDataDao = DaoManager.createDao(cs, ScriptData.class);
		
		AgentKeyDao = DaoManager.createDao(cs, AgentKey.class);
		
		KeyDataRecordDao = DaoManager.createDao(cs, KeyDataRecord.class);
		RecordPermissionsRecordDao = DaoManager.createDao(cs, RecordPermissionsManager.RecordPermissionsRecord.class);
		ResidentPermissionsRecordDao = DaoManager.createDao(cs, ResidentPermissionsManager.ResidentPermissionsRecord.class);
		
		InviteCodeDao = DaoManager.createDao(cs, InviteCode.class);
		
		CurrencyDao = DaoManager.createDao(cs, CurrencyRecord.class);
		CurrencyHistoryDao = DaoManager.createDao(cs, CurrencyHistory.class);
		CurrencyInfoDao = DaoManager.createDao(cs, CurrencyInfo.class);
		
		ProductDao = DaoManager.createDao(cs, Product.class);
		PurchaseDao = DaoManager.createDao(cs, Purchase.class);
		
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
		
		Random rand = new Random();
		ServervilleMain.ServiceScheduler.scheduleAtFixedRate(agentPurger, rand.nextInt(60), 60, TimeUnit.MINUTES);
	}
	
}
