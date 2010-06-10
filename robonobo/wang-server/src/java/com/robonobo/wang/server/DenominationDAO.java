package com.robonobo.wang.server;

import java.util.List;

import com.robonobo.wang.beans.DenominationPrivate;
import com.robonobo.wang.beans.DenominationPublic;

public interface DenominationDAO {
	public List<DenominationPublic> getDenomsPublic() throws DAOException;
	public List<DenominationPrivate> getDenomsPrivate() throws DAOException;
	public void deleteAllDenoms() throws DAOException;
	public void putDenom(DenominationPrivate denom) throws DAOException;
}