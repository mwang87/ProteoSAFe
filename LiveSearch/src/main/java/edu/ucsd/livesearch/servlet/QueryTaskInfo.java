package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.xml.Wrapper;
import edu.ucsd.saint.commons.xml.XmlUtils;

/**
 * Servlet implementation class for Servlet: QueryTaskInfo
 *
 */
 public class QueryTaskInfo extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
   
    /**
	 * 
	 */
	private static final long serialVersionUID = -836368049594309253L;

	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public QueryTaskInfo() {
		super();
	}   	
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Wrapper D = new Wrapper(XmlUtils.createXML());
		Element tasks = D.E("tasks");
		String site = WebAppProps.get("livesearch.site.name");
		for(Task task: TaskManager.queryTasksBySite(TaskStatus.RUNNING, site)){
			String id = task.getID();
			tasks.appendChild(
				D.E("task",
					D.A("id", id),
					D.A("user", task.getUser())));
		}
		response.setContentType("text/xml");
		XmlUtils.printXML(tasks, response.getOutputStream());
	}  	
	
	/* (non-Java-doc)
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}   	  	    
}