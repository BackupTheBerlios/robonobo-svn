package com.robonobo.midas.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import com.robonobo.midas.model.MidasUserConfig;

@Repository("userConfigDao")
public class UserConfigDaoImpl extends MidasDao implements UserConfigDao {
	@Override
	public MidasUserConfig getUserConfig(long userId) {
		return (MidasUserConfig) getSession().get(MidasUserConfig.class, userId);
	}

	@Override
	public MidasUserConfig getUserConfig(String key, String value) {
		String hql = "from MidasUserConfig uc where uc.items[:pKey] = :pVal";
		Session s = getSession();
		Query q = s.createQuery(hql);
		q.setString("pKey", key);
		q.setString("pVal", value);
		return (MidasUserConfig) q.uniqueResult();
	}
	
	@Override
	public void saveUserConfig(MidasUserConfig config) { 
		getSession().saveOrUpdate(config);
	}
	
	@Override
	public void deleteUserConfig(MidasUserConfig config) {
		getSession().delete(config);
	}
}
