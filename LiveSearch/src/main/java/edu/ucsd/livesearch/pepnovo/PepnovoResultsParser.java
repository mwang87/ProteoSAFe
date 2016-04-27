package edu.ucsd.livesearch.pepnovo;

import java.io.Writer;

import edu.ucsd.livesearch.ResultsParser;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

/**
 * Pepnovo results parser implementation.
 * 
 * @author Jeremy Carver
 */
public class PepnovoResultsParser
implements ResultsParser
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Task task;
	private PepnovoResult result;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public PepnovoResultsParser(Task task) {
		setTask(task);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public final String getResultType() {
		return "pepnovo";
	}
	
	public final String getDownloadType() {
		return getResultType();
	}
	
	// TODO: Implement PepnovoResult.size() method
	public final long size() {
		//return getResult().size();
		return 0L;
	}
	
	public final boolean available() {
		return isValid(getTask());
	}
	
	public final boolean ready() {
		return getResult().ready();
	}
	
	public final void writeHitsJS(Writer writer, String variable) {
		PepnovoUtils.writeHitsJS(writer, variable, getResult());
	}
	
	public final void writeHitsJS(Writer writer, String variable, Long hit) {
		PepnovoUtils.writeHitsJS(writer, variable, getResult());
	}
	
	public final void writeErrorsJS(Writer writer, String variable) {
		
	}
	
	/*========================================================================
	 * Public utility methods
	 *========================================================================*/
	public static final boolean isValid(Task task) {
		return (task != null &&
				task.getStatus() != TaskStatus.NONEXIST &&
				task.getFlowName().matches("PEPNOVO"));
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	protected final Task getTask() {
		return task;
	}
	
	protected final void setTask(Task task) {
		if (task == null)
			throw new IllegalArgumentException("PepnovoResultsParser " +
				"objects must be initialized with a non-null task.");
		this.task = task;
		this.result = new PepnovoResult(task);
	}

	protected final PepnovoResult getResult() {
		return result;
	}
}
