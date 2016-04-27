package edu.ucsd.livesearch.task;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.storage.FileManager;
import edu.ucsd.livesearch.storage.UploadManager;
import edu.ucsd.livesearch.storage.UploadManager.PendingUpload;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

public class TaskQueue
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	// task ID -> set of pending upload file paths for that task
	private Map<String, Set<String>> queue;
	private final Logger logger;
	
	/*========================================================================
	 * Constructor
	 *========================================================================*/
	public TaskQueue() {
		queue = new HashMap<String, Set<String>>();
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public final void queueTask(Task task) {
		if (task == null)
			return;
		StringBuffer message = new StringBuffer("Attempting to queue task \"");
		message.append(task.getID());
		message.append("\":\n");
		// find any pending uploads for this task
		Set<String> pending = new HashSet<String>();
		// check user-selected spectrum files, see if any are pending uploads
		List<String> spectra = task.queryUploadsByPurpose("spectrum");
		if (spectra != null && spectra.isEmpty() == false) {
			for (String spectrum : spectra) {
				File spectrumFile = FileManager.getFile("f." + spectrum);
				if (spectrumFile != null) {
					String path = spectrumFile.getAbsolutePath();
					if (isUploadPending(path))
						pending.add(path);
				}
			}
		}
		// check user-selected sequence files, see if any are pending uploads
		List<String> sequences = task.queryUploadsByPurpose("sequence");
		if (sequences != null && sequences.isEmpty() == false) {
			for (String sequence : sequences) {
				File sequenceFile = FileManager.getFile("f." + sequence);
				if (sequenceFile != null) {
					String path = sequenceFile.getAbsolutePath();
					if (isUploadPending(path))
						pending.add(path);
				}
			}
		}
		// if any pending uploads were found, queue the task
		if (pending != null && pending.isEmpty() == false) {
			queue.put(task.getID(), pending);
			message.append("\tThis task could not be launched, because the ");
			message.append("following associated files are still uploading:");
			for (String path : pending) {
				message.append("\n\t\t");
				message.append(path);
			}
		}
		// otherwise just launch the task
		else {
			launchTask(task);
			// remove the task from the queue, in case it was already there
			queue.remove(task.getID());
			message.append("\tThis task will be launched immediately, ");
			message.append("because no pending uploads could be found for it.");
		}
		logger.info(message.toString());
	}
	
	public final List<Task> pollQueue() {
		// first poll the file upload queue to remove any stalled uploads,
		// and in turn clear out any stalled tasks that depend on them
		UploadManager.pollQueue();
		if (queue == null || queue.size() < 1)
			return null;
		else {
			logger.info("Polling task queue of size " + queue.size() + ":");
			// build list of tasks ready to be launched, and launch them
			List<Task> launchedTasks = new Vector<Task>();
			for (String taskID : queue.keySet()) {
				if (isTaskReady(taskID)) {
					Task task = TaskManager.queryTask(taskID);
					launchTask(task);
					launchedTasks.add(task);
				}
			}
			// remove all launched tasks from the task queue
			for (Task task : launchedTasks)
				queue.remove(task.getID());
			// return the list of launched tasks
			if (launchedTasks.isEmpty())
				return null;
			else return launchedTasks;
		}
	}
	
	public final boolean cancelTask(Task task) {
		if (task == null)
			return false;
		return cancelTask(task.getID());
	}
	
	public final boolean cancelTask(String taskId) {
		if (taskId == null)
			return false;
		else if (queue.remove(taskId) == null)
			return false;
		else {
			logger.info("Task \"" + taskId + "\" was cancelled while " +
				"still in the task queue, so it has been removed.");
			return true;
		}
	}
	
	public final List<Task> cancelUpload(String path) {
		if (path == null || queue == null || queue.size() < 1)
			return null;
		else {
			// build list of tasks affected by the upload cancellation
			List<Task> failedTasks = new Vector<Task>();
			for (String taskID : queue.keySet()) {
				Set<String> paths = queue.get(taskID);
				if (paths != null && paths.contains(path)) {
					// get task
					Task task = TaskManager.queryTask(taskID);
					if (task == null)
						continue;
					StringBuffer error = new StringBuffer(
						"Task could not be launched because upload ");
					// get user
					String user = task.getUser();
					if (user != null) {
						// user must be packaged in a collection for calls to
						// FileManager.truncatePath()
						Set<String> users = new HashSet<String>(1);
						users.add(user);
						error.append("\"");
						error.append(FileManager.resolvePath(path, users));
						error.append("\" ");
					}
					error.append("did not complete successfully.");
					// error message must be packaged in a collection
					List<String> errors = new Vector<String>();
					errors.add(error.toString());
					TaskManager.setFailed(task, errors);
					task.setStatus(TaskStatus.FAILED);
					task.setFailures(errors);
					// add task to list of failed tasks
					failedTasks.add(task);
				}
			}
			// remove all failed tasks from the task queue
			for (Task task : failedTasks)
				queue.remove(task.getID());
			// return the list of failed tasks
			if (failedTasks.isEmpty())
				return null;
			else return failedTasks;
		}
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public final Map<String, Set<String>> getQueue() {
		return new HashMap<String, Set<String>>(queue);
	}
	
	public final boolean isUploadPending(String path) {
		if (path == null)
			return false;
		// check pending uploads for this path
		PendingUpload upload = UploadManager.getUploadByPath(path);
		if (upload != null && upload.isCompleted() == false)
			return true;
		else return false;
	}
	
	// truncated file path -> pending upload record
	public final Map<String, PendingUpload> getUploadsByTask(String taskId) {
		// get task
		if (taskId == null)
			return null;
		Task task = TaskManager.queryTask(taskId);
		if (task == null || task instanceof NullTask)
			return null;
		// get user
		String user = task.getUser();
		if (user == null)
			return null;
		// user must be packaged in a collection for calls to
		// FileManager.truncatePath()
		Set<String> users = new HashSet<String>(1);
		users.add(user);
		// build truncated path -> upload record map
		Set<String> pendingUploads = getQueue().get(taskId);
		if (pendingUploads == null || pendingUploads.isEmpty())
			return null;
		Map<String, PendingUpload> taskUploads =
			new HashMap<String, PendingUpload>(pendingUploads.size());
		for (String path : pendingUploads) {
			String truncatedPath = FileManager.resolvePath(path, users);
			PendingUpload upload = UploadManager.getUploadByPath(path);
			if (truncatedPath == null || upload == null || upload.isCompleted())
				continue;
			else taskUploads.put(truncatedPath, upload);
		}
		if (taskUploads.isEmpty())
			return null;
		else return taskUploads;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	protected final boolean isTaskReady(String taskID) {
		if (taskID == null)
			return false;
		Set<String> pending = queue.get(taskID);
		if (pending != null && pending.isEmpty() == false) {
			for (String path : pending) {
				if (isUploadPending(path)) {
					logger.info("Queued task \"" + taskID +
						"\" is not yet ready to be launched, because " +
						"uploaded file \"" + path + "\" is still pending.");
					return false;
				}
			}
		}
		logger.info("Queued task \"" + taskID + "\" is now ready to be " +
			"launched, because no pending uploads could be found for it.");
		return true;
	}
	
	protected final void launchTask(Task task) {
		// TODO: check for empty files, fail task if any are found
		if (task != null && task instanceof NullTask == false)
			WorkflowUtils.launchWorkflow(task);
	}
}
