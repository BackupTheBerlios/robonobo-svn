package com.robonobo.wang.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.wang.beans.BlindedCoin;
import com.robonobo.wang.beans.CoinRequestPublic;
import com.robonobo.wang.beans.DenominationPrivate;
import com.robonobo.wang.client.LucreFacade;
import com.robonobo.wang.proto.WangProtocol.BlindedCoinListMsg;
import com.robonobo.wang.proto.WangProtocol.CoinRequestListMsg;
import com.robonobo.wang.proto.WangProtocol.CoinRequestMsg;
import com.robonobo.wang.proto.WangProtocol.BlindedCoinListMsg.Status;

public class GetCoinsServlet extends WangServlet {
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
		CoinRequestListMsg.Builder crlBldr = CoinRequestListMsg.newBuilder();
		readFromInput(crlBldr, req);
		CoinRequestListMsg crl = crlBldr.build();
		double coinValue = 0;
		for (CoinRequestMsg coinReq : crl.getCoinRequestList()) {
			coinValue += getDenomValue(coinReq.getDenom());
		}
		BlindedCoinListMsg.Builder blBldr = BlindedCoinListMsg.newBuilder();
		try {
			UserAccount lockUser = uaDao.getAndLockUserAccount(user.getEmail());
			if (lockUser.getBalance() < coinValue) {
				blBldr.setStatus(Status.InsufficientWang);
			} else {
				for (CoinRequestMsg coinReq : crl.getCoinRequestList()) {
					DenominationPrivate denom = denomPrivs.get(coinReq.getDenom());
					BlindedCoin bc = lucre.signCoinRequest(denom, new CoinRequestPublic(coinReq));
					blBldr.addCoin(bc.toMsg());
				}
				blBldr.setStatus(Status.OK);
				lockUser.setBalance(lockUser.getBalance() - coinValue);
				uaDao.putUserAccount(lockUser);
			}
		} catch (DAOException e) {
			throw new ServletException(e);
		}
		writeToOutput(blBldr.build(), resp);
		resp.setStatus(HttpServletResponse.SC_OK);
		log.info("User "+user.getEmail()+" withdrew "+crl.getCoinRequestCount()+" coins worth "+WANG_CHAR+coinValue);
	}

	private double getDenomValue(Integer denom) {
		return Math.pow(2, denom);
	}

}
