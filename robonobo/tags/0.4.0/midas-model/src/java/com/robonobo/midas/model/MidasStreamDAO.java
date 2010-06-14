package com.robonobo.midas.model;

import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasStreamDAO {
	public static void deleteStream(MidasStream stream) {
		Session session = PersistenceManager.currentSession();
		session.delete(stream);
	}

	public static List<MidasStream> findLatest(int limit) {
		Session session = PersistenceManager.currentSession();
		Criteria crit = session.createCriteria(MidasStream.class);
		crit.addOrder(Order.desc("modified"));
		crit.setMaxResults(limit);
		return crit.list();
	}

	public static MidasStream loadStream(String streamId) {
		Session session = PersistenceManager.currentSession();
		MidasStream stream = (MidasStream) session.get(MidasStream.class, streamId);
		return stream;
	}

	public static void saveStream(MidasStream stream) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(stream);
		session.flush();
	}
}
