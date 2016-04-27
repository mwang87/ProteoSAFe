package edu.ucsd.livesearch.task;


import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.storage.UploadManager.PendingUpload;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;
import edu.ucsd.livesearch.util.Commons;
import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.WebAppProps;
import edu.ucsd.saint.commons.http.HttpGetAgent;
import edu.ucsd.saint.commons.http.SteadyRetry;

public class WorkflowUtils{

	private static final Logger logger = LoggerFactory.getLogger(WorkflowUtils.class);
	private static TaskQueue queue;
	private static Set<Task> submissionQueue; // 
	
	static{
		queue = new TaskQueue();
		submissionQueue = new HashSet<Task>();
	}
	
	public static void queueUploadingTask(Task task) {
		if (task == null)
			return;
		TaskManager.setUploading(task);
		queue.queueTask(task);
		pollQueue();
	}
	
	public static List<Task> pollQueue() {
		return queue.pollQueue();
	}
	
	public static Map<String, Set<String>> getQueuedTasks() {
		return queue.getQueue();
	}
	
	public static Map<String, PendingUpload> getUploadsByTask(String taskId) {
		return queue.getUploadsByTask(taskId);
	}
	
	public static List<Task> cancelUpload(String path) {
		return queue.cancelUpload(path);
	}

	public static void relaunchWorkflows(){
		String site = WebAppProps.get("livesearch.site.name");
		for(Task t: TaskManager.queryTasksBySite(TaskStatus.RUNNING, site)){
			WorkflowUtils.launchWorkflow(t);
		}
	}

	public static void launchWorkflow(Task task){
		TaskManager.setRunning(task);
		synchronized(submissionQueue){
			submissionQueue.add(task);
		}
	}

	private static Thread launch = null;

	public static void start(){
		relaunchWorkflows();
		if(launch == null){
			launch = new LaunchThread();
			Helpers.startAsDaemon(launch);
		}
	}
	
	private static class LaunchThread extends Thread{
		public LaunchThread(){
			super("Task launching thread");
		}

		public void run(){
			try{
				while(true){
					Thread.sleep(30000);
					doLaunch();
				}
			}
			catch(Exception e){
				logger.info("Task launching thread terminated", e);
			}
		}
	}

	private static void doLaunch(){
		List<Task> launched = new LinkedList<Task>();
		List<Task> inSubmission = new LinkedList<Task>();
		synchronized(submissionQueue){
			inSubmission.addAll(submissionQueue);
		}
		for(Task task: inSubmission){
			if(informFlowEngine(task))
				launched.add(task);
		}
		synchronized(submissionQueue){
			for(Task task: launched)
				submissionQueue.remove(task);
		}
	}

	public static void stop(){
		launch.interrupt();
		launch = null;
	}
	
	private static boolean informFlowEngine(Task task){
		String id = task.getID();
		String flow = task.getFlowName();
		String context = WebAppProps.get("liveflow.service.url");
		String service = WebAppProps.get("liveflow.service.launch");

		String url = context + "/" + service;
		url = url.replace("{task}", id);
		url = url.replace("{flow}", flow.toLowerCase());
		url = url.replace("{site}", WebAppProps.get("livesearch.site.url"));

		HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(0, 1));
		try{
			logger.info(String.format(
					"Begin launching task [%s: %s] to%n [%s]", id, flow, url));			
			HttpEntity entity = agent.execute(url);
			entity.getContent().close();
			logger.info(String.format(
				"Succeeded in launching task [%s: %s] to%n [%s]", id, flow, url));
			return true;
		}catch(Exception e){
			logger.info(String.format(
				"Failed to launch task [%s:%s]to%n [%s]", id, flow, context));
			return false;
		}
		finally{
			agent.close();
		}
	}
	
//	private static boolean informGridEngine(List<Task> tasks)throws Exception{
//		String context = WebAppProps.get("livegrid.service.url");
//		String service = WebAppProps.get("livegrid.service.inform");
//		String url = context + "/" + service;
//		
//		HttpClient httpclient = new DefaultHttpClient();
//		HttpPost httppost = new HttpPost(url);
//		MultipartEntity reqEntity = new MultipartEntity();
//		
//		Document doc = XmlUtils.createXML();
//		DocumentWrapper D = new DocumentWrapper(doc);
//		Element emtTasks = D.E("tasks");
//		for(Task task: tasks){
//			String id = task.getID();
//			String flow = flowCodes.get(task.getFlowName());
//			emtTasks.appendChild(
//				D.E("task", 
//				D.E("ID", D.T(id)),
//				D.E("flow", D.T(flow)),
//				D.E("user", D.T(task.getUser()))));
//		}
//		OutputStream out = new ByteArrayOutputStream();
//		XmlUtils.printXML(doc, out);
//		StringBody content = new StringBody(out.toString());
//		reqEntity.addPart("info", content);
//		httppost.setEntity(reqEntity);
//
//		final int maxRetry = 3;
//		for(int i = 0; i < maxRetry; i++){
//			try{
//				HttpResponse response = httpclient.execute(httppost);
//				HttpEntity resEntity = response.getEntity();
//				response.getStatusLine();
//				StatusLine status = response.getStatusLine();
//				if (status.getStatusCode() == 200 || resEntity != null) {
//				    resEntity.consumeContent();
//					logger.info(String.format("Succeeded in informing task info"));
//					return true;
//				}
//        	}
//        	catch(Exception e){
//				logger.info(String.format("Failed to inform task info (%d)", i), e);        	
//			}
//			Thread.sleep(180000);
//        }
//		return false;
//	}
	
	public static boolean abortWorkflow(Task task){
		// if task was just queued, simply remove it from the queue
		if (queue.cancelTask(task))
			return true;
		
		String id = task.getID();
	    String context = WebAppProps.get("liveflow.service.url");
	    String service = WebAppProps.get("liveflow.service.purge");

	    String url = context + "/" + service;
	    url = url.replace("{task}", id);
	    boolean success = false;
		HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(60000, 30));
	    try{
	    	HttpEntity entity = agent.execute(url);
	    	entity.getContent().close();
        	success = true;
	    }catch(Exception e){
	    	logger.info(String.format(
	    		"Failed to purge task [%s]", task.getID()), e);
	    }
		finally{
			agent.close();
		}
        if(success) logger.info(String.format(
            "Succeeded in aborting workflow instance [%s]", id));
        else logger.info(String.format(
            "Failed to abort workflow instance [%s]", id));
        return success;
	}

	public static void notifyCompletion(Task task, String status, String message){
		if(status.equals("FINISHED"))
			TaskManager.setDone(task);
		else{
			Collection<String> msgs = new LinkedList<String>();
			msgs.add(message);
			TaskManager.setFailed(task, msgs);
			String site = WebAppProps.get("livesearch.site.url");
			String msgContent = String.format(
				"Task is failed.  Follow the link for more information: %s/status.jsp?task=%s",
				new Object[] { site, task.getID() });
			Commons.contactAdministrator(msgContent);
		}
		Commons.sendCompletionEmail(task, status);
		
	}
}
