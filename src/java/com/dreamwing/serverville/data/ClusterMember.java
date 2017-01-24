package com.dreamwing.serverville.data;

import java.sql.SQLException;
import java.util.Date;

import com.dreamwing.serverville.db.DatabaseManager;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "cluster_member")
public class ClusterMember {
	
	@DatabaseField(columnName="address", id=true, canBeNull=false)
	public String Address;
	
	@DatabaseField(columnName="servernum", unique=true, canBeNull=false)
	public short ServerNumber;
	
	@DatabaseField(columnName="created", dataType=DataType.DATE_LONG, canBeNull=false)
	public Date Created;
	
	public static short getServerNum(String address) throws SQLException
	{
		ClusterMember member = DatabaseManager.ClusterMemberDao.queryForId(address);
		if(member != null)
			return member.ServerNumber;
		
		member = new ClusterMember();
		member.Address = address;
		member.Created = new Date();
		
		while(true)
		{
			int prevNum = getLargestExistingServerNum();
			member.ServerNumber = (short)(prevNum + 1);
			
			if(DatabaseManager.ClusterMemberDao.create(member) == 1)
			{
				break;
			}
		}
		
		return member.ServerNumber;
	}
	
	private static int getLargestExistingServerNum() throws SQLException
	{
		ClusterMember largestMember = DatabaseManager.ClusterMemberDao.queryBuilder().orderBy("servernum", false).limit(1L).queryForFirst();
		if(largestMember != null)
			return largestMember.ServerNumber;
		
		return -1;
	}
}
