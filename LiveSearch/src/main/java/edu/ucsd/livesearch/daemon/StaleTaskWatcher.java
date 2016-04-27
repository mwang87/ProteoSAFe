package edu.ucsd.livesearch.daemon;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.liveadmin.batch.FindStaleTasks;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.Helpers;

public class StaleTaskWatcher
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(StaleTaskWatcher.class);
	private static final int CHECK_HOUR = 3;	// check once per day at 3 am
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private static Thread watcher = null;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static void start() {
		if (watcher == null) {
			watcher = new StaleTaskWatcherThread();
			Helpers.startAsDaemon(watcher);
		}
	}
	
	public static void stop() {
		watcher.interrupt();
		watcher = null;
	}
	
	/*========================================================================
	 * Thread implementation
	 *========================================================================*/
	private static class StaleTaskWatcherThread
	extends Thread
	{
		/*====================================================================
		 * Constants
		 *====================================================================*/
		private static final String THREAD_NAME = "Stale task watcher thread";
		
		/*====================================================================
		 * Constructors
		 *====================================================================*/
		public StaleTaskWatcherThread() {
			super(THREAD_NAME);
		}
		
		/*====================================================================
		 * Public interface methods
		 *====================================================================*/
		@Override
		public void run() {
			try {
				while (true) {
					// if it's the right hour, check for stale tasks
					if (checkHour()) {
						String result = FindStaleTasks.check();
						// if any stale tasks were found, notify the admins
						if (result != null)
							Commons.contactAdministrator(result);
					}
					// wait for one hour
					Thread.sleep(3600000);
				}
			} catch (Throwable error) {
				logger.info(THREAD_NAME + " terminated", error);
			}
		}
		
		/*====================================================================
		 * Convenience methods
		 *====================================================================*/
		private boolean checkHour() {
			Calendar calendar = Calendar.getInstance();
			return calendar.get(Calendar.HOUR_OF_DAY) == CHECK_HOUR;
		}
	}
}
