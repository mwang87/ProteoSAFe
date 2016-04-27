package edu.ucsd.livesearch.result.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.servlet.ManageParameters;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

public class FileListResult
implements Result
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(FileListResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File folder;
	private Task task;
	private Collection<File> files;
	private Map<String, String> mappedFilenames;
	private Properties statistics;
	private boolean loaded;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public FileListResult(File folder, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		// set result folder
		if (folder == null)
			throw new NullPointerException("Result folder cannot be null.");
		else if (folder.isDirectory() == false)
			throw new IllegalArgumentException(
				String.format("Result folder \"%s\" must be a directory.",
					folder.getAbsolutePath()));
		else if (folder.canRead() == false)
			throw new IllegalArgumentException(
				String.format("Result folder \"%s\" must be readable.",
					folder.getAbsolutePath()));
		this.folder = folder;
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
		// find and read the dataset statistics file, if present
		statistics = new Properties();
		File statsFile = task.getPath("statistics");
		if (statsFile.isDirectory() && statsFile.canRead()) {
			File[] contents = statsFile.listFiles();
			if (contents != null && contents.length > 0) {
				statsFile = contents[0];
				BufferedReader reader = null;
				if (statsFile.isFile() && statsFile.canRead()) try {
					reader = new BufferedReader(new FileReader(statsFile));
					statistics.load(reader);
				} finally {
					try { reader.close(); }
					catch (Throwable error) {}
				}
			}
		}
		mappedFilenames = null;
		close();
	}
	
	public FileListResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		throw new UnsupportedOperationException();
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public boolean isLoaded() {
		return true;
	}

	@Override
	public void load()
	throws IOException {
		close();
		// collect all child files of the selected folder
		try {
			File[] folderContents = folder.listFiles();
			files = new LinkedHashSet<File>(folderContents.length);
			for (File file : folderContents)
				files.add(file);
			loaded = true;
		} catch (Throwable error) {
			close();
			throw new IOException(error);
		}
	}

	@Override
	public void close() {
		files = null;
		loaded = false;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		return folder;
	}
	
	@Override
	public String getData() {
		if (loaded == false) try {
			load();
		} catch (IOException error) {
			throw new IllegalStateException(error);
		}
		StringBuffer fileList = new StringBuffer("{");
		for (File file : files) {
			String filename = file.getName();
			fileList.append("\"");
			fileList.append(file.getName());
			fileList.append("\":{");
			fileList.append("filename:\"");
			fileList.append(getMappedFilename(filename));
			fileList.append("\",psms:");
			fileList.append(getPSMCount(filename));
			fileList.append(",invalid:");
			fileList.append(getInvalidCount(filename));
			fileList.append(",percent:\"");
			fileList.append(getInvalidPercentage(filename));
			fileList.append("\",proteins:");
			fileList.append(getProteinCount(filename));
			fileList.append(",peptides:");
			fileList.append(getPeptideCount(filename));
			fileList.append("},");
		}
		// chomp trailing comma
		if (fileList.charAt(fileList.length() - 1) == ',')
			fileList.setLength(fileList.length() - 1);
		fileList.append("}");
		return fileList.toString();
	}
	
	@Override
	public Long getSize() {
		if (loaded == false) try {
			load();
		} catch (IOException error) {
			throw new IllegalStateException(error);
		}
		return (long)files.size();
	}
	
	@Override
	public Task getTask() {
		return task;
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean execute() {
		return (folder.isDirectory() && folder.canRead());
	}
	
	@Override
	public boolean resourceExists() {
		return folder.exists();
	}
	
	@Override
	public boolean resourceDated() {
		return false;
	}
	
	@Override
	public String getResourceName() {
		return folder.getAbsolutePath();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String getMappedFilename(String filename) {
		if (filename == null)
			return null;
		// build filename mapping list, if not already built
		if (mappedFilenames == null) {
			Collection<String> mappings =
				ManageParameters.getTaskParameter(task, "upload_file_mapping");
			if (mappings != null && mappings.isEmpty() == false) {
				mappedFilenames =
					new LinkedHashMap<String, String>(mappings.size());
				for (String mapping : mappings) {
					String[] tokens = mapping.split("\\|");
					if (tokens == null || tokens.length != 2)
						logger.error(String.format("\"upload_file_mapping\" " +
							"parameter value \"%s\" is invalid - " +
							"it should contain two tokens separated by " +
							"a pipe (\"|\") character.", mapping));
					else mappedFilenames.put(tokens[0], tokens[1]);
				}
			}
			// ensure that filename map is not null, even if
			// no mapping parameters were found in params.xml
			else mappedFilenames = new LinkedHashMap<String, String>();
		}
		// return mapped filename
		String mapped = mappedFilenames.get(filename);
		// the exact name might not have matched, if it's a converted file;
		// in this case, compare filename bases, since these should be identical
		if (mapped == null) {
			String baseFilename = FilenameUtils.getBaseName(filename);
			String extension = FilenameUtils.getExtension(filename);
			for (String internalFilename : mappedFilenames.keySet()) {
				if (baseFilename.equals(
					FilenameUtils.getBaseName(internalFilename))) {
					mapped = changeExtension(
						mappedFilenames.get(internalFilename), extension);
					break;
				}
			}
		}
		if (mapped == null)
			return filename;
		else return mapped;
	}
	
	private int getCount(String filename, String key) {
		if (filename == null || key == null)
			return 0;
		String count =
			statistics.getProperty(String.format("%s.%s", filename, key));
		if (count == null)
			return 0;
		else try {
			return Integer.parseInt(count);
		} catch (Throwable error) {
			return 0;
		}
	}
	
	private int getPSMCount(String filename) {
		return getCount(filename, "totalPSMs");
	}
	
	private int getInvalidCount(String filename) {
		return getCount(filename, "invalidPSMs");
	}
	
	private int getProteinCount(String filename) {
		return getCount(filename, "proteins");
	}
	
	private int getPeptideCount(String filename) {
		return getCount(filename, "peptides");
	}
	
	private String getInvalidPercentage(String filename) {
		// be sure not to divide by zero
		int invalid = getInvalidCount(filename);
		if (invalid < 1)
			return "0%";
		int psms = getPSMCount(filename);
		if (psms < 1)
			return "0%";
		return String.format("%s%%", ((double)invalid / (double)psms * 100.0));
	}
	
	private String changeExtension(String filename, String extension) {
		if (filename == null)
			return null;
		// if the new extension is null, then just remove the extension
		else if (extension == null)
			return String.format("%s%s", FilenameUtils.getPath(filename),
				FilenameUtils.getBaseName(filename));
		// otherwise change the old extension to the new one
		else return String.format("%s%s.%s", FilenameUtils.getPath(filename),
			FilenameUtils.getBaseName(filename), extension);
	}
}
