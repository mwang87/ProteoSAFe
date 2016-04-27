package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

/**
 * Servlet implementation class for Servlet: DownloadResource
 *
 */
 public class SuspendTask extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8145758554955040826L;
	private Logger logger = LoggerFactory.getLogger(SuspendTask.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		boolean	isAdmin = ServletUtils.isAdministrator(request.getSession());
		if(!isAdmin) return;
		Task task = TaskManager.queryTask(request.getParameter("task"));
		try{
			if(!(task instanceof NullTask) && task.getStatus() == TaskStatus.RUNNING){
				TaskManager.setSuspended(task);
				WorkflowUtils.abortWorkflow(task);
			}
		}
		catch(Exception e){
			logger.error("Failed to abort task [" + task.getID() + "]", e);
		}
		response.sendRedirect("status.jsp?task=" + task.getID());
	}  	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
//	private static File getStorageBase(){ 
//		return new File("/usr/local/tomcat/webapps/test/"); 
//	}
}
