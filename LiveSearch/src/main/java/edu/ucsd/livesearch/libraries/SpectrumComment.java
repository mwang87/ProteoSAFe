package edu.ucsd.livesearch.libraries;

import java.sql.Timestamp;

public class SpectrumComment {

	
	int spectrum_id;
	String comment;
	String task_id;
	String annotation_task_id;
	String user_id;
	String execution_site;
	Timestamp create_time;

	
	

	

	

	public SpectrumComment(int spectrum_id, String comment, String task_id) {
		super();
		this.spectrum_id = spectrum_id;
		this.comment = comment;
		this.task_id = task_id;
	}
	
	@Override
	public String toString() {
		return "SpectrumComment [spectrum_id=" + spectrum_id + ", comment="
				+ comment + ", task_id=" + task_id + ", annotation_task_id="
				+ annotation_task_id + "]";
	}
	
	public String toJSON(){
		StringBuilder result = new StringBuilder();
		
		result.append("{ ");
		result.append("\"spectrum_id\" : \"" + spectrum_id + "\" ,");
		result.append("\"comment\" : \"" + comment + "\" ,");
		result.append("\"task\" : \"" + task_id + "\" ,");
		result.append("\"execution_site\" : \"" + execution_site + "\" ,");
		result.append("\"annotation_task_id\" : \"" + annotation_task_id + "\" ,");
		result.append("\"create_time\" : \"" + create_time + "\" ,");
		result.append("\"user_id\" : \"" + user_id + "\" ");
		
		result.append(" }");
		
		return result.toString();
	}

	

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	
	public String getAnnotation_id() {
		return annotation_task_id;
	}

	public void setAnnotation_id(String annotation_id) {
		this.annotation_task_id = annotation_id;
	}
	
	public int getSpectrum_id() {
		return spectrum_id;
	}
	public void setSpectrum_id(int spectrum_id) {
		this.spectrum_id = spectrum_id;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getTask_id() {
		return task_id;
	}
	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	public String getExecution_site() {
		return execution_site;
	}
	public void setExecution_site(String execution_site) {
		this.execution_site = execution_site;
	}
	public Timestamp getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Timestamp create_time) {
		this.create_time = create_time;
	}
	
	
}
