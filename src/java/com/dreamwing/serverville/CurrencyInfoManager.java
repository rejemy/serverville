package com.dreamwing.serverville;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.data.CurrencyHistory;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.CurrencyRecord;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;

public class CurrencyInfoManager
{
	private static ConcurrentMap<String,CurrencyInfo> CurrencyDb;
	
	public static void init() throws SQLException
	{
		reloadCurrencies();
	}
	
	public static List<CurrencyInfo> reloadCurrencies() throws SQLException
	{
		ConcurrentMap<String,CurrencyInfo> currencyDb = new ConcurrentHashMap<String,CurrencyInfo>();
		
		List<CurrencyInfo> currencies = CurrencyInfo.loadCurrencies();
		
		for(CurrencyInfo currency : currencies)
		{
			currencyDb.put(currency.CurrencyId, currency);
		}
		
		CurrencyDb = currencyDb;
		
		return currencies;
	}
	
	public static CurrencyInfo getCurrencyInfo(String id)
	{
		return CurrencyDb.get(id);
	}
	
	public static CurrencyInfo reloadCurrencyInfo(String id) throws SQLException
	{
		CurrencyInfo currency = CurrencyInfo.loadCurrency(id);
		if(currency == null)
			CurrencyDb.remove(id);
		else
			CurrencyDb.put(currency.CurrencyId, currency);
		
		return currency;
	}
	
	// TODO: send reload message to cluster
	public static void addCurrency(CurrencyInfo currency) throws SQLException
	{
		currency.save();
		CurrencyDb.put(currency.CurrencyId, currency);
	}
	
	// TODO: send reload message to cluster
	public static void removeCurrency(String id) throws SQLException
	{
		CurrencyDb.remove(id);
		CurrencyInfo.deleteById(id);
	}
	
	public static int changeCurrencyBalance(ServervilleUser user, CurrencyInfo currency, int delta, String reason) throws SQLException, JsonApiException
	{
		CurrencyRecord record = CurrencyRecord.load(user.getId(), currency.CurrencyId);
		if(record == null)
		{
			record = new CurrencyRecord();
			record.UserId = user.getId();
			record.CurrencyId = currency.CurrencyId;
			record.Balance = currency.Starting;
		}
		else
		{
			validateCurrencyBalance(user, currency, record, true);
		}
		
		if(delta == 0)
			return record.Balance;
		
		int newBalance = record.Balance + delta;
		
		if(currency.Max != null && newBalance > currency.Max)
		{
			throw new JsonApiException(ApiErrors.CURRENCY_LIMIT, "New balance of "+newBalance+" would be over limit "+currency.Max);
		}
		else if(currency.Min != null && newBalance < currency.Min)
		{
			throw new JsonApiException(ApiErrors.CURRENCY_LIMIT, "New balance of "+newBalance+" would be under limit "+currency.Min);
		}
		else if((delta > 0 && newBalance < record.Balance) || (delta < 0 && newBalance > record.Balance))
		{
			throw new JsonApiException(ApiErrors.CURRENCY_OVERFLOW, "Changing balance of "+record.Balance +" by "+delta+" would cause overflow");
		}
		
		updateRecord(currency, record, newBalance, reason);
		
		return record.Balance;
	}
	
	public static int setCurrencyBalance(ServervilleUser user, CurrencyInfo currency, int balance, String reason) throws SQLException, JsonApiException
	{
		if(currency.Max != null && balance > currency.Max)
		{
			throw new JsonApiException(ApiErrors.CURRENCY_LIMIT, "New balance of "+balance+" would be over limit "+currency.Max);
		}
		
		if(currency.Min != null && balance < currency.Min)
		{
			throw new JsonApiException(ApiErrors.CURRENCY_LIMIT, "New balance of "+balance+" would be under limit "+currency.Min);
		}
		
		CurrencyRecord record = CurrencyRecord.load(user.getId(), currency.CurrencyId);
		if(record == null)
		{
			record = new CurrencyRecord();
			record.UserId = user.getId();
			record.CurrencyId = currency.CurrencyId;
			record.Balance = currency.Starting;
		}
		
		record.Remainder = 0.0;
		updateRecord(currency, record, balance, reason);
		
		return balance;
	}
	
