package com.robonobo.mina.instance;

import java.util.HashMap;
import java.util.Map;

import com.robonobo.mina.message.handlers.AbandonEscrowHandler;
import com.robonobo.mina.message.handlers.AcctClosedHandler;
import com.robonobo.mina.message.handlers.AdvEscrowHandler;
import com.robonobo.mina.message.handlers.AdvSourceHandler;
import com.robonobo.mina.message.handlers.AuctionResultHandler;
import com.robonobo.mina.message.handlers.BeginEscrowHandler;
import com.robonobo.mina.message.handlers.BidHandler;
import com.robonobo.mina.message.handlers.BidUpdateHandler;
import com.robonobo.mina.message.handlers.ByeHandler;
import com.robonobo.mina.message.handlers.CloseAcctHandler;
import com.robonobo.mina.message.handlers.DontWantSourceHandler;
import com.robonobo.mina.message.handlers.EscrowBeganHandler;
import com.robonobo.mina.message.handlers.EscrowFinishedHandler;
import com.robonobo.mina.message.handlers.EscrowLockedHandler;
import com.robonobo.mina.message.handlers.EscrowPaidHandler;
import com.robonobo.mina.message.handlers.GotSourceHandler;
import com.robonobo.mina.message.handlers.HelloHandler;
import com.robonobo.mina.message.handlers.LockEscrowHandler;
import com.robonobo.mina.message.handlers.MessageHandler;
import com.robonobo.mina.message.handlers.MinChargeHandler;
import com.robonobo.mina.message.handlers.NoBidHandler;
import com.robonobo.mina.message.handlers.PayUpHandler;
import com.robonobo.mina.message.handlers.PingHandler;
import com.robonobo.mina.message.handlers.PongHandler;
import com.robonobo.mina.message.handlers.QueryEscrowHandler;
import com.robonobo.mina.message.handlers.ReqConnHandler;
import com.robonobo.mina.message.handlers.ReqPageHandler;
import com.robonobo.mina.message.handlers.ReqSourceStatusHandler;
import com.robonobo.mina.message.handlers.SourceStatusHandler;
import com.robonobo.mina.message.handlers.StartSourceHandler;
import com.robonobo.mina.message.handlers.StopSourceHandler;
import com.robonobo.mina.message.handlers.StreamStatusHandler;
import com.robonobo.mina.message.handlers.TopUpHandler;
import com.robonobo.mina.message.handlers.UnAdvSourceHandler;
import com.robonobo.mina.message.handlers.WantSourceHandler;

public class MessageMgr {
	private MinaInstance mina;
	private Map<String, MessageHandler> handlersByName = new HashMap<String, MessageHandler>();
	
	public MessageMgr(MinaInstance mina) {
		this.mina = mina;
		registerDefaultHandlers();
	}
	
	public void registerHandler(String msgName, MessageHandler handler) {
		handlersByName.put(msgName, handler);
	}
	
	public MessageHandler getHandler(String msgName) {
		return handlersByName.get(msgName);
	}
	
	protected void registerDefaultHandlers() {
		MessageHandler mh;
		
		mh = new AbandonEscrowHandler();
		mh.setMina(mina);
		registerHandler("AbandonEscrow", mh);
		
		mh = new AcctClosedHandler();
		mh.setMina(mina);
		registerHandler("AcctClosed", mh);
		
		mh = new AdvEscrowHandler();
		mh.setMina(mina);
		registerHandler("AdvEscrow", mh);
		
		mh = new AdvSourceHandler();
		mh.setMina(mina);
		registerHandler("AdvSource", mh);
		
		mh = new AuctionResultHandler();
		mh.setMina(mina);
		registerHandler("AuctionResult", mh);
		
		mh = new BeginEscrowHandler();
		mh.setMina(mina);
		registerHandler("BeginEscrow", mh);
		
		mh = new BidHandler();
		mh.setMina(mina);
		registerHandler("Bid", mh);
		
		mh = new BidUpdateHandler();
		mh.setMina(mina);
		registerHandler("BidUpdate", mh);
		
		mh = new ByeHandler();
		mh.setMina(mina);
		registerHandler("Bye", mh);
		
		mh = new CloseAcctHandler();
		mh.setMina(mina);
		registerHandler("CloseAcct", mh);
		
		mh = new DontWantSourceHandler();
		mh.setMina(mina);
		registerHandler("DontWantSource", mh);
		
		mh = new EscrowBeganHandler();
		mh.setMina(mina);
		registerHandler("EscrowBegan", mh);
		
		mh = new EscrowFinishedHandler();
		mh.setMina(mina);
		registerHandler("EscrowFinished", mh);
		
		mh = new EscrowLockedHandler();
		mh.setMina(mina);
		registerHandler("EscrowLocked", mh);
		
		mh = new EscrowPaidHandler();
		mh.setMina(mina);
		registerHandler("EscrowPaid", mh);
		
		mh = new GotSourceHandler();
		mh.setMina(mina);
		registerHandler("GotSource", mh);
		
		mh = new HelloHandler();
		mh.setMina(mina);
		registerHandler("Hello", mh);
		
		mh = new LockEscrowHandler();
		mh.setMina(mina);
		registerHandler("LockEscrow", mh);
		
		mh = new MinChargeHandler();
		mh.setMina(mina);
		registerHandler("MinCharge", mh);
		
		mh = new NoBidHandler();
		mh.setMina(mina);
		registerHandler("NoBid", mh);
		
		mh = new PayUpHandler();
		mh.setMina(mina);
		registerHandler("PayUp", mh);
		
		mh = new PingHandler();
		mh.setMina(mina);
		registerHandler("Ping", mh);
		
		mh = new PongHandler();
		mh.setMina(mina);
		registerHandler("Pong", mh);
		
		mh = new QueryEscrowHandler();
		mh.setMina(mina);
		registerHandler("QueryEscrow", mh);
		
		mh = new ReqConnHandler();
		mh.setMina(mina);
		registerHandler("ReqConn", mh);
		
		mh = new ReqPageHandler();
		mh.setMina(mina);
		registerHandler("ReqPage", mh);
		
		mh = new ReqSourceStatusHandler();
		mh.setMina(mina);
		registerHandler("ReqSourceStatus", mh);
		
		mh = new SourceStatusHandler();
		mh.setMina(mina);
		registerHandler("SourceStatus", mh);
		
		mh = new StartSourceHandler();
		mh.setMina(mina);
		registerHandler("StartSource", mh);
		
		mh = new StopSourceHandler();
		mh.setMina(mina);
		registerHandler("StopSource", mh);
		
		mh = new StreamStatusHandler();
		mh.setMina(mina);
		registerHandler("StreamStatus", mh);
		
		mh = new TopUpHandler();
		mh.setMina(mina);
		registerHandler("TopUp", mh);
		
		mh = new UnAdvSourceHandler();
		mh.setMina(mina);
		registerHandler("UnAdvSource", mh);
		
		mh = new WantSourceHandler();
		mh.setMina(mina);
		registerHandler("WantSource", mh);
	}
}
