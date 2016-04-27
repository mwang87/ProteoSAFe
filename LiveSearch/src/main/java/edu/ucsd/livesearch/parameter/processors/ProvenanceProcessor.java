package edu.ucsd.livesearch.parameter.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.task.TaskManager;

public class ProvenanceProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(ProvenanceProcessor.class);
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Scans the task's uploaded files to determine which tasks and/or datasets,
	 * if any, they came from, and properly records this information.
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
		List<String> errors = new ArrayList<String>();
		// examine all uploaded files assigned to this task
		Collection<String> uploads =
			builder.getParameterValues("upload_file_mapping");
		if (uploads == null || uploads.isEmpty())
			return null;
		// gather all task and dataset references
		Map<String, Task> tasks = new LinkedHashMap<String, Task>();
		Map<String, Dataset> datasets = new LinkedHashMap<String, Dataset>();
		for (String upload : uploads) {
			String[] tokens = upload.split("\\|");
			if (tokens == null || tokens.length < 2)
				continue;
			String path = FilenameUtils.separatorsToUnix(tokens[1]);
			String[] directories = path.split("/");
			if (directories == null || directories.length < 1)
				continue;
			// if this file is from an imported dataset share,
			// then note it, unless it's already been noted
			else if (Dataset.isValidDatasetIDString(directories[0]) &&
				datasets.containsKey(directories[0]) == false) {
				// note it only if the dataset exists
				Dataset dataset =
					DatasetManager.queryDatasetByID(directories[0]);
				if (dataset != null)
					datasets.put(directories[0], dataset);
			}
			// if this file is from an imported task share,
			// then note it, unless it's already been noted
			else if (directories.length >= 2 &&
				TaskManager.isValidTaskIDString(directories[1]) &&
				tasks.containsKey(directories[1]) == false) {
				// note it only if the task exists
				Task task = TaskManager.queryTask(directories[1]);
				if (task != null && task instanceof NullTask == false)
					tasks.put(directories[1], task);
			}
		}
		// record all task references
		StringBuffer reanalyzed = new StringBuffer();
		for (String taskID : tasks.keySet()) {
			reanalyzed.append(taskID).append(";");
			if (copyParams(builder, tasks.get(taskID)) == false)
				errors.add(String.format(
					"There was an error copying the parameters for task [%s].",
					taskID));
		}
		// chomp trailing semicolon
		if (reanalyzed.length() > 0 &&
			reanalyzed.charAt(reanalyzed.length() - 1) == ';')
			reanalyzed.setLength(reanalyzed.length() - 1);
		// record the list of task references in params.xml
		if (reanalyzed.length() > 0)
			builder.setParameterValue(
				"reanalyzed_tasks", reanalyzed.toString());
		// record all dataset references
		reanalyzed = new StringBuffer();
		for (String datasetID : datasets.keySet())
			reanalyzed.append(datasetID).append(";");
		// chomp trailing semicolon
		if (reanalyzed.length() > 0 &&
			reanalyzed.charAt(reanalyzed.length() - 1) == ';')
			reanalyzed.setLength(reanalyzed.length() - 1);
		// record the list of dataset references in params.xml
		if (reanalyzed.length() > 0)
			builder.setParameterValue(
				"reanalyzed_datasets", reanalyzed.toString());
		if (errors.isEmpty())
			return null;
		else return errors;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private boolean copyParams(TaskBuilder builder, Task task) {
		if (builder == null || task == null)
			return false;
		Task thisTask = builder.getTask();
		// ensure that this task's provenance folder exists
		File provenance = thisTask.getPath("ccms_provenance");
		if (provenance.exists() == false && provenance.mkdir() == false)
			return false;
		// copy the other task's params.xml to this task's provenance folder
		File params = task.getPath("params/params.xml");
		if (params.canRead() == false)
			return false;
		File paramsCopy =
			new File(provenance, String.format("%s_params.xml", task.getID()));
		try {
			FileUtils.copyFile(params, paramsCopy);
			return true;
		} catch (Throwable error) { return false; }
	}
}
