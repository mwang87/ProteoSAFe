package edu.ucsd.livegrid.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edu.ucsd.livegrid.GridPlanner;


public class DaemonListener implements ServletContextListener {
	public void contextInitialized(ServletContextEvent event) {
		GridPlanner.getInstance().start();
	}

	public void contextDestroyed(ServletContextEvent event) {
		GridPlanner.getInstance().stop();
	}

}
