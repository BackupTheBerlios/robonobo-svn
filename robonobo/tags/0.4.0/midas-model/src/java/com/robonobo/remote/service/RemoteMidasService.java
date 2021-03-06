package com.robonobo.remote.service;

import java.io.IOException;
import java.util.List;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import com.robonobo.common.persistence.PersistenceManager;
import com.robonobo.common.remote.RemoteCall;
import com.robonobo.core.api.proto.CoreApi.FriendRequestMsg;
import com.robonobo.core.api.proto.CoreApi.PlaylistMsg;
import com.robonobo.core.api.proto.CoreApi.StreamMsg;
import com.robonobo.core.api.proto.CoreApi.UserMsg;
import com.robonobo.midas.model.MidasFriendRequest;
import com.robonobo.midas.model.MidasInvite;
import com.robonobo.midas.model.MidasPlaylist;
import com.robonobo.midas.model.MidasStream;
import com.robonobo.midas.model.MidasUser;

/**
 * The server end of a remote midas service (client end is RemoteMidasFacade)
 * 
 * @author macavity
 * 
 */
public class RemoteMidasService implements ServerInvocationHandler {
	Connector connector;
	String secret;
	LocalMidasService localService;
	Log log = LogFactory.getLog(getClass());

	/**
	 * @param url The jboss-remoting url on which to listen
	 * @param secret The sekrit string that must be passed with all calls
	 */
	public RemoteMidasService(String url, String secret) throws Exception {
		this.secret = secret;
		log.info("Starting remote midas service on " + url);
		InvokerLocator locator = new InvokerLocator(url);
		localService = LocalMidasService.getInstance();
		connector = new Connector();
		connector.setInvokerLocator(locator.getLocatorURI());
		connector.start();
		connector.addInvocationHandler("midas", this);
	}

	public void shutdown() {
		log.info("Stopping remote midas service");
		connector.stop();
	}

	public Object invoke(InvocationRequest req) throws Throwable {
		Object obj = req.getParameter();
		if (!(obj instanceof RemoteCall)) {
			log.error("Remote invocation with parameter " + obj.getClass().getName());
			throw new IllegalArgumentException("Invalid param");
		}
		RemoteCall params = (RemoteCall) obj;
		if (!secret.equals(params.getSecret())) {
			log.error("Remote invocation with invalid secret '" + params.getSecret() + "'");
			throw new IllegalArgumentException("Invalid secret");
		}
		PersistenceManager.createSession();
		boolean gotError = false;
		try {
			String method = params.getMethodName();
			if (method.equals("getUserByEmail")) {
				return getUserByEmail(params);
			} else if (method.equals("getUserById")) {
				return getUserById(params);
			} else if (method.equals("saveUser")) {
				saveUser(params);
				return null;
			} else if (method.equals("getUserAsVisibleBy")) {
				return getUserAsVisibleBy(params);
			} else if (method.equals("getPlaylistById")) {
				return getPlaylistById(params);
			} else if (method.equals("savePlaylist")) {
				savePlaylist(params);
				return null;
			} else if (method.equals("deletePlaylist")) {
				deletePlaylist(params);
				return null;
			} else if (method.equals("getStreamById")) {
				return getStreamById(params);
			} else if (method.equals("saveStream")) {
				saveStream(params);
				return null;
			} else if (method.equals("deleteStream")) {
				deleteStream(params);
				return null;
			} else if (method.equals("countUsers")) {
				return countUsers();
			} else if (method.equals("createUser")) {
				return createUser(params);
			} else if (method.equals("getAllUsers")) {
				return getAllUsers();
			} else if(method.equals("deleteUser")) {
				deleteUser(params);
				return null;
			} else if (method.equals("createOrUpdateFriendRequest")) {
				return createOrUpdateFriendRequest(params);
			} else if(method.equals("getFriendRequest")) {
				return getFriendRequest(params);
			} else if(method.equals("getPendingFriendRequests")) {
				return getPendingFriendRequests(params);
			} else if(method.equals("ignoreFriendRequest")) {
				ignoreFriendRequest(params);
				return null;
			} else if (method.equals("acceptFriendRequest")) {
				return acceptFriendRequest(params);
			} else if (method.equals("createOrUpdateInvite")) {
				return createOrUpdateInvite(params);
			} else if(method.equals("deleteInvite")) {
				deleteInvite(params);
				return null;
			} else if(method.equals("getInvite")) {
				return getInvite(params);
			} else
				throw new IllegalArgumentException("Invalid method");
		} catch (Exception e) {
			gotError = true;
			throw new Exception(e);
		} finally {
			PersistenceManager.closeSession(gotError);
		}
	}	

	private String acceptFriendRequest(RemoteCall params) throws IOException {
		FriendRequestMsg msg = FriendRequestMsg.newBuilder().mergeFrom((byte[])params.getArg()).build();
		return localService.acceptFriendRequest(new MidasFriendRequest(msg));
	}
	
	private byte[] getFriendRequest(RemoteCall params) {
		MidasFriendRequest fr = localService.getFriendRequest((String) params.getArg());
		return fr.toMsg().toByteArray();
	}
	
