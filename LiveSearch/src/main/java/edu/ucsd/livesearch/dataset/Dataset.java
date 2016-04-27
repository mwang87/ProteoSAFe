package edu.ucsd.livesearch.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import edu.ucsd.livesearch.publication.Publication;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.livesearch.util.FileIOUtils;
import edu.ucsd.saint.commons.WebAppProps;

public class Dataset
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	public static final String DATASET_ID_PREFIX = "MSV";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private int     datasetID;
	private Task    task;
	private File	datasetDirectory;
	private Date    created;
	private String  repositoryPath;
	private String  description;
	private int     fileCount;
	private long    fileSize;
	private String	species;
	private String	instrument;
	private String	modification;
	private String	pi;
	private boolean isComplete;
	private boolean isPrivate;
	private boolean readable;
	private Map<String, String>     annotations;
	private Collection<Publication> publications;

	/**
	 * Dataset constructor. 
	 * @param datasetID
	 * @param created
	 * @param taskID
	 * @param repositoryPath
	 * @param description
	 * @param fileCount
	 * @param fileSize
	 * @param species
	 * @param instrument
	 * @param modification
	 * @param pi
	 * @param isComplete
	 * @param isPrivate
	 * @param task can be passed so the we don't have to do the query. If null, then query is made to DB.
	 */
	public Dataset(
		int datasetID, Date created, String taskID, String repositoryPath,
		String description, int fileCount, long fileSize, String species,
		String instrument, String modification, String pi,
		boolean isComplete, boolean isPrivate, Task task
	) {
		// retrieve associated task
		if (taskID == null)
			throw new NullPointerException(
				"A dataset's task ID cannot be null.");
		// query for task only if it's not already known
		if (task == null)
			task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask)
			throw new NullPointerException(
				"No task could be found for this dataset.");
		else this.task = task;
		// set scalar properties
		setDatasetID(datasetID);
		setCreated(created);
		setRepositoryPath(repositoryPath);
		setDescription(description);
		setFileCount(fileCount);
		setFileSize(fileSize);
		setSpecies(species);
		setInstrument(instrument);
		setModification(modification);
		setPI(pi);
		setComplete(isComplete);
		setPrivate(isPrivate);
		annotations = new LinkedHashMap<String, String>();
		publications = null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public static final int parseDatasetIDString(String datasetID) {
		if (datasetID == null)
			throw new NullPointerException("Dataset ID string cannot be null.");
		else if (datasetID.startsWith(DATASET_ID_PREFIX) == false)
			throw new IllegalArgumentException(String.format(
				"Dataset ID string \"%s\" does not begin with " +
				"the required MassIVE dataset ID prefix \"%s\".",
				datasetID, DATASET_ID_PREFIX));
		else try {
			return Integer.parseInt(
				datasetID.substring(DATASET_ID_PREFIX.length()));
		} catch (NumberFormatException error) {
			throw new IllegalArgumentException(String.format(
				"Dataset ID string \"%s\" does not end with " +
				"a valid integer ID.", datasetID), error);
		}
	}
	
	public static final String generateDatasetIDString(int datasetID) {
		return String.format("%s%09d", DATASET_ID_PREFIX, datasetID);
	}
	
	public static final boolean isValidDatasetIDString(String datasetID) {
		try {
			String generated =
				generateDatasetIDString(parseDatasetIDString(datasetID));
			return generated.equals(datasetID);
		} catch (Throwable error) {
			return false;
		}
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getOwner() {
		return getTask().getUser();
	}
	
	public boolean isOwnedBy(String user) {
		if (user == null)
			return false;
		else return user.equals(getOwner());
	}
	
	public final int getDatasetID() {
		return datasetID;
	}
	
	public final void setDatasetID(int datasetID) {
		this.datasetID = datasetID;
	}
	
	public final String getDatasetIDString() {
		return generateDatasetIDString(datasetID);
	}
	
	public final void setDatasetIDString(String datasetID) {
		this.datasetID = parseDatasetIDString(datasetID);
	}
	
	public final Date getCreated() {
		return created;
	}
	
	public final void setCreated(Date created) {
		this.created = created;
	}
	
	public final long getCreatedMilliseconds() {
		return created.getTime();
	}
	
	public final String getCreatedString() {
		return new SimpleDateFormat("MMM. d, yyyy, h:mm a").format(created);
	}
	
	public final Task getTask() {
		return task;
	}
	
	public final String getTaskID() {
		return getTask().getID();
	}
	
	public final String getRepositoryPath() {
		return repositoryPath;
	}
	
	public final void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
		// verify repository path
		datasetDirectory = null;
		readable = false;
		File repositoryRoot = new File(repositoryPath);
		if (repositoryRoot.canRead() && repositoryRoot.isDirectory()) {
			datasetDirectory = new File(repositoryRoot, getDatasetIDString());
			if (datasetDirectory.canRead() && datasetDirectory.isDirectory())
				readable = true;
		}
	}
	
	public final File getDatasetDirectory() {
		return datasetDirectory;
	}
	
	public final boolean isReadable() {
		return readable;
	}
	
	public final String getDescription() {
		return description;
	}
	
	public final void setDescription(String description) {
		this.description = description;
	}
	
	public final int getFileCount() {
		return fileCount;
	}
	
	public final void setFileCount(int fileCount) {
		this.fileCount = fileCount;
	}
	
	public final String getFileCountString() {
		return String.format("%,d", fileCount);
	}
	
	public final long getFileSize() {
		return fileSize;
	}
	
	public final void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	
	public final String getFileSizeString() {
		// 1 TB or more
		if (fileSize >= 1099511627776L)
			return String.format("%.2f TB", fileSize / 1099511627776.0);
		// between 1 GB and 1 TB
		else if (fileSize >= 1073741824L)
			return String.format("%.2f GB", fileSize / 1073741824.0);
		// between 1 MB and 1 GB
		else if (fileSize >= 1048576L)
			return String.format("%.2f MB", fileSize / 1048576.0);
		// between 1 KB and 1 MB
		else if (fileSize >= 1024L)
			return String.format("%.2f KB", fileSize / 1024.0);
		// less than 1 KB
		else return fileSize + " bytes";
	}
	
	public String getSpecies() {
		return species;
	}
	
	public void setSpecies(String species) {
		this.species = species;
	}
	
	public String getInstrument() {
		return instrument;
	}
	
	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}
	
	public String getModification() {
		return modification;
	}
	
	public void setModification(String modification) {
		this.modification = modification;
	}
	
	public String getPI() {
		return pi;
	}
	
	public void setPI(String pi) {
		this.pi = pi;
	}
	
	public final boolean isComplete() {
		return isComplete;
	}
	
	public final void setComplete(boolean isComplete) {
		this.isComplete = isComplete;
	}
	
	public final String getSubmissionStatus() {
		if (isComplete)
			return "Complete";
		else return "Partial";
	}
	
	public final boolean isPrivate() {
		return isPrivate;
	}
	
	public final void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}
	
	/**
	 * Experimental Feature to check if converted version of dataset is available
	 * @return
	 */
	public final boolean isConvertedAndComputable() {
		String uploadPath = WebAppProps.getPath("livesearch.ftp.path");
		String datasetDataPath =
			uploadPath + "/" + getDatasetIDString();
		String datasetDataccms_peakpath = datasetDataPath + "/" + "ccms_peak";
		String datasetDatapeakpath = datasetDataPath + "/" + "peak";
		boolean pathExists = false;
		// check if spectrum folder exists
		try {
			FileIOUtils.validateDirectory(new File(datasetDataccms_peakpath));
			pathExists = true;
		} catch (Exception error) {}
		// check if peaks folder exists
		try {
			FileIOUtils.validateDirectory(new File(datasetDatapeakpath));
			pathExists = true;
		} catch (Exception error) {}
		return pathExists;
	}
	
	public final String getAnnotation(String name) {
		if (name == null)
			return null;
		else return annotations.get(name);
	}
	
	public final Map<String, String> getAllAnnotations() {
		return new LinkedHashMap<String, String>(annotations);
	}
	
	public final void setAnnotation(String name, String value) {
		if (name == null)
			return;
		else if (value == null)
			clearAnnotation(name);
		else annotations.put(name, value);
	}
	
	public final void clearAnnotation(String name) {
		if (name == null)
			return;
		else annotations.remove(name);
	}
	
	public final Set<String> getAnnotationNames() {
		return new LinkedHashSet<String>(annotations.keySet());
	}
	
	public final Collection<Publication> getPublications() {
		if (publications == null)
			DatasetManager.queryDatasetPublications(this);
		return publications;
	}
	
	public final String getPublicationsJSON() {
		Collection<Publication> publications = this.getPublications();
		if (publications == null)
			return "[]";
		String outputJSON = "[";
		int count = 0;
		for (Publication publication : publications) {
			count++;
			outputJSON += publication.getJSON();
			if (count < publications.size())
				outputJSON += ",";
		}
		outputJSON += "]";
		return outputJSON;
	}
	
	public final void setPublications(Collection<Publication> publications) {
		this.publications = publications;
	}
	
	public final void addPublication(Publication publication) {
		if (publication == null)
			return;
		else if (publications == null)
			publications = new LinkedHashSet<Publication>();
		publications.add(publication);
	}
	
	public final File getDatasetFile(String path)
	throws FileNotFoundException {
		if (path == null)
			return null;
		// retrieve and validate repository root
		String repositoryPath = getRepositoryPath();
		if (repositoryPath == null)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since registered " +
				"repository path is null.", path));
		File repositoryRoot = new File(repositoryPath);
		if (repositoryRoot.isDirectory() == false)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since registered " +
				"repository root [%s] is not an existing directory.",
				path, repositoryPath));
		else if (repositoryRoot.canRead() == false)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since registered " +
				"repository root [%s] cannot be read.", path, repositoryPath));
		// retrieve and validate this dataset's repository directory
		File dataset = new File(repositoryRoot, getDatasetIDString());
		if (dataset.isDirectory() == false)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since dataset directory " +
				"[%s] is not an existing directory.", path,
				dataset.getAbsolutePath()));
		else if (dataset.canRead() == false)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since dataset directory " +
				"[%s] cannot be read.", path, dataset.getAbsolutePath()));
		// retrieve and validate the requested file
		File datasetFile = new File(dataset, path);
		if (datasetFile.canRead() == false)
			throw new FileNotFoundException(String.format(
				"Could not find dataset file [%s], since no such file " +
				"could be found under dataset directory [%s].",
				datasetFile.getAbsolutePath(), dataset.getAbsolutePath()));
		else return datasetFile;
	}
	
	public final String getSummaryString() {
		return String.format("[Dataset %s] - \"%s\"",
			getDatasetIDString(), getTask().getDescription());
	}
	
	@Override
	public final String toString() {
		return getSummaryString();
	}
}
