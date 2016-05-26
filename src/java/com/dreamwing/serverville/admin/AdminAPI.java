package com.dreamwing.serverville.admin;

import java.io.File;
import java.io.IOException;
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

import javax.script.ScriptException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.agent.AgentKeyManager;
import com.dreamwing.serverville.data.AdminUserSession;
import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.data.JsonDataType;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.ScriptData;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.log.IndexedFileManager.LogSearchHits;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpHandlerOptions;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.net.SubnetMask;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptLoadException;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.test.SelfTest;
import com.dreamwing.serverville.test.SelfTest.TestStatus;
import com.dreamwing.serverville.util.PasswordUtil;
import okhttp3.MediaType;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.HttpHeaderNames;

public class AdminAPI {

	private static final Logger l = LogManager.getLogger(AdminAPI.class);

	public static class SignInReply
	{
		public String message;
		public String session_id;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST, auth=false)
	public static ChannelFuture signIn(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String username = req.getOneBody("username", null);
		String email = req.getOneBody("email", null);
		String password = req.getOneBody("password");
		
		if(username == null && email == null)
		{
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply either a username or email");
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
			throw new JsonApiException(ApiErrors.BAD_AUTH, "Password does not match");
		}
		

		AdminUserSession session = AdminUserSession.startNewSession(admin.getId());
		
		reply.session_id = session.getId();
		
		req.Connection.User = admin;

		return HttpHelpers.sendJson(req, reply);
	}
	
	
	
	public static class ServerInfo
	{
		public String hostname;
		public double uptime_seconds;
		public String java_version;
		public String os;
	}
	
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture info(HttpRequestInfo req)
	{
		ServerInfo info = new ServerInfo();
		info.hostname = ServervilleMain.Hostname;
		info.uptime_seconds = ServervilleMain.getUptime() / 1000.0;
		info.java_version = System.getProperty("java.version")+" ("+System.getProperty("java.vendor")+")";
		info.os = System.getProperty("os.name")+" "+System.getProperty("os.version");
		
		return HttpHelpers.sendJson(req, info);
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
	public static ChannelFuture logfiles(HttpRequestInfo req)
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
		
		
		return HttpHelpers.sendJson(req, result);
	}
	

	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture searchLogs(HttpRequestInfo req)
	{
		String query = req.getOneQuery("q", "*:*");

		long from = (long)req.getOneQueryAsDouble("from", 0.0);
		long to = (long)req.getOneQueryAsDouble("to", 0.0);
	
		try
		{
			LogSearchHits hits = ServervilleMain.LogSearcher.query(query, from, to);
			return HttpHelpers.sendJson(req, hits);
		}
		catch(ParseException e)
		{
			return HttpHelpers.sendError(req, ApiErrors.INVALID_QUERY, e.getMessage());
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
		public int tests_failed;
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
		results.tests_failed = 0;
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
				
				results.tests_failed += 1;
			}
			
			
			results.results.add(testResult);
		}
		
