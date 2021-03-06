package com.robonobo.mina.message.handlers;

import java.io.IOException;
import java.io.InputStream;

import com.robonobo.mina.message.MessageHolder;
import com.robonobo.mina.message.proto.MinaProtocol.ReqConn;

public class ReqConnHandler extends AbstractMessageHandler {
	@Override
	public void handleMessage(MessageHolder mh) {
		ReqConn rc = (ReqConn) mh.getMessage();
		if(rc.getToNodeId().equals(mina.getMyNodeId())) {
			// Make a ControlConnection to this host
			// TODO: #CC limits
			mina.getCCM().initiateNewCC(rc.getFromNode(), null, false, null);
		}
		else if(mina.getConfig().isSupernode())
			mina.getCCM().sendOrForwardMessageTo("ReqConn", rc, rc.getToNodeId());
		else
			log.error(mh.getFromCC()+" sent me reqconn: "+rc+", but I am not a supernode");
	}

	@Override
	public ReqConn parse(String cmdName, InputStream is) throws IOException {
		return ReqConn.newBuilder().mergeFrom(is).build();
	}
}
