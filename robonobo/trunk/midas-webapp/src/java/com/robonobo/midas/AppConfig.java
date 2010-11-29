package com.robonobo.midas;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;

import com.robonobo.remote.service.MailService;
import com.robonobo.remote.service.MailServiceImpl;

@Configuration
public class AppConfig implements ServletContextAware {
	private ServletContext sc;

	@Bean
	public RemoteMidasService remoteMidas() throws Exception {
		String url = sc.getInitParameter("remoteMidasListenURL");
		String sekrit = sc.getInitParameter("remoteMidasSecret");
		return new RemoteMidasService(url, sekrit);
	}

	@Bean
	public MailService mail() {
		String smtpServer = sc.getInitParameter("smtpServer");
		return new MailServiceImpl(smtpServer);
	}

	public String getLaunchUrl() {
		return sc.getInitParameter("launchUrl");
		
	}
	
	public String getInviteUrlBase() {
		return sc.getInitParameter("inviteUrlBase");
	}
	
	public String getFriendReqUrlBase() {
		return sc.getInitParameter("friendReqUrlBase");
	}
	
	public String getFromName() {
		return sc.getInitParameter("fromName");
	}
	
	public String getFromEmail() {
		return sc.getInitParameter("fromEmail");
	}

	@Override
	public void setServletContext(ServletContext servC) {
		sc = servC;
	}
}
