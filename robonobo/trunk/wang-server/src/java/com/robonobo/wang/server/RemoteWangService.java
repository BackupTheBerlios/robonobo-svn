package com.robonobo.wang.server;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import com.robonobo.common.remote.RemoteCall;

/**
 * The server end of a remote wang service (client end is RemoteWangFacade in
 * midas-model project)
 * 
 * @author macavity
 * 
 */
public class RemoteWangService implements ServerInvocationHandler {
	Connector connector;
	String secret;
	DbMgr dbMgr;
	UserAccountDAO uaDao;
	Log log = LogFactory.getLog(getClass());

	public RemoteWangService(String url, String secret) throws Exception {
		this.secret = secret;
		uaDao = SpringServlet.getInstance().getUserAccountDAO();
		dbMgr = SpringServlet.getInstance().getDbMgr();
		log.info("Starting remote wang service on " + url);
		InvokerLocator locator = new InvokerLocator(url);
		connector = new Connector();
		connector.setInvokerLocator(locator.getLocatorURI());
		connector.start();
		connector.addInvocationHandler("wang", this);
	}

	public void shutdown() {
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
		String method = params.getMethodName();
		// Make sure everything happens inside a transaction
		dbMgr.begin();
		boolean gotErr = false;
		try {
			if (method.equals("getBalance")) {
				String email = (String) params.getArg();
				String passwd = (String) params.getExtraArgs().get(0);
				return getBalance(email, passwd);
			} else if (method.equals("changePassword")) {
				String email = (String) params.getArg();
				String oldPasswd = (String) params.getExtraArgs().get(0);
				String newPasswd = (String) params.getExtraArgs().get(1);
				changePassword(email, oldPasswd, newPasswd);
				return null;
			} else if (method.equals("countUsers")) {
				return countUsers();
			} else if(method.equals("createUser")) {
				String email = (String) params.getArg();
				String friendlyName = (String) params.getExtraArgs().get(0);
				String password = (String) params.getExtraArgs().get(1);
				createUser(email, friendlyName, password);
				return null;
			} else if(method.equals("topUpBalance")) {
				String email = (String) params.getArg();
				double amount = Double.parseDouble((String) params.getExtraArgs().get(0));
				topUpBalance(email, amount);
				return null;
			} else
				throw new IllegalArgumentException("Invalid method");
		} catch (Exception e) {
			gotErr = true;
			throw e;
		} finally {
			if (gotErr)
				dbMgr.rollback();
			else
				dbMgr.commit();
		}
	}

	private void topUpBalance(String email, double amount) throws Exception {
		UserAccount ua = uaDao.getAndLockUserAccount(email);
		try {
			ua.setBalance(ua.getBalance()+amount);
		} finally {
			uaDao.putUserAccount(ua);
		}
	}

	private void createUser(String email, String friendlyName, String password) throws Exception {
		uaDao.createUserAccount(friendlyName, email, password);
	}
	
	private Double getBalance(String email, String passwd) throws Exception {
		UserAccount ua = uaDao.getUserAccount(email);
		if (!ua.getPassword().equals(passwd))
			throw new IllegalAccessException("Invalid password");
		return ua.getBalance();
	}

	private void changePassword(String email, String oldPasswd, String newPasswd) throws Exception {
		UserAccount ua = uaDao.getAndLockUserAccount(email);
		try {
			if (!ua.getPassword().equals(oldPasswd))
				throw new IllegalAccessException("Invalid password");
			ua.setPassword(newPasswd);
		} finally {
			uaDao.putUserAccount(ua);
		}
	}

	private Long countUsers() throws Exception {
		return uaDao.countUsers();
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
