package com.robonobo.common.persistence;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class PersistenceFilter implements Filter {
	public void init(FilterConfig arg0) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		PersistenceManager.createSession();
		boolean gotError = false;
		try {
			chain.doFilter(request, response);
		} catch(ServletException e) {
			gotError = true;
			throw e;
		} catch(IOException e) {
			gotError = true;
			throw e;
		} finally {
			PersistenceManager.closeSession(gotError);
		}
	}

	public void destroy() {
	}
}
