package com.robonobo.midas.dao;

import org.springframework.stereotype.Repository;

import com.robonobo.midas.model.MidasUserConfig;

@Repository("userConfigDao")
public class UserConfigDaoImpl extends MidasDao implements UserConfigDao {
	/* (non-Javadoc)
	 * @see com.robonobo.midas.dao.MidasUserConfigDao#getUserConfig(long)
	 */
	@Override
	public MidasUserConfig getUserConfig(long userId) {
		return (MidasUserConfig) getSession().get(MidasUserConfig.class, userId);
	}
	
	/* (non-Javadoc)
	 * @see com.robonobo.midas.dao.MidasUserConfigDao#saveUserConfig(com.robonobo.midas.model.MidasUserConfig)
	 */
	@Override
	public void saveUserConfig(MidasUserConfig config) { 
		getSession().saveOrUpdate(config);
	}
	
	/* (non-Javadoc)
	 * @see com.robonobo.midas.dao.MidasUserConfigDao#deleteUserConfig(com.robonobo.midas.model.MidasUserConfig)
	 */
	@Override
	public void deleteUserConfig(MidasUserConfig config) {
		getSession().delete(config);
	}
}
