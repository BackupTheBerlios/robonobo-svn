package com.robonobo.midas.model;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasUserDAO {
	public static MidasUser retrieveById(long id) {
		Session session = PersistenceManager.currentSession();
		MidasUser user = (MidasUser) session.get(MidasUser.class, id);
		return user;
	}

	public static MidasUser retrieveByEmail(String email) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasUser.class);
		c.add(Expression.eq("email", email));
		List<MidasUser> list = c.list();
		MidasUser user = null;
		if(list.size() > 0)
			user = list.get(0);
		return user;
	}

	@SuppressWarnings("unchecked")
	public static List<MidasUser> retrieveAll() {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasUser.class);
		return c.list();
	}
	
	public static MidasUser create(MidasUser user) {
		MidasUser currentU = retrieveByEmail(user.getEmail());
		if(currentU != null)
			throw new IllegalArgumentException("User with email "+user.getEmail()+" already exists");
		save(user);
		return user;
	}
	
	public static void detach(MidasUser user) {
		Session session = PersistenceManager.currentSession();
		session.evict(user);
	}

	public static void save(MidasUser user) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(user);
	}
	
	public static void delete(MidasUser user) {
		Session session = PersistenceManager.currentSession();
		session.delete(user);
	}
	
	public static Long getUserCount() {
		Session session = PersistenceManager.currentSession();
		Query q = session.createQuery("select count(user) from MidasUser");
		return (Long) q.uniqueResult();
	}
}