		return results;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture startSelftest(HttpRequestInfo req)
	{
		SelfTest.start(false);
		
		SelfTestStarted result = new SelfTestStarted();
		result.started_at = SelfTest.getStartTime();
		
		return HttpHelpers.sendJson(req, result);
	}
	

	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture selftest(HttpRequestInfo req)
	{
		int startAt = req.getOneQueryAsInt("first", 0);
		
		SelfTestStatus status = getTestStatus(startAt);
		return HttpHelpers.sendJson(req, status);
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
	
	private static UserInfo getUserInfo(ServervilleUser user)
	{
		UserInfo info = new UserInfo();
		
		info.id = user.getId();
		info.email = user.getEmail();
		info.username = user.getUsername();
		info.created = user.Created.getTime();
		info.modified = user.Modified.getTime();
		info.admin_level = user.AdminLevel;
		
		return info;
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture user(HttpRequestInfo req) throws SQLException
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
			return HttpHelpers.sendError(req, ApiErrors.MISSING_INPUT, "Must query on one of id, email or username");
		
		if(user == null)
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		
		UserInfo info = getUserInfo(user);
		
		return HttpHelpers.sendJson(req, info);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture createUser(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String username = req.getOneBody("username");
		String password = req.getOneBody("password");
		String email = req.getOneBody("email");
		
		String adminLevelStr = req.getOneBody("admin_level", "user");
		int adminLevel = ServervilleUser.parseAdminLevel(adminLevelStr);
		if(adminLevel < 0)
			return HttpHelpers.sendError(req, ApiErrors.INVALID_INPUT, "admin_level is not a valid value");

		if(!PasswordUtil.validatePassword(password))
			return HttpHelpers.sendError(req, ApiErrors.INVALID_INPUT, "password is not a valid password");
		
		ServervilleUser newuser = ServervilleUser.create(password, username, email, adminLevel);
		
		UserInfo info = getUserInfo(newuser);
		
		return HttpHelpers.sendJson(req, info);
	}
	

	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture deleteUser(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String userId = req.getOneBody("user_id");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		if(user.getId().equals(req.getUser().getId()))
		{
			return HttpHelpers.sendError(req, ApiErrors.INVALID_INPUT, "Don't delete yourself, silly!");
		}
		
		user.delete();
		
		return HttpHelpers.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserPassword(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String userId = req.getOneBody("user_id");
		String password = req.getOneBody("password");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		user.setPassword(password);
		
		return HttpHelpers.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserUsername(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String userId = req.getOneBody("user_id");
		String username = req.getOneBody("username");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		user.setUsername(username);
		
		return HttpHelpers.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserEmail(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String userId = req.getOneBody("user_id");
		String email = req.getOneBody("email");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		user.setEmail(email);
		
		return HttpHelpers.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setUserAdminLevel(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String userId = req.getOneBody("user_id");
		String adminLevelStr = req.getOneBody("admin_level");
		
		ServervilleUser user = ServervilleUser.findById(userId);
		if(user == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		int adminLevel = ServervilleUser.parseAdminLevel(adminLevelStr);
		if(adminLevel < 0)
			return HttpHelpers.sendError(req, ApiErrors.INVALID_INPUT, "invalid admin_level");
		
		user.AdminLevel = adminLevel;
		user.update();
		
		return HttpHelpers.sendSuccess(req);
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
	public static ChannelFuture agentKeys(HttpRequestInfo req) throws SQLException
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
		
		return HttpHelpers.sendJson(req, result);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture createAgentKey(HttpRequestInfo req) throws SQLException
	{
		String comment = req.getOneBody("comment", null);
		String iprange = req.getOneBody("iprange", null);
		long expirationLong = req.getOneBodyAsLong("expiration", 0);
		
		Date expiration = null;
		if(expirationLong > 0)
			expiration = new Date(expirationLong);
		
		if(iprange != null)
		{
			try
			{
				new SubnetMask(iprange);
			}
			catch(Exception e)
			{
				return HttpHelpers.sendError(req, ApiErrors.INVALID_IP_RANGE, iprange);
			}
		}
		
		AgentKey key = AgentKeyManager.createAgentKey(comment, iprange, expiration);
		
		AgentKeyInfo reply = new AgentKeyInfo();
		
		reply.key = key.Key;
		reply.comment = key.Comment;
		reply.iprange = key.IPRange;
		if(key.Expiration != null)
			reply.expiration = key.Expiration.getTime();
		else
			reply.expiration = 0;
		
		return HttpHelpers.sendJson(req, reply);
	}
	
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture editAgentKey(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String key = req.getOneBody("key");
		
		if(key == null || key.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a key");
		
		String comment = req.getOneBody("comment", null);
		String iprange = req.getOneBody("iprange", null);
		String expirationStr = req.getOneBody("expiration", null);
		
		AgentKey editKey = AgentKey.load(key);
		if(editKey == null)
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		
		if(comment != null)
			editKey.Comment = comment;
		if(iprange != null)
		{
			try
			{
				new SubnetMask(iprange);
			}
			catch(Exception e)
			{
				return HttpHelpers.sendError(req, ApiErrors.INVALID_IP_RANGE, iprange);
			}
			editKey.IPRange = iprange;
		}
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
		
		return HttpHelpers.sendJson(req, reply);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture deleteAgentKey(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String key = req.getOneBody("key");
		
		if(key == null || key.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a key");
		
		AgentKey editKey = AgentKey.load(key);
		if(editKey == null)
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		
		editKey.delete();
		
		return HttpHelpers.sendSuccess(req);
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
	public static ChannelFuture scriptInfo(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneQuery("id");

		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a script id");
		
		ScriptData script = ScriptData.findById(id);
		if(script == null)
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		
		ScriptDataInfo info = new ScriptDataInfo();
		
		info.id = script.Id;
		info.source = script.ScriptSource;
		info.created = script.Created.getTime();
		info.modified = script.Modified.getTime();
		
		return HttpHelpers.sendJson(req, info);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture script(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneQuery("id");

		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a script id");
		
		ScriptData script = ScriptData.findById(id);
		if(script == null)
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		
		return HttpHelpers.sendText(req, script.ScriptSource, "application/javascript");
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture scripts(HttpRequestInfo req) throws SQLException
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
		
		return HttpHelpers.sendJson(req, list);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture addScript(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneQuery("id");

		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a script id");
		
		String contentType = req.Request.headers().get(HttpHeaderNames.CONTENT_TYPE);
		MediaType contentMediaType = MediaType.parse(contentType);
		
		contentType = contentMediaType.type()+"/"+contentMediaType.subtype();
		
		if(contentType == null || !contentType.equals("application/javascript"))
			return HttpHelpers.sendError(req, ApiErrors.INVALID_CONTENT, "Script must be of type application/javascript");
		
		String scriptData = req.getBody();
		if(scriptData == null || scriptData.length() == 0)
			return HttpHelpers.sendError(req, ApiErrors.INVALID_CONTENT, "Must include a script body");
		
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
			return HttpHelpers.sendError(req, ApiErrors.JAVASCRIPT_ERROR, errorMessage);
		}
		
		if(found)
		{
			newScript.update();
		}
		else
		{
			newScript.create();
		}
		
		try {
			ScriptManager.scriptsUpdated();
		} catch (InterruptedException e) {
			String errorMessage = e.getMessage();
			return HttpHelpers.sendError(req, ApiErrors.UNKNOWN, errorMessage);
		} catch (ScriptException e) {
			String errorMessage = e.getCause().getMessage()+" at line "+e.getLineNumber();
			return HttpHelpers.sendError(req, ApiErrors.JAVASCRIPT_ERROR, errorMessage);
		}
		
		return HttpHelpers.sendSuccess(req);
	}

	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture deleteScript(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneBody("id");

		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a script id");

		List<ScriptData> scripts = ScriptData.loadAll();

		ScriptData script = null;

		for(int s=0; s<scripts.size(); s++)
		{
			ScriptData iter = scripts.get(s);
			
			if(iter.Id.equals(id))
			{
				scripts.remove(s);
				script = iter;
				break;
			}
		}
		
		if(script == null)
		{
			return HttpHelpers.sendError(req, ApiErrors.NOT_FOUND);
		}
		
		try {
			ScriptEngineContext testContext = new ScriptEngineContext();
			testContext.init(scripts);
		} catch (ScriptLoadException e) {
			String errorMessage = e.getCause().getMessage();
			errorMessage = errorMessage.replaceAll("\\<eval\\>", e.ScriptId);
			return HttpHelpers.sendError(req, ApiErrors.JAVASCRIPT_ERROR, errorMessage);
		}
		
		script.delete();
		
		try {
			ScriptManager.scriptsUpdated();
		} catch (InterruptedException e) {
			String errorMessage = e.getMessage();
			return HttpHelpers.sendError(req, ApiErrors.UNKNOWN, errorMessage);
		} catch (ScriptException e) {
			String errorMessage = e.getCause().getMessage()+" at line "+e.getLineNumber();
			return HttpHelpers.sendError(req, ApiErrors.JAVASCRIPT_ERROR, errorMessage);
		}
		
		return HttpHelpers.sendSuccess(req);
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.GET)
	public static ChannelFuture dataKey(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneQuery("id");
		String key = req.getOneQuery("key");
		
		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a data id");
		if(key == null || key.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a data key name");
		
		KeyDataItem item = KeyDataManager.loadKey(id, key);
		if(item == null || item.isDeleted())
			throw new JsonApiException(ApiErrors.NOT_FOUND);
		
		Object value;
		try {
			value = item.asDecodedObject();
		} catch (IOException e) {
			l.error("Exception decoding json value", e);
			value = "<error>";
		}
		String replyVal = value.toString();
		
		return HttpHelpers.sendText(req, replyVal, "text/plain");
	}
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST)
	public static ChannelFuture setDataKey(HttpRequestInfo req) throws SQLException, JsonApiException
	{
		String id = req.getOneQuery("id");
		String key = req.getOneQuery("key");
		String dataType = req.getOneQuery("data_type", null);

		if(id == null || id.length() == 0)
			throw new JsonApiException(ApiErrors.MISSING_INPUT, "Must supply a data id");
		if(!KeyDataItem.isValidKeyname(key))
			throw new JsonApiException(ApiErrors.INVALID_KEY_NAME, key);

		
		String value = req.getBody();
		if(value == null || value.length() == 0)
			return HttpHelpers.sendError(req, ApiErrors.INVALID_CONTENT, "Can't set an empty value this way");
		
		KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(key, JsonDataType.fromString(dataType), value);
		KeyDataManager.saveKey(id, item);
		
		return HttpHelpers.sendSuccess(req);
	}
}
