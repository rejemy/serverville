package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.data.AdminUserSession.AdminUserSessionLookup;
import com.dreamwing.serverville.data.UserSession.UserSessionLookup;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.CurrencyUtil;
import com.dreamwing.serverville.util.SVID;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "user")
public class ServervilleUser {
	
	private static final Logger l = LogManager.getLogger(ServervilleUser.class);
	
	public static final int AdminLevel_User = 0b0000;
	public static final int AdminLevel_AgentReadOnly = 0b0001;
	public static final int AdminLevel_Agent = 0b0011;
	public static final int AdminLevel_AdminReadOnly = 0b0101;
	public static final int AdminLevel_Admin = 0b1111;
	
	public static final int Agent_ReadPriv =  0b0001;
	public static final int Agent_WritePriv = 0b0010;
	public static final int Admin_ReadPriv =  0b0100;
	public static final int Admin_WritePriv = 0b1000;
	
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	private String Id;
	
	@DatabaseField(columnName="sessionid")
	private String SessionId;
	
	@DatabaseField(columnName="username")
	private String Username;
	
	@DatabaseField(columnName="email")
	private String Email;
	
	@DatabaseField(columnName="passwdhash")
	public String PasswordHash;
	
	@DatabaseField(columnName="lang")
	public String Language;
	
	@DatabaseField(columnName="country")
	public String Country;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	@DatabaseField(columnName="admin", canBeNull=false, defaultValue="0")
	public int AdminLevel;
	
	@DatabaseTable(tableName = "user_username")
	public static class UsernameLookup
	{
		@DatabaseField(columnName="username", id=true, canBeNull=false)
		public String Username;
		
		@DatabaseField(columnName="id", canBeNull=false)
		private String Id;
		
		@DatabaseField(columnName="hold")
		private Boolean Hold;
	}
	
	@DatabaseTable(tableName = "user_email")
	public static class EmailLookup
	{
		@DatabaseField(columnName="email", id=true, canBeNull=false)
		public String Email;
		
		@DatabaseField(columnName="id", canBeNull=false)
		private String Id;
		
		@DatabaseField(columnName="hold")
		private Boolean Hold;
	}
	
	public ServervilleUser() {}	
	
	public String getId() { return Id; }
	public String getUsername() { return Username; }
	public String getSessionId() { return SessionId; }
	public String getEmail() { return Email; }
	
	public boolean isAnonymous()
	{
		return PasswordHash == null && Email == null && Username == null;
	}
	
	public static ServervilleUser create(String password, String username, String email, int adminLevel, String language, String country) throws SQLException, JsonApiException
	{
		if(country != null && !CurrencyUtil.isValidCountry(country))
			throw new JsonApiException(ApiErrors.INVALID_COUNTRY_CODE);
		
		ServervilleUser user = new ServervilleUser();
		user.Id = SVID.makeSVID();
		user.Username = username;
		user.Email = email;
		user.Language = language;
		user.Country = country;
		user.Created = new Date();
		user.Modified = user.Created;
		user.AdminLevel = adminLevel;
		
		if(password != null)
			user.setPasswordHash(password);
		
		UsernameLookup usernamelookup = null;
		EmailLookup emailLookup = null;
		
		try
		{	
			// Ok, some complex shit here. First we try to hold the desired username and email
			if(username != null)
			{
				usernamelookup = new UsernameLookup();
				usernamelookup.Username = username;
				usernamelookup.Id = user.Id;
				usernamelookup.Hold = true;
				DatabaseManager.ServervilleUser_UsernameDao.create(usernamelookup);
			}
			
			if(email != null)
			{
				emailLookup = new EmailLookup();
				emailLookup.Email = email;
				emailLookup.Id = user.Id;
				emailLookup.Hold = true;
				DatabaseManager.ServervilleUser_EmailDao.create(emailLookup);
			}
			
			// Ok, we held the desired username and email, now create the account object
			DatabaseManager.ServervilleUserDao.create(user);
			
			// Sweet, ok, that worked. Now convert he held email and username into regular lookups
			
			if(usernamelookup != null)
			{
				try
				{
					usernamelookup.Hold = null;
					DatabaseManager.ServervilleUser_UsernameDao.update(usernamelookup);
					usernamelookup = null;
				}
				catch(SQLException e)
				{
					l.error("Error converting username hold to username lookup, created a ghost account: "+username, e);
				}
			}
			
			if(emailLookup != null)
			{
				try
				{
					emailLookup.Hold = null;
					DatabaseManager.ServervilleUser_EmailDao.update(emailLookup);
					emailLookup = null;
				}
				catch(SQLException e)
				{
					l.error("Error converting email hold to email lookup, created a ghost account: "+email, e);
				}
			}
			
			// Create KeyData record
			KeyDataRecord record = new KeyDataRecord();
			record.Id = user.Id;
			record.Type = "user";
			record.Owner = user.Id;
			record.Parent = null;
			record.Version = 0;
			record.Created = new Date();
			record.Modified = record.Created;
			
			DatabaseManager.KeyDataRecordDao.createOrUpdate(record);
			
		}
		finally
		{
			
			// Clean up held lookups if something goes wrong during creation
			if(usernamelookup != null)
			{
				try
				{
					DatabaseManager.ServervilleUser_UsernameDao.deleteById(username);
				}
				catch(SQLException e)
				{
					l.error("Couldn't clean up held username: "+username, e);
				}
			}
			
			if(emailLookup != null)
			{
				try
				{
					DatabaseManager.ServervilleUser_EmailDao.deleteById(email);
				}
				catch(SQLException e)
				{
					l.error("Couldn't clean up held email: "+email, e);
				}
			}
		}
		
		return user;
	}
	
