package com.robonobo.mina.agoric;

import static com.robonobo.common.util.TimeUtil.msElapsedSince;
import static com.robonobo.common.util.TimeUtil.msUntil;
import static com.robonobo.common.util.TimeUtil.now;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

import com.google.protobuf.ByteString;
import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.CurrencyException;
import com.robonobo.mina.external.buffer.PageBuffer;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.message.proto.MinaProtocol.Agorics;
import com.robonobo.mina.message.proto.MinaProtocol.Bid;
import com.robonobo.mina.message.proto.MinaProtocol.BidUpdate;
import com.robonobo.mina.message.proto.MinaProtocol.CloseAcct;
import com.robonobo.mina.message.proto.MinaProtocol.EscrowBegan;
import com.robonobo.mina.message.proto.MinaProtocol.ReceivedBid;
import com.robonobo.mina.message.proto.MinaProtocol.TopUp;
import com.robonobo.mina.network.ControlConnection;
import com.robonobo.mina.network.LCPair;
import com.robonobo.mina.util.Attempt;

/**
 * Handles accounts we have with other nodes, and their auction states
 * 
 * @author macavity
 */
public class BuyMgr {
	static final int AUCTION_STATE_HISTORY = 8;
	MinaInstance mina;
	Map<String, AuctionState> asMap = new HashMap<String, AuctionState>();
	Map<String, Agorics> agMap = new HashMap<String, Agorics>();
	Map<String, Account> accounts = new HashMap<String, Account>();
	Map<String, List<Attempt>> onConfirmedBidAttempts = new HashMap<String, List<Attempt>>();
	Map<String, Attempt> onAcctCloseAttempts = new HashMap<String, Attempt>();
	Log log;

	public BuyMgr(MinaInstance mina) {
		this.mina = mina;
		log = mina.getLogger(getClass());
	}

	public synchronized boolean haveAuctionState(String nodeId) {
		return asMap.containsKey(nodeId);
	}

	public synchronized AuctionState getAuctionState(String nodeId) {
		return asMap.get(nodeId);
	}

	public synchronized Agorics getAgorics(String nodeId) {
		return agMap.get(nodeId);
	}

	public synchronized boolean haveActiveAccount(String nodeId) {
		return accounts.containsKey(nodeId);
	}

	/**
	 * @return <=0 if no agreed bid yet
	 */
	public synchronized double getAgreedBidTo(String nodeId) {
		if (!accounts.containsKey(nodeId))
			return -1;
		AuctionState as = accounts.get(nodeId).getMostRecentAs();
		if (as == null)
			return -1;
		return as.getMyBid();
	}

	public synchronized float calculateMyGamma(String nodeId) {
		Account ac = accounts.get(nodeId);
		if (ac == null)
			return 0f;
		AuctionState as = ac.getMostRecentAs();
		if (as == null)
			return 0f;
		double myBid = as.getMyBid();
		if (myBid == 0d)
			return 0f;
		double topBid = as.getTopBid();
		return (float) (myBid / topBid);
	}

	/**
	 * Gets the most recent auction status index in nodeId's auction
	 * 
	 * @return -1 If no auction status yet received
	 */
	public synchronized int getCurrentStatusIdx(String nodeId) {
		if (!accounts.containsKey(nodeId))
			return -1;
		AuctionState as = accounts.get(nodeId).getMostRecentAs();
		if (as == null)
			return -1;
		return as.getIndex();
	}

	public void setupAccountAndBid(final String nodeId, final double openingBid, final Attempt onBidSuccess) {
		if (haveActiveAccount(nodeId))
			openBidding(nodeId, openingBid, onBidSuccess);
		else {
			Attempt openAcctAttempt = new Attempt(mina, 0, "openAcct-" + nodeId) {
				protected void onSuccess() {
					openBidding(nodeId, openingBid, onBidSuccess);
				}
			};
			setupAccount(nodeId, openAcctAttempt);
		}
	}

