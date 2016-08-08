package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DatabaseField;

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
	public int Rate;
	
	@DatabaseField(columnName="history", canBeNull=false)
	public boolean KeepHistory;
	
	public static List<CurrencyInfo> loadCurrencies() throws SQLException
	{
		return DatabaseManager.CurrencyInfoDao.queryForAll();
	}
}
