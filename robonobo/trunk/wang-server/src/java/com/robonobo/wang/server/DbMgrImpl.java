package com.robonobo.wang.server;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

public class DbMgrImpl implements DbMgr, InitializingBean {
	private ThreadLocal<Connection> connectionsInUse = new ThreadLocal<Connection>();
	private String dataSourceName;
	private DataSource dataSource;

	public DbMgrImpl() {
	}

	public Connection getConnection() throws SQLException {
		Connection conn = connectionsInUse.get();
		if(conn == null) {
			conn = dataSource.getConnection();
			connectionsInUse.set(conn);
		}
		return conn;
	}

	public void begin() throws SQLException {
		Connection conn = getConnection();
		conn.createStatement().execute("begin");
	}

	public void commit() throws SQLException {
		Connection conn = getConnection();
		conn.createStatement().execute("commit");
		conn.close();
		connectionsInUse.remove();
	}

	public void rollback() throws SQLException {
		Connection conn = getConnection();
		conn.createStatement().execute("rollback");
		conn.close();
		connectionsInUse.remove();
	}

	public void finished() {
		connectionsInUse.remove();
	}
	
	public void afterPropertiesSet() throws Exception {
		Context initCtx = new InitialContext();
		Context envCtx = (Context) initCtx.lookup("java:comp/env");
		dataSource = (DataSource) envCtx.lookup(dataSourceName); 
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}
}