	private void setupAccount(final String nodeId, final Attempt onAcctCreation) {
		// TODO Non-sucking payment methods
		boolean doneAlready = false;
		synchronized (this) {
			// Are we already in the process of setting up this account?
			Account account = accounts.get(nodeId);
			if (account != null)
				doneAlready = true;
		}
		if (doneAlready) {
			onAcctCreation.succeeded();
			return;
		}

		// If our currency client isn't ready yet (fast connection, this one!),
		// wait until it is
		while (!mina.getCurrencyClient().isReady()) {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				return;
			}
			if (mina.getCCM().isShuttingDown()) {
				onAcctCreation.cancel();
				return;
			}
		}
		if (!haveAuctionState(nodeId)) {
			log.error("Not attempting to setup account - we have no auctionstatus from node " + nodeId);
			onAcctCreation.failed();
			return;
		}

		Agorics ag;
		synchronized (this) {
			ag = agMap.get(nodeId);
		}
		String paymentMethod = getBestPaymentMethod(ag);
		if (paymentMethod == null) {
			log.info("Failed to setup account with " + nodeId + " - no acceptable payment methods");
			onAcctCreation.failed();
			return;
		}
		if (paymentMethod.equals("upfront")) {
			setupUpfrontAccount(nodeId, onAcctCreation);
			return;
		} else if (paymentMethod.startsWith("escrow:")) {
			String escrowProvId = paymentMethod.substring(7);
			setupEscrowAccount(nodeId, escrowProvId, onAcctCreation);
			return;
		}
		log.error("Error: could not setup account with " + nodeId + " - unknown payment method '" + paymentMethod + "'");
	}

	private void setupUpfrontAccount(final String nodeId, Attempt onAcctCreation) {
		log.info("Setting up upfront account with node " + nodeId);
		ControlConnection cc = mina.getCCM().getCCWithId(nodeId);
		if (cc == null) {
			log.error("No cc for setting up account with " + nodeId);
			onAcctCreation.failed();
			return;
		}
		double cashToSend = mina.getCurrencyClient().getOpeningBalance();
		byte[] token;
		try {
			token = mina.getCurrencyClient().withdrawToken(cashToSend, "Setting up account with node " + nodeId);
		} catch (CurrencyException e) {
			log.error("Error withdrawing token of value " + cashToSend + " trying to open account with " + nodeId);
			onAcctCreation.failed();
			return;
		}
		synchronized (this) {
			Account a = new Account();
			a.addRecentAs(asMap.get(nodeId));
			accounts.put(nodeId, a);
		}
		try {
			TopUp tu = TopUp.newBuilder().setCurrencyToken(ByteString.copyFrom(token)).build();
			cc.sendMessageOrThrow("TopUp", tu);
		} catch (Exception e) {
			// This failed - recover the cash
			final byte[] tok = token;
			mina.getExecutor().execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					log.error("Attempting to return cash for failed openacct");
					mina.getCurrencyClient().depositToken(tok,
							"Returning cash after failing to open account with node " + nodeId);
				}
			});
		}
		synchronized (this) {
			accounts.get(nodeId).balance += cashToSend;
		}
		onAcctCreation.succeeded();
	}

	private void setupEscrowAccount(final String nodeId, final String escrowProvId, final Attempt onAcctCreation) {
		Attempt a = new Attempt(mina, mina.getConfig().getMessageTimeout(), "escrow-" + nodeId) {
			protected void onSuccess() {
				// We're now connected to the escrow provider
				double cashToSend = mina.getCurrencyClient().getOpeningBalance();
				String escrowId = mina.getEscrowMgr().startNewEscrow(cashToSend);
				EscrowBegan.Builder ebb = EscrowBegan.newBuilder();
				ebb.setAmount(cashToSend);
				ebb.setEscrowId(escrowId);
				mina.getCCM().getCCWithId(nodeId).sendMessage("EscrowBegan", ebb.build());
				synchronized (BuyMgr.this) {
					Account a = new Account();
					a.addRecentAs(asMap.get(nodeId));
					a.balance += cashToSend;
					accounts.put(nodeId, a);
				}
			}
		};
		a.addContingentAttempt(onAcctCreation);
		mina.getEscrowMgr().setupEscrowAccount(escrowProvId, a);
	}

	/**
	 * @syncpriority 140
	 */
	public void closeAccount(String nodeId, Attempt onClose) {
		boolean doIt = false;
		synchronized (this) {
			doIt = (accounts.containsKey(nodeId) && !onAcctCloseAttempts.containsKey(nodeId));
		}
		if (doIt) {
			ControlConnection cc = mina.getCCM().getCCWithId(nodeId);
			if (cc != null) {
				synchronized (this) {
					onAcctCloseAttempts.put(nodeId, onClose);
				}
				CloseAcct ca = CloseAcct.newBuilder().build();
				cc.sendMessage("CloseAcct", ca);
				sentBid(nodeId, 0);
			}
		}
	}

	public synchronized boolean accountIsClosing(String nodeId) {
		return onAcctCloseAttempts.containsKey(nodeId);
	}

	public void accountClosed(String nodeId, byte[] currencyToken) {
		Account acct;
		synchronized (this) {
			acct = accounts.remove(nodeId);
		}
		if (acct == null)
			log.error("Received acctclosed from " + nodeId + ", but I have no registered account");
		else {
			try {
				double val = mina.getCurrencyClient().depositToken(currencyToken,
						"Balance returned from node " + nodeId);
				if ((val - acct.balance) < 0) {
					// TODO Something more serious here
					log.error("ERROR: balance mismatch when closing acct with " + nodeId + ": I say " + acct.balance
							+ ", he gave me " + val);
				}
			} catch (CurrencyException e) {
				log.error("Error when depositing token from " + nodeId, e);
			}
			Attempt onClose = onAcctCloseAttempts.remove(nodeId);
			if (onClose != null)
				onClose.succeeded();
		}
	}

	public void openBidding(final String sellerNodeId, final double bidAmount, final Attempt... onConfirmedBids) {
		// Check the auction state - we might not be able to bid yet, if so,
		// wait until we can
		synchronized (this) {
			AuctionState as = asMap.get(sellerNodeId);
			if (as == null)
				throw new SeekInnerCalmException();

			// If we're already waiting for a confirmed bid, just add our
			// attempt, it'll be called when we're done
			if (onConfirmedBidAttempts.containsKey(sellerNodeId)) {
				List<Attempt> list = onConfirmedBidAttempts.get(sellerNodeId);
				for (Attempt a : onConfirmedBids) {
					list.add(a);
				}
				return;
			}

			// Make sure we're allowed to send the bid now
			if (as.getBidsOpen() > 0) {
				Date bidsOpenTime = new Date(as.getTimeReceived().getTime() + as.getBidsOpen());
				if (bidsOpenTime.after(now())) {
					// Not allowed to open bids yet - call us back when we can
					long msUntilBid = msUntil(bidsOpenTime);
					log.debug("Waiting " + msUntilBid + "ms to open bid to " + sellerNodeId);
					mina.getExecutor().schedule(new CatchingRunnable() {
						public void doRun() throws Exception {
							openBidding(sellerNodeId, bidAmount, onConfirmedBids);
						}
					}, msUntilBid, TimeUnit.MILLISECONDS);
					return;
				}
			}

			// Take note of the attempt
			onConfirmedBidAttempts.put(sellerNodeId, new ArrayList<Attempt>());
			List<Attempt> list = onConfirmedBidAttempts.get(sellerNodeId);
			for (Attempt a : onConfirmedBids) {
				list.add(a);
			}
		}
		ControlConnection cc = mina.getCCM().getCCWithId(sellerNodeId);
		if (cc == null) {
			log.error("Not opening bidding to " + sellerNodeId + " - no connection");
			onConfirmedBidAttempts.remove(sellerNodeId);
			for (Attempt a : onConfirmedBids) {
				a.failed();
			}
			return;
		}
		// Let's get this party started
		Bid bidMsg = Bid.newBuilder().setAmount(bidAmount).build();
		cc.sendMessage("Bid", bidMsg);
		sentBid(sellerNodeId, bidAmount);
	}

	/**
	 * An auction has moved on - update the bids
	 */
	public synchronized void bidUpdate(String fromNodeId, BidUpdate bu) {
		AuctionState as = asMap.get(fromNodeId);
		if (as == null) {
			log.error("Received bidupdate from " + fromNodeId + ", but I have no auctionstate");
			return;
		}
		as.setBids(mungeBids(as, bu));
	}

	private List<ReceivedBid> mungeBids(AuctionState as, BidUpdate bu) {
		Map<String, ReceivedBid> map = new HashMap<String, ReceivedBid>();
		for (ReceivedBid bid : as.getBids()) {
			map.put(bid.getListenerId(), bid);
		}
		for (int i = 0; i < bu.getListenerIdCount(); i++) {
			ReceivedBid.Builder bb = ReceivedBid.newBuilder();
			String lId = bu.getListenerId(i);
			double bidAmt = bu.getBidAmount(i);
			bb.setListenerId(lId);
			bb.setBid(bidAmt);
			map.put(lId, bb.build());
		}
		ArrayList<ReceivedBid> list = new ArrayList<ReceivedBid>(map.size());
		list.addAll(map.values());
		Collections.sort(list, new ReceivedBidComparator());
		return list;
	}

	public void newAuctionState(final String nodeId, AuctionState as) {
		// DEBUG
		log.debug("BuyMgr got new auctionstate (tid: " + Thread.currentThread().getId() + ")");
		as.setTimeReceived(now());
		List<Attempt> attempts = null;
		boolean gotConfirmedBid;
		synchronized (this) {
			// Check our quoted bid, check it conforms to the last one we sent
			AuctionState oldAs = asMap.get(nodeId);
			if (oldAs != null) {
				if(AuctionState.INDEX_MOD.gte(oldAs.getIndex(), as.getIndex())) {
					// This auctionstate isn't newer than what we have, ignore
					return;
				}
				double myLastBid = oldAs.getLastSentBid();
				if (myLastBid > 0) {
					String myTok = as.getYouAre();
					if (myTok == null) {
						// I am not in this auction result, and I expect to be -
						// probably bidding had just finished as our bid arrived
						if (onConfirmedBidAttempts.get(nodeId).size() > 0) {
							final double openingBid = myLastBid;
							List<Attempt> myAttempts = onConfirmedBidAttempts.remove(nodeId);
							final Attempt[] myAttemptArr = new Attempt[myAttempts.size()];
							myAttempts.toArray(myAttemptArr);
							int openBidTime = as.getBidsOpen();
							log.debug("Got AS from " + nodeId
									+ " which I am not in, but expected to be... opening bidding again in "
									+ openBidTime + "ms");
							mina.getExecutor().schedule(new CatchingRunnable() {
								public void doRun() throws Exception {
									openBidding(nodeId, openingBid, myAttemptArr);
								}
							}, openBidTime, TimeUnit.MILLISECONDS);
						}
					} else {
						double quotedBid = as.getMyBid();
						if ((quotedBid - myLastBid) != 0) {
							log.error("ERROR: node " + nodeId + " quoted my last bid as " + quotedBid + ", but it was "
									+ myLastBid + " (tid: " + Thread.currentThread().getId() + ")");
							// TODO Something much more serious here
						}
					}
					as.setLastSentBid(oldAs.getLastSentBid());
				}
			}
			// Keep track of this AS
			if (accounts.containsKey(nodeId))
				accounts.get(nodeId).addRecentAs(as);
			attempts = onConfirmedBidAttempts.remove(nodeId);
			gotConfirmedBid = (as.getMyBid() > 0);
			asMap.put(nodeId, as);
		}
		if (gotConfirmedBid && attempts != null) {
			// We've got a confirmed bid in this auction state - if we've
			// got attempts waiting on such a thing, fire them
			for (Attempt a : attempts) {
				a.succeeded();
			}
		}
	}

	public void sentBid(String nodeId, double bid) {
		synchronized (this) {
			AuctionState as = asMap.get(nodeId);
			as.setLastSentBid(bid);
		}
	}

	public String getBestPaymentMethod(Agorics theirAgs) {
		String[] theirMethods = theirAgs.getAcceptPaymentMethods().split(",");
		// If they accept an escrow provider that we're already connected to, use that
		// Otherwise, if they accept an escrow provider that we also accept, use that
		// Otherwise use upfront if we support it
		for (String method : theirMethods) {
			if (method.startsWith("escrow:")) {
				String escrowProvNodeId = method.substring(7);
				if (mina.getEscrowMgr().isAcceptableEscrowProvider(escrowProvNodeId)
						&& mina.getCCM().haveRunningOrPendingCCTo(escrowProvNodeId))
					return method;
			}
		}
		for (String method : theirMethods) {
			if (method.startsWith("escrow:")) {
				String escrowProvNodeId = method.substring(7);
				if (mina.getEscrowMgr().isAcceptableEscrowProvider(escrowProvNodeId))
					return method;
			}
		}
		boolean gotUpfront = false;
		for (String method : theirMethods) {
			if (method.equals("upfront"))
				gotUpfront = true;
		}
		if (!gotUpfront)
			return null;
		for (String method : mina.getCurrencyClient().getAcceptPaymentMethods().split(",")) {
			if (method.equals("upfront"))
				return "upfront";
		}
		return null;
	}

	public synchronized void gotAgorics(String nodeId, Agorics agorics) {
		agMap.put(nodeId, agorics);
	}

	public void receivedPage(String fromNodeId, int statusIdx, long pageLen) {
		synchronized (this) {
			Account acct = accounts.get(fromNodeId);
			if (acct == null) {
				log.error("ERROR: received page from " + fromNodeId + " with status index " + statusIdx
						+ ", but I have no account with that node");
				// TODO Something much more serious here
				return;

			}
			AuctionState as = acct.getAs(statusIdx);
			if (as == null) {
				log.error("ERROR: received page from " + fromNodeId + " with status index " + statusIdx
						+ ", but I have no record of that status!");
				// TODO Something much more serious here
				return;
			}
			// Deduct from our account balance. All prices are per megabyte.
			double pageCost = ((double) pageLen / (1024 * 1024)) * as.getMyBid();
			acct.balance -= pageCost;
		}
		checkAcctBalance(fromNodeId);
	}

	private void checkAcctBalance(String fromNodeId) {
		// Make sure we have enough in our account for 30 secs' worth of
		// reception for all streams (or the rest of the stream if less)
		long bytesRequired = 0;
		ControlConnection cc = mina.getCCM().getCCWithId(fromNodeId);
		if (cc == null) {
			// Connection has died, but page made it through gasping and
			// wheezing before everything got killed
			return;
		}
		LCPair[] lcps = cc.getLCPairs();
		for (LCPair lcp : lcps) {
			int bytesForTime = lcp.getFlowRate() * mina.getConfig().getBalanceBufferTime();
			PageBuffer pb = lcp.getSM().getPageBuffer();
			long bytesForRestOfStream = (pb.getTotalPages() - pb.getLastContiguousPage()) * pb.getAvgPageSize();
			bytesRequired += Math.min(bytesForTime, bytesForRestOfStream);
		}
		double myBid;
		double balance;
		synchronized (this) {
			myBid = asMap.get(fromNodeId).getMyBid();
			balance = accounts.get(fromNodeId).balance;
		}
		double endsRequired = ((double) bytesRequired / (1024 * 1024)) * myBid;
		if (balance < endsRequired) {
			byte[] token;
			try {
				token = mina.getCurrencyClient().withdrawToken(endsRequired,
						"Topping up account with node " + fromNodeId);
			} catch (CurrencyException e) {
				log.error("Error withdrawing token of value " + endsRequired + " trying to top up account with "
						+ fromNodeId);
				return;
			}
			synchronized (this) {
				accounts.get(fromNodeId).balance += endsRequired;
			}
			TopUp tu = TopUp.newBuilder().setCurrencyToken(ByteString.copyFrom(token)).build();
			cc.sendMessage("TopUp", tu);
		}
	}

	public void gotPayUpDemand(String fromNodeId, double toldBalance) {
		gotPayUpDemand(fromNodeId, toldBalance, false);
	}

	private void gotPayUpDemand(final String fromNodeId, final double toldBalance, boolean tryingAgain) {
		synchronized (this) {
			// TODO For now we're just accepting all payment demands, even if we
			// don't agree with them - obviously need to change this once we've
			// figured out why the mismatches occur
			double myBalance = accounts.get(fromNodeId).balance;
			if ((toldBalance - myBalance) != 0) {
				log.error("ERROR: got mismatch in payment demand from " + fromNodeId + ": they say " + toldBalance
						+ ", I say " + myBalance);
				// if (!tryingAgain) {
				// int catchupSecs = mina.getConfig().getPayUpCatchUpTime();
				// log.info("Mismatch in payment demand from " + fromNodeId +
				// ": waiting " + catchupSecs + "s to catch up");
				// // This might be down to some pages still being in flight -
				// // back off to let them arrive, then calculate again
				// mina.getExecutor().schedule(new CatchingRunnable() {
				// public void doRun() throws Exception {
				// gotPayUpDemand(fromNodeId, toldBalance, true);
				// }
				// }, catchupSecs, TimeUnit.SECONDS);
				// } else {
				// // We've waited for things to catch up, and there's still a
				// // discrepancy...
				// // TODO What should we do here...?
				// log.error("ERROR: got mismatch in payment demand from " +
				// fromNodeId + ": they say " + toldBalance + ", I say " +
				// myBalance);
				// return;
				// }
			}
			// TODO This will just pay everyone whatever they demand...
			accounts.get(fromNodeId).balance = toldBalance;
		}
		checkAcctBalance(fromNodeId);
	}

	public synchronized void notifyDeadConnection(String nodeId) {
		// TODO Non-sucking payment methods
		log.debug("BuyMgr cleaning up after " + nodeId);
		asMap.remove(nodeId);
		agMap.remove(nodeId);
	}

	/**
	 * Notifies that we have had a minimum charge applied to us, as we were the top bidder and weren't receiving data
	 * fast enough
	 */
	public synchronized void minCharge(String nodeId, double charge) {
		// You trying to jack me, vato?
		Account acct = accounts.get(nodeId);
		AuctionState as = acct.getMostRecentAs();
		if (!as.amITopBid()) {
			log.error("Node " + nodeId + " applied mincharge to me, but I am not top bidder!");
			// TODO Something much more serious here
			return;
		}
		long msSinceAuction = msElapsedSince(as.timeReceived);
		long minBytes = (long) (agMap.get(nodeId).getMinTopRate() * (msSinceAuction / 1000f));
		long shortfallBytes = minBytes - acct.bytesRecvdOnMostRecentAs;
		double calcCharge = (shortfallBytes / (1024d * 1024d)) * as.getMyBid();
		if (calcCharge < charge) {
			// TODO Wiggle room? What should we do here?
			log.error("Node " + nodeId + " demanded unfair mincharge; he says " + charge + ", I say " + calcCharge);
			return;
		}
		acct.balance -= charge;
	}

	private synchronized void nukeAccount(String nodeId) {
		accounts.remove(nodeId);
	}

	class Account {
		double balance = 0;
		long bytesRecvdOnMostRecentAs = 0;
		/** Keep track of the last few auction states we've received */
		private Map<Integer, AuctionState> recentAs = new HashMap<Integer, AuctionState>();
		private LinkedList<Integer> recentAsIdxs = new LinkedList<Integer>();

		void addRecentAs(AuctionState as) {
			if (recentAs.size() >= AUCTION_STATE_HISTORY) {
				int oldestAsIdx = recentAsIdxs.remove();
				recentAs.remove(oldestAsIdx);
			}
			recentAs.put(as.getIndex(), as);
			recentAsIdxs.add(as.getIndex());
		}

		AuctionState getAs(int asIdx) {
			return recentAs.get(asIdx);
		}

		AuctionState getMostRecentAs() {
			if (recentAsIdxs.size() == 0)
				return null;
			return recentAs.get((int) recentAsIdxs.getLast());
		}
	}
}
