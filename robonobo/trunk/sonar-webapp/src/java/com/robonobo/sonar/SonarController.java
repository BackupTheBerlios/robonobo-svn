package com.robonobo.sonar;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.core.api.proto.CoreApi.NodeList;
import com.robonobo.sonar.retention.RetentionStrategy;
import com.robonobo.sonar.retention.SupernodesOnlyStrategy;

@Controller
public class SonarController {
	Log log = LogFactory.getLog(getClass());
//	RetentionStrategy rStrat = new PublicSupernodesOnlyStrategy();
	RetentionStrategy rStrat = new SupernodesOnlyStrategy();

	@Autowired
	private NodeDao nodeDao;
	
	@RequestMapping(value="/", method=RequestMethod.POST)
	@Transactional(rollbackFor=Exception.class)
	public void getSupernodes(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Node.Builder nb = Node.newBuilder();
		nb.mergeFrom(req.getInputStream());
		Node node = nb.build();
		if(rStrat.shouldRetainNode(node)) {
			log.info("Retaining node "+node);
			nodeDao.deleteDuplicateNodes(node);
			nodeDao.saveNode(node);
		}

		List<Node> nodes = nodeDao.getAllSupernodes(node);
		NodeList nl = NodeList.newBuilder().addAllNode(nodes).build();
		nl.writeTo(resp.getOutputStream());
		resp.setContentType("application/data");
		resp.setStatus(HttpServletResponse.SC_OK);
		log.info("Sent " + nodes.size() + " nodes to node " + node.getId());
	}
}
