package com.robonobo.midas.dao;

import com.robonobo.midas.model.MidasUserConfig;

public interface UserConfigDao {

	public abstract MidasUserConfig getUserConfig(long userId);

	public abstract void saveUserConfig(MidasUserConfig config);

	public abstract void deleteUserConfig(MidasUserConfig config);

}