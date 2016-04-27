package edu.ucsd.liveadmin;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class DaemonListener implements ServletContextListener {
	public void contextInitialized(ServletContextEvent event) {
		UpdateSequences.start();
	}

	public void contextDestroyed(ServletContextEvent event) {
		UpdateSequences.stop();
	}

}
