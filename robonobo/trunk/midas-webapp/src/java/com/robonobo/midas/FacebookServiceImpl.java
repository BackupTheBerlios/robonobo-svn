package com.robonobo.midas;

import static com.robonobo.common.util.TimeUtil.*;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restfb.*;
import com.restfb.types.User;
import com.robonobo.core.api.model.UserConfig;
import com.robonobo.midas.dao.UserConfigDao;
import com.robonobo.midas.dao.UserDao;
import com.robonobo.midas.model.MidasUser;
import com.robonobo.midas.model.MidasUserConfig;
import com.robonobo.remote.service.MidasService;

@Service("facebook")
public class FacebookServiceImpl implements InitializingBean, FacebookService {
	@Autowired
	UserConfigDao userConfigDao;
	@Autowired
	UserDao userDao;
	@Autowired
	MidasService midas;
	Log log = LogFactory.getLog(getClass());
	static final long MIN_MS_BETWEEN_FB_HITS = 1000;
	Lock rateLimitLock = new ReentrantLock(true);
	Date lastHitFBTime = new Date(0);

	public void afterPropertiesSet() throws Exception {
		// TODO Sort out real-time API connection - post our list of subscriptions
		// TODO Kick off background thread to hit all our facebook-connected users' details and make sure they're
		// current
	}

	/**
	 * Updates who knows whom based on facebook friends
	 * 
	 * @param oldUserCfg
	 *            If this is not null and the facebook id has not changed in the newUserCfg, then nothing will be
	 *            updated
	 * @throws FacebookException
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateFriends(MidasUser user, MidasUserConfig oldUserCfg, MidasUserConfig newUserCfg) {
		if (!newUserCfg.getItems().containsKey("facebookId"))
			return;
		if (oldUserCfg != null && oldUserCfg.getItem("facebookId") != null
				&& oldUserCfg.getItem("facebookId").equals(newUserCfg.getItem("facebookId")))
			return;
		// TODO removing friends?
		// Get this user's list of friends from facebook
		FacebookClient fbCli = new RateLimitFBClient(newUserCfg.getItem("facebookAccessToken"));
		Connection<User> friends;
		try {
			friends = fbCli.fetchConnection("me/friends", User.class);
		} catch (FacebookException e) {
			log.error("Caught exception fetching friends from facebook for user id " + user.getUserId(), e);
			return;
		}
		// For each friend in the list, see if there is a robonobo user with that facebook id
		boolean changedUser = false;
		for (User fbFriend : friends.getData()) {
			UserConfig uc = userConfigDao.getUserConfig("facebookId", fbFriend.getId());
			// If so, make a friendship between them
			if (uc != null) {
				long friendId = uc.getUserId();
				MidasUser friend = userDao.getById(friendId);
				user.getFriendIds().add(friendId);
				friend.getFriendIds().add(user.getUserId());
				userDao.save(friend);
				changedUser = true;
			}
		}
		if (changedUser)
			userDao.save(user);
	}

	// rateLimitLock is fair, so threads will queue up here waiting to be allowed to hit fb
	protected void rateLimit() {
		rateLimitLock.lock();
		try {
			long elapsed = now().getTime() - lastHitFBTime.getTime();
			if (elapsed < MIN_MS_BETWEEN_FB_HITS)
				Thread.sleep(MIN_MS_BETWEEN_FB_HITS - elapsed);
			lastHitFBTime = now();
		} catch (InterruptedException justReturn) {
		} finally {
			rateLimitLock.unlock();
		}
	}

	class RateLimitFBClient extends DefaultFacebookClient {
		public RateLimitFBClient(String accessToken) {
			super(accessToken);
		}

		@Override
		public <T> Connection<T> fetchConnection(String connection, Class<T> connectionType, Parameter... parameters)
				throws FacebookException {
			rateLimit();
			return super.fetchConnection(connection, connectionType, parameters);
		}

		@Override
		public <T> T fetchObject(String object, Class<T> objectType, Parameter... parameters) throws FacebookException {
			rateLimit();
			return super.fetchObject(object, objectType, parameters);
		}
	}
}
