package com.robonobo.mina.network.eon;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.logging.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.eon.DEONConnection;
import com.robonobo.eon.DEONPacket;
import com.robonobo.eon.EONConnectionEvent;
import com.robonobo.eon.EONException;
import com.robonobo.eon.EONManager;
import com.robonobo.eon.EonSocketAddress;
import com.robonobo.eon.SEONConnection;
import com.robonobo.eon.SEONConnectionChannel;
import com.robonobo.eon.SEONConnectionListener;
import com.robonobo.mina.external.MinaException;
import com.robonobo.mina.external.node.EonEndPoint;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.network.ControlConnection;
import com.robonobo.mina.network.EndPointMgr;
import com.robonobo.mina.network.RemoteNodeHandler;
import com.robonobo.mina.util.MinaConnectionException;

public class EonEndPointMgr implements EndPointMgr {
	private static final int LISTENER_EON_PORT = 23;
	private static final int LOCAL_LISTENER_EON_PORT = 17;
	private MinaInstance mina;
	private EonEndPoint myListenEp;
	private EonEndPoint gatewayEp;
	private Log log;
	private EONManager eonMgr;
	private SEONConnection listenConn;
	private Thread localNodesThread;
	boolean localListenerReady = false;

	public EonEndPointMgr() {
	}

	public void start() throws Exception {
		log = mina.getLogger(getClass());
		String myAddr = mina.getConfig().getLocalAddress();
		if (myAddr == null)
			throw new MinaException("No local IP address defined");
		InetAddress myAddress = InetAddress.getByName(myAddr);
		int udpPortToListen = mina.getConfig().getListenUdpPort();
		myListenEp = new EonEndPoint(myAddress, udpPortToListen, LISTENER_EON_PORT);
		eonMgr = new EONManager("mina", mina.getExecutor(), udpPortToListen);
		eonMgr.setMaxOutboundBps(mina.getConfig().getMaxOutboundBps());
		if (mina.getConfig().getGatewayAddress() != null)
			gatewayEp = new EonEndPoint(InetAddress.getByName(mina.getConfig().getGatewayAddress()), mina.getConfig().getGatewayUdpPort(), LISTENER_EON_PORT);
		log.info("Starting Eon endpoint on " + myListenEp);
		eonMgr.start();
		listenConn = eonMgr.createSEONConnection();
		listenConn.addListener(new IncomingConnectionListener());
		listenConn.bind(mina.getConfig().getListenEonPort());
		if (mina.getConfig().getLocateLocalNodes()) {
			localNodesThread = new Thread(new LocalNodesListener());
			localNodesThread.start();
		}
	}

	public void stop() {
		if (localNodesThread != null)
			localNodesThread.interrupt();
		log.info("Shutting down Eon endpoint on " + myListenEp);
		listenConn.close();
		eonMgr.stop();
	}

	public boolean canConnectTo(Node node) {
		for (EndPoint ep : node.getEndPointList()) {
			if(EonEndPoint.isEonUrl(ep.getUrl()))
				return true;
		}
		return false;
	}

	public ControlConnection connectTo(Node node, List<EndPoint> alreadyTriedEps) {
		// Go through the endpoints of this node. If there is an eon endpoint,
		// connect to that, otherwise return null.
		nextEp: for (EndPoint ep : node.getEndPointList()) {
			if (EonEndPoint.isEonUrl(ep.getUrl())) {
				if (alreadyTriedEps != null) {
					for (EndPoint triedEp : alreadyTriedEps) {
						if (triedEp.equals(ep))
							continue nextEp;
					}
				}
				EonEndPoint theirEp = new EonEndPoint(ep.getUrl());
				try {
					log.info("Connecting to node " + node.getId() + " on " + theirEp.getUrl());
					SEONConnection newConn = eonMgr.createSEONConnection();
					EonSocketAddress theirSockAddr = new EonSocketAddress(theirEp.getAddress(), theirEp.getUdpPort(), theirEp.getEonPort());
					newConn.connect(theirSockAddr);
					EonSocketAddress mySockAddr = newConn.getLocalSocketAddress();
					EonEndPoint myEp = new EonEndPoint(mySockAddr.getAddress(), mySockAddr.getUdpPort(), mySockAddr.getEonPort());
					EonConnectionFactory scm = new EonConnectionFactory(eonMgr, mina);
					ControlConnection cc = new ControlConnection(mina, node, myEp.toMsg(), theirEp.toMsg(), newConn, scm);
					return cc;
				} catch (EONException e) {
					log.error("Error creating eon control connection to " + theirEp);
					return null;
				}
			}
		}
		return null;
	}

	public EndPoint getPublicEndPoint() {
		if (gatewayEp != null)
			return gatewayEp.toMsg();
		if (!myListenEp.getAddress().isSiteLocalAddress())
			return myListenEp.toMsg();
		return null;
	}

	public EndPoint getLocalEndPoint() {
		return myListenEp.toMsg();
	}

