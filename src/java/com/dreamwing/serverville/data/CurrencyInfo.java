package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "currency_info")
public class CurrencyInfo
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String CurrencyId;
	
	@DatabaseField(columnName="starting", canBeNull=false)
	public int Starting;
	
	@DatabaseField(columnName="min", canBeNull=true)
	public Integer Min;
	
	@DatabaseField(columnName="max", canBeNull=true)
	public Integer Max;
	
	@DatabaseField(columnName="rate", canBeNull=false)
	public double Rate;
	
	@DatabaseField(columnName="history", canBeNull=false)
	public boolean KeepHistory;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	private static final Predicate<String> ValidCurrencyIdRegex = Pattern.compile("^[a-zA-Z_][0-9a-zA-Z_\\$]*$").asPredicate();
	
	public static boolean isValidCurrencyId(String id)
	{
		if(id == null)
			return false;
		return ValidCurrencyIdRegex.test(id);
	}
	
	public static List<CurrencyInfo> loadCurrencies() throws SQLException
	{
		return DatabaseManager.CurrencyInfoDao.queryForAll();
	}
	
	public static CurrencyInfo loadCurrency(String id) throws SQLException
	{
		return DatabaseManager.CurrencyInfoDao.queryForId(id);
	}
	
	public void save() throws SQLException
	{
		DatabaseManager.CurrencyInfoDao.createOrUpdate(this);
	}
	
	public static void deleteById(String id) throws SQLException
	{
		DatabaseManager.CurrencyInfoDao.deleteById(id);
	}
}
