package com.robonobo.wang.server;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

@Configuration
public class AppConfig implements ServletContextAware {
	private ServletContext sc;
	
	@Bean
	public RemoteWangService getRemoteWang() throws Exception {
		String listenUrl = sc.getInitParameter("remoteWangListenURL");
		String sekrit = sc.getInitParameter("remoteWangSecret");
		return new RemoteWangService(listenUrl, sekrit);
	}
	
	@Override
	public void setServletContext(ServletContext sc) {
		this.sc = sc;
	}
}
