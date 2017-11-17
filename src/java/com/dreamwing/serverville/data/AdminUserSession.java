package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpSession;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.PasswordUtil;
import com.dreamwing.serverville.util.SVID;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "adminsession")
public class AdminUserSession implements HttpSession
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	private String Id;
	
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="started", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Started;
	
	@DatabaseField(columnName="lastactive", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date LastActive;
	
	@DatabaseField(columnName="expired", canBeNull=false)
	public boolean Expired;
	
	@DatabaseField(columnName="connected", canBeNull=false)
	public boolean Connected;
	
	@DatabaseTable(tableName = "adminsession_userid")
	public static class AdminUserSessionLookup
	{
		@DatabaseField(columnName="userid", canBeNull=false)
		public String UserId;
		
		@DatabaseField(columnName="sessionid", canBeNull=false)
		public String SessionId;
	}
	
	public AdminUserSession() {}
	
	public String getId() { return Id; }
	
	public static AdminUserSession startNewSession(String userId) throws SQLException
	{
		AdminUserSession session = new AdminUserSession();
		session.Id = PasswordUtil.makeRandomString(8)+"/"+SVID.makeSVID();
		session.UserId = userId;
		session.Started = new Date();
		session.LastActive = session.Started;
		session.Expired = false;
		
		session.create();
		return session;
	}
	
	public static AdminUserSession findById(String id) throws SQLException
	{
		return DatabaseManager.AdminUserSessionDao.queryForId(id);
	}
	
	
	public static List<AdminUserSessionLookup> findAllLookupsByUserId(String userId) throws SQLException
	{
		return DatabaseManager.AdminUserSession_UserIdDao.queryForEq("userid", userId);
	}
	
	public void deleteLookupByUserIdAndId(String userId, String sessionId) throws SQLException
	{
		@SuppressWarnings("unchecked")
		PreparedDelete<AdminUserSessionLookup> deleteQuery = (PreparedDelete<AdminUserSessionLookup>)
				DatabaseManager.AdminUserSession_UserIdDao.deleteBuilder().where().eq("userid", userId).and().eq("sessionid", sessionId).prepare();
		
		DatabaseManager.AdminUserSession_UserIdDao.delete(deleteQuery);
	}

	public void create() throws SQLException
	{
		DatabaseManager.AdminUserSessionDao.create(this);
		
		AdminUserSessionLookup lookup = new AdminUserSessionLookup();
		lookup.UserId = UserId;
		lookup.SessionId = Id;
		
		DatabaseManager.AdminUserSession_UserIdDao.create(lookup);
	}
	
	public void update() throws JsonApiException, SQLException
	{
		if(DatabaseManager.AdminUserSessionDao.update(this) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
	}
	
	public void refresh() throws SQLException
	{
		DatabaseManager.AdminUserSessionDao.refresh(this);
	}
	
	public void delete() throws SQLException
	{
		DatabaseManager.AdminUserSessionDao.deleteById(Id);
		deleteLookupByUserIdAndId(UserId, Id);
	}
	
	public static void deleteSessionsInactiveSince(long since) throws SQLException
	{
		List<AdminUserSession> oldSessions = DatabaseManager.AdminUserSessionDao.queryBuilder().where().lt("lastactive", since).query();
		
		for(AdminUserSession oldSession : oldSessions)
		{
			oldSession.delete();
		}
	}
}
