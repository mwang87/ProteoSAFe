package edu.ucsd.livesearch.storage;

import java.sql.Timestamp;

public class ResourceRecord{
	private String taskID;
	private String user;
	private String originalName;
	private String savedAs;
	private String purpose;
	private Timestamp creationTime;
	
	ResourceRecord(String taskID, String user, String original, String savedAs, String purpose, Timestamp uploadTime) {
		super();
		this.taskID = taskID;
		this.user = user;
		this.originalName = original;
		this.savedAs = savedAs;
		this.purpose = purpose;
		this.creationTime = uploadTime;
	}

	public String getOriginalName() {
		return originalName;
	}

	public String getSavedAs() {
		return savedAs;
	}

	public String getID() {
		return taskID;
	}

	public Timestamp getCreationTime() {
		return creationTime;
	}

	public String getUser() {
		return user;
	}

	public String getPurpose() {
		return purpose;
	}
}
