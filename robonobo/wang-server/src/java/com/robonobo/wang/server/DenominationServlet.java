package com.robonobo.wang.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.wang.beans.DenominationPublic;
import com.robonobo.wang.proto.WangProtocol.DenominationListMsg;

public class DenominationServlet extends WangServlet {
	private List<DenominationPublic> pubDenoms;

	public DenominationServlet() {
	}

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			pubDenoms = denomDao.getDenomsPublic();
		} catch (DAOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if(getAuthUser(req, resp) == null) {
			send401(req, resp);
			return;
		}
		resp.setContentType("application/data");
		DenominationListMsg.Builder bldr = DenominationListMsg.newBuilder();
		for (DenominationPublic denom : pubDenoms) {
			bldr.addDenomination(denom.toMsg());
		}
		writeToOutput(bldr.build(), resp);
		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
