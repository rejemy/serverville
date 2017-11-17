package com.dreamwing.serverville.launcher;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LaunchedEnvironment extends Thread
{
	private int Id;
	private Process ServerProcess;
	private File WorkingDir;
	private List<String> JavaArgs;
	
	private static Logger l = LogManager.getLogger(LaunchedEnvironment.class);
	
	public LaunchedEnvironment(int id, File workingDir, List<String> args)
	{
		super("ChildServerMonitor"+id);
		Id = id;
		WorkingDir = workingDir;
		JavaArgs = args;
	}
	
	public void startEnvironment()
	{
		start();
	}
	
	public void stopEnvironment()
	{
		if(ServerProcess == null)
			return;
		
		ServerProcess.destroy();
	}
	
	public void run()
	{
		while(true)
		{
			try
			{
				l.info("Starting child process "+Id+" in "+WorkingDir);
				ServerProcess = ProcessLauncher.startProcess(WorkingDir, JavaArgs);
				l.info("Child process "+Id+" is up");
				int processStatus = ServerProcess.waitFor();
				ServerProcess = null;
				if(processStatus == 0)
					return;
				l.error("Child process "+Id+" exited with code "+processStatus);
				
			}
			catch(Exception e)
			{
				l.error("Exception in child process "+Id+" monitor thread:", e);
			}
		}
	}
}
