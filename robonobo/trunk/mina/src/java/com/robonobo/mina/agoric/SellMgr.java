package com.robonobo.mina.agoric;

import static com.robonobo.common.util.TimeUtil.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;

import com.google.protobuf.ByteString;
import com.robonobo.common.concurrent.*;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.CurrencyClient;
import com.robonobo.core.api.CurrencyException;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.mina.external.ConnectedNode;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.handlers.MessageHandler;
import com.robonobo.mina.message.proto.MinaProtocol.AcctClosed;
import com.robonobo.mina.message.proto.MinaProtocol.AuctionResult;
import com.robonobo.mina.message.proto.MinaProtocol.AuctionStateMsg;
import com.robonobo.mina.message.proto.MinaProtocol.BidUpdate;
import com.robonobo.mina.message.proto.MinaProtocol.MinCharge;
import com.robonobo.mina.message.proto.MinaProtocol.PayUp;
import com.robonobo.mina.message.proto.MinaProtocol.ReceivedBid;
import com.robonobo.mina.message.proto.MinaProtocol.SourceStatus;
import com.robonobo.mina.message.proto.MinaProtocol.StreamStatus;
import com.robonobo.mina.network.ControlConnection;

/**
 * Handles auctions of this node's bandwidth, and accounts that others have with us
 */
public class SellMgr {
	MinaInstance mina;
	Log log;
	// Cache our status, don't calculate more than once per sec
	AuctionStateMsg cachedStateMsg;
	Date cachedStateTime;
	Date openForBidsTime = new Date(0);
	boolean auctionInProgress = false;
	/** Agreed bids, used to calculate data cost (key is node id) */
	Map<String, Double> agreedBids = new HashMap<String, Double>();
	/** The current auction status (key is node id) */
	Map<String, Double> currentBids = new HashMap<String, Double>();
	/** nodeids that are still actively bidding */
	Set<String> activeBidders = new HashSet<String>();
	/** nodeIds we're waiting to hear from */
	Set<String> waitingForBids = new HashSet<String>();
	/** nodeIds that are not allowed to bid until we open bidding again (new nodes can bid any time) */
	Set<String> bidFrozen = new HashSet<String>();
	Timeout bidTimeout;
	Map<String, List<MessageHolder>> msgsWaitingForAcct = new HashMap<String, List<MessageHolder>>();
	Map<String, List<MessageHolder>> msgsWaitingForAgreedBid = new HashMap<String, List<MessageHolder>>();
	/**
	 * We use an incrementing (mod 64) index to track versions of auction state. This index is sent with each page, so
	 * both sides can agree on page cost
	 */
	int stateIndex = 0;

	/**
	 * We use token values to refer to our nodes, so their bids can be identified without giving out their node ids
	 */
	Map<String, String> nodeIdTokens = new HashMap<String, String>();
	int lastNodeIdToken = 0;
	Map<String, Account> accounts = new HashMap<String, Account>();
	/**
	 * The node id of the top bidder. This guy is charged at a minimum rate to prevent him DoSing
	 */
	String topBidder;
	Date auctionFinishTime;
	/**
	 * If someone disappears during our can't-bid time, fire off a bidupdate when the time elapses
	 */
	ScheduledFuture<?> bidUpdateTask = null;

	public SellMgr(MinaInstance mina) {
		this.mina = mina;
		log = mina.getLogger(getClass());
		bidTimeout = new Timeout(mina.getExecutor(), new CatchingRunnable() {
			public void doRun() throws Exception {
				bidTimeout();
			}
		});
	}

	public AuctionStateMsg getState(String forNodeId) {
		return getState(true, forNodeId);
	}

