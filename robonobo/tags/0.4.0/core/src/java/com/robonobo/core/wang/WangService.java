package com.robonobo.core.wang;

import static com.robonobo.common.util.TimeUtil.now;
import static com.robonobo.common.util.TimeUtil.timeInFuture;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.CurrencyClient;
import com.robonobo.core.api.CurrencyException;
import com.robonobo.core.api.StreamVelocity;
import com.robonobo.core.api.UserPlaylistListener;
import com.robonobo.core.api.model.Playlist;
import com.robonobo.core.api.model.User;
import com.robonobo.core.api.proto.CoreApi.Node;
import com.robonobo.core.service.AbstractRuntimeServiceProvider;
import com.robonobo.wang.WangException;
import com.robonobo.wang.beans.CoinList;
import com.robonobo.wang.client.WangClient;
import com.robonobo.wang.proto.WangProtocol.CoinListMsg;

public class WangService extends AbstractRuntimeServiceProvider implements CurrencyClient {
	RobonoboWangConfig config;
	WangClient client;
	Log log = LogFactory.getLog(getClass());
	double cachedBankBalance = 0;
	Date nextCheckBalanceTime = new Date(0);
	boolean clientStarted = false;

	public WangService() {
		addHardDependency("core.users");
	}

	public String getName() {
		return "Wang banking service";
	}

	public String getProvides() {
		return "core.wang";
	}

	@Override
	public void startup() throws Exception {
		config = (RobonoboWangConfig) getRobonobo().getConfig("wang");
		File coinStoreDir = new File(getRobonobo().getHomeDir(), "coins");
		coinStoreDir.mkdirs();
		config.setCoinStoreDir(coinStoreDir.getAbsolutePath());
		// Start client on successful login
		getRobonobo().getEventService().addUserPlaylistListener(new LoginListener());
	}

	/** Called when we login */
	public void startClient() {
		try {
			if (client != null)
				client.stop();

			User me = getRobonobo().getUsersService().getMyUser();
			config.setAccountEmail(me.getEmail());
			config.setAccountPwd(me.getPassword());
			client = new WangClient(config);
			client.start();
			clientStarted = true;
			fireUpdatedBalance();
		} catch (WangException e) {
			log.error("Caught exception starting wang client", e);
		}
	}

	@Override
	public void shutdown() throws Exception {
		clientStarted = false;
		if (client != null)
			client.stop();
	}

	public boolean isReady() {
		return clientStarted;
	}
	
	public String getAcceptPaymentMethods() {
		// TODO Add in escrow nodes ("escrow:<nodeid>,escrow:<nodeid>") here
		return "upfront";
	}
	
	public Node[] getTrustedEscrowNodes() {
		return new Node[0];
	}
	
	public String currencyUrl() {
		return config.getCurrencyUrl();
	}

	public double getBidIncrement() {
		return Math.pow(2,config.getBidIncrement());
	}

	public double getMinBid() {
		return Math.pow(2,config.getMinBid());
	}

	public int getMinTopRate() {
		return config.getMinTopRate();
	}

	public double getOpeningBalance() {
		return Math.pow(2, config.getOpeningBalance());
	}

	public double getMaxBid(StreamVelocity sv) {
		switch (sv) {
		case LowestCost:
			return Math.pow(2, config.getLowestCostMaxBid());
		case MaxRate:
			return Math.pow(2, config.getMaxRateMaxBid());
		default:
			throw new SeekInnerCalmException();
		}
	}

	public double getBankBalance() throws CurrencyException {
		return getBankBalance(false);
	}

	public double getBankBalance(boolean forceUpdate) throws CurrencyException {
		updateBalanceIfNecessary(forceUpdate);
		return cachedBankBalance;
	}

	public double getOnHandBalance() throws CurrencyException {
		return client.getOnHandBalance();
	}
	
	public double depositToken(byte[] token) throws CurrencyException {
		try {
			CoinListMsg coins = CoinListMsg.parseFrom(token);
			client.putCoins(coins);
			double result = CoinList.totalValue(coins);
			synchronized (this) {
				cachedBankBalance += result;
			}
			fireUpdatedBalance();
			return result;
		} catch (WangException e) {
			throw new CurrencyException(e);
		} catch (IOException e) {
			throw new CurrencyException(e);
		}

	}

	public byte[] withdrawToken(double value) throws CurrencyException {
		try {
			CoinListMsg coins = client.getCoins(value);
			synchronized (this) {
				cachedBankBalance -= CoinList.totalValue(coins);
			}
			fireUpdatedBalance();
			return coins.toByteArray();
		} catch (WangException e) {
			throw new CurrencyException(e);
		}
	}

	/** Done in separate thread for responsiveness */ 
	private void fireUpdatedBalance() {
		getRobonobo().getExecutor().execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				updateBalanceIfNecessary(false);
				getRobonobo().getEventService().fireWangBalanceChanged(cachedBankBalance);
			}
		});
	}

	private void updateBalanceIfNecessary(boolean forceUpdate) {
		synchronized (this) {
			if (forceUpdate || now().after(nextCheckBalanceTime)) {
				try {
					cachedBankBalance = client.getAccurateBankBalance();
				} catch (WangException e) {
					log.error("Error updating bank balance", e);
				}
				nextCheckBalanceTime = timeInFuture(config.getBankBalanceCacheTime() * 1000);
			}
		}
	}

	class LoginListener implements UserPlaylistListener {
		public void loggedIn() {
			startClient();
		}

		public void playlistChanged(Playlist p) {
			// Do nothing
		}

		public void userChanged(User u) {
			// Do nothing
		}
	}
}
