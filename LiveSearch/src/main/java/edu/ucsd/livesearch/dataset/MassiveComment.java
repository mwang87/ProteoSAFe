package edu.ucsd.livesearch.dataset;

import java.sql.Timestamp;

import org.json.simple.JSONObject;

public class MassiveComment {
	String user;
	String task_id;
	int massive_id;
	String comment;
	Timestamp create_time;
	String execution_site;
	
	

	public String toJSON(){
		StringBuilder result = new StringBuilder();
		
		result.append("{ ");
		result.append("\"user_id\" : \"" + user + "\" ,");
		result.append("\"task\" : \"" + task_id + "\" ,");
		result.append("\"execution_site\" : \"" + execution_site + "\" ,");
		result.append("\"massive_id\" : \"" + massive_id + "\" ,");
		result.append("\"comment\" : \"" + JSONObject.escape(comment) + "\" ,");
		result.append("\"create_time\" : \"" + create_time + "\" ");
		
		result.append(" }");
		
		return result.toString();
	}
	
	
	
	@Override
	public String toString() {
		return "MassiveComment [user=" + user + ", task_id=" + task_id
				+ ", massive_id=" + massive_id + ", comment=" + comment
				+ ", create_time=" + create_time + ", execution_site="
				+ execution_site + "]";
	}



	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getTask_id() {
		return task_id;
	}
	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	public int getMassive_id() {
		return massive_id;
	}
	public void setMassive_id(int massive_id) {
		this.massive_id = massive_id;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public Timestamp getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Timestamp create_time) {
		this.create_time = create_time;
	}
	
	public String getExecution_site() {
		return execution_site;
	}

	public void setExecution_site(String execution_site) {
		this.execution_site = execution_site;
	}
	
}
