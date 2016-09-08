package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "purchase")
public class Purchase
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String PuchaseId;
	
	@DatabaseField(columnName="userid", canBeNull=false)
	public String UserId;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="modified", dataType=DataType.DATE_LONG, canBeNull=false, version=true)
	public Date Modified;
	
	@DatabaseField(columnName="productid", canBeNull=false)
	public String ProductId;
	
	@DatabaseField(columnName="method", canBeNull=false)
	public String Method;
	
	@DatabaseField(columnName="transactionid", canBeNull=true)
	public String TransactionId;
	
	@DatabaseField(columnName="currency", canBeNull=true)
	public String Currency;
	
	@DatabaseField(columnName="price", canBeNull=true)
	public Integer Price;
	
	@DatabaseField(columnName="success", canBeNull=false)
	public boolean Success;
	
	@DatabaseField(columnName="error", canBeNull=true)
	public String Error;
	
	public static Purchase load(String purchaseId) throws SQLException
	{
		return DatabaseManager.PurchaseDao.queryForId(purchaseId);
	}
	
	public static List<Purchase> loadAllForUser(String userId) throws SQLException
	{
		return DatabaseManager.PurchaseDao.queryBuilder().where()
				.eq("userid", userId).query();
	}
	
	public void create() throws SQLException
	{
		DatabaseManager.PurchaseDao.create(this);
	}
	
	public void update() throws SQLException
	{
		DatabaseManager.PurchaseDao.update(this);
	}
}
