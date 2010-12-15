package com.robonobo.midas.controller;

import static com.robonobo.common.util.TextUtil.*;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.robonobo.common.util.TextUtil;
import com.robonobo.core.api.proto.CoreApi.UserMsg;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.remote.service.MailService;

@Controller
public class UserController extends BaseController {
	@Autowired
	MailService mail;
	
	@RequestMapping(value="/users/byid/{uIdStr}", method=RequestMethod.GET)
	public void getUserById(@PathVariable("uIdStr") String uIdStr, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		long uId = Long.parseLong(uIdStr, 16);
		MidasUser targetU = midas.getUserById(uId);
		if(targetU == null) {
			send404(req, resp);
			return;
		}
		getUser(targetU, req, resp);
	}
	
	@RequestMapping(value="/users/byemail/{email}.{ext}", method=RequestMethod.GET) 
	public void getUserByEmail(@PathVariable("email") String emailStr, @PathVariable("ext") String ext, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Spring's habit of chopping off the file extension is rather annoying
		String email = urlDecode(emailStr)+"."+ext;
		MidasUser targetU = midas.getUserByEmail(email);
		if(targetU == null) {
			send404(req, resp);
			return;
		}
		getUser(targetU, req, resp);
	}
	
	@RequestMapping(value="/users/testing-topup")
	public void requestTopUp(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		MidasUser user = getAuthUser(req);
		if(user == null) {
			send401(req, resp);
			return;
		}
		
		String fromName = "robonobo website";
		String fromEmail = "NO_REPLY@robonobo.com";
		String toName = "";
		String toEmail = "info@robonobo.com";
		String subject = "TopUp request: " + user.getEmail();
		String body = "User " + user.getFriendlyName() + " (" + user.getEmail() + ") has requested an account topup.";
		mail.sendMail(fromName, fromEmail, toName, toEmail, subject, body);
		
		resp.setContentType("text/plain");
		resp.getWriter().println("TopUp request received OK.");
		resp.setStatus(HttpServletResponse.SC_OK);
	}
	
	protected void getUser(MidasUser targetUser, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		MidasUser authUser = getAuthUser(req);
		if(authUser == null) {
			send401(req, resp);
			return;
		}
		MidasUser returnUser = midas.getUserAsVisibleBy(targetUser, authUser);
		if(returnUser == null) {
			send401(req, resp);
			return;
		}
		UserMsg uMsg = returnUser.toMsg(false);
		writeToOutput(uMsg, resp);
		log.debug("User "+authUser.getEmail()+" retrieving user: "+uMsg);
	}
}
