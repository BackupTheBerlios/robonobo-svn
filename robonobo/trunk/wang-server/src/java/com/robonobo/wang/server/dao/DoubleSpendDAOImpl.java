package com.robonobo.wang.server.dao;

import static org.springframework.jdbc.datasource.DataSourceUtils.*;

import java.sql.*;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.robonobo.common.util.TextUtil;

@Repository("doubleSpendDao")
public class DoubleSpendDAOImpl implements DoubleSpendDao {
	private static final String CHECK_SQL = "SELECT count(*) FROM doublespend WHERE coin_hash = ?";
	private static final String ADD_SQL = "INSERT INTO doublespend (coin_hash) VALUES (?)";

	@Autowired
	private DataSource dataSource;
	
	@Override
	public boolean isDoubleSpend(String coinId) throws DAOException {
		long coinIdHash = TextUtil.longHash(coinId);
		try {
			Connection conn = getConnection(dataSource);
			PreparedStatement ps = conn.prepareStatement(CHECK_SQL);
			ps.setLong(1, coinIdHash);
			ResultSet rs = ps.executeQuery();
			if(!rs.next())
				throw new DAOException("Couldn't get row count");
			return rs.getInt(1) > 0;
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	@Override
	public void add(String coinId) throws DAOException {
		long coinIdHash = TextUtil.longHash(coinId);
		try {
			Connection conn = getConnection(dataSource);
			PreparedStatement ps = conn.prepareStatement(ADD_SQL);
			ps.setLong(1, coinIdHash);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}
}
