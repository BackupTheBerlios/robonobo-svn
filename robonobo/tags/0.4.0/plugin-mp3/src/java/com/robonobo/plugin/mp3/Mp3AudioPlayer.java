package com.robonobo.plugin.mp3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import com.robonobo.common.concurrent.CatchingRunnable;
import com.robonobo.common.concurrent.SafetyNet;
import com.robonobo.common.pageio.buffer.FilePageBuffer;
import com.robonobo.common.pageio.buffer.PageBufferInputStream;
import com.robonobo.common.pageio.buffer.SimplePageInfoStore;
import com.robonobo.common.pageio.paginator.EqualSizeFilePaginator;
import com.robonobo.common.pageio.paginator.Paginator;
import com.robonobo.common.util.CodeUtil;
import com.robonobo.common.util.ExceptionEvent;
import com.robonobo.common.util.ExceptionListener;
import com.robonobo.core.api.AudioPlayer;
import com.robonobo.core.api.AudioPlayerListener;
import com.robonobo.core.api.model.Stream;
import com.robonobo.mina.external.buffer.PageBuffer;

public class Mp3AudioPlayer implements AudioPlayer {
	private Log log = LogFactory.getLog(getClass());
	private Stream s;
	private PageBuffer pb;
	private List<AudioPlayerListener> listeners = new ArrayList<AudioPlayerListener>();
	private BasicPlayer basicPlayer;
	/**
	 * The number of bytes we have progressed through our current inputstream, if any
	 */
	private int progressBytes = 0;
	/**
	 * The position we have seeked to, if any
	 */
	private int seekPos = 0; 
	private MP3PlaybackListener listener = new MP3PlaybackListener();
	private Status status;
	private ThreadPoolExecutor executor;

	public Mp3AudioPlayer(Stream s, PageBuffer pb, ThreadPoolExecutor executor) {
		this.s = s;
		this.pb = pb;
		this.executor = executor;
	}

