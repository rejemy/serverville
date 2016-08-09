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
import com.j256.ormlite.stmt.PreparedUpdate;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "currency")
public class CurrencyRecord
{
	@DatabaseField(columnName="userid", uniqueCombo=true, canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="currency", uniqueCombo=true, canBeNull=false)
	public String CurrencyId;
	
	@DatabaseField(columnName="balance", canBeNull=false)
	public int Balance;
	
	@DatabaseField(columnName="remainder", canBeNull=false)
	public double Remainder;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	public static CurrencyRecord load(String userId, String currencyId) throws SQLException
	{
		return DatabaseManager.CurrencyDao.queryBuilder().where()
				.eq("userid", userId).and().eq("currency", currencyId).queryForFirst();
	}
	
	public static List<CurrencyRecord> loadAllForUser(String userId) throws SQLException
	{
		return DatabaseManager.CurrencyDao.queryBuilder().where()
				.eq("userid", userId).query();
	}
	
	public void create() throws SQLException, JsonApiException
	{
		if(Modified == null)
			Modified = new Date();
		
		if(DatabaseManager.CurrencyDao.create(this) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
	}
	
	public void update() throws SQLException, JsonApiException
	{
		Date modified = new Date();
		UpdateBuilder<CurrencyRecord,Void> builder = DatabaseManager.CurrencyDao.updateBuilder();
		builder.updateColumnValue("balance", Balance);
		builder.updateColumnValue("remainder", Remainder);
		builder.updateColumnValue("modified", modified);
		
		@SuppressWarnings("unchecked")
		PreparedUpdate<CurrencyRecord> updateQuery = (PreparedUpdate<CurrencyRecord>)
			builder.where()
			.eq("userid", UserId).and().eq("currency", CurrencyId)
			.and().eq("modified", Modified).prepare();
		
		if(DatabaseManager.CurrencyDao.update(updateQuery) != 1)
			throw new JsonApiException(ApiErrors.CONCURRENT_MODIFICATION);
		
		Modified = modified;
	}
	
	public static void deleteAllForUser(String userId) throws SQLException
	{
		@SuppressWarnings("unchecked")
		PreparedDelete<CurrencyRecord> deleteQuery = (PreparedDelete<CurrencyRecord>)
				DatabaseManager.CurrencyDao.deleteBuilder().where().eq("userid", userId).prepare();
		
		DatabaseManager.CurrencyDao.delete(deleteQuery);
	}
	
}
