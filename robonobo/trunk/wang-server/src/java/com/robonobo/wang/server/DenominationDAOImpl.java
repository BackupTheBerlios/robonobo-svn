package com.robonobo.wang.server;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.wang.beans.DenominationPrivate;
import com.robonobo.wang.beans.DenominationPublic;

public class DenominationDAOImpl implements DenominationDAO {
	private static final String GET_DENOMS_SQL = "SELECT * FROM denomination";
	static final String DELETE_DENOMS_SQL = "DELETE FROM denomination";
	static final String INSERT_DENOM_SQL = "INSERT INTO denomination (id, denom, generator, prime, public_key, private_key) VALUES (?, ?, ?, ?, ?, ?)";
	DbMgr dbMgr;
	private Log log = LogFactory.getLog(getClass());

	public List<DenominationPublic> getDenomsPublic() throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(GET_DENOMS_SQL);
			List<DenominationPublic> result = new ArrayList<DenominationPublic>();
			while (rs.next()) {
				result.add(getDenomPublic(rs));
			}
			return result;
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	public List<DenominationPrivate> getDenomsPrivate() throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(GET_DENOMS_SQL);
			List<DenominationPrivate> result = new ArrayList<DenominationPrivate>();
			while (rs.next()) {
				result.add(getDenomPrivate(rs));
			}
			return result;
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	public void deleteAllDenoms() throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			Statement s = conn.createStatement();
			s.executeUpdate(DELETE_DENOMS_SQL);
		} catch (SQLException e) {
			throw new DAOException(e);
		}

	}

	public void putDenom(DenominationPrivate denom) throws DAOException {
		try {
			Connection conn = dbMgr.getConnection();
			PreparedStatement ps = conn.prepareStatement(INSERT_DENOM_SQL);
			ps.setInt(1, denom.getPrivateKey().hashCode());
			ps.setInt(2, denom.getDenom());
			ps.setString(3, denom.getGenerator().toString());
			ps.setString(4, denom.getPrime().toString());
			ps.setString(5, denom.getPublicKey().toString());
			ps.setString(6, denom.getPrivateKey().toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new DAOException(e);
		}		
	}

	private DenominationPublic getDenomPublic(ResultSet rs) throws SQLException {
		BigInteger gen = new BigInteger(rs.getString("generator"));
		BigInteger prime = new BigInteger(rs.getString("prime"));
		BigInteger pubKey = new BigInteger(rs.getString("public_key"));
		DenominationPublic result = new DenominationPublic(gen, prime, pubKey);
		result.setDenom(rs.getInt("denom"));
		return result;
	}

	private DenominationPrivate getDenomPrivate(ResultSet rs) throws SQLException {
		BigInteger gen = new BigInteger(rs.getString("generator"));
		BigInteger prime = new BigInteger(rs.getString("prime"));
		BigInteger pubKey = new BigInteger(rs.getString("public_key"));
		BigInteger priKey = new BigInteger(rs.getString("private_key"));
		DenominationPrivate result = new DenominationPrivate(gen, prime, pubKey, priKey);
		result.setDenom(rs.getInt("denom"));
		return result;
	}

	public void setDbMgr(DbMgr dbMgr) {
		this.dbMgr = dbMgr;
	}
}
