package com.robonobo.wang.server;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class PersistenceFilter implements Filter {

	public void init(FilterConfig fc) throws ServletException {
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain fc) throws IOException, ServletException {
		DbMgr dbMgr = SpringServlet.getInstance().getDbMgr();
		boolean gotEx = false;
		try {
			dbMgr.begin();
			fc.doFilter(req, resp);
		} catch (Exception e) {
			gotEx = true;
			throw new ServletException(e);
		} finally {
			try {
				if (gotEx)
					dbMgr.rollback();
				else
					dbMgr.commit();
			} catch (SQLException sqle) {
				throw new ServletException(sqle);
			}
		}

	}

	public void destroy() {
	}
}
