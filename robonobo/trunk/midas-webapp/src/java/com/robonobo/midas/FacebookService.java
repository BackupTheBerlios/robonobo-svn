package com.robonobo.midas;

import com.robonobo.midas.model.MidasUser;
import com.robonobo.midas.model.MidasUserConfig;

public interface FacebookService {
	/**
	 * Updates who knows who based on facebook friends
	 */
	public abstract void updateFriends(MidasUser user, MidasUserConfig oldUserCfg, MidasUserConfig newUserCfg);

	public abstract String getFacebookVerifyTok();

	public abstract MidasUserConfig getUserConfigByFacebookId(String fbId);

	public abstract void updateFacebookName(String fbId, String newName);

}