	public void register(String password, String username, String email) throws SQLException, JsonApiException
	{
		if(Username != null || Email != null)
			throw new JsonApiException(ApiErrors.ALREADY_REGISTERED);
		
		Username = username;
		Email = email;

		setPassword(password);
		
		UsernameLookup usernamelookup = null;
		EmailLookup emailLookup = null;
		
		try
		{	
			// Ok, some complex shit here. First we try to hold the desired username and email
			if(username != null)
			{
				usernamelookup = new UsernameLookup();
				usernamelookup.Username = username;
				usernamelookup.Id = Id;
				usernamelookup.Hold = true;
				DatabaseManager.ServervilleUser_UsernameDao.create(usernamelookup);
			}
			
			if(email != null)
			{
				emailLookup = new EmailLookup();
				emailLookup.Email = email;
				emailLookup.Id = Id;
				emailLookup.Hold = true;
				DatabaseManager.ServervilleUser_EmailDao.create(emailLookup);
			}
			
			update();
			
			// Sweet, ok, that worked. Now convert he held email and username into regular lookups
			
			
			if(usernamelookup != null)
			{
				try
				{
					usernamelookup.Hold = null;
					DatabaseManager.ServervilleUser_UsernameDao.update(usernamelookup);
					usernamelookup = null;
				}
				catch(SQLException e)
				{
					l.error("Error converting username hold to username lookup: "+username, e);
				}
			}
			
			if(emailLookup != null)
			{
				try
				{
					emailLookup.Hold = null;
					DatabaseManager.ServervilleUser_EmailDao.update(emailLookup);
					emailLookup = null;
				}
				catch(SQLException e)
				{
					l.error("Error converting email hold to email lookup: "+email, e);
				}
			}
		}
		finally
		{
			
			// Clean up held lookups if something goes wrong during creation
			if(usernamelookup != null)
			{
				try
				{
					DatabaseManager.ServervilleUser_UsernameDao.deleteById(username);
				}
				catch(SQLException e)
				{
					l.error("Couldn't clean up held username: "+username, e);
				}
			}
			
			if(emailLookup != null)
			{
				try
				{
					DatabaseManager.ServervilleUser_EmailDao.deleteById(email);
				}
				catch(SQLException e)
				{
					l.error("Couldn't clean up held email: "+email, e);
				}
			}
		}
	}
	
	
	public static ServervilleUser findById(String id) throws SQLException
	{
		return DatabaseManager.ServervilleUserDao.queryForId(id);
	}
	
	public static ServervilleUser findBySessionId(String sessionId) throws SQLException
	{
		UserSession lookup = DatabaseManager.ServervilleUser_UserSessionDao.queryForId(sessionId);
		if(lookup == null)
			return null;
		
		return DatabaseManager.ServervilleUserDao.queryForId(lookup.Id);
	}
	
	public static ServervilleUser findByUsername(String username) throws SQLException
	{
		UsernameLookup lookup = DatabaseManager.ServervilleUser_UsernameDao.queryBuilder().where().eq("username", username).and().isNull("hold").queryForFirst();
		if(lookup == null)
			return null;
		
		return DatabaseManager.ServervilleUserDao.queryForId(lookup.Id);
	}
	
	public static ServervilleUser findByEmail(String email) throws SQLException
	{
		EmailLookup lookup = DatabaseManager.ServervilleUser_EmailDao.queryBuilder().where().eq("email", email).and().isNull("hold").queryForFirst();
		if(lookup == null)
			return null;
		
		return DatabaseManager.ServervilleUserDao.queryForId(lookup.Id);
	}
	
