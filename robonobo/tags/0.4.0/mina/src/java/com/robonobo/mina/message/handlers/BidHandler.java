package com.robonobo.mina.message.handlers;

import java.io.IOException;
import java.io.InputStream;

import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.proto.MinaProtocol.Bid;
import com.robonobo.mina.network.ControlConnection;

public class BidHandler extends AbstractMessageHandler {

	@Override
	public void handleMessage(MessageHolder mh) {
		Bid b = (Bid) mh.getMessage();
		ControlConnection cc = mh.getFromCC();
		if(mina.getSellMgr().haveActiveAccount(cc.getNodeId()))
			mina.getSellMgr().bid(cc.getNodeId(), b.getAmount());
		else
			mina.getSellMgr().cmdPendingOpenAccount(mh);
	}

	@Override
	public Bid parse(String cmdName, InputStream is) throws IOException {
		return Bid.newBuilder().mergeFrom(is).build();
	}

}