	private static int updateRecord(CurrencyInfo currency, CurrencyRecord record, int balance, String reason) throws SQLException, JsonApiException
	{
		int before = record.Balance;
		
		record.Balance = balance;
		if(record.Modified != null)
			record.update();
		else
			record.create();
		
		if(currency.KeepHistory)
		{
			CurrencyHistory history = new CurrencyHistory();
			history.UserId = record.UserId;
			history.CurrencyId = currency.CurrencyId;
			history.Before = before;
			history.Delta = balance - before;
			history.After = balance;
			history.Action = reason;
			history.Modified = record.Modified;
			
			history.create();
		}
		
		return balance;
	}
	
	private static void validateCurrencyBalance(ServervilleUser user, CurrencyInfo currency, CurrencyRecord record, boolean write) throws SQLException, JsonApiException
	{
		Date lastUpdated = record.Modified;
		boolean updated = false;
		
		if(currency.Max != null && record.Balance > currency.Max)
		{
			updateRecord(currency, record, currency.Max, "Correction (Over max)");
			updated = true;
		}
		else if(currency.Min != null && record.Balance < currency.Min)
		{
			updateRecord(currency, record, currency.Min, "Correction (Under min)");
			updated = true;
		}
		
		if(currency.Rate != 0)
		{
			Date now = new Date();
			double hours = (now.getTime() - lastUpdated.getTime()) / (1000.0 * 60.0 * 60.0);
			double delta = currency.Rate * hours;
			double newBalance = record.Balance + record.Remainder + delta;
			
			if(delta < 0)
			{
				double min = currency.Min != null ? currency.Min : Integer.MIN_VALUE;
				if(newBalance < min)
					newBalance = min;
			}
			else if(delta > 0)
			{
				double max = currency.Max != null ? currency.Max : Integer.MAX_VALUE;
				if(newBalance > max)
					newBalance = max;
			}
			
			int newIntBalance = (int)Math.floor(newBalance);
			double newRemainder = newBalance - newIntBalance;
			
			
			if((write || updated) && (record.Balance != newIntBalance || record.Remainder != newRemainder))
			{
				record.Remainder = newRemainder;
				updateRecord(currency, record, newIntBalance, "Recharge");
			}
			else
			{
				record.Remainder = newRemainder;
				record.Balance = newIntBalance;
			}
		}
	}
	
	public static int getCurrencyBalance(ServervilleUser user, CurrencyInfo currency) throws SQLException, JsonApiException
	{
		CurrencyRecord record = CurrencyRecord.load(user.getId(), currency.CurrencyId);
		if(record == null)
			return currency.Starting;
		
		validateCurrencyBalance(user, currency, record, false);
		
		return record.Balance;
	}
	
	public static Map<String,Integer> getCurrencyBalances(ServervilleUser user) throws SQLException, JsonApiException
	{
		List<CurrencyRecord> records = CurrencyRecord.loadAllForUser(user.getId());
		
		Map<String,Integer> balances = new HashMap<String,Integer>();
		
		for(CurrencyRecord record : records)
		{
			CurrencyInfo currency = CurrencyDb.get(record.CurrencyId);
			if(currency == null)
			{
				// We have a currency that's been deleted. Ignore for now
				continue;
			}

			validateCurrencyBalance(user, currency, record, false);

			balances.put(record.CurrencyId, record.Balance);
		}
		
		for(CurrencyInfo currency : CurrencyDb.values())
		{
			balances.putIfAbsent(currency.CurrencyId, currency.Starting);
		}
		
		
		return balances;
	}
}
