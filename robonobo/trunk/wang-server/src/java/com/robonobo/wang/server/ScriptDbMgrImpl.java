package com.robonobo.wang.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.springframework.beans.factory.InitializingBean;

public class ScriptDbMgrImpl implements DbMgr, InitializingBean {
	private ThreadLocal<Connection> connectionsInUse = new ThreadLocal<Connection>();

	private String driverClass;
	private String dbUrl;
	private String dbUser;
	private String dbPassword;

	public Connection getConnection() throws SQLException {
		Connection conn = connectionsInUse.get();
		if(conn == null) {
			conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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
		connectionsInUse.remove();
	}

	public void rollback() throws SQLException {
		Connection conn = getConnection();
		conn.createStatement().execute("rollback");
		connectionsInUse.remove();
	}

	public void finished() {
		connectionsInUse.remove();
	}
	
	public void afterPropertiesSet() throws Exception {
		Class.forName(driverClass);
	}
	
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

}
