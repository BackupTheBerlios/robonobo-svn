package com.robonobo.midas.model;

import org.hibernate.Session;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasLibraryDAO {
	public static MidasLibrary getLibrary(long userId) {
		Session session = PersistenceManager.currentSession();
		MidasLibrary result = (MidasLibrary) session.get(MidasLibrary.class, userId);
		return result;
	}
	
	public static void saveLibrary(MidasLibrary lib) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(lib);
		session.flush();
	}
	
	public static void deleteLibrary(MidasLibrary lib) {
		Session session = PersistenceManager.currentSession();
		session.delete(lib);
	}
}
