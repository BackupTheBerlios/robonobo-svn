package com.robonobo.midas.model;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import com.robonobo.common.persistence.PersistenceManager;

public class MidasInviteDAO {
	public static MidasInvite retrieveByEmail(String email) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasInvite.class);
		c.add(Expression.eq("email", email));
		List<MidasInvite> list = c.list();
		MidasInvite result = null;
		if(list.size() > 0)
			result = list.get(0);
		return result;
	}
	
	public static MidasInvite retrieveByInviteCode(String inviteCode) {
		Session session = PersistenceManager.currentSession();
		Criteria c = session.createCriteria(MidasInvite.class);
		c.add(Expression.eq("inviteCode", inviteCode));
		List<MidasInvite> list = c.list();
		MidasInvite result = null;
		if(list.size() > 0)
			result = list.get(0);
		return result;	
	}
	
	public static void save(MidasInvite invite) {
		Session session = PersistenceManager.currentSession();
		session.saveOrUpdate(invite);
	}
	
	public static void delete(MidasInvite invite) {
		Session session = PersistenceManager.currentSession();
		session.delete(invite);
	}
}
