package edu.ucsd.liveadmin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.parameter.GenerateMasses;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.WebAppProps;

public class Cleanup
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(Cleanup.class);
	private static final String TASK_SPACE =
		WebAppProps.getPath("livesearch.user.path", "");
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final void ConvertParameterFiles() {
		
	}
	
	public static final String GenerateMassesFiles() {
		// retrieve task directory map
		Map<String, List<File>> taskDirectoryMap = null;
		try {
			taskDirectoryMap = getTaskDirectoryMap();
		} catch (IOException error) {
			logger.error("There was an error retrieving task directories",
				error);
			return null;
		}
		// traverse users
		int examined = 0;
		int succeeded = 0;
		int failed = 0;
		for (String user : taskDirectoryMap.keySet()) {
			// traverse task directories belonging to this user
			List<File> taskDirectories = taskDirectoryMap.get(user);
			if (taskDirectories == null || taskDirectories.size() < 1)
				continue;
			for (File taskDirectory : taskDirectories) {
				examined++;
				// retrieve and validate this task
				Task task = TaskManager.queryTask(taskDirectory.getName());
				if (task == null || task instanceof NullTask) {
					logger.error("Task directory (\"" +
						taskDirectory.getAbsolutePath() +"\") does not " +
						"correspond to a valid task in the database.");
					failed++;
					continue;
				}
				// generate masses file for this task, if necessary
				GenerateMasses masses = new GenerateMasses(task);
				if (masses.resourceExists() && masses.resourceDated() == false)
					continue;
				else if (OnDemandLoader.load(masses))
					succeeded++;
				else {
					logger.error("There was an error generating " +
						"masses file (\"" + masses.getResourceName() + "\").");
					failed++;
				}
			}
		}
		String message = "Generating amino acid masses files:" +
			"\nTasks with an existing masses file = " +
				(examined - (succeeded + failed)) +
			"\nTasks succeeded = " + succeeded +
			"\nTasks failed = " + failed;
		logger.info(message);
		return message.replace("\n", "<br/>");
	}
	
	public static final String DeleteLabelFiles() {
		// retrieve task directory map
		Map<String, List<File>> taskDirectoryMap = null;
		try {
			taskDirectoryMap = getTaskDirectoryMap();
		} catch (IOException error) {
			logger.error("There was an error retrieving task directories.",
				error);
			return null;
		}
		// traverse users
		int examined = 0;
		int succeeded = 0;
		int failed = 0;
		for (String user : taskDirectoryMap.keySet()) {
			// traverse task directories belonging to this user
			List<File> taskDirectories = taskDirectoryMap.get(user);
			if (taskDirectories == null || taskDirectories.size() < 1)
				continue;
			for (File taskDirectory : taskDirectories) {
				examined++;
				// retrieve and validate this task's label directory
				File labelDirectory = new File(taskDirectory, "label");
				// if the label directory does not exist, continue
				try { FileIOUtils.validateFile(labelDirectory); }
				catch (IOException error) { continue; }
				// throw an error if the label directory
				// exists but is inaccessible
				try {
					FileIOUtils.validateReadableFile(labelDirectory);
					FileIOUtils.validateDirectory(labelDirectory);
				} catch (IOException error) {
					logger.error("Label directory (\"" +
						labelDirectory.getAbsolutePath() +
						"\") cannot be accessed.", error);
					failed++;
					continue;
				}
				// delete all files in this task's label directory
				File[] labels = labelDirectory.listFiles();
				if (labels == null || labels.length < 1)
					continue;
				boolean deletionFailed = false;
				for (int i=0; i<labels.length; i++) {
					File label = labels[i];
					try {
						if (label.delete() == false)
							throw new IOException("File deletion failed.");
					} catch (IOException error) {
						logger.error("Label file (\"" +
							label.getAbsolutePath() + "\") cannot be deleted",
							error);
						deletionFailed = true;
						continue;
					}
				}
				if (deletionFailed)
					failed++;
				else succeeded++;
			}
		}
		String message = "Deleting label files:" +
			"\nTasks with no label files = " +
				(examined - (succeeded + failed)) +
			"\nTasks succeeded = " + succeeded +
			"\nTasks failed = " + failed;
		logger.info(message);
		return message.replace("\n", "<br/>");
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static void failFileAccess(String message)
	throws IOException {
		logger.error(message);
		throw new IOException(message);
	}
	
	private static void failFileAccess(String message, IOException error)
	throws IOException {
		logger.error(message, error);
		IOException checked = new IOException(message);
		checked.initCause(error);
		throw checked;
	}
	
	private static Map<String, List<File>> getTaskDirectoryMap()
	throws IOException {
		// set up top-level tasks directory
		File directory = new File(TASK_SPACE);
		try {
			FileIOUtils.validateReadableFile(directory);
			FileIOUtils.validateDirectory(directory);
		} catch (IOException error) {
			failFileAccess("Task space (\"" + TASK_SPACE +
				"\") cannot be accessed.", error);
		}
		// traverse users
		File[] users = directory.listFiles();
		if (users == null || users.length < 1)
			failFileAccess("No user directories were found " +
				"under task space (\"" + TASK_SPACE + "\").");
		Map<String, List<File>> taskDirectoryMap =
			new HashMap<String, List<File>>(users.length);
		for (int i=0; i<users.length; i++) {
			// set up user directory
			File user = users[i];
			// skip non-directory files
			try { FileIOUtils.validateDirectory(user); }
			catch (IOException error) { continue; }
			// throw an error if this is a directory but cannot be read
			try { FileIOUtils.validateReadableFile(user); }
			catch (IOException error) {
				failFileAccess("User task directory (\"" + 
					user.getAbsolutePath() + "\") cannot be accessed.", error);
			}
			// traverse task directories belonging to this user
			File[] tasks = user.listFiles();
			if (tasks == null || tasks.length < 1)
				continue;
			for (int j=0; j<tasks.length; j++) {
				// set up task directory
				File task = tasks[j];
				// skip non-directory files
				try { FileIOUtils.validateDirectory(task); }
				catch (IOException error) { continue; }
				// throw an error if this is a directory but cannot be read
				try { FileIOUtils.validateReadableFile(task); }
				catch (IOException error) {
					failFileAccess("Task directory (\"" +
						task.getAbsolutePath() + "\") cannot be accessed.",
						error);
				}
			}
			taskDirectoryMap.put(user.getName(), Arrays.asList(tasks));
		}
		return taskDirectoryMap;
	}
}
