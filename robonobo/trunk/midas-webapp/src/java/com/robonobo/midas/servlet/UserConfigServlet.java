package com.robonobo.midas.servlet;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.core.api.model.UserConfig;
import com.robonobo.core.api.proto.CoreApi.UserConfigMsg;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.midas.model.MidasUserConfig;
import com.robonobo.remote.service.LocalMidasService;
import com.robonobo.remote.service.MidasService;

public class UserConfigServlet extends MidasServlet {
	private MidasService service = LocalMidasService.getInstance();
	private Pattern userIdPattern = Pattern.compile("/([0-9a-eA-E]+).*");
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser authUser = getAuthUser(req);
		if(authUser == null || authUser.getUserId() != getUserId(req)) {
			send401(req, resp);
			return;
		}
		MidasUserConfig config = service.getUserConfig(authUser);
		if(config == null) {
			config = new MidasUserConfig();
			config.setUserId(authUser.getUserId());
		}
		resp.setContentType("application/data");
		writeToOutput(config.toMsg(), resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		MidasUser authUser = getAuthUser(req);
		if(authUser == null || authUser.getUserId() != getUserId(req)) {
			send401(req, resp);
			return;
		}
		UserConfigMsg.Builder b = UserConfigMsg.newBuilder();
		readFromInput(b, req);
		MidasUserConfig newCfg = new MidasUserConfig(b.build());
		MidasUserConfig curCfg = service.getUserConfig(authUser);
		if(curCfg == null)
			service.putUserConfig(newCfg);
		else {
			// User has existing config - add/replace items from the serialized one
			for (String iName : newCfg.getItems().keySet()) {
				curCfg.getItems().put(iName, newCfg.getItems().get(iName));
			}
			service.putUserConfig(curCfg);
		}
	}
	
	private long getUserId(HttpServletRequest req) {
		Matcher m = userIdPattern.matcher(req.getPathInfo());
		if (!m.matches())
			throw new SeekInnerCalmException();
		return Long.parseLong(m.group(1), 16);
	}
}
