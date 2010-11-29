package com.robonobo.midas.dao;

import java.util.List;

import com.robonobo.midas.model.MidasUser;

public interface UserDao {

	public abstract MidasUser retrieveById(long id);

	public abstract MidasUser retrieveByEmail(String email);

	public abstract List<MidasUser> retrieveAll();

	public abstract MidasUser create(MidasUser user);

	public abstract void save(MidasUser user);

	public abstract void delete(MidasUser user);

	public abstract Long getUserCount();

}