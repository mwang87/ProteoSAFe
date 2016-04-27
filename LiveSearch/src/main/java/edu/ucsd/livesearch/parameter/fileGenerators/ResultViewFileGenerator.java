package edu.ucsd.livesearch.parameter.fileGenerators;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Document;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.servlet.DownloadWorkflowInterface;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.util.FileIOUtils;

public class ResultViewFileGenerator
implements ParameterProcessor
{
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Copies the appropriate result view specification file for this task's
	 * workflow type to the correct task directory.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		
		// get master copy of result view specification document
		String workflow = task.getFlowName();
		Document resultDocument = null;
		try {
			resultDocument = DownloadWorkflowInterface.getWorkflowSpecification(
				workflow, "result", null, null);
		} catch (Throwable error) {
			List<String> errors = new Vector<String>(1);
			errors.add("Failed to retrieve result view specification file.");
			return errors;
		}
		
		// save document to file in task directory
		File resultFile = task.getPath("workflow/result.xml");
		boolean written = FileIOUtils.writeFile(
			resultFile, FileIOUtils.printXML(resultDocument), false);
		if (written == false) {
			List<String> errors = new Vector<String>(1);
			errors.add("Failed to write result view specification file.");
			return errors;
		} else return null;
	}
}
