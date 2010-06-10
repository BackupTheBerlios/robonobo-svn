package com.robonobo.wang.server;

import java.math.BigInteger;

public interface DoubleSpendDAO {
	public abstract boolean isDoubleSpend(String coinId) throws DAOException;
	public abstract void add(String coinId) throws DAOException;
}