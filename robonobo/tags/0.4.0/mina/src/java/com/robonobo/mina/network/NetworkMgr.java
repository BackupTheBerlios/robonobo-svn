package com.robonobo.mina.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.external.MinaException;
import com.robonobo.mina.external.NodeLocator;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.util.MinaConnectionException;

public class NetworkMgr {
	private final Log log;
	private final MinaInstance mina;
	private boolean listenerReady = false;
	// TODO: holepunching
	//	private InetAddress firstFoundPublicAddr;
	//	private InetSocketAddress provPublicDetails;
	//	private boolean testedHolepunching = false;
	private ScheduledFuture nodeLocatorTask;
	private final NodeLocatorList nodeLocators = new NodeLocatorList();
	private List<EndPointMgr> endPointMgrs = new ArrayList<EndPointMgr>();
	
	private String myNodeId;
	private String myAppUri;
	private boolean iAmSuper;
	
	/** Node descriptor to be sent out publically */
	private Node publicNodeDesc;
	/** Node descriptor for local nodes */
	private Node localNodeDesc;
	private boolean publicallyReachable;

	public NetworkMgr(MinaInstance mina) {
		this.mina = mina;
		log = mina.getLogger(getClass());
		instantiateEndPointMgrs();
	}

	private void instantiateEndPointMgrs() {
		// Grab our list of epmgr classnames, and instantiate them
		String[] classNames = mina.getConfig().getEndPointMgrClasses().split(",");
		if(classNames.length == 0)
			throw new RuntimeException("No endpoint manager classes defined");
		for (String className : classNames) {
			try {
				Class clazz = Class.forName(className);
				EndPointMgr epMgr = (EndPointMgr) clazz.newInstance();
				epMgr.setMina(mina);
				endPointMgrs.add(epMgr);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void addNodeLocator(NodeLocator locator) {
		nodeLocators.add(locator);
	}

	public void advisePublicIPDetails(InetSocketAddress details, InetAddress fromAddr) {
		// if(testedHolepunching)
		// return;
		// if(!localAddrMgr.addrIsPublic(details.getAddress()))
		// return;
		// if(myPublicEp != null)
		// return;
		//		
		// // Assume our NAT device can do holepunching - test only!
		// if(mina.getConfig().isAssumeNatHolepunch()) {
		// log.warn("Assuming NAT holepunch, using external details "+details);
		// EonHolePunchEndPoint tmpEp = new
		// EonHolePunchEndPoint(details.getAddress(), details.getPort(),
		// mina.getConfig().getLocalEonPort());
		// myPublicNodeDesc.addEndPoint(tmpEp);
		// myHolepunchEp = tmpEp;
		// testedHolepunching = true;
		// return;
		// }
		//		
		// // If our UDP port stays the same when talking to different IPs, our
		// NAT device supports holepunching
		// if(firstFoundPublicAddr == null) {
		// firstFoundPublicAddr = fromAddr;
		// provPublicDetails = details;
		// }
		// else if(!fromAddr.equals(firstFoundPublicAddr)) {
		// if(details.equals(provPublicDetails)) {
		// log.info("My NAT device DOES support holepunching, using external
		// details " + details);
		// EonHolePunchEndPoint tmpEp = new
		// EonHolePunchEndPoint(details.getAddress(), details.getPort(),
		// mina.getConfig().getLocalEonPort());
		// myPublicNodeDesc.addEndPoint(tmpEp);
		// myHolepunchEp = tmpEp;
		// }
		// else
		// log.info("My NAT device DOES NOT support holepunching");
		// testedHolepunching = true;
		// }
	}

	private Node getNode(boolean isLocal, Collection<EndPoint> eps) {
		Node.Builder builder = Node.newBuilder();
		builder.setProtocolVersion(MinaInstance.MINA_PROTOCOL_VERSION);
		builder.setId(myNodeId);
		builder.setApplicationUri(myAppUri);
		builder.setSupernode(iAmSuper);
		builder.addAllEndPoint(eps);
		builder.setLocal(isLocal);
		return builder.build();
	}
	
	public Node getDescriptorForTalkingTo(Node otherNode, boolean isLocal) {
		List<EndPoint> eps = new ArrayList<EndPoint>();
		for (EndPointMgr epMgr : endPointMgrs) {
			EndPoint ep = epMgr.getEndPointForTalkingTo(otherNode);
			if(ep != null)
				eps.add(ep);
		}
		return getNode(isLocal, eps);
	}

	public void removeNodeLocator(NodeLocator locator) {
		nodeLocators.remove(locator);
	}

	public void locateMoreNodes() {
		mina.getExecutor().execute(new LocateNodesRunner());
	}

	public void start() throws MinaException {
		myNodeId = mina.getMyNodeId();
		iAmSuper = mina.getConfig().isSupernode();
		myAppUri = mina.getImplementingApplication().getHomeUri();
		if(mina.getConfig().isSupernode())
			log.info("I am a supernode.  Fear me.");
		else
			log.info("I am a leaf node.");

		for (EndPointMgr epMgr : endPointMgrs) {
			log.info("Starting "+epMgr.getClass().getName());
			try {
				epMgr.start();
			} catch(Exception e) {
				throw new MinaException(e);
			}
		}

		publicallyReachable = false;
		List<EndPoint> eps = new ArrayList<EndPoint>();
		if(mina.getConfig().getSendPrivateAddrsToLocator()) {
			// Debug only!  Send private addresses to node locator
			publicallyReachable = true;
			for (EndPointMgr epMgr : endPointMgrs) {
				eps.add(epMgr.getLocalEndPoint());
			}
		} else {
			// Only send public addresses
			for (EndPointMgr epMgr : endPointMgrs) {
				EndPoint ep = epMgr.getPublicEndPoint();
				if(ep != null) {
					publicallyReachable = true;
					eps.add(ep);
				}
			}
		}
		publicNodeDesc = getNode(false, eps);

		eps.clear();
		for (EndPointMgr epMgr : endPointMgrs) {
			eps.add(epMgr.getLocalEndPoint());
		}
		localNodeDesc = getNode(true, eps);

		nodeLocatorTask = mina.getExecutor().scheduleAtFixedRate(new LocateNodesRunner(), 0, mina.getConfig().getLocateNodesFreq(), TimeUnit.SECONDS);
	}

	public void stop() {
		if(nodeLocatorTask != null)
			nodeLocatorTask.cancel(true);
		for (EndPointMgr epMgr : endPointMgrs) {
			log.info("Stopping "+epMgr.getClass().getName());
			epMgr.stop();
		}
	}

	public Node getPublicNodeDesc() {
		return publicNodeDesc;
	}

	public Node getLocalNodeDesc() {
		return localNodeDesc;
	}

	public boolean amIPublicallyReachable() {
		return publicallyReachable;
	}

	public boolean canConnectTo(Node node) {
		if(node.getProtocolVersion() > MinaInstance.MINA_PROTOCOL_VERSION)
			return false;
		// TODO Support lower protocol versions (when we have more than one...)
		if(mina.getCCM().haveRunningOrPendingCCTo(node.getId())) return true;
		if(amIPublicallyReachable()) return true;
		for (EndPointMgr epMgr : endPointMgrs) {
			if(epMgr.canConnectTo(node))
				return true;
		}
		return false;
	}

	/**
	 * Don't call this method directly, use CCMgr.initiateNewCC().  This method may not return for 30+secs
	 */
	public ControlConnection makeCCTo(Node node, List<EndPoint> triedEps) {
		for (EndPointMgr epMgr : endPointMgrs) {
			ControlConnection cc = epMgr.connectTo(node, triedEps);
			if(cc != null)
				return cc;
		}
		return null;
	}

	private class LocateNodesRunner extends CatchingRunnable {
		public LocateNodesRunner() {
			super("NodeLocator");
		}

		public void doRun() {
			if(mina.getConfig().getLocateLocalNodes())
				locateLocalNodes();
			if(mina.getConfig().isSupernode())
				sendDetailsToLocator();
			else if(mina.getConfig().getLocateRemoteNodes() && !mina.getCCM().haveSupernode())
				locateSupernodes();
		}

		private void sendDetailsToLocator() {
			for (NodeLocator locator : nodeLocators.getLocators()) {
				locator.locateSuperNodes(publicNodeDesc);
			}
		}

		private void locateLocalNodes() {
			for (EndPointMgr epMgr : endPointMgrs) {
				epMgr.locateLocalNodes();
			}
		}

		private void locateSupernodes() {
			log.debug("Locating supernodes");
			NodeLocator[] locators = nodeLocators.getLocators();
			for(int i = 0; i < locators.length; i++) {
				NodeLocator nl = locators[i];
				List<Node> nodeList = nl.locateSuperNodes(publicNodeDesc);
				if(nodeList != null) {
					Iterator<Node> iter = nodeList.iterator();
					while(iter.hasNext()) {
						Node thisNode = iter.next();
						log.debug("Checking remote node "+thisNode);
						try {
							mina.getCCM().initiateNewCC(thisNode, null);
						} catch(MinaConnectionException e) {
							log.error("Error connecting to node " + thisNode.getId(), e);
						}
					}
					log.debug("Locating remote nodes using '" + nl.toString() + "' found " + nodeList.size() + " nodes");
				} else
					log.debug("Locating remote nodes using '" + nl.toString() + "' didnt find any nodes");
			}
		}
	}

	private class NodeLocatorList {
		private final Set locators = new HashSet();

		synchronized void add(NodeLocator locator) {
			locators.add(locator);
		}

		synchronized NodeLocator[] getLocators() {
			NodeLocator[] arr = new NodeLocator[locators.size()];
			locators.toArray(arr);
			return arr;
		}

		synchronized void remove(NodeLocator locator) {
			locators.remove(locator);
		}
	}

	public List<EndPointMgr> getEndPointMgrs() {
		return endPointMgrs;
	}
}
