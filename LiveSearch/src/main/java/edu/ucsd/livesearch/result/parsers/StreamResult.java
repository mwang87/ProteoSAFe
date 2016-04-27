package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

/**
 * Result implementation for handling workflow result files that are meant to
 * be streamed directly to the result view, without any parsing or processing
 * (e.g. images or pre-rendered HTML files).
 * 
 * The purpose of this class is basically just to verify the existence and
 * readability of such a result file, and to provide a string handle to it
 * so that the client can then fetch it via a separate service.
 * 
 * @author Jeremy Carver
 */
public class StreamResult
implements Result
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(StreamResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected File resultFile;
	protected Task task;
	protected String contentType;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public StreamResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		init(resultFile, task, block);
	}
	
	public StreamResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		// validate result
		if (result == null)
			throw new NullPointerException("Previous result cannot be null.");
		// ensure that previous result file is written
		else if (OnDemandLoader.load(result) == false)
			throw new IllegalArgumentException(
				"Previous result file could not be written.");
		init(result.getFile(), result.getTask(), block);
	}
	
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		// set result file
		if (resultFile == null)
			throw new NullPointerException("Result file cannot be null.");
		else if (resultFile.isFile() == false)
			throw new IllegalArgumentException(
				String.format(
					"Result file \"%s\" must be a normal (non-directory) file.",
					resultFile.getAbsolutePath()));
		else if (resultFile.canRead() == false)
			throw new IllegalArgumentException(
				String.format("Result file \"%s\" must be readable.",
					resultFile.getAbsolutePath()));
		this.resultFile = resultFile;
		// set task
		if (task == null)
			throw new NullPointerException("Task cannot be null.");
		else if (task instanceof NullTask)
			throw new IllegalArgumentException(
				"Task cannot be an instance of \"NullTask\".");
		else if (TaskStatus.DONE.equals(task.getStatus()) == false)
			throw new IllegalArgumentException(
				"Task must be successfully completed to process its results.");
		this.task = task;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean isLoaded() {
		return resultFile.canRead();
	}
	
	public void load()
	throws IOException {
		if (isLoaded() == false)
			throw new IOException("Result file could not be loaded.");
	}
	
	public void close() {
		return;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public File getFile() {
		return resultFile;
	}
	
	public String getData() {
		if (isLoaded() == false)
			return null;
		try {
			String taskRoot = task.getPath("").getAbsolutePath();
			String absolutePath = resultFile.getAbsolutePath();
			if (absolutePath.startsWith(taskRoot) == false) {
				logger.error(
					String.format("Error determining relative path of task " +
						"result file \"%s\": this file does not appear to be " +
						"present under task directory \"%s\".",
						absolutePath, taskRoot));
				return null;
			}
			String relativePath = absolutePath.substring(taskRoot.length());
			if (relativePath.startsWith("/"))
				relativePath = relativePath.substring(1);
			return "\"" + relativePath + "\"";
		} catch (Throwable error) {
			logger.error(
				String.format("Error determining relative path of task " +
				"result file \"%s\"", resultFile.getAbsolutePath()),
				error);
			return null;
		}
	}
	
	public Long getSize() {
		if (isLoaded() == false)
			return 0L;
		else return resultFile.length();
	}
	
	public Task getTask() {
		return task;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	public boolean execute() {
		return resultFile.canRead();
	}
	
	public boolean resourceExists() {
		return resultFile.exists();
	}
	
	public boolean resourceDated() {
		return false;
	}
	
	public String getResourceName() {
		return resultFile.getAbsolutePath();
	}
}
