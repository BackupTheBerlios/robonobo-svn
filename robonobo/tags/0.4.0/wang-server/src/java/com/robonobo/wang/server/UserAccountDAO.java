package com.robonobo.wang.server;


public interface UserAccountDAO {
	public void createUserAccount(String friendlyName, String email, String password) throws DAOException;
	public UserAccount getUserAccount(String email) throws DAOException;
	public UserAccount getAndLockUserAccount(String email) throws DAOException;
	public void putUserAccount(UserAccount ua) throws DAOException;
	public Long countUsers() throws DAOException;
}