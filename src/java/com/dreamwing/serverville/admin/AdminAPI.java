package com.dreamwing.serverville.admin;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.log.IndexedFileManager.LogSearchHits;
import com.dreamwing.serverville.net.HttpHandlerOptions;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpUtil;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptLoadException;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.test.SelfTest;
import com.dreamwing.serverville.test.SelfTest.TestStatus;
import com.dreamwing.serverville.util.PasswordUtil;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaders.Names;

public class AdminAPI {

	//private static final Logger l = LogManager.getLogger(AdminAPI.class);

	public static class SignInReply
	{
		public String message;
		public String session_id;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST, auth=false)
	public static ChannelFuture signIn(HttpRequestInfo req) throws Exception
	{
		String username = req.getOneBody("username", null);
		String email = req.getOneBody("email", null);
		String password = req.getOneBody("password");
		
		if(username == null && email == null)
		{
			throw new Exception("Must give either a username or a email to sign in");
		}
		
		SignInReply reply = new SignInReply();
		
		ServervilleUser admin = null;
		
		if(username != null)
		{
			admin = ServervilleUser.findByUsername(username);
		}
		else if(email != null)
		{
			admin = ServervilleUser.findByEmail(email);
		}
		
		if(admin == null || !admin.checkPassword(password) || admin.AdminLevel < ServervilleUser.AdminLevel_AdminReadOnly)
		{
			reply.message = "Invalid user/password";
			return HttpUtil.sendJson(req, reply);
		}
		

		AdminUserSession session = AdminUserSession.startNewSession(admin.getId());
		
		reply.session_id = session.getId();
		
		req.Connection.User = admin;

		return HttpUtil.sendJson(req, reply);
	}
	
	
	
	public static class ServerInfo
	{
		public String hostname;
		public double uptime_seconds;
		public String java_version;
		public String os;
	}
	
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture info(HttpRequestInfo req) throws Exception
	{
		ServerInfo info = new ServerInfo();
		info.hostname = ServervilleMain.Hostname;
		info.uptime_seconds = ServervilleMain.getUptime() / 1000.0;
		info.java_version = System.getProperty("java.version")+" ("+System.getProperty("java.vendor")+")";
		info.os = System.getProperty("os.name")+" "+System.getProperty("os.version");
		
		return HttpUtil.sendJson(req, info);
	}
	
	
	public static class LogFileInfo
	{
		public String filename;
		public double created;
		public double modified;
		public int size;
	}
	
	public static class LogFileListing
	{
		public List<LogFileInfo> files;
	}

	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture logfiles(HttpRequestInfo req) throws Exception
	{
		File logDir = KeyDataManager.getLogDir();
		
		File logFiles[] = logDir.listFiles();
		
		LogFileListing result = new LogFileListing();
		result.files = new LinkedList<LogFileInfo>();
		
		for(File logFile : logFiles)
		{
			if(!logFile.isFile() || logFile.isHidden() || !logFile.canRead() || !logFile.getName().endsWith(".log"))
				continue;
			
			try
			{
				Path logPath = logFile.toPath();
				BasicFileAttributes attr = Files.readAttributes(logPath, BasicFileAttributes.class);
				
				LogFileInfo info = new LogFileInfo();
				info.filename = logFile.getName();
				info.created = attr.creationTime().toMillis();
				info.modified = attr.lastModifiedTime().toMillis();
				info.size = (int)logFile.length();
				
				result.files.add(info);
			}
			catch(Exception e)
			{
				continue;
			}
		}
		
		
		return HttpUtil.sendJson(req, result);
	}
	

	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture searchLogs(HttpRequestInfo req) throws Exception
	{
		String query = req.getOneQuery("q", "*:*");

		long from = (long)req.getOneQueryAsDouble("from", 0.0);
		long to = (long)req.getOneQueryAsDouble("to", 0.0);
	
		try
		{
			LogSearchHits hits = ServervilleMain.LogSearcher.query(query, from, to);
			return HttpUtil.sendJson(req, hits);
		}
		catch(ParseException e)
		{
			return HttpUtil.sendError(req, "Invalid query: "+e.getMessage(), HttpResponseStatus.BAD_REQUEST);
		}
		
		
	}
	
	public static class SelfTestResult
	{
		public int index;
		public String description;
		public double started_at;
		public double time;
		public String status;
		public String error;
		public String stack_trace;
	}
	
	public static class SelfTestStatus
	{
		public double started_at;
		public double time;
		public int total_tests;
		public int tests_completed;
		public List<SelfTestResult> results;
	}
	
