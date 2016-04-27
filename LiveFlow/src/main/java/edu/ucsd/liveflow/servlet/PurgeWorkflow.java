package edu.ucsd.liveflow.servlet;

import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.liveflow.FlowEngineFacade;

public class PurgeWorkflow extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 388404523081095698L;
	private final Logger logger = LoggerFactory.getLogger(PurgeWorkflow.class);
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException{		
		String task = req.getParameter("task");
		try{
			boolean success = FlowEngineFacade.getFacade().purgeWorkflow(task);				
			res.setContentType("text/html");
			PrintStream out = new PrintStream(res.getOutputStream());
			out.printf("<html><body>%n" +
					"Task: [%s]<br/>%n" +
					"Success: %b%n" +
					"</body></html>",
				task, success);
		}
		catch(Exception e){
			logger.error(String.format( 
				"Failed to purge workflow for task [%s]", task), e);
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		doGet(req, res);
	}
}
