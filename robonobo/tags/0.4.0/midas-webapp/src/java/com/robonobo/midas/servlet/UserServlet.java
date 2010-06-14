package com.robonobo.midas.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.common.util.TextUtil;
import com.robonobo.core.api.proto.CoreApi.UserMsg;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MidasService;

@SuppressWarnings("serial")
public class UserServlet extends MidasServlet {
	private MidasService service = LocalMidasService.getInstance();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser targetU = getRequestedUser(req);
		if (targetU == null) {
			send404(req, resp);
			return;
		}
		MidasUser requestor = getAuthUser(req);
		if (requestor == null) {
			send401(req, resp);
			return;
		}
		MidasUser returnUser = service.getUserAsVisibleBy(targetU, requestor);
		if(returnUser == null) {
			send401(req, resp);
			return;
		}
		resp.setContentType("application/data");
		UserMsg uMsg = returnUser.toMsg(false);
		writeToOutput(uMsg, resp);
		log.debug("User "+requestor.getEmail()+" retrieving user: "+uMsg);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// Not allowing user puts, need to update details through the website
	}

	private MidasUser getRequestedUser(HttpServletRequest req) {
		// They can ask for the user either by id (/users/byid/<id>) or by email (/users/byemail/<email>)
		String[] pathEles = getPathElements(req);
		String lookupType = pathEles[pathEles.length - 2];
		String lookupVal = pathEles[pathEles.length - 1];
		MidasUser targetU = null;
		if("byid".equalsIgnoreCase(lookupType))
			targetU = service.getUserById(Long.parseLong(lookupVal));
		else if("byemail".equalsIgnoreCase(lookupType))
			targetU = service.getUserByEmail(TextUtil.urlDecode(lookupVal));
		return targetU;
	}	
}
