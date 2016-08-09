package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "currency_history")
public class CurrencyHistory
{
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="currency", canBeNull=false)
	public String CurrencyId;
	
	@DatabaseField(columnName="before", canBeNull=false)
	public int Before;
	
	@DatabaseField(columnName="delta", canBeNull=false)
	public int Delta;
	
	@DatabaseField(columnName="after", canBeNull=false)
	public int After;
	
	@DatabaseField(columnName="action", canBeNull=false)
	public String Action;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Modified;
	
	public static List<CurrencyHistory> loadAllForUser(String userId) throws SQLException
	{
		return DatabaseManager.CurrencyHistoryDao.queryBuilder().where()
				.eq("userid", userId).query();
	}
	
	public static List<CurrencyHistory> loadAllForUser(String userId, Date since) throws SQLException
	{
		return DatabaseManager.CurrencyHistoryDao.queryBuilder().where()
				.eq("userid", userId).and().gt("modified", since).query();
	}
	
	public static List<CurrencyHistory> loadAllForUser(String userId, Date from, Date to) throws SQLException
	{
		return DatabaseManager.CurrencyHistoryDao.queryBuilder().where()
				.eq("userid", userId).and().gt("modified", from).and().lt("modified", to).query();
	}
	
	public void create() throws SQLException, JsonApiException
	{
		if(DatabaseManager.CurrencyHistoryDao.create(this) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
	}
	
	
	public static void deleteAllForUser(String userId) throws SQLException
	{
		@SuppressWarnings("unchecked")
		PreparedDelete<CurrencyHistory> deleteQuery = (PreparedDelete<CurrencyHistory>)
				DatabaseManager.CurrencyHistoryDao.deleteBuilder().where().eq("userid", userId).prepare();
		
		DatabaseManager.CurrencyHistoryDao.delete(deleteQuery);
	}
}
