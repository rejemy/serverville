package com.dreamwing.serverville.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;

import com.dreamwing.serverville.data.KeyDataItem;


public class ServervilleQueryRunner extends QueryRunner {

	//private static final Logger l = LogManager.getLogger(ServervilleQueryRunner.class);
	
    /**
     * Constructor for QueryRunner.
     */
    public ServervilleQueryRunner() {
        super();
    }

    /**
     * Constructor for QueryRunner that controls the use of <code>ParameterMetaData</code>.
     *
     * @param pmdKnownBroken Some drivers don't support {@link java.sql.ParameterMetaData#getParameterType(int) };
     * if <code>pmdKnownBroken</code> is set to true, we won't even try it; if false, we'll try it,
     * and if it breaks, we'll remember not to use it again.
     */
    public ServervilleQueryRunner(boolean pmdKnownBroken) {
        super(pmdKnownBroken);
    }

    /**
     * Constructor for QueryRunner that takes a <code>DataSource</code> to use.
     *
     * Methods that do not take a <code>Connection</code> parameter will retrieve connections from this
     * <code>DataSource</code>.
     *
     * @param ds The <code>DataSource</code> to retrieve connections from.
     */
    public ServervilleQueryRunner(DataSource ds) {
        super(ds);
    }

    /**
     * Constructor for QueryRunner that takes a <code>DataSource</code> and controls the use of <code>ParameterMetaData</code>.
     * Methods that do not take a <code>Connection</code> parameter will retrieve connections from this
     * <code>DataSource</code>.
     *
     * @param ds The <code>DataSource</code> to retrieve connections from.
     * @param pmdKnownBroken Some drivers don't support {@link java.sql.ParameterMetaData#getParameterType(int) };
     * if <code>pmdKnownBroken</code> is set to true, we won't even try it; if false, we'll try it,
     * and if it breaks, we'll remember not to use it again.
     */
    public ServervilleQueryRunner(DataSource ds, boolean pmdKnownBroken) {
        super(ds, pmdKnownBroken);
    }
    
    /**
     * Execute a batch of SQL INSERT, UPDATE, or DELETE queries.
     *
     * @param conn The Connection to use to run the query.  The caller is
     * responsible for closing this Connection.
     * @param sql The SQL to execute.
     * @param params An array of query replacement parameters.  Each row in
     * this array is one set of batch replacement values.
     * @return The number of rows updated per statement.
     * @throws SQLException 
     * @throws Exception 
     * @since DbUtils 1.1
     */
    public int[] batch(Connection conn, String sql, String id, long time, Collection<KeyDataItem> params) throws SQLException {
        return this.batch(conn, false, sql, id, time, params);
    }

    /**
     * Execute a batch of SQL INSERT, UPDATE, or DELETE queries.  The
     * <code>Connection</code> is retrieved from the <code>DataSource</code>
     * set in the constructor.  This <code>Connection</code> must be in
     * auto-commit mode or the update will not be saved.
     *
     * @param sql The SQL to execute.
     * @param params An array of query replacement parameters.  Each row in
     * this array is one set of batch replacement values.
     * @return The number of rows updated per statement.
     * @throws SQLException 
     * @throws Exception 
     * @since DbUtils 1.1
     */
    public int[] batch(String sql, String id, long time, Collection<KeyDataItem> params) throws SQLException {
        Connection conn = this.prepareConnection();

        return this.batch(conn, true, sql, id, time, params);
    }
    
    /**
     * Calls update after checking the parameters to ensure nothing is null.
     * @param conn The connection to use for the batch call.
     * @param closeConn True if the connection should be closed, false otherwise.
     * @param sql The SQL statement to execute.
     * @param params An array of query replacement parameters.  Each row in
     * this array is one set of batch replacement values.
     * @return The number of rows updated in the batch.
     * @throws SQLException 
     * @throws Exception 
     */
    private int[] batch(Connection conn, boolean closeConn, String sql, String id, long time, Collection<KeyDataItem> params) throws SQLException {
        if (conn == null) {
            throw new SQLException("Null connection");
        }

        if (sql == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null SQL statement");
        }

        if (params == null) {
            if (closeConn) {
                close(conn);
            }
            throw new SQLException("Null parameters. If parameters aren't need, pass an empty array.");
        }

        PreparedStatement stmt = null;
        int[] rows = null;
        try {
            stmt = this.prepareStatement(conn, sql);

            for(KeyDataItem data : params)
            {
            	if(!data.dirty)
            		continue;
            	
            	if(data.key == null || data.key.length() == 0)
    			{
    				throw new IllegalArgumentException("Data item has invalid key: "+data.key);
    			}
            	
    			data.encode();
            	this.fillStatement(stmt, id, time, data);
                stmt.addBatch();
            }

            rows = stmt.executeBatch();

        } catch (SQLException e) {
            this.rethrow(e, sql, params.toArray());
		} finally {
            close(stmt);
            if (closeConn) {
                close(conn);
            }
        }

        return rows;
    }
    
    /**
     * Fill the <code>PreparedStatement</code> replacement parameters with the
     * given objects.
     *
     * @param stmt
     *            PreparedStatement to fill
     * @param params
     *            Query replacement parameters; <code>null</code> is a valid
     *            value to pass in.
     * @throws SQLException
     *             if a database access error occurs
     */
    public void fillStatement(PreparedStatement stmt, String id, long time, KeyDataItem item)
            throws SQLException {

        stmt.setObject(1, id);
        stmt.setObject(2, item.key);
        stmt.setObject(3, item.data);
        stmt.setObject(4, item.datatype.toInt());
        stmt.setObject(5, time);
        stmt.setObject(6, time);
        stmt.setObject(7, item.deleted);
    }
}
