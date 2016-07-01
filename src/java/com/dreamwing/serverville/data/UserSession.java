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

@DatabaseTable(tableName = "user_session")
public class UserSession implements HttpSession
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String Id;
	
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="started", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Started;
	
	@DatabaseField(columnName="lastactive", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date LastActive;
	
	@DatabaseField(columnName="expired", canBeNull=false)
	public boolean Expired;
	
	@DatabaseField(columnName="connected", canBeNull=false)
	public boolean Connected;
	
	
	@DatabaseTable(tableName = "user_session_userid")
	public static class UserSessionLookup
	{
		@DatabaseField(columnName="userid", canBeNull=false)
		private String UserId;
		
		@DatabaseField(columnName="sessionid", canBeNull=false)
		public String SessionId;
	}
	
	@Override
	public String getId()
	{
		return Id;
	}
	
	public static UserSession startNewSession(String userId) throws SQLException
	{
		/*AdminUserSession oldSession = findByUserId(userId);
		if(oldSession != null)
		{
			oldSession.delete();
		}*/
		
		UserSession session = new UserSession();
		session.Id = PasswordUtil.makeRandomString(8)+"/"+SVID.makeSVID();
		session.UserId = userId;
		session.Started = new Date();
		session.LastActive = session.Started;
		session.Expired = false;
		
		session.create();
		return session;
	}
	
	public static UserSession findById(String id) throws SQLException
	{
		return DatabaseManager.ServervilleUser_UserSessionDao.queryForId(id);
	}
	
	
	public static List<UserSessionLookup> findAllLookupsByUserId(String userId) throws SQLException
	{
		return DatabaseManager.ServervilleUser_UserSession_UserIdDao.queryForEq("userid", userId);
	}
	
	public void deleteLookupByUserIdAndId(String userId, String sessionId) throws SQLException
	{
		@SuppressWarnings("unchecked")
		PreparedDelete<UserSessionLookup> deleteQuery = (PreparedDelete<UserSessionLookup>)
				DatabaseManager.ServervilleUser_UserSession_UserIdDao.deleteBuilder().where().eq("userid", userId).and().eq("sessionid", sessionId).prepare();
		
		DatabaseManager.ServervilleUser_UserSession_UserIdDao.delete(deleteQuery);
	}

	public void create() throws SQLException
	{
		DatabaseManager.ServervilleUser_UserSessionDao.create(this);
		
		UserSessionLookup lookup = new UserSessionLookup();
		lookup.UserId = UserId;
		lookup.SessionId = Id;
		
		DatabaseManager.ServervilleUser_UserSession_UserIdDao.create(lookup);
	}
	
	public void refresh() throws SQLException
	{
		DatabaseManager.ServervilleUser_UserSessionDao.refresh(this);
	}
	
	public void update() throws JsonApiException, SQLException
	{
		if(DatabaseManager.ServervilleUser_UserSessionDao.update(this) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
	}
	
	public void delete(boolean checkForAnonymousAccount) throws SQLException
	{
		DatabaseManager.ServervilleUser_UserSessionDao.deleteById(Id);
		deleteLookupByUserIdAndId(UserId, Id);
		
		if(checkForAnonymousAccount)
		{
			// Did we delete the last session of an anonymous user? Delete the user also, no way to get back into it anyway
			List<UserSessionLookup> remainingSessions = findAllLookupsByUserId(UserId);
			if(remainingSessions.isEmpty())
			{
				ServervilleUser user = ServervilleUser.findById(UserId);
				if(user != null)
				{
					if(user.isAnonymous())
						user.delete();
				}
			}
			
		}
	}
	
	public static void deleteSessionsInactiveSince(long since) throws SQLException
	{
		List<UserSession> oldSessions = DatabaseManager.ServervilleUser_UserSessionDao.queryBuilder().where().lt("lastactive", since).query();
		
		for(UserSession oldSession : oldSessions)
		{
			oldSession.delete(true);
		}
	}
}

