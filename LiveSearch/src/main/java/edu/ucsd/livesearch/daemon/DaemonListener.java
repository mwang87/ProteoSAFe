package edu.ucsd.livesearch.daemon;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.livesearch.util.VersionTuple;

public class DaemonListener
implements ServletContextListener
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(DaemonListener.class);
	
	/*========================================================================
	 * Event listener methods
	 *========================================================================*/
	public void contextInitialized(ServletContextEvent event) {
		VersionTuple version = Commons.getVersion();
		if (version == null)
			logger.info("LiveSearch version could not be determined.");
		else logger.info(String.format(
			"LiveSearch version = [%s], cache token = [%s].",
			version.toString(), version.getCache()));
		WorkflowUtils.start();
		StaleTaskWatcher.start();
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		WorkflowUtils.stop();
		StaleTaskWatcher.stop();
	}
}
