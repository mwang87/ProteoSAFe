package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.util.Commons;

public class RelaunchWorkflows extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5927365661171904329L;
	private static final Logger logger = LoggerFactory.getLogger(RelaunchWorkflows.class);	

	public void doGet(HttpServletRequest req, HttpServletResponse res){		
		try{
			WorkflowUtils.relaunchWorkflows();
		}
		catch(Exception e){
			logger.error("Failed to relaunch all running flow instances", e);
			Commons.contactAdministrator("Failed to relaunch all running flow instances");
		}
	}
	  
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doGet(req, res);
	}
}
