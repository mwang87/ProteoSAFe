package edu.ucsd.livesearch.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.subscription.CIJobTimeStampComparater;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.subscription.ContinuousIDJob;
import edu.ucsd.livesearch.subscription.ContinuousIDManager;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;

public class ContinuousIDServlet extends BaseServlet{
	
	private static final Logger logger =
		LoggerFactory.getLogger(ContinuousIDServlet.class);
	
	//Getting Sub Status
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
		String username = (String)request.getSession().getAttribute("livesearch.user");
		String task_id = request.getParameter("task");
		String massive_id = request.getParameter("massiveid");
		//Checking if user is logged in;
		//if(username == null){
		//	response.getOutputStream().print("LOGIN");
		//	return;
		//}
		
		if(task_id != null){
			Dataset massive_dataset = DatasetManager.queryDatasetByTaskID(task_id);
			int dataset_id = massive_dataset.getDatasetID();
			response.getOutputStream().print("{\"dataset_id\" : " + dataset_id + ",");
			List<ContinuousIDJob> jobs = ContinuousIDManager.get_CI_Jobs(dataset_id);
			
			Collections.sort(jobs, new CIJobTimeStampComparater());
			
			//Writing jobs into json
			String jobs_json = "[";
			for(int i = 0; i < jobs.size(); i++){
				ContinuousIDJob job = jobs.get(i);
				
				jobs_json += job.toJSON();
				
				if(i < jobs.size() -1 ){
					jobs_json += ",\n";
				}
			}
			jobs_json += "]";
			
			response.getOutputStream().print("\"jobs\" : " + jobs_json + " }");
			
			return;
		}
		else{
			if(massive_id != null){
				Dataset massive_dataset = DatasetManager.queryDatasetByID(massive_id);
				int dataset_id = massive_dataset.getDatasetID();
				response.getOutputStream().print("{\"dataset_id\" : " + dataset_id + ",");
				List<ContinuousIDJob> jobs = ContinuousIDManager.get_CI_Jobs(dataset_id);
				
				Collections.sort(jobs, new CIJobTimeStampComparater());
				
				//Writing jobs into json
				String jobs_json = "[";
				for(int i = 0; i < jobs.size(); i++){
					ContinuousIDJob job = jobs.get(i);
					
					jobs_json += job.toJSON();
					
					if(i < jobs.size() -1 ){
						jobs_json += ",\n";
					}
				}
				jobs_json += "]";
				
				response.getOutputStream().print("\"jobs\" : " + jobs_json + " }");
				
				return;
			}
		}

		response.getOutputStream().print("ERROR");
	}
	
	/*
	 * Adding information 
	 */
	@Override
	protected void doPost(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException{
		String username = (String)request.getSession().getAttribute("livesearch.user");
		if(username.compareTo("continuous") == 0){
			String function = request.getParameter("function");
			if(function.compareTo("addcontinuous") == 0){
				String task_id = request.getParameter("task");
				String massive_id = request.getParameter("massive_id");
				Dataset dataset = DatasetManager.queryDatasetByID(massive_id);
				Task task = TaskManager.queryTask(task_id);
				if( task != null ){
					//SHITs!!!!
				}
				
				logger.info("Adding Continuous ID from Task " + task_id + " to Dataset " + massive_id);
				
				//response.getOutputStream().print(dataset.getDatasetIDString() + "\t" + task.getDescription());
				ContinuousIDManager.create_CI_Job(task_id, dataset.getDatasetID(), 0);
				response.getOutputStream().print("{\"status\": \"success\"}");
			}
			if(function.compareTo("removecontinuous") == 0){
				String task_id = request.getParameter("task");
				String massive_id = request.getParameter("massive_id");
				Dataset dataset = DatasetManager.queryDatasetByID(massive_id);
				Task task = TaskManager.queryTask(task_id);
				if( task != null ){
					//SHITs!!!!
				}
				
				logger.info("Removing Continuous ID from Task " + task_id + " to Dataset " + massive_id);
				
				ContinuousIDManager.remove_CI_Job(task_id, dataset.getDatasetID());
				response.getOutputStream().print("{\"status\": \"success\"}");
			}
		}
		else{
			response.getOutputStream().print("Not Ming");
		}
	}
}
