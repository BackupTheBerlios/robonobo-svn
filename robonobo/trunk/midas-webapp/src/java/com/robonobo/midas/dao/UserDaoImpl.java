package com.robonobo.midas.dao;

import java.util.List;

import org.hibernate.*;
import org.hibernate.criterion.Expression;
import org.springframework.stereotype.Repository;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.midas.model.MidasUser;

@Repository("userDao")
public class UserDaoImpl extends MidasDao implements UserDao {
	@Override
	public MidasUser retrieveById(long id) {
		return (MidasUser) getSession().get(MidasUser.class, id);
	}

	@Override
	public MidasUser retrieveByEmail(String email) {
		Criteria c = getSession().createCriteria(MidasUser.class);
		c.add(Expression.eq("email", email));
		List<MidasUser> list = c.list();
		MidasUser user = null;
		if(list.size() > 0)
			user = list.get(0);
		return user;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<MidasUser> retrieveAll() {
		return getSession().createCriteria(MidasUser.class).list();
	}
	
	@Override
	public MidasUser create(MidasUser user) {
		MidasUser currentU = retrieveByEmail(user.getEmail());
		if(currentU != null)
			throw new IllegalArgumentException("User with email "+user.getEmail()+" already exists");
		save(user);
		return user;
	}
	
	@Override
	public void save(MidasUser user) {
		getSession().saveOrUpdate(user);
	}
	
	@Override
	public void delete(MidasUser user) {
		getSession().delete(user);
	}
	
	@Override
	public Long getUserCount() {
		Query q = getSession().createQuery("select count(user) from MidasUser");
		return (Long) q.uniqueResult();
	}
}