package com.robonobo.mina.instance;

import java.util.Collection;

import com.robonobo.common.concurrent.Batcher;
import com.robonobo.mina.message.proto.MinaProtocol.AdvSource;

public class StreamAdvertiser extends Batcher<String> {
	MinaInstance mina;

	public StreamAdvertiser(MinaInstance mina) {
		super(mina.getConfig().getSourceRequestBatchTime(), mina.getExecutor());
		this.mina = mina;
	}

	/**
	 * @syncpriority 140
	 */
	public void advertiseStream(String streamId) {
		// If we don't yet have a connection to a supernode, don't advertise, as
		// when we do connect we will send an advert for all [re]broadcasting
		// streams, and we'd like to avoid sending duplicate streams on startup
		if (mina.getCCM().haveSupernode())
			add(streamId);
	}

	@Override
	protected void runBatch(Collection<String> streamIds) {
		AdvSource as = AdvSource.newBuilder().addAllStreamId(streamIds).build();
		mina.getCCM().sendMessageToNetwork("AdvSource", as);
	}

}
