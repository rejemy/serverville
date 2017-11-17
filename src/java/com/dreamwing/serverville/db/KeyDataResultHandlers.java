package com.dreamwing.serverville.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

import com.dreamwing.serverville.data.KeyDataItem;

public final class KeyDataResultHandlers
{
	
	public static class ItemResultSetHandler implements ResultSetHandler<KeyDataItem> {
	
		@Override
		public KeyDataItem handle(ResultSet rs) throws SQLException {
			if (!rs.next()) {
				return null;
			}
			
			KeyDataItem result = new KeyDataItem(rs);

			return result;
		}
	}
	
	public static class ItemsResultSetHandler implements ResultSetHandler<List<KeyDataItem>> {
		
		@Override
		public List<KeyDataItem> handle(ResultSet rs) throws SQLException {
			if (!rs.next()) {
				return null;
			}
			
			ArrayList<KeyDataItem> results = new ArrayList<KeyDataItem>();
			
			do
			{
				results.add(new KeyDataItem(rs));
			} while(rs.next());
			
			return results;
		}
	
	}
	
	public static class IntResultSetHandler implements ResultSetHandler<Integer> {
		
		@Override
		public Integer handle(ResultSet rs) throws SQLException {
			if (!rs.next()) {
				return null;
			}
			
			return rs.getInt(1);
			
		}
	
	}
	

}