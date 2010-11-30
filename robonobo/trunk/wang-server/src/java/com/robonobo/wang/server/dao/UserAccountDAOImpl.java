package com.robonobo.wang.server.dao;

import static org.springframework.jdbc.datasource.DataSourceUtils.*;

import java.sql.*;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.robonobo.wang.server.UserAccount;

@Repository("userAccountDao")
public class UserAccountDAOImpl implements UserAccountDao {
	private static final String CREATE_UA_SQL = "INSERT INTO user_account (id, friendly_name, email, password, balance) values ( (select case when count(id) = 0 then 1 else max(id)+1 end from user_account), ?, ?, ?, 0)";
	private static final String GET_UA_SQL = "SELECT * FROM user_account WHERE email = ?";
	private static final String LOCK_UA_SQL = GET_UA_SQL + " FOR UPDATE";
	private static final String PUT_UA_SQL = "UPDATE user_account SET friendly_name = ?, password = ?, balance = ? WHERE email = ?";
	static final String COUNT_SQL = "SELECT count(*) FROM user_account";

	@Autowired
	private DataSource dataSource;
	private Log log = LogFactory.getLog(getClass());

	/* (non-Javadoc)
	 * @see com.robonobo.wang.server.dao.UserAccountDao#createUserAccount(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void createUserAccount(String friendlyName, String email, String password) throws DAOException {
		try {
			Connection conn = getConnection(dataSource);
			PreparedStatement st = conn.prepareStatement(CREATE_UA_SQL);
			st.setString(1, friendlyName);
			st.setString(2, email);
			st.setString(3, password);
			st.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.robonobo.wang.server.dao.UserAccountDao#getUserAccount(java.lang.String)
	 */
	@Override
	public UserAccount getUserAccount(String email) throws DAOException {
		try {
			Connection conn = getConnection(dataSource);
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

	/* (non-Javadoc)
	 * @see com.robonobo.wang.server.dao.UserAccountDao#getAndLockUserAccount(java.lang.String)
	 */
	@Override
	public UserAccount getAndLockUserAccount(String email) throws DAOException {
		try {
			Connection conn = getConnection(dataSource);
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

	/* (non-Javadoc)
	 * @see com.robonobo.wang.server.dao.UserAccountDao#putUserAccount(com.robonobo.wang.server.UserAccount)
	 */
	@Override
	public void putUserAccount(UserAccount ua) throws DAOException {
		try {
			Connection conn = getConnection(dataSource);
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

	/* (non-Javadoc)
	 * @see com.robonobo.wang.server.dao.UserAccountDao#countUsers()
	 */
	@Override
	public Long countUsers() throws DAOException {
		try {
			Connection conn = getConnection(dataSource);
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
}
