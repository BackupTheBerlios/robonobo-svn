package com.robonobo.hibernate.autobroadcast;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;

import com.robonobo.common.persistence.PersistenceManager;

public class ConfiguredBroadcastManager {

	public boolean exists(int id) {
		Criteria c = PersistenceManager.currentSession().createCriteria(ConfiguredBroadcast.class);
		c.add(Expression.idEq(new Integer(id)));
		return c.list().size()>0;
	}
	
	public List getConfiguredBroadcasts() {
		return PersistenceManager.currentSession().createCriteria(ConfiguredBroadcast.class).list();
	}
	
	public ConfiguredBroadcast create(String channelUri, String broadcastSource, Map sourceArgs) {
		ConfiguredBroadcast cb = new ConfiguredBroadcast();
		cb.setChannelUri(channelUri);
		cb.setSourceArgs(sourceArgs);
		cb.setBroadcastSourceClass(broadcastSource);
		PersistenceManager.currentSession().save(cb);
		return cb;
	}
	
	public void update(ConfiguredBroadcast cb) {
		PersistenceManager.currentSession().update(cb);
	}
	
	public void delete(ConfiguredBroadcast cb) {
		PersistenceManager.currentSession().delete(cb);
	}
}

