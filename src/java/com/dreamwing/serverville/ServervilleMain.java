package com.dreamwing.serverville;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;

import com.dreamwing.serverville.admin.AdminServerSocketInitializer;
import com.dreamwing.serverville.agent.AgentServerSocketInitializer;
import com.dreamwing.serverville.client.ClientSessionManager;
import com.dreamwing.serverville.client.ClientSocketInitializer;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.launcher.Launcher;
import com.dreamwing.serverville.log.IndexedFileAppender;
import com.dreamwing.serverville.log.IndexedFileManager;
import com.dreamwing.serverville.net.SslProtocolDetector;
import com.dreamwing.serverville.residents.ResidentManager;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.stripe.StripeInterface;
import com.dreamwing.serverville.test.SelfTest;
import com.dreamwing.serverville.util.CurrencyUtil;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.LocaleUtil;


public class ServervilleMain {

	public static Properties DefaultProperties;
	public static Properties ServerProperties;
	
	public static String PropertiesFilename = "serverville.properties";
	
	static
	{
		DefaultProperties = new Properties();
		
		DefaultProperties.setProperty("shutdown_on_input", "false");
		DefaultProperties.setProperty("pidfile", "");
		DefaultProperties.setProperty("log4j_config", "log4j2.xml");
		DefaultProperties.setProperty("data_root", "data");
		DefaultProperties.setProperty("client_port", "8000");
		DefaultProperties.setProperty("agent_port", "8001");
		DefaultProperties.setProperty("admin_port", "8002");
		DefaultProperties.setProperty("res_root", "res");
		DefaultProperties.setProperty("require_invite", "false");
		DefaultProperties.setProperty("cache_files_under", "30000");
		DefaultProperties.setProperty("pretty_json", "false");
		DefaultProperties.setProperty("selftest_on_start", "false");
		DefaultProperties.setProperty("selftest_timeout", "3000");
		DefaultProperties.setProperty("exit_on_selftest_fail", "false");
		DefaultProperties.setProperty("jdbc_driver", "com.mysql.jdbc.Driver");
		DefaultProperties.setProperty("jdbc_url", "");
		DefaultProperties.setProperty("jdbc_user", "");
		DefaultProperties.setProperty("jdbc_password", "");
		DefaultProperties.setProperty("ssl_key_file", "");
		DefaultProperties.setProperty("ssl_cert_chain_file", "");
		DefaultProperties.setProperty("admin_ssl_only", "false");
		DefaultProperties.setProperty("agent_ssl_only", "false");
		DefaultProperties.setProperty("client_ssl_only", "false");
		DefaultProperties.setProperty("hostname", "localhost");
		DefaultProperties.setProperty("stripe_api_key", "");
		DefaultProperties.setProperty("default_language", "en-US");
		DefaultProperties.setProperty("default_currency", "USD");
		DefaultProperties.setProperty("writeable_directories", "");
	}
	
	private static Logger l;
	
	public static ServervilleMain Singleton;
	
	public static Path WorkingPath;
	public static Path DataRoot;
	public static Path ResRoot;
	
	public static String Hostname;

	private volatile boolean Running=true;
	
	public static IndexedFileManager LogSearcher=null;
	
	public static long StartupTime; 
	
	public static ScheduledExecutorService ServiceScheduler;
	
	public static boolean RequireInvite=false;
	
	public static void main(String[] args)
	{
		System.out.println("Starting up!");
		
		StartupTime = System.currentTimeMillis();
		
		initProps();
		
		ServervilleMain server=null;
		try
		{
			server = new ServervilleMain();
		}
		catch(Exception e)
		{
			l.error("Exception in server creation:", e);
			System.exit(1);
		}
		
		try
		{
			server.runServer();
		}
		catch(Exception e)
		{
			l.error("Exception in server startup:", e);
			System.exit(1);
		}
	}
	
	public ServervilleMain() throws Exception
	{
		Singleton = this;
		
		init();
	}

	public static void initProps()
	{
		try {
			Hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			Hostname = "localhost";
		}
		System.out.println("Initializing Serverville Server on "+Hostname+" with properties in "+PropertiesFilename);
		
		try
		{
			ServerProperties = loadPropertiesFile(PropertiesFilename);
		}
		catch(Exception e)
		{
			System.out.println("Couldn't load properties. Using defaults.");
			ServerProperties = DefaultProperties;
		}
		
		configureLogger(ServerProperties.getProperty("log4j_config"));
		
		String workingDir = System.getProperty("user.dir");
		l.info("Starting up in working directory "+workingDir);
		if(ServerProperties == DefaultProperties)
			l.info("Using default properties");
		else
			l.info("Using properties from "+PropertiesFilename);
	}
	
