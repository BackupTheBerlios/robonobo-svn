package com.robonobo.sonar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.GeneratedMessage;
import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.core.api.proto.CoreApi.NodeList;
import com.robonobo.sonar.beans.SonarEndPoint;
import com.robonobo.sonar.beans.SonarNode;
import com.robonobo.sonar.discovery.DiscoveryStrategy;
import com.robonobo.sonar.discovery.MostRecentFirstStrategy;
import com.robonobo.sonar.retention.RetentionStrategy;
import com.robonobo.sonar.retention.SupernodesOnlyStrategy;

public class SonarServlet extends HttpServlet {
	public final static String APP_NAME = "Sonar";
	public final static String APP_VERSION = "0.1";
	Log log = LogFactory.getLog(getClass());
	DiscoveryStrategy dStrat = new MostRecentFirstStrategy();
//	RetentionStrategy rStrat = new PublicSupernodesOnlyStrategy();
	RetentionStrategy rStrat = new SupernodesOnlyStrategy();

	public SonarServlet() {
	}
	
	@Override
	public void init() throws ServletException {
		super.init();
		PersistenceManager.createSession();
		try {
			purgeNodes();
		} finally {
			PersistenceManager.closeSession(false);
		}
	}

	protected void purgeNodes() {
		// purge old nodes
		Session s = PersistenceManager.currentSession();
		Criteria c = s.createCriteria(SonarNode.class);
		Iterator i = c.list().iterator();
		while(i.hasNext())
			s.delete(i.next());
	}

	protected void purgeNodesWithSameEndPoint(Node node) {
		Session session = PersistenceManager.currentSession();
		// trash old versions of this url
		Set<SonarNode> nodesToDelete = new HashSet<SonarNode>();
		for (EndPoint ep : node.getEndPointList()) {
			Criteria crit = session.createCriteria(SonarEndPoint.class);
			crit.add(Expression.eq("url", ep.getUrl()));
			List<SonarEndPoint> matchingEps = crit.list();
			for (SonarEndPoint mEp : matchingEps) {
				nodesToDelete.add(mEp.getNode());
				session.delete(mEp);
			}
		}
		for (SonarNode nd : nodesToDelete) {
			session.delete(nd);
		}
		session.flush();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Do nothing - we're post only
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Session session = PersistenceManager.currentSession();
		try {
			Node.Builder nb = Node.newBuilder();
			readFromInput(nb, req);
			Node nodeMsg = nb.build();
			// If we're keeping these details, delete any old instances on the same ip
			if(rStrat.shouldRetainNode(nodeMsg)) {
				log.info("Retaining node "+nodeMsg);
				purgeNodesWithSameEndPoint(nodeMsg);
				SonarNode newNode = new SonarNode(nodeMsg);
				newNode.setLastSeen(TimeUtil.now());
				session.saveOrUpdate(newNode);
				session.flush();
			}
			List<Node> nodes = dStrat.discover(nodeMsg);
			NodeList nl = NodeList.newBuilder().addAllNode(nodes).build();
			writeToOutput(nl, resp);
			resp.setContentType("application/data");
			resp.setStatus(HttpServletResponse.SC_OK);
			log.info("Sent " + nodes.size() + " nodes to node " + nodeMsg.getId());
		} catch(Exception e) {
			throw new ServletException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void readFromInput(AbstractMessage.Builder bldr, HttpServletRequest req) throws ServletException, IOException {
		bldr.mergeFrom(req.getInputStream());
	}

	protected void writeToOutput(GeneratedMessage msg, HttpServletResponse resp) throws IOException {
		msg.writeTo(resp.getOutputStream());
	}
}
