package edu.ucsd.livesearch.subscription;

import java.sql.Timestamp;

import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskFactory;


public class ContinuousIDJob {
	private String task_id;
	private int dataset_id;
	private Timestamp timestamp;
	private int reported;
	private String job_status;
	private String execution_site;
	private String workflow_name;
	

	


	public String toJSON(){
		StringBuilder result = new StringBuilder();
		
		result.append("{ ");
		result.append("\"task\" : \"" + task_id + "\" ,");
		result.append("\"workflowname\" : \"" +  workflow_name + "\" ,");
		result.append("\"dataset_id\" : \"" + dataset_id + "\" ,");
		result.append("\"timestamp\" : \"" + timestamp + "\" ,");
		result.append("\"reported\" : \"" + reported + "\" ,");
		result.append("\"jobstatus\" : \"" + job_status + "\" ,");
		result.append("\"execution_site\" : \"" + execution_site + "\" ");
		
		result.append(" }");
		
		return result.toString();
	}


	public ContinuousIDJob(String task_id, int dataset_id, Timestamp timestamp,
			int reported, String job_status) {
		super();
		this.task_id = task_id;
		this.dataset_id = dataset_id;
		this.timestamp = timestamp;
		this.reported = reported;
		this.job_status = job_status;
	}
	


	@Override
	public String toString() {
		return "ContinuousIDJob [task_id=" + task_id + ", dataset_id="
				+ dataset_id + ", timestamp=" + timestamp + ", reported="
				+ reported + ", job_status=" + job_status + "]";
	}




	public String getJob_status() {
		return job_status;
	}



	public void setJob_status(String job_status) {
		this.job_status = job_status;
	}



	public String getTask_id() {
		return task_id;
	}
	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	public int getDataset_id() {
		return dataset_id;
	}
	public void setDataset_id(int dataset_id) {
		this.dataset_id = dataset_id;
	}
	public Timestamp getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	public int getReported() {
		return reported;
	}
	public void setReported(int reported) {
		this.reported = reported;
	}
	public String getExecution_site() {
		return execution_site;
	}
	public void setExecution_site(String execution_site) {
		this.execution_site = execution_site;
	}
	public void setWorkflow_name(String workflow_name) {
		this.workflow_name = workflow_name;
	}

}
