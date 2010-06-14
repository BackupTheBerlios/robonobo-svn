package com.robonobo.wang.server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class SpringServlet extends HttpServlet {
	private static SpringServlet instance;
	private XmlWebApplicationContext appContext;

	public static SpringServlet getInstance() {
		return instance;
	}

	public SpringServlet() {
		instance = this;
	}

	@Override
	public void init() throws ServletException {
		PropertyConfigurator.configureAndWatch("log4j.properties");
		appContext = new XmlWebApplicationContext();
		appContext.setServletContext(getServletContext());
		appContext.refresh();
	}

	public UserAccountDAO getUserAccountDAO() {
		return (UserAccountDAO) appContext.getBean("userAccountDAO");
	}

	public DenominationDAO getDenominationDAO() {
		return (DenominationDAO) appContext.getBean("denominationDAO");
	}

	public DoubleSpendDAO getDoubleSpendDAO() {
		return (DoubleSpendDAO) appContext.getBean("doubleSpendDAO");
	}

	public DbMgr getDbMgr() {
		return (DbMgr) appContext.getBean("dbMgr");
	}
}