	public AuctionStateMsg getState(boolean useCache, String forNodeId) {
		// TODO Keep a separate set of bid-frozen nodes - it's not just those with agreed bids, it's also folks who have
		// dropped out recently
		synchronized (this) {
			if (useCache && cachedStateMsg != null
					&& timeInPast(mina.getConfig().getAuctionStateCacheTime()).before(cachedStateTime)) {
				AuctionStateMsg.Builder bldr = AuctionStateMsg.newBuilder(cachedStateMsg);
				if (agreedBids.containsKey(forNodeId)) {
					bldr.setYouAre(getNodeIdToken(forNodeId));
					if (now().after(openForBidsTime))
						bldr.setBidsOpen(0);
					else
						bldr.setBidsOpen((int) msUntil(openForBidsTime));
				} else {
					bldr.setYouAre("");
					bldr.clearBidsOpen();
				}
				return bldr.build();
			}
		}
		AuctionStateMsg.Builder asmBldr = AuctionStateMsg.newBuilder();
		ConnectedNode[] conNodes = mina.getCCM().getConnectedNodes();
		synchronized (this) {
			asmBldr.setIndex(stateIndex);
			// If we have an agreed bid for this guy, he's not allowed to bid until we open again - if he's a new node,
			// he can bid when he likes
			if (agreedBids.containsKey(forNodeId)) {
				if (now().after(openForBidsTime))
					asmBldr.setBidsOpen(0);
				else
					asmBldr.setBidsOpen((int) msUntil(openForBidsTime));
			}
			for (ConnectedNode cn : conNodes) {
				if (agreedBids.containsKey(cn.nodeId)) {
					ReceivedBid.Builder bidBldr = ReceivedBid.newBuilder();
					bidBldr.setListenerId(getNodeIdToken(cn.nodeId));
					bidBldr.setBid(agreedBids.get(cn.nodeId));
					bidBldr.setFlowRate(cn.uploadRate);
					asmBldr.addBid(bidBldr);
				}
			}
			if (agreedBids.containsKey(forNodeId))
				asmBldr.setYouAre(getNodeIdToken(forNodeId));
			cachedStateMsg = asmBldr.build();
			cachedStateTime = now();
			return cachedStateMsg;
		}
	}

	public synchronized boolean haveAgreedBid(String nodeId) {
		return agreedBids.containsKey(nodeId);
	}

	public synchronized double getAgreedBidFrom(String nodeId) {
		if (agreedBids.containsKey(nodeId))
			return agreedBids.get(nodeId);
		return 0d;
	}

	private synchronized String getNodeIdToken(String nodeId) {
		if (!nodeIdTokens.containsKey(nodeId)) {
			lastNodeIdToken++;
			nodeIdTokens.put(nodeId, String.valueOf(lastNodeIdToken));
		}
		return nodeIdTokens.get(nodeId);
	}

	private void cleanUp() {
		// Clean any data for nodes we no longer have connections too
		Set<String> liveNodes = mina.getCCM().getConnectedNodeIds();
		synchronized (this) {
			Iterator<Entry<String, String>> tokIter = nodeIdTokens.entrySet().iterator();
			while (tokIter.hasNext()) {
				Entry<String, String> entry = tokIter.next();
				if (!liveNodes.contains(entry.getKey()))
					tokIter.remove();
			}
			Iterator<Entry<String, List<MessageHolder>>> msgIter = msgsWaitingForAcct.entrySet().iterator();
			while (msgIter.hasNext()) {
				if(!liveNodes.contains(msgIter.next().getKey()))
					msgIter.remove();
			}
			msgIter = msgsWaitingForAgreedBid.entrySet().iterator();
			while (msgIter.hasNext()) {
				if(!liveNodes.contains(msgIter.next().getKey()))
					msgIter.remove();
			}
		}
	}

	public void topUpAccount(String nodeId, byte[] currencyToken) {
		double tokVal;
		try {
			CurrencyClient cClient = mina.getCurrencyClient();
			tokVal = cClient.depositToken(currencyToken, "Account topup from node " + nodeId);
		} catch (CurrencyException e) {
			log.error("Error depositing token from " + nodeId, e);
			return;
		}
		log.debug("Node " + nodeId + " topped up their account with value " + tokVal);
		List<MessageHolder> msgsToHandle = null;
		synchronized (this) {
			if (!accounts.containsKey(nodeId))
				accounts.put(nodeId, new Account());
			Account acct = accounts.get(nodeId);
			acct.balance += tokVal;
			acct.needsTopUp = false;
			msgsToHandle = msgsWaitingForAcct.remove(nodeId);
		}

		adjustGammas();

		if (msgsToHandle != null) {
			for (MessageHolder mh : msgsToHandle) {
				handleMsg(mh);
			}
		}
	}

