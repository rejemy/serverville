package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "admin_log")
public class AdminActionLog
{
	@DatabaseField(columnName="requestid", id=true, canBeNull=false)
	public String RequestId;
	
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="connectionid", canBeNull=false)
	public String ConnectionId;
	
	@DatabaseField(columnName="api", canBeNull=false)
	public String API;
	
	@DatabaseField(columnName="request")
	public String Request;
	
	public void create() throws SQLException
	{
		DatabaseManager.AdminActionLogDao.create(this);
	}
	
	public static List<AdminActionLog> getAll() throws SQLException
	{
		return DatabaseManager.AdminActionLogDao.queryForAll();
	}
	
	public static List<AdminActionLog> getAllBetween(Date from, Date to) throws SQLException
	{
		QueryBuilder<AdminActionLog, String> queryBuilder = DatabaseManager.AdminActionLogDao.queryBuilder();
		
		return queryBuilder.where().gt("created", from.getTime()).and().lt("created", to.getTime()).query();
	}
	
	public static List<AdminActionLog> getForUser(String userId) throws SQLException
	{
		QueryBuilder<AdminActionLog, String> queryBuilder = DatabaseManager.AdminActionLogDao.queryBuilder();
		
		return queryBuilder.where().eq("userid", userId).query();
	}
	
	public static List<AdminActionLog> getForUserBetween(String userId, Date from, Date to) throws SQLException
	{
		QueryBuilder<AdminActionLog, String> queryBuilder = DatabaseManager.AdminActionLogDao.queryBuilder();
		
		return queryBuilder
				.where().gt("created", from.getTime()).and().lt("created", to.getTime())
				.and().eq("userid", userId).query();
	}
}
