package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.util.PasswordUtil;
import com.dreamwing.serverville.util.SVID;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "adminsession")
public class AdminUserSession {
	
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	private String Id;
	
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="started", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Started;
	
	@DatabaseField(columnName="lastactive", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date LastActive;
	
	@DatabaseTable(tableName = "adminsession_userid")
	public static class UserIdLookup
	{
		@DatabaseField(columnName="userid", canBeNull=false)
		public String UserId;
		
		@DatabaseField(columnName="id", canBeNull=false)
		public String Id;
	}
	
	public AdminUserSession() {}
	
	public String getId() { return Id; }
	
	public static AdminUserSession startNewSession(String userId) throws SQLException
	{
		/*AdminUserSession oldSession = findByUserId(userId);
		if(oldSession != null)
		{
			oldSession.delete();
		}*/
		
		AdminUserSession session = new AdminUserSession();
		session.Id = PasswordUtil.makeRandomString(8)+"/"+SVID.makeSVID();
		session.UserId = userId;
		session.Started = new Date();
		session.LastActive = session.Started;
		
		session.create();
		return session;
	}
	
	public static AdminUserSession findById(String id) throws SQLException
	{
		return DatabaseManager.AdminUserSessionDao.queryForId(id);
	}
	
	
	public static List<UserIdLookup> findAllLookupsByUserId(String userId) throws SQLException
	{
		return DatabaseManager.AdminUserSession_UserIdDao.queryForEq("userid", userId);
	}
	
	public void deleteLookupByUserIdAndId(String userId, String sessionId) throws SQLException
	{
		@SuppressWarnings("unchecked")
		PreparedDelete<UserIdLookup> deleteQuery = (PreparedDelete<UserIdLookup>)
				DatabaseManager.AdminUserSession_UserIdDao.deleteBuilder().where().eq("userid", userId).and().eq("id", sessionId).prepare();
		
		DatabaseManager.AdminUserSession_UserIdDao.delete(deleteQuery);
	}

	public void create() throws SQLException
	{
		DatabaseManager.AdminUserSessionDao.create(this);
		
		UserIdLookup lookup = new UserIdLookup();
		lookup.UserId = UserId;
		lookup.Id = Id;
		
		DatabaseManager.AdminUserSession_UserIdDao.createOrUpdate(lookup);
	}
	
	public void delete() throws SQLException
	{
		DatabaseManager.AdminUserSessionDao.deleteById(Id);
		deleteLookupByUserIdAndId(UserId, Id);
	}
}
