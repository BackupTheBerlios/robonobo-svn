package com.robonobo.sonar;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

@Configuration
public class AppConfig implements ServletContextAware {
	private ServletContext sc;
	
	@Bean
	public CleanupBean getCleanupBean() {
		return new CleanupBean();
	}
	
	public long getMaxNodeAge() {
		return Long.parseLong(sc.getInitParameter("maxNodeAgeMs"));
	}
	
	@Override
	public void setServletContext(ServletContext sc) {
		this.sc = sc;
	}
}
