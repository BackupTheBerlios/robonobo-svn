package com.robonobo.midas.servlet;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.remote.service.RemoteMidasService;

/**
 * Sets up the remote midas service, if configured
 * @author macavity
 *
 */
@SuppressWarnings("serial")
public class RemotingServlet extends GenericServlet {
	RemoteMidasService remoteMidasService;
	
	public RemotingServlet() {
	}
	
	@Override
	public void init() throws ServletException {
		String url = getServletContext().getInitParameter("remoteMidasListenURL");
		String sekrit = getServletContext().getInitParameter("remoteMidasSecret");
		if(url != null) {
			if(sekrit == null)
				throw new ServletException("Must specify remoteMidasServlet init param");
			try {
				remoteMidasService = new RemoteMidasService(url, sekrit);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
	}
	
	@Override
	public void destroy() {
		if(remoteMidasService != null)
			remoteMidasService.shutdown();
	}
	
	@Override
	public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
		// Should never be called
		throw new SeekInnerCalmException("Not implemented");
	}

}