	private byte[][] getPendingFriendRequests(RemoteCall params) {
		List<MidasFriendRequest> frList = localService.getPendingFriendRequests((Long) params.getArg());
		byte[][] result = new byte[frList.size()][];
		for(int i=0;i<frList.size();i++) {
			result[i] = frList.get(i).toMsg().toByteArray(); 
		}
		return result;
	}
	
	private void ignoreFriendRequest(RemoteCall params) throws IOException {
		FriendRequestMsg msg = FriendRequestMsg.newBuilder().mergeFrom((byte[])params.getArg()).build();
		localService.ignoreFriendRequest(new MidasFriendRequest(msg));
	}
	
	private void deleteInvite(RemoteCall params) {
		localService.deleteInvite((String) params.getArg());
	}
	
	private Object getInvite(RemoteCall params) {
		MidasInvite invite = localService.getInvite((String) params.getArg());
		if(invite == null)
			return null;
		return invite.toMsg().toByteArray();
	}
	
	private void deleteStream(RemoteCall params) throws IOException {
		StreamMsg msg = StreamMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasStream s = new MidasStream(msg);
		localService.deleteStream(s);
	}

	private void saveStream(RemoteCall params) throws IOException {
		StreamMsg msg = StreamMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasStream s = new MidasStream(msg);
		localService.saveStream(s);
	}

	private Object getStreamById(RemoteCall params) {
		String sId = (String) params.getArg();
		return localService.getStreamById(sId).toMsg().toByteArray();
	}

	private void deletePlaylist(RemoteCall params) throws IOException {
		PlaylistMsg msg = PlaylistMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasPlaylist pl = new MidasPlaylist(msg);
		localService.deletePlaylist(pl);
	}

	private void savePlaylist(RemoteCall params) throws IOException {
		PlaylistMsg msg = PlaylistMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasPlaylist pl = new MidasPlaylist(msg);
		localService.savePlaylist(pl);
	}

	private Object getPlaylistById(RemoteCall params) {
		String plId = (String) params.getArg();
		return localService.getPlaylistById(plId).toMsg().toByteArray();
	}

	private Object getUserAsVisibleBy(RemoteCall params) throws IOException {
		UserMsg targetMsg = UserMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		UserMsg reqMsg = UserMsg.newBuilder().mergeFrom((byte[]) params.getExtraArgs().get(0)).build();
		MidasUser target = new MidasUser(targetMsg);
		MidasUser requestor = new MidasUser(reqMsg);
		return localService.getUserAsVisibleBy(target, requestor).toMsg(true).toByteArray();
	}

	private Object createOrUpdateFriendRequest(RemoteCall params) throws IOException {
		UserMsg requestorMsg = UserMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		UserMsg requesteeMsg = UserMsg.newBuilder().mergeFrom((byte[]) params.getExtraArgs().get(0)).build();
		PlaylistMsg plMsg = PlaylistMsg.newBuilder().mergeFrom((byte[]) params.getExtraArgs().get(1)).build();
		return localService.createOrUpdateFriendRequest(new MidasUser(requestorMsg), new MidasUser(requesteeMsg), new MidasPlaylist(plMsg)).toMsg().toByteArray();
	}

	private Object createOrUpdateInvite(RemoteCall params) throws IOException {
		String email = (String) params.getArg();
		UserMsg friendMsg = UserMsg.newBuilder().mergeFrom((byte[]) params.getExtraArgs().get(0)).build();
		PlaylistMsg plMsg = PlaylistMsg.newBuilder().mergeFrom((byte[]) params.getExtraArgs().get(1)).build();
		return localService.createOrUpdateInvite(email, new MidasUser(friendMsg), new MidasPlaylist(plMsg)).toMsg().toByteArray();
	}

	private void saveUser(RemoteCall params) throws IOException {
		UserMsg msg = UserMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasUser user = new MidasUser(msg);
		localService.saveUser(user);
	}

	private Object createUser(RemoteCall params) throws IOException {
		UserMsg msg = UserMsg.newBuilder().mergeFrom((byte[]) params.getArg()).build();
		MidasUser user = new MidasUser(msg);
		return localService.createUser(user).toMsg(true).toByteArray();
	}

	private Object getUserById(RemoteCall params) {
		Long userId = (Long) params.getArg();
		return localService.getUserById(userId).toMsg(true).toByteArray();
	}

	private Object getUserByEmail(RemoteCall params) {
		String email = (String) params.getArg();
		MidasUser user = localService.getUserByEmail(email);
		if (user == null)
			return null;
		return user.toMsg(true).toByteArray();
	}

	private Long countUsers() {
		return localService.countUsers();
	}

	private Object getAllUsers() {
		List<MidasUser> allUsers = localService.getAllUsers();
		byte[][] arrOfArrs = new byte[allUsers.size()][];
		for (int i = 0; i < arrOfArrs.length; i++) {
			arrOfArrs[i] = allUsers.get(i).toMsg(true).toByteArray();
		}
		return arrOfArrs;
	}

	private void deleteUser(RemoteCall params) {
		localService.deleteUser((Long)params.getArg());
	}

	public void addListener(InvokerCallbackHandler arg0) {
		// Do nothing
	}

	public void removeListener(InvokerCallbackHandler arg0) {
		// Do nothing
	}

	public void setInvoker(ServerInvoker arg0) {
		// Do nothing
	}

	public void setMBeanServer(MBeanServer arg0) {
		// Do nothing
	}
}
