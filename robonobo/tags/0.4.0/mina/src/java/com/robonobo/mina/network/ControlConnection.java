package com.robonobo.mina.network;

import static com.robonobo.common.util.TimeUtil.now;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.google.protobuf.GeneratedMessage;
import com.robonobo.common.async.PushDataChannel;
import com.robonobo.common.async.PushDataReceiver;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.dlugosz.Dlugosz;
import com.robonobo.common.io.ByteBufferInputStream;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.message.HelloHelper;
import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.handlers.MessageHandler;
import com.robonobo.mina.message.proto.MinaProtocol.Bye;
import com.robonobo.mina.message.proto.MinaProtocol.Hello;
import com.robonobo.mina.message.proto.MinaProtocol.Ping;
import com.robonobo.mina.message.proto.MinaProtocol.Pong;
import com.robonobo.mina.util.Attempt;
import com.robonobo.mina.util.LocalConnHelper;
import com.robonobo.mina.util.MinaConnectionException;
/**
 * @syncpriority 60
 */
public class ControlConnection implements PushDataReceiver {
	protected Log log;
	protected PushDataChannel dataChan;
	protected MinaInstance mina;
	protected String nodeId;
	protected Node nodeDesc;
	protected Date lastDataRecvd;
	protected Future<?> pingTask;
	protected Attempt helloAttempt, pingAttempt;
	protected Set<LCPair> lcPairs;
	protected Set<BCPair> bcPairs;
	protected List<MessageHolder> waitingMsgs; // Messages received before Hello
	protected boolean handshakeComplete;
	protected boolean closing;
	protected boolean closed = false;
	protected EndPoint theirEp;
	protected EndPoint myEp;
	protected LocalConnHelper connHelper;
	/** This provides Broadcast/Listen connections associated with this CC */
	protected StreamConnectionFactory scf;
	/** The gamma that will be set on any bcPairs added to this conn */
	protected float broadcastGamma = 1f;

	public static final int NETWORK_READ_SIZE = 2048;
	private ByteBufferInputStream incoming;
	private int msgNameLength = -1;
	private String msgName = null;
	private int serialMsgLength = -1;

	private ControlConnection(MinaInstance mina) {
		this.mina = mina;
		log = mina.getLogger(getClass());
		lcPairs = new HashSet<LCPair>();
		bcPairs = new HashSet<BCPair>();
		waitingMsgs = new ArrayList<MessageHolder>();
		handshakeComplete = false;
		closing = false;
	}

	/** Called when we are connecting to a remote endpoint */
	public ControlConnection(MinaInstance mina, Node nd, EndPoint myEp, EndPoint theirEp, PushDataChannel dataChan, StreamConnectionFactory scf) {
		this(mina);
		nodeDesc = nd;
		if (isLocal())
			connHelper = new LocalConnHelper();
		nodeId = nodeDesc.getId();
		this.myEp = myEp;
		this.theirEp = theirEp;
		this.dataChan = dataChan;
		this.scf = scf;
		incoming = new ByteBufferInputStream();
		Hello hello = Hello.newBuilder().setNode(mina.getNetMgr().getDescriptorForTalkingTo(nodeDesc, isLocal())).build();
		helloAttempt = new MessageAttempt("Hello", mina.getConfig().getMessageTimeout(), "HelloAttempt");
		helloAttempt.start();
		sendMessageImmediate("Hello", hello);
		dataChan.setDataReceiver(this);
	}

	/** Called when we are responding to a remote request */
	public ControlConnection(MinaInstance mina, HelloHelper helHelper, StreamConnectionFactory scf) throws MinaConnectionException {
		this(mina);
		nodeDesc = helHelper.getHello().getNode();
		nodeId = nodeDesc.getId();
		if (nodeDesc.getSupernode() && mina.getConfig().isSupernode()) {
			log.error("Error - node " + nodeId + " is a supernode, it cannot connect to me");
			close(true, "Supernodes cannot connect to supernodes");
			return;
		}
		this.scf = scf;
		if (isLocal())
			connHelper = new LocalConnHelper();
		dataChan = helHelper.getDataChannel();
		myEp = helHelper.getMyEp();
		theirEp = helHelper.getTheirEp();
		incoming = helHelper.getIncoming();
		// Return from the ctor here and complete the handshake in
		// completeHandshake as it allows us to be added to the list of
		// connections - guards against multiple connections to the same node
	}

