package com.robonobo.common.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Expression;

public class PersistenceManager {
	private static final SessionFactory sessionFactory;
	private static ThreadLocal<Session> sessions = new ThreadLocal<Session>();
	
	static {
		try {
			sessionFactory = new Configuration().configure().buildSessionFactory();
		} catch(Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static void createSession() {
		Session s = sessionFactory.openSession();
		Transaction t = s.getTransaction();
		t.begin();
		sessions.set(s);
	}

	public static Session currentSession() {
		return sessions.get();
	}

	public static void closeSession(boolean gotError) {
		Session s = sessions.get();
		if(s == null)
			return;
		sessions.remove();
		Transaction t = s.getTransaction();
		if(gotError)
			t.rollback();
		else
			t.commit(); // This closes the session too
	}

	public static boolean exists(Class c, String id) {
		Session s = currentSession();
		Transaction t = s.getTransaction();
		s.beginTransaction();
		Criteria crit = s.createCriteria(c);
		crit.add(Expression.idEq(id));
		return !crit.list().isEmpty();
	}
}
