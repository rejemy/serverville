package com.dreamwing.serverville.agent;

import java.sql.SQLException;
import java.util.Date;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.data.AgentKey;
import com.dreamwing.serverville.db.DatabaseManager;
import com.dreamwing.serverville.util.SVIDCodec;
import com.j256.ormlite.stmt.PreparedDelete;

public class AgentKeyManager {

	private static final Logger l = LogManager.getLogger(AgentKey.class);
	
	public static AgentKey createAgentKey(String comment, String ipRange, Date expiration) throws SQLException
	{
		AgentKey key = new AgentKey();
		
		Random rand = new Random();
		byte[] keyBytes = new byte[32];
		rand.nextBytes(keyBytes);
		
		key.Key = SVIDCodec.encode(keyBytes);
		key.Comment = comment;
		key.IPRange = ipRange;
		key.Expiration = expiration;
		
		DatabaseManager.AgentKeyDao.create(key);
		
		return key;
	}
	
	public static void purgeExpiredKeys()
	{
		long now = System.currentTimeMillis();
		
		try {
			
			@SuppressWarnings("unchecked")
			PreparedDelete<AgentKey> deleteQuery = (PreparedDelete<AgentKey>) DatabaseManager.AgentKeyDao.deleteBuilder().where().lt("expiration", now).prepare();
			
			DatabaseManager.AgentKeyDao.delete(deleteQuery);
			
		} catch (SQLException e) {
			l.error("SQL error trying to purge old agent keys: ", e);
		}
	}
	
}