	public void completeHandshake() {
		handshakeComplete = true;
		Hello hel = Hello.newBuilder().setNode(mina.getNetMgr().getDescriptorForTalkingTo(nodeDesc, isLocal())).build();
		sendMessageImmediate("Hello", hel);
		lastDataRecvd = new Date();
		startPinging();
		mina.getCCM().notifySuccessfulConnection(this);
		dataChan.setDataReceiver(this);
	}

	@Override
	public String toString() {
		return "CC[" + nodeId + "]";
	}

	public void providerClosed() {
		close();
	}

	public void close() {
		close(false, null);
	}

	/**
	 * @syncpriority 60
	 */
	public synchronized void close(boolean sendByeMsg, String reason) {
		if (closing)
			return;
		closing = true;
		if (sendByeMsg) {
			try {
				Bye bye = Bye.newBuilder().setReason(reason).build();
				sendMessage("Bye", bye, false);
			} catch (Exception ignore) {
			}
		}
		log.info("CC " + nodeId + " exiting");
		// Kill our CPairs, and tell our manager to clean up after us
		// Calling higher syncpriority methods, use separate thread
		mina.getExecutor().execute(new KillCPairsRunner());
		if (pingTask != null)
			pingTask.cancel(true);
		if (helloAttempt != null)
			helloAttempt.cancel();
		if (pingAttempt != null)
			pingAttempt.cancel();
		dataChan.close();
		closed = true;
	}

	public synchronized boolean isClosed() {
		return closed;
	}

	/**
	 * @syncpriority 60
	 * 
	 */
	public synchronized void abort() {
		if (pingTask != null)
			pingTask.cancel(true);
		if (helloAttempt != null)
			helloAttempt.cancel();
		if (pingAttempt != null)
			pingAttempt.cancel();
	}

	/**
	 * @syncpriority 60
	 */
	public void sendMessage(String msgName, GeneratedMessage msg) {
		try {
			sendMessage(msgName, msg, true);
		} catch (Exception e) {
			log.error(this + " Error sending command", e);
			close();
		}
	}

	public void sendMessageOrThrow(String msgName, GeneratedMessage msg) throws Exception {
		try {
			sendMessage(msgName, msg, true);
		} catch (Exception e) {
			log.error(this + " Error sending command", e);
			close();
			throw e;
		}
	}

	private void sendMessageImmediate(String msgName, GeneratedMessage msg) {
		try {
			sendMessage(msgName, msg, false);
		} catch (Exception e) {
			log.error(this + " Error sending command", e);
			close();
		}
	}

