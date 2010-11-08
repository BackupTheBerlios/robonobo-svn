package com.robonobo.plugins.playlistmirror;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.util.TimeUtil;
import com.robonobo.core.api.model.CloudTrack;
import com.robonobo.core.api.model.Stream;
import com.robonobo.core.api.model.Track;
import com.robonobo.core.service.AbstractService;
import com.robonobo.midas.model.MidasPlaylist;

/**
 * Monitors the database, looks for modified playlists and downloads any new
 * streams. To use this plugin, add the jar to the classpath, and in robo.cfg
 * add 'com.robonobo.plugins.playlistmirror.PlaylistMirrorPlugin' to
 * extraServices and
 * 'playlistMirror:com.robonobo.plugins.playlistmirror.PlaylistMirrorConfig' to
 * extraConfigs
 * 
 * @author macavity
 * 
 */
public class PlaylistMirrorPlugin extends AbstractService {
	PlaylistMirrorConfig cfg;
	Log log = LogFactory.getLog(getClass());
	SessionFactory sessionFactory;
	Future<?> checkerTask;
	Date lastCheckTime = null;
	Pattern playlistIdPat;
	Map<String, Integer> delayDeleteShares = new HashMap<String, Integer>();

	public PlaylistMirrorPlugin() {
		addHardDependency("core.downloads");
		addHardDependency("core.shares");
	}

	public String getName() {
		return "Playlist Mirror Plugin";
	}

	public String getProvides() {
		return "plugin.playlistmirror";
	}

	@Override
	public void startup() throws Exception {
		cfg = (PlaylistMirrorConfig) getRobonobo().getConfig("playlistMirror");
		if (cfg == null) {
			log.error("Error: no playlistMirror.cfg defined in robonobo");
			return;
		}
		playlistIdPat = Pattern.compile(cfg.playlistIdRegex);
		if (cfg.connectionUrl == null || cfg.connectionDriverClass == null || cfg.connectionDialect == null
				|| cfg.connectionUsername == null || cfg.connectionPassword == null) {
			log.error("Error: playlist mirror plugin not configured - you must specify values in playlistMirror.cfg");
			return;
		}

		Configuration hibCfg = new Configuration();
		hibCfg.setProperty("hibernate.connection.driver_class", cfg.connectionDriverClass);
		hibCfg.setProperty("hibernate.dialect", cfg.connectionDialect);
		hibCfg.setProperty("hibernate.connection.url", cfg.connectionUrl);
		hibCfg.setProperty("hibernate.connection.username", cfg.connectionUsername);
		hibCfg.setProperty("hibernate.connection.password", cfg.connectionPassword);
		hibCfg.addClass(com.robonobo.midas.model.MidasPlaylist.class);
		hibCfg.addClass(com.robonobo.midas.model.MidasStream.class);
		hibCfg.addClass(com.robonobo.midas.model.MidasStreamAttribute.class);
		hibCfg.addClass(com.robonobo.midas.model.MidasUser.class);
		sessionFactory = hibCfg.buildSessionFactory();

		/**
		 * Keep track of shares we should delete after a time (for monitoring)
		 */
		String ddsStr = cfg.getDelayedDeleteShares();
		if (ddsStr != null) {
			String[] ddsArr = ddsStr.split(",");
			for (String dds : ddsArr) {
				String[] flarp = dds.split("\\|");
				String streamId = flarp[0];
				int waitSecs = Integer.parseInt(flarp[1]);
				delayDeleteShares.put(streamId, waitSecs);
			}
		}

		checkerTask = getRobonobo().getExecutor().scheduleAtFixedRate(new UpdatedPlaylistChecker(), 0, cfg.checkFrequency, TimeUnit.SECONDS);
	}

	@Override
	public void shutdown() throws Exception {
		if (sessionFactory != null) {
			sessionFactory.close();
			checkerTask.cancel(true);
		}
	}

	private void delayedDeleteShare(final String delSid) {
		log.info("Deleting monitor share for stream "+delSid);
		getRobonobo().getShareService().deleteShare(delSid);
	}

	class UpdatedPlaylistChecker extends CatchingRunnable {
		@Override
		public void doRun() throws Exception {
			if (lastCheckTime == null) {
				lastCheckTime = TimeUtil.now();
				return;
			}
			Session session = sessionFactory.openSession();
			Criteria crit = session.createCriteria(MidasPlaylist.class);
			log.debug("Checking for playlists updated since "+TimeUtil.getDateTimeFormat().format(lastCheckTime));
			crit.add(Restrictions.gt("updated", lastCheckTime));
			List<MidasPlaylist> playlists = crit.list();
			log.debug("Found "+playlists.size()+" updated playlists");
			for (MidasPlaylist p : playlists) {
				if (playlistIdPat.matcher(p.getPlaylistId()).matches()) {
					log.info("Mirroring playlist " + p.getTitle() + " (" + p.getPlaylistId() + ")");
					for (String streamId : p.getStreamIds()) {
						Track t = getRobonobo().getTrackService().getTrack(streamId);
						if(t instanceof CloudTrack) {
							getRobonobo().getDownloadService().addDownload(streamId);
							// If this is in our list, delete the share after a
							// while (so when the monitor offers it, we download
							// it again)
							if (delayDeleteShares.containsKey(streamId)) {
								final String delSid = streamId;
								Integer delaySecs = delayDeleteShares.get(streamId);
								log.debug("Scheduling delete of "+streamId+" to run in "+delaySecs+"s");
								getRobonobo().getExecutor().schedule(new CatchingRunnable() {
									public void doRun() throws Exception {
										delayedDeleteShare(delSid);
									}
								}, delaySecs, TimeUnit.SECONDS);
							}
						}
					}
				}
			}
			lastCheckTime = TimeUtil.now();
			session.close();
		}
	}
}
