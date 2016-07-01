package com.dreamwing.serverville.net;

import java.sql.SQLException;

public interface HttpSession
{
	String getId();
	void refresh() throws SQLException;
}
