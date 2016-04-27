package edu.ucsd.livesearch.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.WorkflowUtils;
import edu.ucsd.saint.commons.IOUtils;
import edu.ucsd.saint.commons.http.HttpParameters;

@SuppressWarnings("serial")
public class NotifyCompletion
extends HttpServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static Logger logger =
		LoggerFactory.getLogger(NotifyCompletion.class);
	
	/*========================================================================
	 * Servlet methods
	 *========================================================================*/
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			HttpParameters params = new HttpParameters(req, null);
			Task task = TaskManager.queryTask(params.getParameter("task"));
			if (task instanceof NullTask == false) {
				String status = params.getParameter("status");
				String message = params.getParameter("message");
				FileItem layout = params.getFile("layout");
				if (layout != null)
					IOUtils.dumpToFile(layout.getInputStream(),
						task.getPath(".info/layout.svg"));			
				WorkflowUtils.notifyCompletion(task, status, message);
			} else logger.info(String.format(
				"Error marking task as completed: Task [%s] does not exist",
				task.getID()));
		} catch (FileUploadException error) {
			logger.info("Failed to mark task as completed", error);
		}
	}
}