	public static class SelfTestStarted
	{
		public double started_at;
	}
	
	private static SelfTestStatus getTestStatus(int startAt)
	{
		SelfTestStatus results = new SelfTestStatus();
		results.started_at = SelfTest.getStartTime();
		results.time = SelfTest.getTime();
		results.total_tests = SelfTest.getNumTests();
		results.tests_completed = SelfTest.getTestsRun();
		results.results = new LinkedList<SelfTestResult>();
		
		for(int i=startAt; i<results.total_tests; i++)
		{
			SelfTest.TestInfo testInfo = SelfTest.getTestInfo(i);
			if(startAt != 0 && testInfo.Status == TestStatus.NONE)
				break;
			
			SelfTestResult testResult = new SelfTestResult();
			testResult.index = testInfo.Number;
			testResult.description = testInfo.Name;
			testResult.started_at = testInfo.Started;
			testResult.time = testInfo.Time;
			testResult.status = testInfo.Status.toString();
			if(testInfo.Error != null)
			{
				testResult.error = testInfo.Error.toString();
				
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				testInfo.Error.printStackTrace(pw);
				testResult.stack_trace = sw.toString();
			}
			
			
			results.results.add(testResult);
		}
		
		return results;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture startSelftest(HttpRequestInfo req) throws Exception
	{
		SelfTest.start(false);
		
		SelfTestStarted result = new SelfTestStarted();
		result.started_at = SelfTest.getStartTime();
		
		return HttpUtil.sendJson(req, result);
	}
	

	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture selftest(HttpRequestInfo req) throws Exception
	{
		int startAt = req.getOneQueryAsInt("first", 0);
		
		SelfTestStatus status = getTestStatus(startAt);
		return HttpUtil.sendJson(req, status);
	}
	
	public static class CreateUserReply
	{
		public String user_id;
	}
	
	public static class UserInfo
	{
		public String id;
		public String email;
		public String username;
		public double created;
		public double modified;
		public double admin_level;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture user(HttpRequestInfo req) throws Exception
	{
		String id = req.getOneQuery("id", null);
		String email = req.getOneQuery("email", null);
		String username = req.getOneQuery("username", null);
		
		ServervilleUser user = null;
		
		if(id != null)
			user = ServervilleUser.findById(id);
		else if(email != null)
			user = ServervilleUser.findByEmail(email);
		else if(username != null)
			user = ServervilleUser.findByUsername(username);
		else
			return HttpUtil.sendError(req, "Must query on one of id,email or username", HttpResponseStatus.BAD_REQUEST);
		
		if(user == null)
			return HttpUtil.sendError(req, "User not found", HttpResponseStatus.NOT_FOUND);
		
		UserInfo info = new UserInfo();
		
		info.id = user.getId();
		info.email = user.getEmail();
		info.username = user.getUsername();
		info.created = user.Created.getTime();
		info.modified = user.Modified.getTime();
		info.admin_level = user.AdminLevel;
		
		return HttpUtil.sendJson(req, info);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture createUser(HttpRequestInfo req) throws Exception
	{
		String username = req.getOneBody("username", null);
		String password = req.getOneBody("password", null);
		String email = req.getOneBody("email", null);
		
		String adminLevelStr = req.getOneBody("admin_level", "user");
		int adminLevel = ServervilleUser.parseAdminLevel(adminLevelStr);
		if(adminLevel < 0)
			return HttpUtil.sendError(req, "Invalid user level: "+adminLevelStr, HttpResponseStatus.BAD_REQUEST);

		if(!PasswordUtil.validatePassword(password))
			return HttpUtil.sendError(req, "Invalid password", HttpResponseStatus.BAD_REQUEST);
		
		ServervilleUser admin = ServervilleUser.create(password, username, email, adminLevel);
		
		CreateUserReply reply = new CreateUserReply();
		reply.user_id = admin.getId();
		
		return HttpUtil.sendJson(req, reply);
	}
	

	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture deleteUser(HttpRequestInfo req) throws Exception
	{
		String userId = req.getOneBody("user_id");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpUtil.sendError(req, "User not found: "+userId, HttpResponseStatus.BAD_REQUEST);
		}
		
		if(user.getId().equals(req.getUser().getId()))
		{
			return HttpUtil.sendError(req, "Don't delete yourself, silly!", HttpResponseStatus.BAD_REQUEST);
		}
		
		user.delete();
		
		return HttpUtil.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserPassword(HttpRequestInfo req) throws Exception
	{
		String userId = req.getOneBody("user_id");
		String password = req.getOneBody("password");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpUtil.sendError(req, "User not found: "+userId, HttpResponseStatus.BAD_REQUEST);
		}
		
		user.setPassword(password);
		
		return HttpUtil.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserUsername(HttpRequestInfo req) throws Exception
	{
		String userId = req.getOneBody("user_id");
		String username = req.getOneBody("username");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpUtil.sendError(req, "User not found: "+userId, HttpResponseStatus.BAD_REQUEST);
		}
		
		user.setUsername(username);
		
		return HttpUtil.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserEmail(HttpRequestInfo req) throws Exception
	{
		String userId = req.getOneBody("user_id");
		String email = req.getOneBody("email");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpUtil.sendError(req, "User not found: "+userId, HttpResponseStatus.BAD_REQUEST);
		}
		
		user.setEmail(email);
		
		return HttpUtil.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserAdminLevel(HttpRequestInfo req) throws Exception
	{
		String userId = req.getOneBody("user_id");
		String adminLevelStr = req.getOneBody("admin_level");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpUtil.sendError(req, "User not found: "+userId, HttpResponseStatus.BAD_REQUEST);
		}
		
		int adminLevel = ServervilleUser.parseAdminLevel(adminLevelStr);
		if(adminLevel < 0)
			return HttpUtil.sendError(req, "Invalid user level: "+adminLevelStr, HttpResponseStatus.BAD_REQUEST);
		
		user.AdminLevel = adminLevel;
		user.update();
		
		return HttpUtil.sendSuccess(req);
	}
	
	public static class AgentKeyInfo
	{
		public String key;
		public String comment;
		public String iprange;
		public double expiration;
	}
	
	public static class AgentKeyInfoList
	{
		public List<AgentKeyInfo> keys;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture agentKeys(HttpRequestInfo req) throws Exception
	{
		AgentKeyInfoList result = new AgentKeyInfoList();
		result.keys = new LinkedList<AgentKeyInfo>();
		
		List<AgentKey> list = AgentKey.loadAll();
		for(AgentKey key : list)
		{
			AgentKeyInfo reply = new AgentKeyInfo();
			
			reply.key = key.Key;
			reply.comment = key.Comment;
			reply.iprange = key.IPRange;
			if(key.Expiration != null)
				reply.expiration = key.Expiration.getTime();
			else
				reply.expiration = 0;
			
			result.keys.add(reply);
		}
		
		return HttpUtil.sendJson(req, result);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture createAgentKey(HttpRequestInfo req) throws Exception
	{
		String comment = req.getOneBody("comment", null);
		String iprange = req.getOneBody("iprange", null);
		long expirationLong = req.getOneBodyAsLong("expiration", 0);
		
		Date expiration = null;
		if(expirationLong > 0)
			expiration = new Date(expirationLong);
		
		AgentKey key = AgentKeyManager.createAgentKey(comment, iprange, expiration);
		
		AgentKeyInfo reply = new AgentKeyInfo();
		
		reply.key = key.Key;
		reply.comment = key.Comment;
		reply.iprange = key.IPRange;
		if(key.Expiration != null)
			reply.expiration = key.Expiration.getTime();
		else
			reply.expiration = 0;
		
		return HttpUtil.sendJson(req, reply);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture editAgentKey(HttpRequestInfo req) throws Exception
	{
		String key = req.getOneBody("key", null);
		if(key == null)
			throw new Exception("Must supply a key id");
		
		String comment = req.getOneBody("comment", null);
		String iprange = req.getOneBody("iprange", null);
		String expirationStr = req.getOneBody("expiration", null);
		
		AgentKey editKey = AgentKey.load(key);
		if(editKey == null)
			throw new Exception("Key not found");
		
		if(comment != null)
			editKey.Comment = comment;
		if(iprange != null)
			editKey.IPRange = iprange;
		if(expirationStr != null)
		{
			long expLong = Long.parseLong(expirationStr);
			if(expLong > 0)
				editKey.Expiration = new Date(expLong);
			else
				editKey.Expiration = null;
		}
		
		editKey.update();
		
		AgentKeyInfo reply = new AgentKeyInfo();
		
		reply.key = editKey.Key;
		reply.comment = editKey.Comment;
		reply.iprange = editKey.IPRange;
		if(editKey.Expiration != null)
			reply.expiration = editKey.Expiration.getTime();
		else
			reply.expiration = 0;
		
		return HttpUtil.sendJson(req, reply);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture deleteAgentKey(HttpRequestInfo req) throws Exception
	{
		String key = req.getOneBody("key", null);
		if(key == null)
			throw new Exception("Must supply a key id");
		
		AgentKey editKey = AgentKey.load(key);
		if(editKey == null)
			throw new Exception("Key not found");
		
		editKey.delete();
		
		return HttpUtil.sendSuccess(req);
	}

	public static class ScriptDataInfo
	{
		public String id;
		public String source;
		public double created;
		public double modified;
	}
	
	public static class ScriptDataInfoList
	{
		public List<ScriptDataInfo> scripts;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture scriptInfo(HttpRequestInfo req) throws SQLException, JsonProcessingException
	{
		String id = req.getOneQuery("id", null);
		if(id == null)
			return HttpUtil.sendError(req, "Must supply a script id", HttpResponseStatus.BAD_REQUEST);
		
		ScriptData script = ScriptData.findById(id);
		if(script == null)
			return HttpUtil.sendError(req, "Script not found: "+id, HttpResponseStatus.NOT_FOUND);
		
		ScriptDataInfo info = new ScriptDataInfo();
		
		info.id = script.Id;
		info.source = script.ScriptSource;
		info.created = script.Created.getTime();
		info.modified = script.Modified.getTime();
		
		return HttpUtil.sendJson(req, info);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture script(HttpRequestInfo req) throws SQLException, JsonProcessingException
	{
		String id = req.getOneQuery("id", null);
		if(id == null)
			return HttpUtil.sendError(req, "Must supply a script id", HttpResponseStatus.BAD_REQUEST);
		
		ScriptData script = ScriptData.findById(id);
		if(script == null)
			return HttpUtil.sendError(req, "Script not found: "+id, HttpResponseStatus.NOT_FOUND);
		

		return HttpUtil.sendText(req, script.ScriptSource, "application/javascript");
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture scripts(HttpRequestInfo req) throws SQLException, JsonProcessingException
	{
		List<ScriptData> scripts = ScriptData.loadAll();
		
		ScriptDataInfoList list = new ScriptDataInfoList();
		list.scripts = new ArrayList<ScriptDataInfo>(scripts.size());
		
		for(ScriptData script : scripts)
		{
			ScriptDataInfo info = new ScriptDataInfo();
			
			info.id = script.Id;
			info.source = script.ScriptSource;
			info.created = script.Created.getTime();
			info.modified = script.Modified.getTime();
			
			list.scripts.add(info);
		}
		
		return HttpUtil.sendJson(req, list);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture addScript(HttpRequestInfo req) throws SQLException, JsonProcessingException
	{
		String id = req.getOneQuery("id", null);
		if(id == null)
			return HttpUtil.sendError(req, "Must supply a script id", HttpResponseStatus.BAD_REQUEST);
		
		String contentType = req.Request.headers().get(Names.CONTENT_TYPE);
		if(contentType == null || !contentType.equals("application/javascript"))
			return HttpUtil.sendError(req, "Script must be of type application/javascript", HttpResponseStatus.BAD_REQUEST);
		
		String scriptData = req.getBody();
		if(scriptData == null || scriptData.length() == 0)
			return HttpUtil.sendError(req, "Must include a script body", HttpResponseStatus.BAD_REQUEST);
		
		List<ScriptData> scripts = ScriptData.loadAll();

		ScriptData newScript = null;
		
		boolean found = false;
		for(ScriptData script : scripts)
		{
			if(script.Id.equals(id))
			{
				newScript = script;
				newScript.ScriptSource = scriptData;
				found = true;
				break;
			}
		}
		
		if(!found)
		{
			newScript = new ScriptData();
			newScript.Id = id;
			newScript.ScriptSource = scriptData;
			newScript.Created = new Date();
			newScript.Modified = new Date();
			
			scripts.add(newScript);
			scripts.sort(new ScriptData.ScriptIdComparator());
		}
		
		try {
			ScriptEngineContext testContext = new ScriptEngineContext();
			testContext.init(scripts);
		} catch (ScriptLoadException e) {
			String errorMessage = e.getCause().getMessage();
			errorMessage = errorMessage.replaceAll("\\<eval\\>", e.ScriptId);
			return HttpUtil.sendError(req, "Javascript error: "+errorMessage, HttpResponseStatus.BAD_REQUEST);
		}
		
		if(found)
		{
			newScript.update();
		}
		else
		{
			newScript.create();
		}
		
		ScriptManager.scriptsUpdated();
		
		return HttpUtil.sendSuccess(req);
	}

	
	
	
}