	public void init() throws Exception
	{
		ServiceScheduler = Executors.newScheduledThreadPool(1);
		
		String workingDir = System.getProperty("user.dir");
		
		WorkingPath = Paths.get(workingDir);
		String dataPath = ServerProperties.getProperty("data_root");
		
		DataRoot = WorkingPath.resolve(dataPath).normalize();
		l.info("Data root is: "+DataRoot);
		File dataRootFile = DataRoot.toFile();
		if(dataRootFile.exists())
		{
			if(!dataRootFile.canWrite() || !dataRootFile.canRead())
				throw new Exception("Data root "+DataRoot+" not read/writable");
		}
		else
		{
			if(!dataRootFile.mkdirs())
				throw new Exception("Could not create data root at "+DataRoot);
		}
		
		String resPath = ServerProperties.getProperty("res_root");
		ResRoot = WorkingPath.resolve(resPath).normalize();
		File resRootFile = ResRoot.toFile();
    	if(!resRootFile.exists() || !resRootFile.canRead() || !resRootFile.isDirectory())
    		throw new Exception("Invalid res root: "+ResRoot);
    	
    	RequireInvite = Boolean.parseBoolean(ServerProperties.getProperty("require_invite"));
    	
    	CurrencyUtil.DefaultCurrency = ServervilleMain.ServerProperties.getProperty("default_currency").toUpperCase();
    	if(!CurrencyUtil.isValidCurrency(CurrencyUtil.DefaultCurrency))
    		throw new Exception("Invalid default currency: "+CurrencyUtil.DefaultCurrency);
    	
    	LocaleUtil.DefaultLanguage = ServervilleMain.ServerProperties.getProperty("default_language");
    	
    	WritableDirectories.init();
    	StripeInterface.init();
    	SslProtocolDetector.init();
    	JSON.init();
    	DatabaseManager.init();
    	KeyDataManager.init();
    	CurrencyInfoManager.init();
    	ProductManager.init();
    	ResidentManager.init();
    	UserManager.init();
    	ScriptManager.init();
    	ClientSessionManager.init();
    	
    	SelfTest.init();
	}
	
	public void runServer() throws Exception
	{
		l.info("Serverville Server starting up!");

		int adminPort = Integer.parseInt(ServerProperties.getProperty("admin_port"));
		if(adminPort != 0)
		{
			l.info("Starting admin listener on port "+adminPort);
			AdminServerSocketInitializer.startListener(adminPort);
		}
		else
		{
			l.info("Skipping admin service");
		}
		
		int agentPort = Integer.parseInt(ServerProperties.getProperty("agent_port"));
		if(agentPort != 0)
		{
			l.info("Starting agent listener on port "+agentPort);
			AgentServerSocketInitializer.startListener(agentPort);
		}
		else
		{
			l.info("Skipping agent service");
		}
		
		int clientPort = Integer.parseInt(ServerProperties.getProperty("client_port"));
		if(clientPort != 0)
		{
			l.info("Starting client listener on port "+clientPort);
			ClientSocketInitializer.startListener(clientPort);
		}
		else
		{
			l.info("Skipping client service");
		}
		
		writePidfile();
		
		l.info("Running!");
		
		if(Boolean.parseBoolean(ServerProperties.getProperty("selftest_on_start")))
		{
			boolean exitOnFail = Boolean.parseBoolean(ServerProperties.getProperty("exit_on_selftest_fail"));
			SelfTest.start(exitOnFail);
		}

		boolean shutdownOnInput = Boolean.parseBoolean(ServerProperties.getProperty("shutdown_on_input"));
		
		try
		{
			while(Running)
			{
				if(System.in.available() > 0)
				{
					System.in.read();
					if(shutdownOnInput)
					{
						shutown();
					}
				}
				Thread.sleep(100);
				//l.info("Random message "+Math.random());
			}
		}
		catch(Exception e)
		{
			shutown();
		}
	}
	

	public static Properties loadPropertiesFile(String filename) throws FileNotFoundException, IOException
	{
		Properties props = new Properties(DefaultProperties);
		
		FileInputStream input = new FileInputStream(filename);
		
		props.load(input);
		
		return props;
	}
	
	public static void configureLogger(String propsfile)
	{
		File prop = new File(propsfile);
		if(prop.exists() && prop.canRead())
		{
			if(!propsfile.endsWith(".xml"))
			{
				System.out.println("Log4j configuration file in unknown format: "+propsfile);
			}
		}
		else
		{
			System.out.println("Log4j configuration file missing or not readable: "+propsfile);
		}
		
		PluginManager.addPackage("com.dreamwing.serverville.log");
		LoggerContext lContext = Configurator.initialize("Serverville", propsfile);
		Configuration logConfig = lContext.getConfiguration();
		IndexedFileAppender appender = (IndexedFileAppender)logConfig.getAppender("file");
		String logFilePath = appender.getFileName();
		
		l = LogManager.getLogger(Launcher.class);
		
		LogSearcher = IndexedFileManager.getManager(logFilePath);
	}
	
	
	public void writePidfile()
	{
		String pidfilename = ServerProperties.getProperty("pidfile");
		if(pidfilename.length() > 0)
		{
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			int atIndex = pid.indexOf("@");
			if(atIndex >= 0)
				pid = pid.substring(0, atIndex);
			try 
			{
				FileWriter pidfile = new FileWriter(pidfilename);
				pidfile.write(pid);
				pidfile.close();
				
				l.info("Wrote pidfile to "+pidfilename);
				
				File pidfileRef = new File(pidfilename);
				pidfileRef.deleteOnExit();
			} catch (IOException e) {
				l.error("Error creating pidfile: ", e);
			}
		}
	}
	
	public void shutown()
	{
		l.info("Starting shutdown...");

		ClientSocketInitializer.shutdown();
		AdminServerSocketInitializer.shutdown();
		
		l.info("Shutdown complete");
		
		System.exit(0);
	}
	
	public static long getUptime()
	{
		return System.currentTimeMillis() - StartupTime;
	}
}
