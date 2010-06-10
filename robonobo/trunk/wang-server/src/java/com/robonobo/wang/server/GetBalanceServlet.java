package com.robonobo.wang.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.wang.proto.WangProtocol.BalanceMsg;

public class GetBalanceServlet extends WangServlet {
	public GetBalanceServlet() {
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserAccount ua = getAuthUser(req, resp);
		if(ua == null) {
			send401(req, resp);
			return;
		}
		resp.setContentType("application/data");
		writeToOutput(BalanceMsg.newBuilder().setAmount(ua.getBalance()).build(), resp);
		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
