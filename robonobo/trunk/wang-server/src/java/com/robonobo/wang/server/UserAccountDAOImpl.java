package com.robonobo.wang.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserAccountDAOImpl implements UserAccountDAO {
	private static final String CREATE_UA_SQL = "INSERT INTO user_account (id, friendly_name, email, password, balance) values ( (select case when count(id) = 0 then 1 else max(id)+1 end from user_account), ?, ?, ?, 0)";
	private static final String GET_UA_SQL = "SELECT * FROM user_account WHERE email = ?";
	private static final String LOCK_UA_SQL = GET_UA_SQL + " FOR UPDATE";
	private static final String PUT_UA_SQL = "UPDATE user_account SET friendly_name = ?, password = ?, balance = ? WHERE email = ?";
	static final String COUNT_SQL = "SELECT count(*) FROM user_account";

	DbMgr dbMgr;
	private Log log = LogFactory.getLog(getClass());

	public void createUserAccount(String friendlyName, String email, String password) throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement st = conn.prepareStatement(CREATE_UA_SQL);
			st.setString(1, friendlyName);
			st.setString(2, email);
			st.setString(3, password);
			st.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}
	
	public UserAccount getUserAccount(String email) throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement st = conn.prepareStatement(GET_UA_SQL);
			st.setString(1, email);
			ResultSet rs = st.executeQuery();
			if (rs.next())
				return getUserAccount(rs);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
		return null;
	}

	public UserAccount getAndLockUserAccount(String email) throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement st = conn.prepareStatement(LOCK_UA_SQL);
			st.setString(1, email);
			ResultSet rs = st.executeQuery();
			if (rs.next())
				return getUserAccount(rs);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
		return null;
	}

	public void putUserAccount(UserAccount ua) throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement st = conn.prepareStatement(PUT_UA_SQL);
			st.setString(1, ua.getName());
			st.setString(2, ua.getPassword());
			st.setDouble(3, ua.getBalance());
			st.setString(4, ua.getEmail());
			st.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	public Long countUsers() throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			ResultSet rs = conn.createStatement().executeQuery(COUNT_SQL);
			if (rs.next())
				return rs.getLong(1);
			else
				throw new DAOException("count did not return");
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	private UserAccount getUserAccount(ResultSet rs) throws SQLException {
		UserAccount ua = new UserAccount();
		ua.setEmail(rs.getString("email"));
		ua.setName(rs.getString("friendly_name"));
		ua.setPassword(rs.getString("password"));
		ua.setBalance(rs.getDouble("balance"));
		return ua;
	}

	public void setDbMgr(DbMgr dbMgr) {
		this.dbMgr = dbMgr;
	}
}
