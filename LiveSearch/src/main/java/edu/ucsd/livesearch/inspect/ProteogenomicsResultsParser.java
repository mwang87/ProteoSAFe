package edu.ucsd.livesearch.inspect;

import java.io.Writer;

import edu.ucsd.livesearch.ResultsParser;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

public class ProteogenomicsResultsParser
implements ResultsParser
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private ProteogenomicsResult result;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ProteogenomicsResultsParser(Task task) {
		setTask(task);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public final String getResultType() {
		return "proteogenomics";
	}
	
	public final String getDownloadType() {
		return getResultType();
	}
	
	public final long size() {
		return getResult().size();
	}
	
	public final boolean available() {
		return isValid(getTask());
	}
	
	public final boolean ready() {
		return getResult().ready();
	}
	
	public final void writeHitsJS(Writer writer, String variable) {
		ProteogenomicsUtils.writeHitsJS(writer, variable, getTask());
	}
	
	public final void writeHitsJS(Writer writer, String variable, Long hit) {
		ProteogenomicsUtils.writeHitsJS(writer, variable, getTask());
	}
	
	public final void writeErrorsJS(Writer writer, String variable) {
		
	}
	
	/*========================================================================
	 * Public utility methods
	 *========================================================================*/
	public static final boolean isValid(Task task) {
		return (task != null &&
				task.getStatus() != TaskStatus.NONEXIST &&
				task.getFlowName().matches("PROTEOGENOMICS"));
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	protected final Task getTask() {
		return task;
	}
	
	protected final void setTask(Task task) {
		if (task == null)
			throw new IllegalArgumentException("ProteogenomicsResultsParser " +
				"objects must be initialized with a non-null task.");
		this.task = task;
		this.result = new ProteogenomicsResult(task);
	}
	
	protected final ProteogenomicsResult getResult() {
		return result;
	}
}
