package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;

import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.util.PasswordUtil;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "invite")
public class InviteCode
{
	@DatabaseField(columnName="id", id=true, canBeNull=false)
	public String Id;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	@DatabaseField(columnName="created_by")
	private String CreatedBy;
	
	public static InviteCode create(String who) throws SQLException
	{
		InviteCode code = new InviteCode();
		code.Id = PasswordUtil.makeRandomString(12).substring(0, 12);
		code.Created = new Date();
		code.CreatedBy = who;
		
		DatabaseManager.InviteCodeDao.create(code);
		
		return code;
	}
	
	public static InviteCode findById(String id) throws SQLException
	{
		String cleanId = cleanId(id);
		return DatabaseManager.InviteCodeDao.queryForId(cleanId);
	}
	
	public static String cleanId(String id)
	{
		return id.replace("-", "").replace("l", "1").replace("I", "1").trim();
	}
	
	public String getFriendlyId()
	{
		return Id.substring(0, 3)+"-"+Id.substring(3, 6)+"-"+Id.substring(6, 9)+"-"+Id.substring(9, 12);
	}
	
	public boolean delete() throws SQLException
	{
		return DatabaseManager.InviteCodeDao.deleteById(Id) == 1;
	}
}
