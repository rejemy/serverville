package com.dreamwing.serverville.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;


public class ProcessLauncher
{
	private static Logger l = LogManager.getLogger(ProcessLauncher.class);
	
	public static Process startProcess(File workingDir, List<String> args) throws IOException
	{
		List<String> argumentsList = new ArrayList<String>();
		
		argumentsList.add(getJreExecutable("java"));
		if(args != null)
			argumentsList.addAll(args);
		
		String classpath = System.getProperty("java.class.path");
		argumentsList.add("-cp");
		argumentsList.add(classpath);
		argumentsList.add(ServervilleMain.class.getName());
		
		ProcessBuilder processBuilder = new ProcessBuilder(argumentsList);
		processBuilder.directory(workingDir);
		
		String commandLine = String.join(" ", argumentsList);
		
		l.info("Starting process with "+commandLine+" in "+workingDir);
		return processBuilder.start();
	}
	
	
	// Stuff borrowed from http://svn.apache.org/viewvc/ant/core/trunk/src/main/org/apache/tools/ant/util/JavaEnvUtils.java?view=co
	// to figure out the JRE
	
	private static final String OS_NAME =
			System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
	private static final String OS_ARCH =
			System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
	private static final String OS_VERSION =
			System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
	private static final String PATH_SEP =
			System.getProperty("path.separator");
	
	/** Are we on a DOS-based system */
	private static final boolean IS_DOS = isFamily("dos");

	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_WINDOWS = "windows";
	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_9X = "win9x";
	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_NT = "winnt";
	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_DOS = "dos";
	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_MAC = "mac";
	/**
	 * OS family that can be tested for. {@value}
	 */
	private static final String FAMILY_UNIX = "unix";


	/**
	 * OpenJDK is reported to call MacOS X "Darwin"
	 * @see https://issues.apache.org/bugzilla/show_bug.cgi?id=44889
	 * @see https://issues.apache.org/jira/browse/HADOOP-3318
	 */
	private static final String DARWIN = "darwin";
	
	/** shortcut for System.getProperty("java.home") */
	private static final String JAVA_HOME = System.getProperty("java.home");
	
	private static boolean isFamily(String family) {
		return isOs(family, null, null, null);
	}
	
	
	private static boolean isOs(String family, String name, String arch, String version)
	{
		boolean retValue = false;
		
		if (family != null || name != null || arch != null || version != null)
		{
			
			boolean isFamily = true;
			boolean isName = true;
			boolean isArch = true;
			boolean isVersion = true;
			
			if (family != null)
			{	
				//windows probing logic relies on the word 'windows' in
				//the OS
				boolean isWindows = OS_NAME.indexOf(FAMILY_WINDOWS) > -1;
				boolean is9x = false;
				boolean isNT = false;
				if (isWindows)
				{
					//there are only four 9x platforms that we look for
					is9x = (OS_NAME.indexOf("95") >= 0
							|| OS_NAME.indexOf("98") >= 0
							|| OS_NAME.indexOf("me") >= 0
					//wince isn't really 9x, but crippled enough to
					//be a muchness. Ant doesnt run on CE, anyway.
							|| OS_NAME.indexOf("ce") >= 0);
					isNT = !is9x;
				}
				if (family.equals(FAMILY_WINDOWS)){
					isFamily = isWindows;
				} else if (family.equals(FAMILY_9X)) {
					isFamily = isWindows && is9x;
				} else if (family.equals(FAMILY_NT)) {
					isFamily = isWindows && isNT;
				} else if (family.equals(FAMILY_DOS)) {
					isFamily = PATH_SEP.equals(";");
				} else if (family.equals(FAMILY_MAC)) {
					isFamily = OS_NAME.indexOf(FAMILY_MAC) > -1
							|| OS_NAME.indexOf(DARWIN) > -1;
				} else if (family.equals(FAMILY_UNIX)) {
					isFamily = PATH_SEP.equals(":")
							&& (!isFamily(FAMILY_MAC) || OS_NAME.endsWith("x")
							|| OS_NAME.indexOf(DARWIN) > -1);
				} else {
					return false;
				}
			}
			if (name != null) {
				isName = name.equals(OS_NAME);
			}
			if (arch != null) {
				isArch = arch.equals(OS_ARCH);
			}
			if (version != null) {
				isVersion = version.equals(OS_VERSION);
			}
			retValue = isFamily && isName && isArch && isVersion;
		}
		return retValue;
	}
	
	private static String getJreExecutable(String command)
	{
		File jExecutable = null;

		if (jExecutable == null) {
			jExecutable = findInDir(JAVA_HOME + "/bin", command);
		}

		if (jExecutable != null) {
			return jExecutable.getAbsolutePath();
		} else {
			// Unfortunately on Windows java.home doesn't always refer
			// to the correct location, so we need to fall back to
			// assuming java is somewhere on the PATH.
			return addExtension(command);
		}
	}
	
	/**
	 * Look for an executable in a given directory.
	 *
	 * @return null if the executable cannot be found.
	 */
	private static File findInDir(String dirName, String commandName) {
		File dir = new File(dirName).toPath().normalize().toFile();
		File executable = null;
		if (dir.exists()) {
			executable = new File(dir, addExtension(commandName));
			if (!executable.exists()) {
				executable = null;
			}
		}
		return executable;
	}
	
	/**
	 * Adds a system specific extension to the name of an executable.
	 *
	 * @since Ant 1.5
	 */
	private static String addExtension(String command) {
		// This is the most common extension case - exe for windows and OS/2,
		// nothing for *nix.
		return command + (IS_DOS ? ".exe" : "");
	}
	

}
