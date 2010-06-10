package com.robonobo.midas.model;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasFriendRequestDAO {
	public static MidasFriendRequest retrieveByUsers(long requestorId, long requesteeId) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasFriendRequest.class);
		c.add(Expression.eq("requestorId", requestorId));
		c.add(Expression.eq("requesteeId", requesteeId));
		List<MidasFriendRequest> list = c.list();
		MidasFriendRequest result = null;
		if(list.size() > 0)
			result = list.get(0);
		return result;
	}
	
	public static List<MidasFriendRequest> retrieveByRequestee(long requesteeId) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasFriendRequest.class);
		c.add(Expression.eq("requesteeId", requesteeId));
		List<MidasFriendRequest> list = c.list();
		return list;
	}
	
	public static MidasFriendRequest retrieveByRequestCode(String requestCode) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasFriendRequest.class);
		c.add(Expression.eq("requestCode", requestCode));
		List<MidasFriendRequest> list = c.list();
		MidasFriendRequest result = null;
		if(list.size() > 0)
			result = list.get(0);
		return result;
	}

	public static void save(MidasFriendRequest req) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(req);
	}
	
	public static void delete(MidasFriendRequest req) {
		Session session = PersistenceManager.currentSession();
		session.delete(req);
	}
}
