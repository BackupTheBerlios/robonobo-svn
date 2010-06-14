package com.robonobo.sonar;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple servlet, just returns the requestor's ip address in the body
 * @author macavity
 *
 */
public class IPDetectServlet extends HttpServlet {
	Log log = LogFactory.getLog(getClass());

	public IPDetectServlet() {
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		String requestorIP = req.getRemoteAddr();
		resp.getOutputStream().write(requestorIP.getBytes());
		resp.setStatus(HttpServletResponse.SC_OK);
		log.info("Sent IP address details to "+requestorIP);
	}
	
}
