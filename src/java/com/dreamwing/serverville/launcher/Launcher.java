package com.dreamwing.serverville.launcher;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;

import com.dreamwing.serverville.log.IndexedFileManager;


public class Launcher
{
	
	public static Properties DefaultProperties;
	public static Properties ServerProperties;
	
	public static String PropertiesFilename = "serverville_launcher.properties";
	
	static
	{
		DefaultProperties = new Properties();
		
		DefaultProperties.setProperty("log4j_config", "launcher_log4j2.xml");
		DefaultProperties.setProperty("launch_list", ".");
		DefaultProperties.setProperty("launch_args", "");
	}
	
	private static Logger l;
	
	public static String Hostname;
	
	public static Launcher Singleton;
	
	public static IndexedFileManager LogSearcher=null;
	
	public static List<LaunchedEnvironment> ServerEnvironments;
	
	public static void main(String[] args)
	{
		System.out.println("Launcher starting up!");
		
		initProps();
		
		String launchList = ServerProperties.getProperty("launch_list");
		
		
		Launcher launcher=null;
		try
		{
			launcher = new Launcher();
		}
		catch(Exception e)
		{
			l.error("Exception in launcher creation:", e);
			System.exit(1);
		}
		
		try
		{
			launcher.runServer(launchList);
		}
		catch(Exception e)
		{
			l.error("Exception in launcher startup:", e);
			System.exit(1);
		}
	}
	
	public Launcher() throws Exception
	{
		Singleton = this;

	}
	
	
	public void runServer(String launchlist) throws Exception
	{
		if(launchlist == null || launchlist.length() == 0)
			throw new Exception("Empty launch list");
		
		String workingDir = System.getProperty("user.dir");
		
		Path workingRoot = Paths.get(workingDir);
		
		String javaArgs = ServerProperties.getProperty("launch_args");
		List<String> javaArgList = null;
		if(javaArgs != null && javaArgs.length() > 0)
		{
			javaArgList = Arrays.asList(javaArgs.split("\\s+"));
		}
		
		ServerEnvironments = new ArrayList<LaunchedEnvironment>();
		int id=1;
		
		String[] launchConfigs = launchlist.split(",");
		for(String launchConfig : launchConfigs)
		{
			if(launchConfig.length() == 0)
				throw new Exception("Empty launch config");
			
			Path configPath = workingRoot.resolve(launchConfig).normalize();
			
			File launchConfigDir = configPath.toFile();
			if(!launchConfigDir.exists())
				throw new Exception("Launch config doesn't exist at: "+configPath);
			
			if(!launchConfigDir.isDirectory())
				throw new Exception("Launch config isn't a valid directory at: "+configPath);
			
			LaunchedEnvironment env = new LaunchedEnvironment(id, configPath.toFile(), javaArgList);
			ServerEnvironments.add(env);
			
			id++;
		}
		
		
		for(LaunchedEnvironment env : ServerEnvironments)
		{
			env.startEnvironment();
		}
		

	}
	
	public static void initProps()
	{
		try {
			Hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			Hostname = "localhost";
		}
		System.out.println("Initializing Serverville Launcher on "+Hostname+" with properties in "+PropertiesFilename);
		
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
		LoggerContext lContext = Configurator.initialize("ServervilleLauncher", propsfile);
		Configuration logConfig = lContext.getConfiguration();
		RollingFileAppender appender = (RollingFileAppender)logConfig.getAppender("file");
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


		l.info("Shutdown complete");
		
		System.exit(0);
	}

}
