package com.robonobo.wang.server;

import java.sql.Connection;
import java.sql.SQLException;

public interface DbMgr {
	public Connection getConnection() throws SQLException;
	public void begin() throws SQLException;
	public void commit() throws SQLException;
	public void rollback() throws SQLException;
}