	public void play() throws IOException {
		executor.execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				try {
					if (basicPlayer == null) {
						basicPlayer = new BasicPlayer();
						basicPlayer.addBasicPlayerListener(listener);
						basicPlayer.open(new PageBufferInputStream(pb));
						seekPos = 0;
						progressBytes = 0;
						basicPlayer.play();
					} else
						basicPlayer.resume();
					status = Status.Playing;
				} catch (Exception e) {
					for (AudioPlayerListener listener : listeners) {
						listener.onError(e.getMessage());
					}
				}
			}
		});
	}

	public void stop() {
		executor.execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				if (basicPlayer != null) {
					try {
						basicPlayer.stop();
					} catch (BasicPlayerException ignore) {
					}
					basicPlayer = null;
				}
				status = Status.Stopped;
			}
		});
	}

	public void pause() throws IOException {
		executor.execute(new CatchingRunnable() {
			public void doRun() throws Exception {
				try {
					basicPlayer.pause();
					status = Status.Paused;
				} catch (BasicPlayerException e) {
					throw new IOException("Caught BasicPlayerException pausing: " + e.getMessage());
				}
			}
		});
	}

	/**
	 * @param ms Position in the stream to seek to, as a millisecs offset from start of stream 
	 */
	public void seek(long ms) throws IOException {
		// Can only seek if playing or paused
		if(status == Status.Stopped) {
			log.error("Can't seek while stopped");
			return;
		}
		int currentPos = seekPos + progressBytes;
		// Work out where we're seeking to in byte terms
		float wayThrough = ms / s.getDuration();
		seekPos = (int) (wayThrough * s.getSize());
		// BasicPlayer can't seek backwards, so if we're seeking backwards, stop
		// and restart with a new player
		if(seekPos < currentPos) {
			executor.execute(new CatchingRunnable() {
				public void doRun() throws Exception {
					basicPlayer.stop();
					PageBufferInputStream pbis = new PageBufferInputStream(pb);
					pbis.skip(seekPos);
					basicPlayer.open(pbis);
					progressBytes = 0;
					basicPlayer.play();
					if(status == Status.Paused)
						basicPlayer.pause();
				}
			});
		} else {
			// See how much data we've received, can't seek beyond that
			int seekLimit = (int) (pb.getLastContiguousPage() * pb.getAvgPageSize());
			if(seekPos > seekLimit)
				seekPos = seekLimit;
			int seekForward = seekPos - currentPos;
			try {
				basicPlayer.seek(seekForward);
			} catch (BasicPlayerException e) {
				// Can't have causative exception in IOE until Java6
				if(CodeUtil.javaMajorVersion() >= 6)
					throw new IOException(e);
				else
					throw new IOException("BasicPlayer error: "+e.getMessage());
			}
		}
	}

	public void addListener(AudioPlayerListener listener) {
		listeners.add(listener);
	}

	public void removeListener(AudioPlayerListener listener) {
		listeners.remove(listener);
	}

	class MP3PlaybackListener implements BasicPlayerListener {

		public void opened(java.lang.Object stream, java.util.Map properties) {

		}

		public void progress(int bytesread, long microseconds, byte[] pcmdata, java.util.Map properties) {
			if (status == Status.Playing)
				progressBytes = bytesread;
			for (AudioPlayerListener listener : listeners) {
				listener.onProgress(microseconds);
			}
		}

		public void setController(BasicController controller) {

		}

		public void stateUpdated(BasicPlayerEvent e) {
			if (e.getCode() == BasicPlayerEvent.EOM) {
				for (AudioPlayerListener listener : listeners) {
					listener.onCompletion();
				}
				status = Status.Stopped;
			}
		}

	}

	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configureAndWatch("../gui/src/java/log4j.properties");
		final Log log = LogFactory.getLog(Mp3AudioPlayer.class);
		SafetyNet.addListener(new ExceptionListener() {
			public void onException(ExceptionEvent ex) {
				log.error("SafetyNet caught exception", ex.getException());
			}
		});
		final PrintStream out = System.out;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		if (args.length != 1) {
			out.println("Usage: Mp3AudioPlayer <sound file>");
			System.exit(1);
		}
		// Paginate our file into a source pagebuffer
		File f = new File(args[0]);
		FileChannel fc = new FileInputStream(f).getChannel();
		Mp3FormatSupportProvider fsp = new Mp3FormatSupportProvider();
		Stream s = fsp.getStreamForFile(f);
		s.setStreamId("id:flarp");
		SimplePageInfoStore srcPis = new SimplePageInfoStore();
		srcPis.init(s.getStreamId());
		final FilePageBuffer srcPb = new FilePageBuffer(s.getStreamId(), f, srcPis);
		Paginator p = new EqualSizeFilePaginator(32 * 1024, f.length(), 0);
		p.paginate(fc, srcPb);
		fc.close();
		// Now create a dest pb, and spawn a thread to put pages into it
		SimplePageInfoStore destPis = new SimplePageInfoStore();
		destPis.init(s.getStreamId());
		File destFile = File.createTempFile("mp3audioplayer", "dat");
		final FilePageBuffer destPb = new FilePageBuffer(s.getStreamId(), destFile, destPis);
		Thread myThread = new Thread(new CatchingRunnable() {
			public void doRun() throws Exception {
				for (long pn = 0; pn < srcPb.getTotalPages(); pn++) {
					out.println("Putting page " + pn);
					destPb.putPage(srcPb.getPage(pn));
					// 1.5 pages per sec == 384kbps... if the test mp3 is higher
					// than this, decrease this sleep time
					Thread.sleep(667L);
				}
			}
		});
		myThread.start();
		// Let's play this sucker
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(8);
		Mp3AudioPlayer player = new Mp3AudioPlayer(s, destPb, executor);
		player.addListener(new AudioPlayerListener() {
			public void onCompletion() {
				out.println("Playback completed!");
			}

			public void onError(String error) {
				out.println("Got error: " + error);
			}

			public void onProgress(long microsecs) {
				out.println("Progress: " + microsecs / 1000 + "ms");
			}
		});
		player.play();
		while (true) {
			out.println("Playing: Hit enter to pause");
			in.readLine();
			player.pause();
			out.println("Paused: Hit enter to play");
			in.readLine();
			player.play();
		}
	}

	public Status getStatus() {
		return status;
	}
}
