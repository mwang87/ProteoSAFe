package edu.ucsd.livesearch.task;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.storage.ResourceManager;
import edu.ucsd.livesearch.storage.ResourceRecord;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
//import edu.ucsd.livesearch.util.VersionTuple;

public class Task{
	String		id, user, flow, version, msg, desc, email, site;
	TaskStatus	status;
	TaskConfig	config;
	Timestamp	createTime, beginTime, endTime;
	long 		elapsedTime;
//	VersionTuple version;
	
	protected Task(){
		id		= "N/A";
		user	= "N/A";
		flow	= null;
		version	= null;
		desc	= "";
		msg		= "";
		email	= "";
		site	= "";
		config  = null;
		status	= TaskStatus.NONEXIST;
		
		createTime = beginTime = endTime = null;			
		elapsedTime = 0;
	}

	Task(
		String id, String user, String flow, TaskStatus status,
		String site, String version
	) {
		this();
		this.id = id;
		this.user = user;
		this.flow = flow;
		this.status = status;
		this.site = site;
		this.version = version;
	}

	public void setTimes(Timestamp createTime, Timestamp beginTime, Timestamp endTime, long elapsedTime){
		this.createTime  = createTime;
		this.beginTime   = beginTime;
		this.endTime     = endTime;
		this.elapsedTime = elapsedTime;
	}

	public void setStatus(TaskStatus status) { this.status = status; }
	public void setMessage(String msg)       { this.msg = msg; }
	public void setDescription(String desc)  { this.desc = desc; }
	public void setNotification(String email){ this.email = email; }

	public String		getID()				{ return id; }
	public String		getUser()			{ return user; }
	public TaskStatus	getStatus()			{ return status; }
	public String		getFlowName()		{ return flow; }
	public String		getFlowVersion()	{ return version; }
//	public VersionTuple getVersion()		{ return version; }
	public String		getMessage()		{ return msg; }
//	public String		getProgress()		{ return TaskManager.queryProgress(taskID); }
	public String		getDescription()	{ return desc; }
	public String		getNotification()	{ return email; }
	public Timestamp	getCreateTime()		{ return createTime; }
	public Timestamp	getBeginTime()		{ return beginTime; }
	public Timestamp	getEndTime()		{ return endTime; }
	public long			getElapsedTime()	{ return elapsedTime; }
	public String		getSite()			{ return site; }
	
	public TaskConfig getConfig(){
		if(config == null) config = new TaskConfig(this);
		return config;
	}
	
	public File	getPath(String path){
		return FileManager.getFile(user, id, path);		
	}

	public void setFailures(List<String> failures){
		TaskManager.setFailed(this, failures);
	}
	
	public void setFlowName(String flow){
		this.flow = flow;
		TaskManager.setFlowName(this, flow);		
	}
	
	public void setFlowVersion(String version){
		this.version = version;
		TaskManager.setFlowVersion(this, version);		
	}

	public void setComment(String comment){	
		TaskManager.setComment(this, comment);		
	}
	
//	public void setSite(String site){
//		TaskManager.setSite(this, site);
//	}
	
	private boolean mapLoaded = false;
	private List<ResourceRecord> uploads = null;
	private Map<String, ResourceRecord> uploadMap = null;
	
	private void loadMapping(){
		uploads = ResourceManager.queryAssociatedResource(this);
		uploadMap = new HashMap<String, ResourceRecord>(uploads.size());
		for (ResourceRecord upload : uploads) {
			uploadMap.put(upload.getSavedAs(), upload);	
		}
		mapLoaded = true;
	}
	
	public String queryOriginalName(String savedAs) {
		if (savedAs == null)
			return null;
		else if (mapLoaded == false)
			loadMapping();
		ResourceRecord upload = uploadMap.get(savedAs);
		// necessary for proper lazy loading
		if (upload == null) {
			loadMapping();
			upload = uploadMap.get(savedAs);
		}
		return upload != null ? upload.getOriginalName() : null;
	}
	
	public String queryInternalName(String originalName) {
		if (originalName == null)
			return null;
		else if (mapLoaded == false)
			loadMapping();
		// look through all the upload mappings to find this original filename
		for (ResourceRecord upload : uploadMap.values()) {
			if (upload == null)
				continue;
			String uploadName = upload.getOriginalName();
			// if the original names don't match, try stripping
			// the path off the original filename from the record
			if (originalName.equals(uploadName) ||
				originalName.equals(FilenameUtils.getName(uploadName)))
				return upload.getSavedAs();
		}
		return null;
	}
	
	public List<String> queryUploadsByPurpose(String purpose){
		if(!mapLoaded) loadMapping();		
		List<String> result = new LinkedList<String>();
		for(ResourceRecord upload: uploads)
			if(purpose == null || purpose.equals(upload.getPurpose()))
				result.add(upload.getOriginalName());
		return result;
	}
	
	public String getSummaryString() {
		return String.format("[Task %s...] - \"%s\"",
			getID().substring(0, 4), getDescription());
	}
	
	@Override
	public String toString() {
		return getSummaryString();
	}
}
