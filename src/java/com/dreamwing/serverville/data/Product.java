package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.util.JSON;
import com.dreamwing.serverville.util.LocaleUtil;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "product")
public class Product
{
	public static class ProductText
	{
		public String name;
		public String desc;
		public String image_url;
	}
	
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String ProductId;
	
	@DatabaseField(columnName="text", useGetSet=true, canBeNull=true)
	public String TextJson;
	public Map<String,ProductText> Text;
	
	@DatabaseField(columnName="price", useGetSet=true, canBeNull=true)
	public String PriceJson;
	public Map<String,Integer> Price;
	
	@DatabaseField(columnName="currencies", useGetSet=true, canBeNull=true)
	public String CurrenciesJson;
	public Map<String,Integer> Currencies;
	
	@DatabaseField(columnName="keydata", useGetSet=true, canBeNull=true)
	public String KeyDataJson;
	public Map<String,Object> KeyData;
	
	@DatabaseField(columnName="script", canBeNull=true)
	public String Script;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	public static List<Product> loadAll() throws SQLException
	{
		return DatabaseManager.ProductDao.queryForAll();
	}
	
	public static Product load(String id) throws SQLException
	{
		return DatabaseManager.ProductDao.queryForId(id);
	}
	
	public void save() throws SQLException
	{
		DatabaseManager.ProductDao.createOrUpdate(this);
	}
	
	public static void deleteById(String id) throws SQLException
	{
		DatabaseManager.ProductDao.deleteById(id);
	}
	
	public String getTextJson()
	{
		return JSON.serializeToStringLogError(Text);
	}
	
	public void setTextJson(String json)
	{
		Text = JSON.deserializeLogError(json, JSON.StringProductTextMapType);
		LocaleUtil.setMapDefaults(Text);
	}
	
	public String getPriceJson()
	{
		return JSON.serializeToStringLogError(Price);
	}
	
	public void setPriceJson(String json)
	{
		Price = JSON.deserializeLogError(json, JSON.StringIntegerMapType);
	}
	
	public String getCurrenciesJson()
	{
		return JSON.serializeToStringLogError(Currencies);
	}
	
	public void setCurrenciesJson(String json)
	{
		Currencies = JSON.deserializeLogError(json, JSON.StringIntegerMapType);
	}
	
	public String getKeyDataJson()
	{
		return JSON.serializeToStringLogError(KeyData);
	}
	
	public void setKeyDataJson(String json)
	{
		KeyData = JSON.deserializeLogError(json, JSON.StringObjectMapType);
	}
}
