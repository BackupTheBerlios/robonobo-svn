package com.robonobo.wang.server.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.robonobo.wang.client.LucreFacade;
import com.robonobo.wang.server.DbMgr;
import com.robonobo.wang.server.DenominationDAO;

public class CreateDenoms {
	private static final int KEY_LENGTH = 512;

	private static void usage() {
		System.err.println("Usage: CreateDenoms [list of denoms]");
		System.exit(1);
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0)
			usage();
		// Parse args into denominations
		int[] denoms = new int[args.length];
		try {
			for (int i = 0; i < args.length; i++) {
				denoms[i] = Integer.parseInt(args[i]);
			}
		} catch (NumberFormatException e) {
			usage();
		}
		// Bring up log4j so we can see what we're doing
		BasicConfigurator.configure();
		Log log = LogFactory.getLog(CreateDenoms.class);
		ApplicationContext appContext = new FileSystemXmlApplicationContext("appContext.xml");
		DbMgr dbMgr = (DbMgr) appContext.getBean("dbMgr");
		DenominationDAO denomDao = (DenominationDAO) appContext.getBean("denominationDAO");

		dbMgr.begin();
		LucreFacade lucre = new LucreFacade();
		boolean gotEx = false;
		try {
			log.info("Deleting all denoms");
			denomDao.deleteAllDenoms();
			for (int i = 0; i < denoms.length; i++) {
				log.info("Creating denom " + denoms[i]);
				denomDao.putDenom(lucre.createDenomination(denoms[i], KEY_LENGTH));
			}
			log.info("Done.");
		} catch (Exception e) {
			gotEx = true;
			dbMgr.rollback();
			throw e;
		} finally {
			if (!gotEx)
				dbMgr.commit();
		}
	}
}
