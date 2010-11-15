package com.robonobo.midas.model;

import org.hibernate.Session;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.core.api.model.Library;

public class MidasLibraryDAO {
	public static Library getLibrary(long userId) {
		Session session = PersistenceManager.currentSession();
		Library result = (Library) session.get(MidasLibrary.class, userId);
		return result;
	}
	
	public static void saveLibrary(Library lib) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(lib);
		session.flush();
	}
	
	public static void deleteLibrary(Library lib) {
		Session session = PersistenceManager.currentSession();
		session.delete(lib);
	}
}
