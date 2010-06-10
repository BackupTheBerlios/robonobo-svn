package com.robonobo.wang.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.wang.beans.Coin;
import com.robonobo.wang.beans.CoinList;
import com.robonobo.wang.beans.DenominationPrivate;
import com.robonobo.wang.client.LucreFacade;
import com.robonobo.wang.proto.WangProtocol.CoinListMsg;
import com.robonobo.wang.proto.WangProtocol.CoinMsg;
import com.robonobo.wang.proto.WangProtocol.DepositStatusMsg;
import com.robonobo.wang.proto.WangProtocol.DepositStatusMsg.Status;

public class DepositCoinsServlet extends WangServlet {
	private LucreFacade lucre;
	private Map<Integer, DenominationPrivate> denomPrivs;

	@Override
	public void init() throws ServletException {
		super.init();
		lucre = new LucreFacade();
		try {
			List<DenominationPrivate> denoms = denomDao.getDenomsPrivate();
			denomPrivs = new HashMap<Integer, DenominationPrivate>();
			for (DenominationPrivate denom : denoms) {
				denomPrivs.put(denom.getDenom(), denom);
			}
		} catch (DAOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserAccount user = getAuthUser(req, resp);
		if (user == null) {
			send401(req, resp);
			return;
		}
		resp.setContentType("application/data");
		CoinListMsg.Builder clBldr = CoinListMsg.newBuilder();
		readFromInput(clBldr, req);
		CoinListMsg cl = clBldr.build();
		double coinValue = 0;
		DepositStatusMsg.Builder dBldr = DepositStatusMsg.newBuilder();
		dBldr.setStatus(Status.OK);
		try {
			List<String> deposCoins = new ArrayList<String>();
			for (CoinMsg coinMsg : cl.getCoinList()) {
				DenominationPrivate denom = denomPrivs.get(coinMsg.getDenom());
				Coin coin = new Coin(coinMsg);
				if (denom == null)
					throw new ServletException("Malformed coin, no denomination");
				if (!lucre.verifyCoin(denom, coin) || doubleSpendDao.isDoubleSpend(coinMsg.getCoinId())) {
					dBldr.setStatus(Status.Error);
					dBldr.addBadCoinId(coinMsg.getCoinId());
					continue;
				}
				coinValue += getDenomValue(coin.getDenom());
				deposCoins.add(coinMsg.getCoinId());
			}
			if (dBldr.getStatus() == Status.OK) {
				for (String coinId : deposCoins) {
					doubleSpendDao.add(coinId);
				}
				log.info("User " + user.getEmail() + " deposited " + deposCoins + " coins worth " + WANG_CHAR
						+ coinValue);
				try {
					UserAccount lockUser = uaDao.getAndLockUserAccount(user.getEmail());
					lockUser.setBalance(lockUser.getBalance() + coinValue);
					uaDao.putUserAccount(lockUser);
				} catch (DAOException e) {
					throw new ServletException(e);
				}
			} else
				log.warn("User " + user.getEmail() + " got bad coin error while attempting to deposit coins");
		} catch (DAOException e) {
			throw new ServletException(e);
		}
		writeToOutput(dBldr.build(), resp);
		resp.setStatus(HttpServletResponse.SC_OK);
	}

	private double getDenomValue(Integer denom) {
		return Math.pow(2, denom);
	}
}
