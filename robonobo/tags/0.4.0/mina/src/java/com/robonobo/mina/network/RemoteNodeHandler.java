package com.robonobo.mina.network;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;

import com.robonobo.common.async.PushDataChannel;
import com.robonobo.common.async.PushDataReceiver;
import com.robonobo.common.dlugosz.Dlugosz;
import com.robonobo.common.io.ByteBufferInputStream;
import com.robonobo.core.api.proto.CoreApi.EndPoint;
import com.robonobo.mina.instance.MinaInstance;
import com.robonobo.mina.message.HelloHelper;
import com.robonobo.mina.message.handlers.HelloHandler;
import com.robonobo.mina.message.proto.MinaProtocol.Hello;
import com.robonobo.mina.util.Attempt;
import com.robonobo.mina.util.MinaConnectionException;

/**
 * This takes an incoming connection and deals with the handshake before creating a ControlConnection
 */
public class RemoteNodeHandler implements PushDataReceiver {
	private MinaInstance mina;
	private Log log;
	private PushDataChannel dataChan;
	private int messageMaxSize;
	private Attempt handleAttempt;
	private StreamConnectionFactory connectionFactory;
	private EndPoint myEp;
	private EndPoint theirEp;
	ByteBufferInputStream incoming = new ByteBufferInputStream();
	private int msgNameLength = -1;
	private String msgName = null;
	private int serialMsgLength = -1;
	private HelloHandler handler;

	/**
	 * @param byteChan The channel for communicating via the underlying socket (or whatever)
	 * @param myEp The endpoint for this end of the controlconnection
	 * @param theirEp The endpoint for the other end of the controlconnection
	 * @param connectionFactory This will be passed through to the new ControlConnection
	 */
	public RemoteNodeHandler(MinaInstance mina, PushDataChannel dataChan, EndPoint myEp, EndPoint theirEp, StreamConnectionFactory connectionFactory) {
		this.mina = mina;
		log = mina.getLogger(getClass());
		this.dataChan = dataChan;
		this.myEp = myEp;
		this.theirEp = theirEp;
		this.connectionFactory = connectionFactory;
		handler = (HelloHandler) mina.getMessageMgr().getHandler("Hello");
		log.debug("Received new connection attempt from "+dataChan);
	}

	public void handle() {
		messageMaxSize = mina.getConfig().getMessageMaxSize();
		handleAttempt = new HandleAttempt(mina.getConfig().getMessageTimeout());
		handleAttempt.start();
		dataChan.setDataReceiver(this);
	}

	public void close() {
		handleAttempt.cancel();
		dataChan.close();
	}

	public void providerClosed() {
		close();
	}
	
	public void receiveData(ByteBuffer buf, Object ignore) throws IOException {
		incoming.addBuffer(buf);
		// Now we see what we can read
		boolean finished = false;
		do {
			if (serialMsgLength >= 0) {
				// We're reading our serialized cmd
				if (incoming.available() >= serialMsgLength) {
					// Parse our bytes into a protocol buffer msg - the protobuf parser will read the entire stream, so fake the eof
					incoming.setPretendEof(serialMsgLength);
					Hello msg = handler.parse(msgName, incoming);
					incoming.clearPretendEof();
					handleAttempt.succeeded();
					// When we set this to null, any incoming data will pile up until the controlconnection sets it again
					dataChan.setDataReceiver(null);
					HelloHelper hh = new HelloHelper(msg, dataChan, myEp, theirEp, incoming);
					log.info("Received Hello from new connection " + msg.getNode().getId());
					try {
						mina.getCCM().makeCCFrom(hh, connectionFactory);
					} catch (MinaConnectionException e) {
						throw new IOException("Caught MinaConnectionException: "+e.getMessage());
					}
					return;
				} else
					finished = true;
			} else if (msgName != null) {
				// We're reading our serialized msg length
				if (Dlugosz.startsWithCompleteNum(incoming))
					serialMsgLength = (int) Dlugosz.readLong(incoming);
				else
					finished = true;
			} else if (msgNameLength >= 0) {
				// We're reading our msg name
				if (incoming.available() >= msgNameLength) {
					byte[] arr = new byte[msgNameLength];
					incoming.read(arr);
					msgName = new String(arr);
					if(!msgName.equals("Hello")) {
						log.error("Node "+theirEp+" sent invalid first message: "+msgName);
						close();
						return;
					}
				} else
					finished = true;
			} else {
				// We're reading our msg name length
				if (Dlugosz.startsWithCompleteNum(incoming))
					msgNameLength = (int) Dlugosz.readLong(incoming);
				else
					finished = true;
			}					
		} while (!finished);
	}
	
	private class HandleAttempt extends Attempt {
		HandleAttempt(int timeoutSecs) {
			super(mina, timeoutSecs*1000, "ConnectHandleAttempt");
		}

		public void onTimeout() {
			log.error("Timeout waiting for remote node " + theirEp + ": killing thread");
			close();
		}
	}
}
