package edu.ucsd.liveflow.servlet;

import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.liveflow.FlowEngineFacade;

public class LaunchWorkflow extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 388404523081095698L;
	private final Logger logger = LoggerFactory.getLogger(LaunchWorkflow.class);
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException{		
		String task = req.getParameter("task");
		String flow = req.getParameter("flow");
		String site = req.getParameter("from");
		try{
			boolean success = FlowEngineFacade.getFacade().launchWorkflow(task, flow, site);				
			res.setContentType("text/html");
			PrintStream out = new PrintStream(res.getOutputStream());
			out.printf("<html><body>%n" +
					"Task: [%s]<br/>%n" +
					"Flow: [%s]<br/>%n" +
					"Site: [%s]<br/>%n" +
					"Success: %b%n" +
					"</body></html>",
				task, flow, site, success);
		}
		catch(Exception e){
			logger.error(String.format( 
				"Failed to launch [%s] workflow for task [%s] from [%s]", flow, task, site), e);
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		doGet(req, res);
	}
}
