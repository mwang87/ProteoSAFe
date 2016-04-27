package edu.ucsd.liveadmin.batch;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.servlet.BaseServlet;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.saint.commons.WebAppProps;

@SuppressWarnings("serial")
public class FindStaleTasks
extends BaseServlet
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(FindStaleTasks.class);
	private static final int STALE_HOURS = 24;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * <p>Accepts and processes requests to scan for stale tasks
	 * in the CCMS system.
	 * 
	 * @param request	an {@link HttpServletRequest} object that contains
	 * 					the request the client has made of the servlet
	 * 
	 * @param response	an {@link HttpServletResponse} object that contains
	 * 					the response the servlet sends to the client
	 * 
	 * @throws IOException		if an input or output error is detected
	 * 							when the servlet handles the POST request
	 * 
	 * @throws ServletException	if the request for the POST could not be
	 * 							handled
	 */
	@Override
	protected void doGet(HttpServletRequest request,
		HttpServletResponse response)
	throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		// check for stale tasks
		String result = check();
		if (result != null)
			out.print(result);
		else out.print("No stale tasks were found.");
	}
	
	public static String check() {
		// collect all currently running tasks
		String site = WebAppProps.get("livesearch.site.name");
		Collection<Task> tasks =
			TaskManager.queryTasksBySite(TaskStatus.RUNNING, site);
		Collection<Task> stale = new HashSet<Task>();
		// check all currently running tasks for staleness
		String now =
			new SimpleDateFormat("h:mm a, EEEE, M/d/yy").format(
				Calendar.getInstance().getTime());
		int staleMillis = STALE_HOURS * 60 * 60 * 1000;
		for (Task task : tasks)
			if (task.getElapsedTime() >= staleMillis)
				stale.add(task);
		// if no stale tasks were found, return null
		if (stale.isEmpty())
			return null;
		// otherwise, generate and return detail text
		else {
			String host = WebAppProps.get("livesearch.host.address");
			String url = WebAppProps.get("livesearch.site.url");
			StringBuffer detail = new StringBuffer();
			detail.append(stale.size());
			detail.append(" stale task");
			if (stale.size() == 1)
				detail.append(" was ");
			else detail.append("s were ");
			detail.append("found on ");
			detail.append(host);
			detail.append(" at ");
			detail.append(now);
			detail.append(":\n\n");
			for (Task task : stale) {
				detail.append(url);
				detail.append("/status.jsp?task=");
				detail.append(task.getID());
				detail.append("\n");
			}
			return detail.toString();
		}
	}
}
