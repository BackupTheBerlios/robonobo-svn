package com.robonobo.wang.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.robonobo.common.util.TextUtil;

public class DoubleSpendDAOImpl implements DoubleSpendDAO {
	private static final String CHECK_SQL = "SELECT count(*) FROM doublespend WHERE coin_hash = ?";
	private static final String ADD_SQL = "INSERT INTO doublespend (coin_hash) VALUES (?)";

	private DbMgr dbMgr;

	public DoubleSpendDAOImpl() {
	}

	public boolean isDoubleSpend(String coinId) throws DAOException {
		long coinIdHash = TextUtil.longHash(coinId);
		try {
			Connection conn = dbMgr.getConnection();
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

	public void add(String coinId) throws DAOException {
		long coinIdHash = TextUtil.longHash(coinId);
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement ps = conn.prepareStatement(ADD_SQL);
			ps.setLong(1, coinIdHash);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	public void setDbMgr(DbMgr dbMgr) {
		this.dbMgr = dbMgr;
	}
}
