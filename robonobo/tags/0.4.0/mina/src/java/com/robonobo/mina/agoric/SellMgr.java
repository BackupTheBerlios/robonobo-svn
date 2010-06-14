package com.robonobo.mina.agoric;

import static com.robonobo.common.util.TimeUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.google.protobuf.ByteString;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.concurrent.Timeout;
import com.robonobo.common.exceptions.SeekInnerCalmException;
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
import com.robonobo.mina.stream.StreamMgr;
import com.robonobo.mina.util.Attempt;

/**
 * Handles auctions of this node's bandwidth, and accounts that others have with
 * us
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
	Timeout bidTimeout;
	Map<String, List<MessageHolder>> msgsWaitingForAcct = new HashMap<String, List<MessageHolder>>();
	/**
	 * We use an incrementing (mod 64) index to track versions of auction state.
	 * This index is sent with each page, so both sides can agree on page cost
	 */
	int stateIndex = 0;

	/**
	 * We use token values to refer to our nodes, so their bids can be
	 * identified without giving out their node ids
	 */
	Map<String, String> nodeIdTokens = new HashMap<String, String>();
	int lastNodeIdToken = 0;
	Map<String, Account> accounts = new HashMap<String, Account>();
	/**
	 * The node id of the top bidder. This guy is charged at a minimum rate to
	 * prevent him DoSing
	 */
	String topBidder;
	Date auctionFinishTime;
	/**
	 * If someone disappears during our can't-bid time, fire off a bidupdate
	 * when the time elapses
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
		synchronized (this) {
			if (useCache && cachedStateMsg != null && timeInPast(mina.getConfig().getAuctionStateCacheTime()).before(cachedStateTime)) {
				AuctionStateMsg.Builder bldr = AuctionStateMsg.newBuilder(cachedStateMsg);
				if (agreedBids.containsKey(forNodeId))
					bldr.setYouAre(getNodeIdToken(forNodeId));
				else
					bldr.setYouAre("");
				return bldr.build();
			}
		}
		AuctionStateMsg.Builder asmBldr = AuctionStateMsg.newBuilder();
		ConnectedNode[] conNodes = mina.getCCM().getConnectedNodes();
		synchronized (this) {
			asmBldr.setIndex(stateIndex);
			if (now().after(openForBidsTime))
				asmBldr.setBidsOpen(0);
			else
				asmBldr.setBidsOpen((int) msUntil(openForBidsTime));
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

	private synchronized void cleanUp() {
		// Prevents memory leaks
		Iterator<Entry<String, String>> tokIter = nodeIdTokens.entrySet().iterator();
		while (tokIter.hasNext()) {
			Entry<String, String> entry = tokIter.next();
			if (!agreedBids.containsKey(entry.getKey()))
				tokIter.remove();
		}
		Iterator<Entry<String, List<MessageHolder>>> msgIter = msgsWaitingForAcct.entrySet().iterator();
		while (msgIter.hasNext()) {
			Entry<String, List<MessageHolder>> entry = msgIter.next();
			if (!agreedBids.containsKey(entry.getKey()))
				msgIter.remove();
		}
	}

	public void topUpAccount(String nodeId, byte[] currencyToken) {
		double tokVal;
		try {
			tokVal = mina.getCurrencyClient().depositToken(currencyToken);
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

	public void cmdPendingOpenAccount(MessageHolder mh) {
		String nodeId = mh.getFromCC().getNodeId();
		// The account may already be open due to threading
		boolean handleNow = false;
		synchronized (this) {
			if (haveActiveAccount(nodeId))
				handleNow = true;
			else {
				if (!msgsWaitingForAcct.containsKey(nodeId))
					msgsWaitingForAcct.put(nodeId, new ArrayList<MessageHolder>());
				msgsWaitingForAcct.get(nodeId).add(mh);
			}
		}
		if (handleNow)
			handleMsg(mh);
	}

	/**
	 * If the requesting node has enough ends to pay for the requested bytes,
	 * charge their account and return the status index used to calculate the
	 * charge. Otherwise, return <0
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
	 * If the charge fails, will note the account as needing to pay, send out a
	 * payup command and adjust gammas
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
			ssBldr.setAuctionState(mina.getSellMgr().getState(reqNode.getId()));
		}
		if (ssList != null)
			ssBldr.addAllSs(ssList);
		return ssBldr.build();
	}

	public void bid(final String fromNodeId, final double newBid) {
		if (newBid != 0 && newBid < mina.getMyAgorics().getMinBid()) {
			log.error("Ignoring too-low bid " + newBid + " from " + fromNodeId);
			return;
		}

		boolean finished = false, sendUpdate = false;
		// When we send a BidUpdate, it will include the highest bid from each
		// participant so far in this auction
		Map<String, Double> updateBidMap = new HashMap<String, Double>();
		boolean sendSourceStatus = false;
		synchronized (this) {
			if (now().before(openForBidsTime)) {
				// Not allowed to bid before opening time unless they're saying
				// goodbye (bid == 0)
				if (newBid == 0) {
					agreedBids.remove(fromNodeId);
					// If this guy was the only bidder, just shut everything
					// down
					if (agreedBids.size() == 0) {
						log.debug(fromNodeId + " bid 0 - cleaning up");
						currentBids.clear();
						auctionFinished();
						if (bidUpdateTask != null) {
							bidUpdateTask.cancel(false);
							bidUpdateTask = null;
						}
					} else {
						log.debug(fromNodeId + " bid 0 - waiting " + msUntil(openForBidsTime) + "ms before triggering bidupdate");
						// When our bids open time arrives, trigger an auction
						if (bidUpdateTask == null) {
							bidUpdateTask = mina.getExecutor().schedule(new CatchingRunnable() {
								public void doRun() throws Exception {
									Map<String, Double> updateBidMap = new HashMap<String, Double>();
									synchronized (SellMgr.this) {
										if (auctionInProgress)
											return;
										// Some folk have dropped out, trigger
										// an auction with no starting bids
										auctionInProgress = true;
										bidUpdateTask = null;
										waitingForBids.addAll(agreedBids.keySet());
										activeBidders.addAll(agreedBids.keySet());
									}
									sendBidUpdate(updateBidMap);
								}
							}, msUntil(openForBidsTime), TimeUnit.MILLISECONDS);
						}
					}
					return;
				} else {
					log.error("Ignoring bid [" + newBid + "] from " + fromNodeId + ": bids not open yet");
					sendSourceStatus = true;
				}
			}
			if (sendSourceStatus) {
				ControlConnection cc = mina.getCCM().getCCWithId(fromNodeId);
				SourceStatus ss = buildSourceStatus(cc.getNodeDescriptor(), null);
				cc.sendMessage("SourceStatus", ss);
				return;
			}

			double oldBid;
			if (currentBids.containsKey(fromNodeId))
				oldBid = currentBids.get(fromNodeId);
			else if (agreedBids.containsKey(fromNodeId))
				oldBid = agreedBids.get(fromNodeId);
			else
				oldBid = 0;

			// Check that this bid is acceptable
			if (newBid > oldBid) {
				// Check they're increasing by enough
				if ((newBid - oldBid) < mina.getMyAgorics().getIncrement()) {
					log.debug("Not allowing bid of " + newBid + " from " + fromNodeId + " - not increasing by min increment");
					return;
				}
			} else if (newBid < oldBid) {
				// They're allowed to decrease their bid only as their first bid
				// of the auction
				if (currentBids.containsKey(fromNodeId)) {
					log.error("Not allowing bid of " + newBid + " from " + fromNodeId + " - can't decrease bid during auction");
					return;
				}
			}

			if (auctionInProgress) {
				if ((!agreedBids.containsKey(fromNodeId)) && (!currentBids.containsKey(fromNodeId))) {
					// Here comes a new challenger!
					activeBidders.add(fromNodeId);
				} else if (!activeBidders.contains(fromNodeId)) {
					log.error("Node " + fromNodeId + " bid " + newBid + ", but is no longer an active bidder");
					return;
				}
				waitingForBids.remove(fromNodeId);
				if (newBid == 0) {
					// They're leaving
					activeBidders.remove(fromNodeId);
					currentBids.remove(fromNodeId);
				} else if (currentBids.containsKey(fromNodeId) && currentBids.get(fromNodeId) == newBid) {
					// They shouldn't do this, but we'll be nice and not Cast
					// them Into Outer Darkness - take it as a NoBid
					activeBidders.remove(fromNodeId);
				} else
					currentBids.put(fromNodeId, newBid);
				finished = (activeBidders.size() <= 1 && waitingForBids.size() == 0);
				if (!finished) {
					sendUpdate = (waitingForBids.size() == 0);
					if (sendUpdate) {
						waitingForBids.addAll(activeBidders);
						updateBidMap.putAll(currentBids);
					}
				}
			} else {
				// Start a new auction
				auctionInProgress = true;
				if (!waitingForBids.isEmpty())
					throw new SeekInnerCalmException();
				// Turn all our listeners into active bidders
				for (String node : agreedBids.keySet()) {
					// If they don't have enough ends, they can't bid, so don't
					// tell them to
					if (!haveActiveAccount(node))
						continue;
					if (!node.equals(fromNodeId)) {
						waitingForBids.add(node);
						activeBidders.add(node);
					}
				}
				currentBids.put(fromNodeId, newBid);
				// A zero bid prevents any more bidding in this auction
				if (newBid != 0)
					activeBidders.add(fromNodeId);
				finished = (activeBidders.size() <= 1 && waitingForBids.size() == 0);
				if (!finished) {
					sendUpdate = true;
					if (newBid != 0)
						updateBidMap.put(fromNodeId, newBid);
				}
			}
		}
		if (finished)
			auctionFinished();
		else if (sendUpdate)
			sendBidUpdate(updateBidMap);
	}

	public void noBid(String fromNodeId) {
		boolean finished = false, sendUpdate = false;
		Map<String, Double> updateBidMap = new HashMap<String, Double>();
		synchronized (this) {
			if (!auctionInProgress) {
				// Er, what?
				log.error("Ignoring erroneous nobid from " + fromNodeId);
				return;
			}
			waitingForBids.remove(fromNodeId);
			activeBidders.remove(fromNodeId);
			finished = (activeBidders.size() <= 1 && waitingForBids.size() == 0);
			if (!finished) {
				sendUpdate = (waitingForBids.size() == 0);
				if (sendUpdate) {
					waitingForBids.addAll(activeBidders);
					updateBidMap.putAll(currentBids);
				}
			}
		}
		if (finished)
			auctionFinished();
		else if (sendUpdate)
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
		}
		if (tbNode != null) {
			ControlConnection cc = mina.getCCM().getCCWithId(tbNode);
			if (cc != null) {
				MinCharge mc = MinCharge.newBuilder().setAmount(tbCharge).build();
				cc.sendMessage("MinCharge", mc);
			}
			chargeAcct(tbNode, tbCharge);
		}

		synchronized (this) {
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
			// If we have any bidders, take note of the next time until we can
			// start bidding again - otherwise we allow new bidders straight
			// away
			if (agreedBids.size() > 0)
				openForBidsTime = timeInFuture(mina.getConfig().getMinTimeBetweenAuctions());
			else
				openForBidsTime = now();
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
		// Make sure we only keep tokens for current bidders
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
				else {
					double bid = agreedBids.get(nodeId);
					gamma = (bid == highBid) ? 1f : (float) (bid / highBid);
				}
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
				templateBldr.addBidAmount(currentBids.get(bNodeId));
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
		if (cc == null)
			return;

		Account acct;
		synchronized (this) {
			acct = accounts.remove(nodeId);
		}
		if (acct == null)
			return;

		// Put in a bid of zero to remove this guy from our auctions
		bid(nodeId, 0);
		// Send the remaining balance
		try {
			byte[] currencyToken = mina.getCurrencyClient().withdrawToken(acct.balance);
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

	public void notifyDeadConnection(String nodeId) {
		bid(nodeId, 0d);
	}

	class Account {
		double balance = 0;
		/**
		 * needsTopUp will be true if the account has insufficient balance to
		 * meet one of our demands, though the balance may still be > 0
		 */
		boolean needsTopUp = false;
		long bytesSinceLastAuction = 0;
	}

	public synchronized int getStateIndex() {
		return stateIndex;
	}
}
