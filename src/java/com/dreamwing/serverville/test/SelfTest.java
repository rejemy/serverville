package com.dreamwing.serverville.test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;


public class SelfTest implements Runnable {
	
	private static final Logger l = LogManager.getLogger(SelfTest.class);
	
	protected static Class<?>[] TestClasses = 
		{
			BasicTests.class,
			DataTests.class,
			ScriptTests.class,
			AdminTests.class,
			ClientTests.class
		};
	
	public enum TestStatus
	{
		NONE,
		RUNNING,
		PASSED,
		FAILED
	}
	
	public static class TestInfo
	{
		public int Number;
		public double Sort;
		public String Name;
		public TestStatus Status;
		public long Started;
		public long Time;
		public Throwable Error;
		
		public Method TestMethod;
		public Class<?> TestClass;
	}
	
	protected static SelfTest Instance;
	
	protected TestInfo[] Tests;
	
	protected Thread TestThead;
	protected volatile boolean Running;
	protected long StartedAt=0;
	protected long Time=0;
	protected int TestsRun=0;
	protected int TestsPassed=0;
	protected boolean ExitOnFail=false;
	
	public static void init()
	{
		if(Instance != null)
			return;
		
		Instance = new SelfTest();
	}
	
	protected SelfTest()
	{
		List<TestInfo> tests = new LinkedList<TestInfo>();
		
		int numTests = 1;
		
		for(Class<?> testClass : TestClasses)
		{
			List<TestInfo> classTests = new ArrayList<TestInfo>();
			
			for(Method method : testClass.getMethods())
			{
				
				Test testOptions = method.getAnnotation(Test.class);
				if(testOptions == null)
					continue;
				
				String testName = testClass.getSimpleName()+"."+method.getName();
				
				int modifiers = method.getModifiers();
				if(Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers))
				{
					l.error("Test method "+testName+" is not a public instance method");
					continue;
				}
				
				if(!method.getReturnType().equals(Void.TYPE) || method.getParameterTypes().length != 0)
				{
					l.error("Test method "+testName+" must take no parameters and return void");
					continue;
				}
				
				TestInfo info = new TestInfo();
				info.Name = testName;
				info.Sort = testOptions.order();
				info.Started = 0;
				info.Status = TestStatus.NONE;
				info.Time = 0;
				info.Error = null;
				
				info.TestMethod = method;
				info.TestClass = testClass;
				
				classTests.add(info);
			}
			
			classTests.sort((t1, t2) -> Double.compare(t1.Sort, t2.Sort));
			
			double lastSort=Double.NEGATIVE_INFINITY;
			for(TestInfo info : classTests)
			{
				if(info.Sort <= lastSort)
				{
					l.error("Test with duplicate sort order: "+info.Name+" in "+info.TestClass.getName());
				}
				lastSort = info.Sort;
				info.Number = numTests++;
			}
			
			tests.addAll(classTests);
		}
		
		Tests = new TestInfo[tests.size()];
		tests.toArray(Tests);
	}
	
	public synchronized static void start(boolean exitOnFail)
	{
		init();
		
		Instance.ExitOnFail = exitOnFail;
		Instance.startTests();
	}

	public static long getStartTime()
	{
		return Instance.StartedAt;
	}
	
	public static long getTime()
	{
		return Instance.Time >= 0 ? Instance.Time : (System.currentTimeMillis() - Instance.StartedAt);
	}
	
	public static int getNumTests()
	{
		return Instance.Tests.length;
	}
	
	public static int getTestsRun()
	{
		return Instance.TestsRun;
	}
	
	public static TestInfo getTestInfo(int testNum)
	{
		if(testNum < 0 || testNum >= Instance.Tests.length)
			return null;
		
		return Instance.Tests[testNum];
	}
	
	protected void resetTests()
	{
		TestsPassed = 0;
		TestsRun = 0;
		StartedAt = 0;
		Time = 0;
		
		for(TestInfo test : Tests)
		{
			test.Status = TestStatus.NONE;
			test.Started = 0;
			test.Time = 0;
			test.Error = null;
		}
	}
	
	protected void startTests()
	{
		if(Running)
			return;
		
		resetTests();
		
		TestThead = new Thread(this, "Selftest thread");
		TestThead.setDaemon(false);
		TestThead.start();
		
		Running = true;
	}
	
	@Override
	public void run()
	{
		Class<?> currTestClass = null;
		Object currTestInstance = null;
		
		StartedAt = System.currentTimeMillis();

		for(TestInfo test : Tests)
		{
			if(currTestClass != test.TestClass)
			{
				currTestClass = test.TestClass;
				try {
					currTestInstance = currTestClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					l.error("Couldn't instantiate test class "+currTestClass.getName(), e);
					
					test.Status = TestStatus.FAILED;
					test.Error = e;
					
					testsDone();
					return;
				}
			}
			
			test.Status = TestStatus.RUNNING;
			test.Started = System.currentTimeMillis();
			
			try
			{
				test.TestMethod.invoke(currTestInstance);
				TestsPassed++;
				
				test.Status = TestStatus.PASSED;
			}
			catch(Exception t)
			{
				test.Status = TestStatus.FAILED;
				if(t.getCause() != null)
					test.Error = t.getCause();
				else
					test.Error = t;
				l.warn("Failure running test "+test.Name+":", test.Error);
			}
			finally
			{
				test.Time = System.currentTimeMillis() - test.Started;
				TestsRun++;
			}
		}
		testsDone();
	}
	
	protected void testsDone()
	{
		Time = System.currentTimeMillis() - StartedAt;
		l.info("Selftest completed in "+Time+"ms with "+TestsPassed+"/"+TestsRun+"/"+Instance.Tests.length+" tests passed/run/total");
		TestThead = null;
		Running = false;
		
		if(Instance.ExitOnFail)
		{
			if(TestsPassed < TestsRun)
			{
				l.warn("Shutting down due to failed tests");
				ServervilleMain.Singleton.shutown();
			}
		}
	}
	
}
