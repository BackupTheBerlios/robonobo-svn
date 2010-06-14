package com.robonobo.sonar;

import static com.robonobo.common.util.TimeUtil.*;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.sonar.beans.SonarNode;

public class CleanupFilter implements Filter
{
	Log log = LogFactory.getLog(getClass());
	int deleteOlderThan;
	Date nextCleanup;
	
	public void init(FilterConfig arg0) throws ServletException {
		deleteOlderThan = Integer.parseInt(arg0.getInitParameter("delete-older-than"));
		nextCleanup = timeInFuture(deleteOlderThan);
	}
	
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// perform the request
		chain.doFilter(request, response);
			
//		if(now().after(nextCleanup)) {
			deleteOldNodes();
			nextCleanup = timeInFuture(deleteOlderThan);
//		}
	}


	private void deleteOldNodes() {
		Session session = PersistenceManager.currentSession();	
		Date date = timeInPast(deleteOlderThan);
		
		// Create criteria and set lastSeen < date.getTime()
		Criteria criteria = session.createCriteria(SonarNode.class);
		criteria.add(Expression.lt("lastSeen", date));
		
		// get nodes and delete them
		List nodes = criteria.list();
		Iterator i = nodes.iterator();
		while(i.hasNext())
			session.delete(i.next());
		
		log.info("GC deleted " + nodes.size() + " old nodes from the database (older than " + String.valueOf(deleteOlderThan) + ", i.e. <" + String.valueOf(date.getTime()) + ")");
	}
	
	public void destroy() {
		
	}
	
}