	public EndPoint getEndPointForTalkingTo(Node node) {
		if (node.getLocal())
			return myListenEp.toMsg();
		if (gatewayEp != null)
			return gatewayEp.toMsg();
		if (!myListenEp.getAddress().isSiteLocalAddress())
			return myListenEp.toMsg();
		return null;
	}

	public void setMina(MinaInstance mina) {
		this.mina = mina;
	}

	public void locateLocalNodes() {
		// Make sure our listener is listening before we send out our advert
		while (!localListenerReady) {
			try {
				synchronized (this) {
					wait(100);
				}
			} catch (InterruptedException ignore) {
			}
		}
		try {
			DatagramSocket sock = new DatagramSocket();
			int locatorPort = mina.getConfig().getLocalLocatorUdpPort();
			log.debug("Locating local nodes on UDP port " + locatorPort);
			EonSocketAddress sourceEp = new EonSocketAddress(myListenEp.getAddress(), myListenEp.getUdpPort(), myListenEp.getEonPort());
			EonSocketAddress destEp = new EonSocketAddress("255.255.255.255", locatorPort, LOCAL_LISTENER_EON_PORT);
			Node myLocalNodeDesc = mina.getNetMgr().getLocalNodeDesc();
			ByteBuffer payload = ByteBuffer.wrap(myLocalNodeDesc.toByteArray());
			DEONPacket pkt = new DEONPacket(sourceEp, destEp, payload);
			ByteBuffer buf = ByteBuffer.allocate(8192);
			pkt.toByteBuffer(buf);
			buf.flip();
			byte[] pktBytes = new byte[buf.limit()];
			System.arraycopy(buf.array(), 0, pktBytes, 0, buf.limit());
			sock.setBroadcast(true);
			sock.send(new DatagramPacket(pktBytes, pktBytes.length, InetAddress.getByName("255.255.255.255"), locatorPort));
			sock.close();
		} catch (Exception e) {
			log.error("Caught " + e.getClass().getName() + " when locating local nodes: " + e.getMessage());
		}
	}

	@Override
	public void configUpdated() {
		eonMgr.setMaxOutboundBps(mina.getConfig().getMaxOutboundBps());
	}
	
	private class LocalNodesListener extends CatchingRunnable {
		private DEONConnection conn;

		public void doRun() {
			conn = eonMgr.createDEONConnection();
			try {
				conn.bind(LOCAL_LISTENER_EON_PORT);
			} catch (EONException e) {
				log.fatal("ERROR: caught EONException while listening for local nodes", e);
				return;
			}
			localListenerReady = true;
			log.debug("Listening for local nodes");
			while (true) {
				try {
					Object[] arr = conn.read();
					ByteBuffer buf = (ByteBuffer) arr[0];
					// EonSocketAddress fromSockAddr = (EonSocketAddress)
					// arr[1];
					handleData(buf);
				} catch (InterruptedException e) {
					log.debug("LocalNodeListener caught interruptedexception: exiting");
					conn.close();
					return;
				} catch (EONException e) {
					log.error("LocalNodeListener caught exception", e);
				}
			}
		}

		public void handleData(ByteBuffer buf) {
			try {
				Node otherNode = Node.parseFrom(buf.array());
				if(!otherNode.getLocal()) {
					log.error("Received local node advert with non-local descriptor: "+new String(buf.array()));
					return;
				}
				if (otherNode.getId().equals(mina.getMyNodeId().toString()))
					return; // This is just me
				else {
					if (!mina.getCCM().haveRunningOrPendingCCTo(otherNode.getId())) {
						log.debug("Received local node request from " + otherNode.getId());
						try {
							mina.getCCM().initiateNewCC(otherNode, null);
						} catch (MinaConnectionException e) {
							log.error("Caught exception attempting to make CC to local node " + otherNode.getId(), e);
						}
					}
				}
			} catch (InvalidProtocolBufferException e) {
				log.error("Error: XML parse error when receiving local node advert", e);
			}
		}
	}

	private class IncomingConnectionListener implements SEONConnectionListener {
		private EonConnectionFactory connectionFactory;

		IncomingConnectionListener() {
			connectionFactory = new EonConnectionFactory(eonMgr, mina);
		}

		public void onClose(EONConnectionEvent event) {
			log.warn("Eon listener closing on " + myListenEp);
		}

		public void onNewSEONConnection(EONConnectionEvent event) {
			SEONConnection conn = (SEONConnection) event.getConnection();
			EonSocketAddress localSockAddr = conn.getLocalSocketAddress();
			EonEndPoint myEp = new EonEndPoint(localSockAddr.getAddress(), localSockAddr.getUdpPort(), localSockAddr.getEonPort());
			EonSocketAddress remoteSockAddr = conn.getRemoteSocketAddress();
			EonEndPoint theirEp = new EonEndPoint(remoteSockAddr.getAddress(), remoteSockAddr.getUdpPort(), remoteSockAddr.getEonPort());
			RemoteNodeHandler handler = new RemoteNodeHandler(mina, conn, myEp.toMsg(), theirEp.toMsg(), connectionFactory);
			handler.handle();
		}
	}
}
