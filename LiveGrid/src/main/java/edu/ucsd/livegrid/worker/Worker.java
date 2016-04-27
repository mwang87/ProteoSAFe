package edu.ucsd.livegrid.worker;

import java.util.Set;

public interface Worker {

	public abstract String getName();

	public abstract void setFlowEngine(String flowEngineAddr, int flowEnginePort);

	public abstract boolean isValid();

	public abstract void terminateZombies();

	public abstract void issueRequest(String subject, String action);

	public Set<String> getSubjects();
	
	public abstract void fulfillRequests();

}