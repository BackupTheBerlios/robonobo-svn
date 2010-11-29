package com.robonobo.sonar;

import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.robonobo.common.concurrent.CatchingRunnable;

public class CleanupBean implements InitializingBean, DisposableBean {
	@Autowired
	private NodeDao nodeDao;
	@Autowired
	private AppConfig appCfg;
	@Autowired
	private PlatformTransactionManager transactionManager;
	private TransactionTemplate transTemplate;
	private Thread thread;
	private Log log = LogFactory.getLog(getClass());

	@Override
	public void afterPropertiesSet() throws Exception {
		transTemplate = new TransactionTemplate(transactionManager);
		// Clean the db on startup
		log.info("Purging all nodes from db");
		transTemplate.execute(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus ts) {
				nodeDao.deleteAllNodes();
				return null;
			}
		});
		
		thread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				while (true) {
					Thread.sleep(appCfg.getMaxNodeAge());
					log.debug("Cleanup Bean running");
					transTemplate.execute(new TransactionCallback<Object>() {
						public Object doInTransaction(TransactionStatus ts) {
							nodeDao.deleteNodesOlderThan(appCfg.getMaxNodeAge());
							return null;
						}
					});
				}
			}
		});
		thread.start();
	}

	@Override
	public void destroy() throws Exception {
		thread.interrupt();
	}
}
