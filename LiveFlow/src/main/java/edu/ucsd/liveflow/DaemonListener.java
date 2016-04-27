package edu.ucsd.liveflow;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class DaemonListener implements ServletContextListener {

	public void contextDestroyed(ServletContextEvent arg0) {
		FlowEngineFacade.stop();
	}

	public void contextInitialized(ServletContextEvent arg0) {
		FlowEngineFacade.start();
	}
}

