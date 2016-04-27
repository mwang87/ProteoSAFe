package edu.ucsd.livesearch.msdeconv;

import javax.servlet.http.HttpServletRequest;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

public class MsdeconvUtils {
	public static String generateBrowseBlock(Task task, HttpServletRequest request){
		StringBuffer buffer = new StringBuffer();
		if(task.getStatus() == TaskStatus.DONE){
			String context = request.getContextPath();
			String format = 
				String.format("<a href='%s/msdeconv/%%s?task=%s'>%%s</a>", context, task.getID());
			buffer.append("[ ");
			buffer.append(String.format(format, "result.jsp", "result")).append(" ] \n");
		}
		return buffer.toString();
	}

}