	public void update() throws JsonApiException, SQLException
	{
		if(DatabaseManager.ServervilleUserDao.update(this) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
	}
	
	
	public void setSessionId(String sessionId) throws SQLException, JsonApiException
	{
		if(Objects.equals(SessionId, sessionId))
			return;
		
		SessionId = sessionId;
		update();
	}
	
	
	public void setUsername(String username) throws SQLException, JsonApiException
	{
		if(Objects.equals(Username, username))
			return;
		
		String oldUsername = Username;
		Username = username;
		
		if(Username != null)
		{
			UsernameLookup lookup = new UsernameLookup();
			lookup.Username = Username;
			lookup.Id = Id;
			DatabaseManager.ServervilleUser_UsernameDao.create(lookup);
		}
		
		if(oldUsername != null)
		{
			DatabaseManager.ServervilleUser_UsernameDao.deleteById(oldUsername);
		}
		
		update();
	}
	

	public void setEmail(String email) throws SQLException, JsonApiException
	{
		if(Objects.equals(Email, email))
			return;
		
		String oldEmail = Email;
		Email = email;
		
		if(Email != null)
		{
			EmailLookup lookup = new EmailLookup();
			lookup.Email = Email;
			lookup.Id = Id;
			DatabaseManager.ServervilleUser_EmailDao.create(lookup);
		}
		
		if(oldEmail != null)
		{
			DatabaseManager.ServervilleUser_EmailDao.deleteById(oldEmail);
		}
		
		update();
	}
	
	private void setPasswordHash(String cleartext)
	{
		PasswordHash = BCrypt.hashpw(cleartext, BCrypt.gensalt(12));
	}
	
	public void setPassword(String cleartext) throws JsonApiException, SQLException
	{
		setPasswordHash(cleartext);
		
		update();
	}
	
	public boolean checkPassword(String cleartext)
	{
		if(cleartext == null || PasswordHash == null)
			return false;
		return BCrypt.checkpw(cleartext, PasswordHash);
	}
	
	public void setLocale(String country, String language) throws JsonApiException, SQLException
	{
		if(country != null && !CurrencyUtil.isValidCountry(country))
			throw new JsonApiException(ApiErrors.INVALID_COUNTRY_CODE);
		
		if(country != null)
			Country = country;
		if(language != null)
			Language = language;
		
		update();
	}
	
	public void delete() throws SQLException
	{
		
		if(Username != null)
		{
			DatabaseManager.ServervilleUser_UsernameDao.deleteById(Username);
		}
		if(Email != null)
		{
			DatabaseManager.ServervilleUser_EmailDao.deleteById(Email);
		}
		
		List<AdminUserSessionLookup> adminSessions = AdminUserSession.findAllLookupsByUserId(getId());
		if(adminSessions != null)
		{
			for(AdminUserSessionLookup lookup : adminSessions)
			{
				AdminUserSession adminSession = AdminUserSession.findById(lookup.SessionId);
				if(adminSession != null)
					adminSession.delete();
			}
		}
		
		List<UserSessionLookup> userSessions = UserSession.findAllLookupsByUserId(getId());
		if(userSessions != null)
		{
			for(UserSessionLookup lookup : userSessions)
			{
				UserSession userSession = UserSession.findById(lookup.SessionId);
				if(userSession != null)
					userSession.delete(false);
			}
		}
		
		DatabaseManager.ServervilleUserDao.delete(this);
		
		KeyDataManager.deleteAllKeys(getId());
		
		CurrencyRecord.deleteAllForUser(getId());
	}
	
	public static int parseAdminLevel(String adminLevelStr)
	{
		switch(adminLevelStr)
		{
		case "user":
			return ServervilleUser.AdminLevel_User;
		case "readOnlyAgent":
			return ServervilleUser.AdminLevel_AgentReadOnly;
		case "agent":
			return ServervilleUser.AdminLevel_Agent;
		case "readOnlyAdmin":
			return ServervilleUser.AdminLevel_AdminReadOnly;
		case "admin":
			return ServervilleUser.AdminLevel_Admin;
		default:
			return -1;
		}
	}
	
	public static String adminLevelToString(int adminLevel)
	{
		switch(adminLevel)
		{
			case ServervilleUser.AdminLevel_User:
				return "user";
			case ServervilleUser.AdminLevel_AgentReadOnly:
				return "readOnlyAgent";
			case ServervilleUser.AdminLevel_Agent:
				return "agent";
			case ServervilleUser.AdminLevel_AdminReadOnly:
				return "readOnlyAdmin";
			case ServervilleUser.AdminLevel_Admin:
				return "admin";
			default:
				return "unknown";
		}
	}
	
	public boolean equals(ServervilleUser user)
	{
		if(user == null)
			return false;
		return this.Id.equals(user.Id);
	}
	
	public String getLanguage()
	{
		if(Language != null)
			return Language;
		return ServervilleMain.ServerProperties.getProperty("default_language");
	}
	
	public Locale getLocale()
	{
		Locale.Builder builder = new Locale.Builder().setLanguageTag(getLanguage());
		if(Country != null)
			builder.setRegion(Country);
		return builder.build();
	}
}
