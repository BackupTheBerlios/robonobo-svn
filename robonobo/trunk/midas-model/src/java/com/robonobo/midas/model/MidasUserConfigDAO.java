package com.robonobo.midas.model;

import org.hibernate.Session;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasUserConfigDAO {
	public static MidasUserConfig getUserConfig(long userId) {
		Session session = PersistenceManager.currentSession();
		MidasUserConfig result = (MidasUserConfig) session.get(MidasUserConfig.class, userId);
		return result;
	}
	
	public static void saveUserConfig(MidasUserConfig config) { 
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(config);
		session.flush();
	}
	
	public static void deleteUserConfig(MidasUserConfig config) {
		Session session = PersistenceManager.currentSession();
		session.delete(config);
	}
}