	private void handleMsg(MessageHolder mh) {
		MessageHandler handler = mina.getMessageMgr().getHandler(mh.getMsgName());
		if (handler == null) {
			log.error("Can't handle msg type " + mh.getMsgName() + ": no handler");
			return;
		}
		handler.handleMessage(mh);
	}

	public synchronized boolean haveActiveAccount(String nodeId) {
		Account acct = accounts.get(nodeId);
		return acct != null && (acct.balance > 0) && !acct.needsTopUp;
	}

	public void msgPendingOpenAccount(final MessageHolder mh) {
		String nodeId = mh.getFromCC().getNodeId();
		synchronized (this) {
			// Might already be open due to threading
			if (haveActiveAccount(nodeId)) {
				mina.getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						handleMsg(mh);
					}
				});
			} else {
				if (!msgsWaitingForAcct.containsKey(nodeId))
					msgsWaitingForAcct.put(nodeId, new ArrayList<MessageHolder>());
				msgsWaitingForAcct.get(nodeId).add(mh);
			}
		}
	}

	public void msgPendingAgreedBid(final MessageHolder mh) {
		String nodeId = mh.getFromCC().getNodeId();
		synchronized (this) {
			// Might already have agreed bid due to threading
			if (haveAgreedBid(nodeId)) {
				mina.getExecutor().execute(new CatchingRunnable() {
					public void doRun() throws Exception {
						handleMsg(mh);
					}
				});
			} else {
				if (!msgsWaitingForAgreedBid.containsKey(nodeId))
					msgsWaitingForAgreedBid.put(nodeId, new ArrayList<MessageHolder>());
				msgsWaitingForAgreedBid.get(nodeId).add(mh);
			}
		}
	}

	/**
	 * If the requesting node has enough ends to pay for the requested bytes, charge their account and return the status
	 * index used to calculate the charge. Otherwise, sends them a PayUp demand (if they have an account) and return <0
	 */
	public int requestAndCharge(String nodeId, long numBytes) {
		double charge;
		synchronized (this) {
			if (!accounts.containsKey(nodeId) || !agreedBids.containsKey(nodeId))
				return -1;
			double bid = agreedBids.get(nodeId);
			if (bid == 0)
				return -1;
			// Charge is per-megabyte
			charge = ((double) numBytes / (1024 * 1024)) * bid;
		}

		boolean chargeOk = chargeAcct(nodeId, charge);

		synchronized (this) {
			if (chargeOk) {
				Account acct = accounts.get(nodeId);
				acct.bytesSinceLastAuction += numBytes;
				return stateIndex;
			} else
				return -1;
		}
	}

	/**
	 * If the charge fails, will note the account as needing to pay, send out a payup command and adjust gammas
	 * 
	 * @return Did this charge succeed?
	 */
	private boolean chargeAcct(String nodeId, double charge) {
		double payUpBalance;
		synchronized (this) {
			Account acct = accounts.get(nodeId);
			if (acct.balance < charge) {
				payUpBalance = acct.balance;
				acct.needsTopUp = true;
			} else {
				acct.balance -= charge;
				return true;
			}
		}
		// Oops, not enough ends... readjust our gammas to knock them out, and
		// tell them the bad news
		adjustGammas();

		ControlConnection cc = mina.getCCM().getCCWithId(nodeId);
		PayUp pu = PayUp.newBuilder().setBalance(payUpBalance).build();
		cc.sendMessage("PayUp", pu);
		return false;
	}

	public SourceStatus buildSourceStatus(Node reqNode, Collection<StreamStatus> ssList) {
		SourceStatus.Builder ssBldr = SourceStatus.newBuilder();
		ssBldr.setFromNode(mina.getNetMgr().getDescriptorForTalkingTo(reqNode, false));
		ssBldr.setToNodeId(reqNode.getId());
		if (mina.getConfig().isAgoric()) {
			ssBldr.setAgorics(mina.getMyAgorics());
			ssBldr.setAuctionState(getState(reqNode.getId()));
		}
		if (ssList != null)
			ssBldr.addAllSs(ssList);
		return ssBldr.build();
	}

	public void removeBidder(String fromNodeId) {
		synchronized (this) {
			if (!(agreedBids.containsKey(fromNodeId) || currentBids.containsKey(fromNodeId))) {
				log.debug("Not removing bidder " + fromNodeId + ": no current or agreed bid");
				return;
			}
			if (auctionInProgress) {
				// TODO: They're only allowed to drop out if they're not top bidder - otherwise they get charged 30s of
				// min_top_rate, to stop them DOSing by inflating the top bid and dropping out
			}
			agreedBids.remove(fromNodeId);
			currentBids.remove(fromNodeId);
			waitingForBids.remove(fromNodeId);
			activeBidders.remove(fromNodeId);
			// We don't remove them from bidFrozen here - that will be done when the next proper auction starts
		}
		updateAuctionStatus();
	}

	public void notifyDeadConnection(String nodeId) {
		removeBidder(nodeId);
		// TODO: We should try and keep their account open so that it's still there when they reconnect - but then we
		// need a way of synchronizing account state when they reconnect - look at this when we're implementing escrow
		synchronized (this) {
			accounts.remove(nodeId);
		}
	}

	public void bid(final String bidderNodeId, final double newBid) {
		if (newBid < mina.getMyAgorics().getMinBid()) {
			log.error("Ignoring too-low bid " + newBid + " from " + bidderNodeId);
			return;
		}
		synchronized (this) {
			// Not allowed to bid before opening time unless they're a new node
			if (bidFrozen.contains(bidderNodeId) && now().before(openForBidsTime)) {
				log.error("Ignoring bid [" + newBid + "] from " + bidderNodeId + ": bids not open yet");
				ControlConnection cc = mina.getCCM().getCCWithId(bidderNodeId);
				SourceStatus ss = buildSourceStatus(cc.getNodeDescriptor(), null);
				cc.sendMessage("SourceStatus", ss);
				return;
			}
			double oldBid;
			if (currentBids.containsKey(bidderNodeId))
				oldBid = currentBids.get(bidderNodeId);
			else if (agreedBids.containsKey(bidderNodeId))
				oldBid = agreedBids.get(bidderNodeId);
			else
				oldBid = 0;

			// Check that this bid is acceptable
			if (newBid > oldBid) {
				// Check they're increasing by enough
				if ((newBid - oldBid) < mina.getMyAgorics().getIncrement()) {
					log.debug("Not allowing bid of " + newBid + " from " + bidderNodeId
							+ " - not increasing by min increment");
					return;
				}
			} else if (newBid < oldBid) {
				// They're allowed to decrease their bid only as their first bid
				// of the auction
				if (currentBids.containsKey(bidderNodeId)) {
					log.error("Not allowing bid of " + newBid + " from " + bidderNodeId
							+ " - can't decrease bid during auction");
					return;
				}
			} else {
				// They sent the same bid as before - they shouldn't do this, but we'll be nice and just take it as a
				// NoBid rather than Casting them into Outer Darkness
				log.error(bidderNodeId + " bid the same as before - being nice, taking it as NoBid");
				noBid(bidderNodeId);
				return;
			}

			if (auctionInProgress) {
				if ((!agreedBids.containsKey(bidderNodeId)) && (!currentBids.containsKey(bidderNodeId))) {
					// Here comes a new challenger!
					activeBidders.add(bidderNodeId);
				} else if (!activeBidders.contains(bidderNodeId)) {
					log.error("Node " + bidderNodeId + " bid " + newBid + ", but is no longer an active bidder");
					return;
				}
				currentBids.put(bidderNodeId, newBid);
				waitingForBids.remove(bidderNodeId);
				// Anyone who bids at least once during the auction is bid frozen after that auction ends (to prevent
				// them playing silly buggers by bidding, dropping out, then bidding again when the auction closes)
				bidFrozen.add(bidderNodeId);
			} else {
				if (!waitingForBids.isEmpty())
					throw new SeekInnerCalmException();
				currentBids.put(bidderNodeId, newBid);
				// updateAuctionStatus will start a new auction
			}
		}
		updateAuctionStatus();
	}

	public void noBid(String fromNodeId) {
		synchronized (this) {
			if (!auctionInProgress) {
				// wat
				log.error("Ignoring erroneous nobid from " + fromNodeId);
				return;
			}
			// If they haven't sent us a bid in this auction yet, use their previous one
			if (!currentBids.containsKey(fromNodeId) && agreedBids.containsKey(fromNodeId))
				currentBids.put(fromNodeId, agreedBids.get(fromNodeId));
			waitingForBids.remove(fromNodeId);
			activeBidders.remove(fromNodeId);
		}
		updateAuctionStatus();
	}

	private void updateAuctionStatus() {
		boolean finished = false;
		Map<String, Double> updateBidMap = null;
		synchronized (this) {
			if (auctionInProgress) {
				finished = (activeBidders.size() <= 1 && waitingForBids.size() == 0);
				if (finished)
					log.debug("No more bids - auction finished");
				else {
					if (waitingForBids.size() == 0) {
						updateBidMap = new HashMap<String, Double>();
						updateBidMap.putAll(currentBids);
						waitingForBids.addAll(activeBidders);
					}
				}
			} else {
				if (currentBids.size() == 0) {
					// Someone just dropped out, null auction
					log.debug("Sending null auction result");
					// Put agreedBids into currentBids to make auctionFinished() work properly
					currentBids.putAll(agreedBids);
					finished = true;
				} else if ((agreedBids.size() == 0) || agreedBids.keySet().equals(currentBids.keySet())) {
					// There is a small chance we might have more than one bidder in currentBids if the threading winds
					// are blowing very strangely
					if (log.isDebugEnabled()) {
						StringBuffer sb = new StringBuffer("Finishing short auction with bids from: ");
						for (String nodeId : currentBids.keySet()) {
							sb.append(nodeId).append(" ");
						}
						log.debug(sb);
					}
					finished = true;
				} else {
					// Start a new auction
					if (log.isDebugEnabled()) {
						StringBuffer sb = new StringBuffer("Starting auction with opening bids from: ");
						for (String nodeId : currentBids.keySet()) {
							sb.append(nodeId).append(" ");
						}
						log.debug(sb);
					}
					auctionInProgress = true;
					// Bid frozen nodes are no longer frozen
					bidFrozen.clear();
					bidFrozen.addAll(currentBids.keySet());
					// Build a map of current bids to send out as a new AuctionStatus
					updateBidMap = new HashMap<String, Double>();
					// Turn all our listeners into active bidders
					for (String node : agreedBids.keySet()) {
						// If they don't have enough ends, they can't bid, so don't
						// tell them to
						if (!haveActiveAccount(node))
							continue;
						activeBidders.add(node);
						// We add their agreed bid (from the last auction) here - if they have sent us a more recent
						// bid, this will be used, see below
						updateBidMap.put(node, agreedBids.get(node));
						// If they have a current bid here, we're not waiting to hear from them
						if (!currentBids.containsKey(node))
							waitingForBids.add(node);
					}
					updateBidMap.putAll(currentBids);
				}
			}
		}
		if (finished)
			auctionFinished();
		else if (updateBidMap != null)
			sendBidUpdate(updateBidMap);
	}

	private void bidTimeout() {
		List<String> nodesToCastIntoOuterDarkness = new ArrayList<String>();
		synchronized (this) {
			nodesToCastIntoOuterDarkness.addAll(waitingForBids);
			for (String nodeId : nodesToCastIntoOuterDarkness) {
				currentBids.put(nodeId, 0d);
			}
		}
		for (String nodeId : nodesToCastIntoOuterDarkness) {
			noBid(nodeId);
		}
	}

	private void auctionFinished() {
		bidTimeout.clear();
		Set<String> sendResultSet = new HashSet<String>();
		// Bookkeeping for the period since the last auction - make sure the top
		// bidder is paying at least the minimum charge
		String tbNode = null;
		double tbCharge = 0;
		// Any messages that are pending an agreed bid, handle them afterwards
		List<MessageHolder> msgsToHandle = new ArrayList<MessageHolder>();
		synchronized (this) {
			if (topBidder != null) {
				Account topAcct = accounts.get(topBidder);
				if (topAcct != null) {
					long msSinceAuct = msElapsedSince(auctionFinishTime);
					long minChargeBytes = (long) (mina.getMyAgorics().getMinTopRate() * (msSinceAuct / 1000f));
					long bytesToCharge = minChargeBytes - topAcct.bytesSinceLastAuction;
					if (bytesToCharge > 0) {
						tbNode = topBidder;
						tbCharge = (bytesToCharge / (1024d * 1024d)) * agreedBids.get(topBidder);
					}
				}
			}

			auctionInProgress = false;
			auctionFinishTime = now();
			// Update our agreed bids
			double topBid = 0;
			topBidder = null;
			// Make sure everyone who had an agreed bid from the last auction
			// gets a result, even if they don't have one from this auction
			sendResultSet.addAll(agreedBids.keySet());
			agreedBids.clear();
			for (String nodeId : currentBids.keySet()) {
				double newBid = currentBids.get(nodeId);
				if (newBid != 0) {
					if (newBid > topBid) {
						topBidder = nodeId;
						topBid = newBid;
					}
					agreedBids.put(nodeId, newBid);
					sendResultSet.add(nodeId);
				}
			}
			// Clear everything
			activeBidders.clear();
			currentBids.clear();
			waitingForBids.clear();
			// Increment our index
			stateIndex = AuctionState.INDEX_MOD.add(stateIndex, 1);
			// If we were still within our bid freeze, we've just had a null auction due to someone dropping out, so
			// keep the openForBids time the same
			if (now().after(openForBidsTime)) {
				if (agreedBids.size() > 0)
					openForBidsTime = timeInFuture(mina.getConfig().getMinTimeBetweenAuctions());
				else
					openForBidsTime = now();
			}
			// See which messages we can now handle
			for (String nodeId : agreedBids.keySet()) {
				List<MessageHolder> msgs = msgsWaitingForAgreedBid.remove(nodeId);
				if(msgs != null)
					msgsToHandle.addAll(msgs);
			}
		}

		if (tbNode != null) {
			ControlConnection cc = mina.getCCM().getCCWithId(tbNode);
			if (cc != null) {
				MinCharge mc = MinCharge.newBuilder().setAmount(tbCharge).build();
				cc.sendMessage("MinCharge", mc);
			}
			chargeAcct(tbNode, tbCharge);
		}

		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer("Auction finished - bids:[");
			boolean first = true;
			for (String nId : agreedBids.keySet()) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(nId).append(":").append(agreedBids.get(nId));
			}
			sb.append("]");
			log.debug(sb);
		}

		adjustGammas();

		// Make an auction result msg and send it to everyone - use cached for
		// all except the first one
		boolean useCache = false;
		for (String nodeId : sendResultSet) {
			AuctionResult.Builder arBldr = AuctionResult.newBuilder();
			AuctionStateMsg asm = getState(useCache, nodeId);
			arBldr.setAuctionState(asm);
			ControlConnection cc = mina.getCCM().getCCWithId(nodeId);
			if (cc != null)
				cc.sendMessage("AuctionResult", arBldr.build());
			useCache = true;
		}
		// Handle those messages
		for (MessageHolder mh : msgsToHandle) {
			handleMsg(mh);
		}
		// Remove any old messages hanging around
		cleanUp();
	}

	private void adjustGammas() {
		Map<String, Float> gammas = new HashMap<String, Float>();
		double highBid = 0;
		synchronized (this) {
			// Figure out our highest bid, he receives gamma=1 and the others
			// are based on him
			for (String nodeId : agreedBids.keySet()) {
				Account acct = accounts.get(nodeId);
				if (acct == null || acct.needsTopUp || acct.balance <= 0)
					continue;
				double bid = agreedBids.get(nodeId);
				if (bid > highBid)
					highBid = bid;
			}
			for (String nodeId : agreedBids.keySet()) {
				Account acct = accounts.get(nodeId);
				float gamma;
				if (acct == null || acct.needsTopUp || acct.balance <= 0)
					gamma = 0;
				else
					gamma = (float) (agreedBids.get(nodeId) / highBid);
				gammas.put(nodeId, gamma);
			}
		}
		mina.getCCM().updateGammas(gammas);
	}

	/** Sends an bid update to everyone in waitingForBids */
	private void sendBidUpdate(Map<String, Double> bidMap) {
		List<String> sendList = new ArrayList<String>();
		BidUpdate.Builder templateBldr = BidUpdate.newBuilder();
		synchronized (this) {
			sendList.addAll(waitingForBids);
			for (String bNodeId : bidMap.keySet()) {
				templateBldr.addListenerId(getNodeIdToken(bNodeId));
				if (currentBids.containsKey(bNodeId))
					templateBldr.addBidAmount(currentBids.get(bNodeId));
				else if (agreedBids.containsKey(bNodeId))
					templateBldr.addBidAmount(agreedBids.get(bNodeId));
				else
					throw new SeekInnerCalmException("Couldn't find bid for node " + bNodeId);
			}
		}
		if (log.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer("Sending bidupdate to: ");
			boolean first = true;
			for (String nId : sendList) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(nId);
			}
			log.debug(sb);
		}

		for (String sNodeId : sendList) {
			BidUpdate.Builder bldr = templateBldr.clone();
			bldr.setYouAre(getNodeIdToken(sNodeId));
			ControlConnection cc = mina.getCCM().getCCWithId(sNodeId);
			if (cc != null)
				cc.sendMessage("BidUpdate", bldr.build());
		}
		bidTimeout.set(mina.getConfig().getBidTimeout());
	}

	public void closeAccount(String nodeId, Attempt onClose) {
		ControlConnection cc = mina.getCCM().getCCWithId(nodeId);
		if (cc == null) {
			if (onClose != null)
				onClose.succeeded();
			return;
		}

		Account acct;
		synchronized (this) {
			acct = accounts.remove(nodeId);
		}
		if (acct == null) {
			if (onClose != null)
				onClose.succeeded();
			return;
		}

		// Put in a bid of zero to remove this guy from our auctions
		removeBidder(nodeId);
		// Send the remaining balance
		try {
			byte[] currencyToken = mina.getCurrencyClient().withdrawToken(acct.balance,
					"Returning remaining balance to node " + nodeId);
			AcctClosed ac = AcctClosed.newBuilder().setCurrencyToken(ByteString.copyFrom(currencyToken)).build();
			cc.sendMessage("AcctClosed", ac);
		} catch (CurrencyException e) {
			log.error("Error withdrawing tokens to pay owed balance to " + nodeId);
		}
		// TODO The attempt is actually unnecessary at the moment as this
		// happens instantaneously, but once we have real payment methods it
		// won't be (probably)
		if (onClose != null)
			onClose.succeeded();
	}

	class Account {
		double balance = 0;
		/**
		 * needsTopUp will be true if the account has insufficient balance to meet one of our demands, though the
		 * balance may still be > 0
		 */
		boolean needsTopUp = false;
		long bytesSinceLastAuction = 0;
	}

	public synchronized int getStateIndex() {
		return stateIndex;
	}
}
