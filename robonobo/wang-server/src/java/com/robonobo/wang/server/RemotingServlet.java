package com.robonobo.wang.server;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.robonobo.common.exceptions.SeekInnerCalmException;

/**
 * Sets up the remote wang service, if configured
 * @author macavity
 *
 */
@SuppressWarnings("serial")
public class RemotingServlet extends GenericServlet {
	RemoteWangService remoteWangService;
	
	public RemotingServlet() {
	}
	
	@Override
	public void init() throws ServletException {
		String url = getServletContext().getInitParameter("remoteWangListenURL");
		String sekrit = getServletContext().getInitParameter("remoteWangSecret");
		if(url != null) {
			if(sekrit == null)
				throw new ServletException("Must specify remoteWangServlet init param");
			try {
				remoteWangService = new RemoteWangService(url, sekrit);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
	}
	
	@Override
	public void destroy() {
		if(remoteWangService != null)
			remoteWangService.shutdown();
	}
	
	@Override
	public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
		// Should never be called
		throw new SeekInnerCalmException("Not implemented");
	}

}
