package com.robonobo.midas.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.robonobo.midas.FacebookService;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.midas.model.MidasUserConfig;
import com.robonobo.remote.service.MidasService;

@Controller
public class FacebookController extends BaseController {
	@Autowired
	FacebookService facebook;
	@Autowired
	MidasService midas;
	ObjectMapper jsonMapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/fb-callback", method = RequestMethod.POST)
	public void facebookCallback(@RequestParam("hub.challenge") String challenge,
			@RequestParam("hub.verify_token") String verifyToken, 
			HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if(!verifyToken.equals(facebook.getFacebookVerifyTok())) {
			// Someone is playing silly buggers, maybe?
			log.error("Facebook called back with invalid verify token "+verifyToken+" (was expecting "+facebook.getFacebookVerifyTok()+")");
			resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		// Parse our json using the wonderful jackson library
		Map<String, Object> json;
		try {
			json = jsonMapper.readValue(req.getInputStream(), Map.class);
		} catch (Exception e) {
			throw new IOException(e);
		}
		if(!"user".equals(json.get("object")))
			throw new IOException("Invalid object in facebook callback: "+json.get("object"));
		List<Object> changedObjs = (List<Object>) json.get("entry");
		log.info("Facebook callback received with "+changedObjs.size()+" changed objects");
		for (Object obj : changedObjs) {
			Map<String, Object> changedObj = (Map<String, Object>) obj;
			String fbId = (String) changedObj.get("uid");
			MidasUserConfig muc = facebook.getUserConfigByFacebookId(fbId);
			if(muc == null)
				continue;
			List<Object> changedFields = (List<Object>) changedObj.get("changed_fields");
			for (Object field : changedFields) {
				if("name".equals(field))
					facebook.updateFacebookName(fbId, (String)field);
				else if("friends".equals(field)) {
					MidasUser user = midas.getUserById(muc.getUserId());
					facebook.updateFriends(user, null, muc);
				}
			}
		}
		// Realtime API spec at http://developers.facebook.com/docs/api/realtime requires us to echo back the challenge, fuck knows why
		resp.getWriter().print(challenge);
	}
}
