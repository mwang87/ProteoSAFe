package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.FormatUtils;

@SuppressWarnings("serial")
public class QueryTaskList
extends BaseServlet
{
	//Getting Sub Status
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String user_target = request.getParameter("user");
		
		boolean isAdmin = AccountManager.getInstance().checkRole(username, "administrator");
		
		if(username == null){
			response.getOutputStream().print("{\"status\":\"login\"}");
			return;
		}
		
		Collection<Task> tasks;
		
		if(user_target != null){
			if(isAdmin){
				if(user_target.equals("all")){
					tasks = TaskManager.queryRecentNumberTasks(1000);
				}
				else{
					tasks = TaskManager.queryOwnedTasks(user_target);
				}
			}
			else{
				response.getOutputStream().print("{\"status\":\"invalid parameters\"}");
				return;
			}
		}
		else{
			tasks = TaskManager.queryOwnedTasks(username);
		}
		
		response.getOutputStream().print("{\"status\":\"success\", \"tasks\" : " + QueryTaskList.getTaskListJSON(tasks) +  " }");
	}
	
	public static String getTaskListJSON(Collection<Task> tasks) {
		if (tasks == null || tasks.isEmpty())
			return "[]";
		// build JSON string
		StringBuffer json = new StringBuffer("[");
		int i = 0;
		for (Task task : tasks) {
			TaskStatus status = task.getStatus();
			if (TaskStatus.DELETED.equals(status))
				continue;
			String desc = task.getDescription();
			if (desc == null || desc.trim().equals(""))
				desc = "";
			String workflow = task.getFlowName();
			if (workflow == null || workflow.trim().equals(""))
				workflow = "N/A";
			json.append("\n\t{\"task\":\"");
			json.append(task.getID());
			json.append("\",\"desc\":\"");
			json.append(JSONObject.escape(desc));
			json.append("\",\"user\":\"");
			json.append(task.getUser());
			json.append("\",\"workflow\":\"");
			json.append(task.getFlowName());
			json.append("\",\"version\":\"");
			json.append(task.getFlowVersion());
			json.append("\",\"site\":\"");
			json.append(task.getSite());
			json.append("\",\"status\":\"");
			json.append(task.getStatus().toString());
			json.append("\",\"createdMillis\":\"");
			json.append(task.getCreateTime().getTime());
			json.append("\",\"created\":\"");
			json.append(new SimpleDateFormat("MMM. d, yyyy, h:mm a").format(
				task.getCreateTime()));
			json.append("\",\"elapsedMillis\":\"");
			json.append(task.getElapsedTime());
			json.append("\",\"elapsed\":\"");
			json.append(FormatUtils.formatTimePeriod(task.getElapsedTime()));
			json.append("\",\"id\":\"");
			json.append(i);
			json.append("\"},");
			i++;
		}
		// chomp trailing comma and close JSON array
		json.setLength(json.length() - 1);
		json.append("\n]");
		return json.toString();
	}

}