	/**
	 * @syncpriority 60
	 */
	private synchronized void sendMessage(String msgName, GeneratedMessage msg, boolean checkReady) throws Exception {
		// We send the message name length, then message name, then message
		// length, then msg
		// Send it all in one go to minimise the number of pkts that get sent
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] msgNameBytes = msgName.getBytes();
		baos.write(Dlugosz.encode(msgNameBytes.length).array());
		baos.write(msgNameBytes);
		baos.write(Dlugosz.encode(msg.getSerializedSize()).array());
		msg.writeTo(baos);
		// This can be called by other threads before the connection's
		// set up properly - wait
		if (checkReady) {
			while (!handshakeComplete)
				wait();
		}
		byte[] sendData = baos.toByteArray();
		log.debug("Sending " + msgName + ": " + msg + " to " + nodeId.toString() + " (" + sendData.length + " bytes)");
		try {
			dataChan.receiveData(ByteBuffer.wrap(sendData), null);
		} catch (IOException e) {
			if (!closing)
				throw e;
		}
	}

	public String getNodeId() {
		return nodeId;
	}

	public Node getNodeDescriptor() {
		return nodeDesc;
	}

	/**
	 * Returns true if the supplied lcp is the highest-priority non-zero-flowrate lcp on this connection
	 * 
	 * @syncpriority 60
	 */
	public synchronized boolean isHighestPriority(LCPair argLcp) {
		int argPri = argLcp.getSM().getPriority();
		for (LCPair iterLcp : lcPairs) {
			if (iterLcp == argLcp)
				continue;
			if (iterLcp.getFlowRate() > 0 && iterLcp.getSM().getPriority() > argPri)
				return false;
		}
		return true;
	}

	/**
	 * This is a local shop! For local people! We'll have no trouble here!
	 */
	public boolean isLocal() {
		return nodeDesc.getLocal();
	}

	public void updateDetails(Node newNodeDesc) {
		String newNodeId = newNodeDesc.getId();
		if (!newNodeId.equals(nodeId))
			close(true, "You're not allowed to change your node ID");
		nodeDesc = newNodeDesc;
	}

	public void notifyPong(Pong pong) {
		pingAttempt.succeeded();
	}

	private void handleMessage(MessageHandler handler, final MessageHolder msgHolder) {
		GeneratedMessage msg = msgHolder.getMessage();
		String msgName = msgHolder.getMsgName();
		final MessageHandler myHandler = (handler == null) ? mina.getMessageMgr().getHandler(msgName) : handler;
		if (myHandler == null) {
			log.error(this + " ERROR: got unknown command type " + msgName);
			return;
		}
		if (log.isDebugEnabled())
			log.debug(this + " received " + msgName + ": " + msg);
		// If we're still in the handshake, complete it
		if (!handshakeComplete) {
			if (msg instanceof Hello)
				receiveHello((Hello) msg);
			else {
				waitingMsgs.add(msgHolder);
			}
			return;
		}
		lastDataRecvd = now();
		myHandler.handleMessage(msgHolder);
	}

	protected void startPinging() {
		double pingFreq = mina.getConfig().getMessageTimeout();
		// We +/- 10% randomly on this, as this means that it's much less likely
		// that two nodes ping each other simultaneously, which wastes bandwidth
		int rnd = new Random().nextInt(20);
		double var = (pingFreq / 100) * (10 - rnd);
		pingFreq += var;
		pingTask = mina.getExecutor().scheduleAtFixedRate(new PingChecker(), (int) pingFreq, (int) pingFreq, TimeUnit.SECONDS);
	}

	/**
	 * @syncpriority 140
	 */
	protected void receiveHello(Hello hello) {
		if (!hello.getNode().getId().equals(nodeId)) {
			helloAttempt.failed();
			log.error("Error: attempting to connect to ID " + nodeId + ", but node claims its ID as " + hello.getNode().getId());
			close(true, "Your Node ID is not the one I was expecting");
			return;
		}
		// Update details of our new buddy
		nodeDesc = hello.getNode();
		nodeId = nodeDesc.getId();
		helloAttempt.succeeded();
		handshakeComplete = true;
		synchronized (this) {
			notifyAll();
		}
		mina.getCCM().notifySuccessfulConnection(this);
		lastDataRecvd = new Date();
		startPinging();
		for (MessageHolder msgHolder : waitingMsgs) {
			handleMessage(null, msgHolder);
		}
		waitingMsgs = null;
	}

	public EndPoint getTheirEp() {
		return theirEp;
	}

	public EndPoint getMyEp() {
		return myEp;
	}

	/**
	 * @syncpriority 60
	 */
	public synchronized void addLCPair(LCPair pair) {
		lcPairs.add(pair);
	}

	/**
	 * @syncpriority 60
	 */
	public synchronized void addBCPair(BCPair pair) {
		pair.setGamma(broadcastGamma);
		bcPairs.add(pair);
	}

	/**
	 * @syncpriority 60
	 */
	public void removeLCPair(ConnectionPair pair) {
		synchronized (this) {
			lcPairs.remove(pair);
		}
		closeIfUnused();
	}

	/**
	 * @syncpriority 60
	 */
	public void removeBCPair(BCPair pair) {
		synchronized (this) {
			bcPairs.remove(pair);
		}
		closeIfUnused();
	}

	private void closeIfUnused() {
		if (closing)
			return;
		if (!isInUse())
			closeGracefully("Connection no longer in use");
	}

	private synchronized boolean isInUse() {
		return nodeDesc.getSupernode() || mina.getConfig().isSupernode() || isLocal() || lcPairs.size() > 0 || bcPairs.size() > 0;
	}

	/**
	 * Shuts down any pending state we have with the other end, eg currency accounts. Guaranteed to close down after a timeout, whatever happens with state.
	 * Note: this method will return immediately - check isClosed() to see if the closing process has finished.
	 */
	public void closeGracefully(String reason) {
		// If we have an account with them, or they with us, close it before we
		// quit
		boolean closeNow = true;
		if (mina.getConfig().isAgoric() && mina.getSellMgr().haveActiveAccount(nodeId)) {
			CloseCCAttempt soc = new CloseCCAttempt(reason);
			soc.start();
			mina.getSellMgr().closeAccount(nodeId, soc);
			closeNow = false;
		}
		if (mina.getConfig().isAgoric() && mina.getBuyMgr().haveActiveAccount(nodeId)) {
			CloseCCAttempt boc = new CloseCCAttempt(reason);
			boc.start();
			mina.getBuyMgr().closeAccount(nodeId, boc);
			closeNow = false;
		}

		if (closeNow)
			close(true, reason);
	}

	private class CloseCCAttempt extends Attempt {
		private String closeReason;

		public CloseCCAttempt(String closeReason) {
			super(mina, mina.getConfig().getMessageTimeout() * 1000, "CloseCC-" + nodeId);
			this.closeReason = closeReason;
		}

		protected void onSuccess() {
			closeGracefully(closeReason);
		}

		protected void onFail() {
			log.error(this + " failing attempt to close connection - closing now");
			close(false, null);
		}

		protected void onTimeout() {
			log.error(this + " timeout closing connection - closing now");
			close(false, null);
		}
	}

	public LocalConnHelper getConnHelper() {
		return connHelper;
	}

	/**
	 * Safe to iterate over
	 */
	public synchronized LCPair[] getLCPairs() {
		LCPair[] result = new LCPair[lcPairs.size()];
		lcPairs.toArray(result);
		return result;
	}

	/**
	 * Safe to iterate over
	 */
	public synchronized BCPair[] getBCPairs() {
		BCPair[] result = new BCPair[bcPairs.size()];
		bcPairs.toArray(result);
		return result;
	}

	/**
	 * @syncpriority 60
	 */
	public synchronized LCPair getLCPair(String streamId) {
		for (LCPair pair : lcPairs) {
			if (pair.getSM().getStreamId().equals(streamId))
				return (LCPair) pair;
		}
		return null;
	}

	/**
	 * @syncpriority 60
	 */
	public synchronized BCPair getBCPair(String streamId) {
		for (BCPair pair : bcPairs) {
			if (pair.getSM().getStreamId().equals(streamId))
				return pair;
		}
		return null;
	}

	/**
	 * The total download rate for this connection. Stream data only, doesn't include control data
	 * 
	 * @syncpriority 60
	 */
	public synchronized int getDownFlowRate() {
		int result = 0;
		for (LCPair pair : lcPairs) {
			result += pair.getFlowRate();
		}
		return result;
	}

	/**
	 * The total upload rate for this connection. Stream data only, doesn't include control data
	 * 
	 * @syncpriority 60
	 */
	public synchronized int getUpFlowRate() {
		int result = 0;
		for (BCPair pair : bcPairs) {
			result += pair.getFlowRate();
		}
		return result;
	}

	public StreamConnectionFactory getSCF() {
		return scf;
	}

	protected class KillCPairsRunner extends CatchingRunnable {
		public KillCPairsRunner() {
			super("KillCPairs");
		}

		public void doRun() {
			mina.getCCM().notifyDeadConnection(ControlConnection.this);
			ConnectionPair[] pairs = new ConnectionPair[lcPairs.size()];
			lcPairs.toArray(pairs);
			for (int i = 0; i < pairs.length; i++) {
				pairs[i].die();
			}
			pairs = new ConnectionPair[bcPairs.size()];
			bcPairs.toArray(pairs);
			for (int i = 0; i < pairs.length; i++) {
				pairs[i].die();
			}
		}
	}

	protected class MessageAttempt extends Attempt {
		String msgName;

		public MessageAttempt(String msgName, int timeoutSecs, String attemptName) {
			super(mina, timeoutSecs * 1000, attemptName);
			this.msgName = msgName;
		}

		public void onTimeout() {
			log.error("Timeout waiting for node " + nodeId + " to respond to " + msgName + ": Closing connection");
			close(true, "You timed out responding to my " + msgName);
		}
	}

	/**
	 * We read data in four states. 1. Read a Dlugosz number - this is the length of the msg name 2. Read the msg name as a string 3. Read a Dlugosz number -
	 * this is the length of the serialized msg 4. Read the serialized msg as a byte array
	 */
	public void receiveData(ByteBuffer buf, Object ignoreMe) throws IOException {
		incoming.addBuffer(buf);
		// Now we see what we can read
		boolean finished = false;
		do {
			if (serialMsgLength >= 0) {
				// We're reading our serialized cmd
				if (incoming.available() >= serialMsgLength) {
					// Parse our bytes into a protocol buffer msg
					MessageHandler handler = mina.getMessageMgr().getHandler(msgName);
					if (handler == null) {
						log.error(this + " ERROR: got unknown command type " + msgName);
						return;
					}
					// The protocol buffer parser will read the entire inputstream, so we fake it
					incoming.setPretendEof(serialMsgLength);
					GeneratedMessage msg = handler.parse(msgName, incoming);
					incoming.clearPretendEof();
					MessageHolder msgHolder = new MessageHolder(msgName, msg, ControlConnection.this, TimeUtil.now());
					handleMessage(handler, msgHolder);
					serialMsgLength = msgNameLength = -1;
					msgName = null;
				} else
					finished = true;
			} else if (msgName != null) {
				// We're reading our serialized msg length
				if (Dlugosz.startsWithCompleteNum(incoming)) {
					serialMsgLength = (int) Dlugosz.readLong(incoming);
				} else
					finished = true;
			} else if (msgNameLength >= 0) {
				// We're reading our msg name
				if (incoming.available() >= msgNameLength) {
					byte[] arr = new byte[msgNameLength];
					incoming.read(arr);
					msgName = new String(arr);
				} else
					finished = true;
			} else {
				// We're reading our msg name length
				if (Dlugosz.startsWithCompleteNum(incoming)) {
					msgNameLength = (int) Dlugosz.readLong(incoming);
				} else
					finished = true;
			}
		} while (!finished);
	}

	public float getBroadcastGamma() {
		return broadcastGamma;
	}

	public void setBroadcastGamma(float broadcastGamma) {
		this.broadcastGamma = broadcastGamma;
	}

	protected class PingChecker extends CatchingRunnable {
		Random rand = new Random();

		public PingChecker() {
			super("PingChecker");
		}

		public void doRun() {
			synchronized (ControlConnection.this) {
				Date nowDate = new Date();
				int timeoutSecs = mina.getConfig().getMessageTimeout();
				Date nextPingDate = new Date(lastDataRecvd.getTime() + timeoutSecs * 1000);
				if (nextPingDate.before(nowDate)) {
					pingAttempt = new MessageAttempt("Ping", timeoutSecs, "PingAttempt");
					pingAttempt.start();
					String tok = String.valueOf(rand.nextInt(9999));
					sendMessage("Ping", Ping.newBuilder().setPingId(tok).build());
				}
			}
		}
	}
}
