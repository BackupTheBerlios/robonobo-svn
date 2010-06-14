package com.robonobo.sonar.discovery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.sonar.beans.SonarNode;

public class MostRecentFirstStrategy implements DiscoveryStrategy {
	public List<Node> discover(Node node) {
		Session session = PersistenceManager.currentSession();
		Criteria criteria = session.createCriteria(SonarNode.class);
		criteria.add(Expression.eq("supernode", true));
		criteria.add(Expression.ne("id", node.getId()));
		criteria.add(Expression.gt("lastSeen", new Date(new Date().getTime() - 300000)));
		criteria.addOrder(Order.desc("lastSeen"));
		List<SonarNode> sonarNodes = criteria.list();
		List<Node> result = new ArrayList<Node>();
		for (SonarNode sn : sonarNodes) {
			result.add(sn.toMsg());
		}
		return result;
	}
}
