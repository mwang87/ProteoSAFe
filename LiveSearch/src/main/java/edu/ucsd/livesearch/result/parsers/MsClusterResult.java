package edu.ucsd.livesearch.result.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FileIOUtils;

public class MsClusterResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(MsClusterResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File source;	// in this case, a directory of cluster files
	private File parsed;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MsClusterResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public MsClusterResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// set source directory to original result file, before parsing
		source = resultFile;
		// set parsed result file path
		String taskRoot = task.getPath("").getAbsolutePath();
		String sourceDirectory = source.getAbsolutePath();
		if (sourceDirectory.startsWith(taskRoot) == false)
			throw new IllegalArgumentException(
				String.format("Result directory \"%s\" must reside somewhere " +
					"underneath task directory \"%s\".",
					sourceDirectory, taskRoot));
		sourceDirectory = sourceDirectory.substring(taskRoot.length());
		if (sourceDirectory.endsWith("/") == false)
			sourceDirectory += "/";
		// only create a new nested directory under temp/ if the source
		// file was not already itself from a temporary directory
		if (sourceDirectory.startsWith("temp/") == false)
			sourceDirectory = "temp/" + sourceDirectory;
		// determine base filename of parsed file
		String parsedBase = FilenameUtils.getBaseName(source.getName());
		String parsedPrefix = block + "_";
		if (parsedBase.startsWith(parsedPrefix) == false)
			parsedBase = parsedPrefix + parsedBase;
		// determine proper suffix for this file
		String parsedSuffix = ".mscluster";
		if (parsedBase.endsWith(parsedSuffix) == false)
			parsedBase = parsedBase + parsedSuffix;
		parsed = task.getPath(sourceDirectory + parsedBase + ".tsv");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void load()
	throws IOException, IllegalArgumentException {
		// the parsed result file must be written in advance
		if (OnDemandLoader.load(this))
			resultFile = parsed;
		else throw new IOException(String.format("A valid result file " +
			"could not be transformed from MS-Cluster folder \"%s\".",
			resultFile.getAbsolutePath()));
		// once the proper parsed file is assigned, load normally
		super.load();
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		return parsed;
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	public boolean execute() {
		PrintWriter writer = null;
		if (source == null)
			return false;
		else try {
			// get filenames from spectrum file list
			List<String> filenames = getClusteredFilenames();
			if (filenames == null || filenames.isEmpty()) {
				// TODO: report error
				return false;
			}
			// create output file, write header line to it
			writer = new PrintWriter(parsed);
			StringBuffer header = new StringBuffer("#SpectrumFile");
			header.append(Character.toString(getDelimiter()));
			header.append("Index");
			header.append(Character.toString(getDelimiter()));
			header.append("Cluster");
			writer.println(header.toString());
			// read cluster files, write each to output file
			for (File clusterFile : source.listFiles()) {
				if (writeClusters(clusterFile, writer, filenames) == false) {
					// TODO: report error
					return false;
				}
			}
			return true;
		} catch (Exception error) {
			logger.error(
				String.format("Failed to write result file \"%s\"",
					parsed.getAbsolutePath()),
				error
			);			
			return false;
		} finally {
			if (writer != null) try {
				writer.close();
			} catch (Throwable error) {}
		}
	}
	
	@Override
	public boolean resourceExists() {
		if (parsed == null)
			return false;
		else return parsed.exists();
	}
	
	@Override
	public boolean resourceDated() {
		if (parsed == null || parsed.exists() == false ||
			source == null || source.exists() == false)
			return false;
		else return parsed.lastModified() < source.lastModified(); 
	}
	
	@Override
	public String getResourceName() {
		return parsed.getAbsolutePath();
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private List<String> getClusteredFilenames() {
		// get spectrum filename list
		File spectrumFileList =
			FileIOUtils.getSingleFile(task.getPath("specList/"));
		if (spectrumFileList == null || spectrumFileList.canRead() == false) {
			// TODO: report error
			return null;
		}
		// read through spectrum filename list, collect filenames
		List<String> clusteredFilenames = new Vector<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(spectrumFileList));
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				else clusteredFilenames.add(line);
			}
		} catch (Throwable error) {
			// TODO: report error
			return null;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (Throwable error) {}
		}
		// return filenames
		if (clusteredFilenames.isEmpty())
			return null;
		else return clusteredFilenames;
	}
	
	private boolean writeClusters(
		File clusterFile, PrintWriter writer, List<String> filenames
	) throws IOException {
		if (clusterFile == null || clusterFile.canRead() == false ||
			writer == null || filenames == null || filenames.isEmpty())
			return false;
		// read through cluster file, write cluster data to output file
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(clusterFile));
			String cluster = null;
			while (true) {
				String line = reader.readLine();
				if (line == null)
					break;
				// if this is an empty line, then the previous cluster has ended
				else if (line.trim().equals("")) {
					cluster = null;
					continue;
				}
				// if this is a non-empty line, and we have not yet encountered
				// the next cluster, then this must be a header line
				else if (cluster == null) {
					String[] header = line.split("\\t");
					if (header == null || header.length < 1) {
						// TODO: report error
						return false;
					}
					// the cluster ID is the first token in a header line
					cluster = header[0];
				}
				// otherwise, this line must be a cluster row
				else {
					String[] row = line.split("\\t");
					if (row == null || row.length < 3) {
						// TODO: report error
						return false;
					}
					// the spectrum file ID is the second token in a row line
					String filename = filenames.get(Integer.parseInt(row[1]));
					if (filename == null) {
						// TODO: report error
						return false;
					}
					// the spectrum index is the third token in a row line
					int index = Integer.parseInt(row[2]);
					StringBuffer hit = new StringBuffer(filename);
					hit.append(Character.toString(getDelimiter()));
					hit.append(index);
					hit.append(Character.toString(getDelimiter()));
					hit.append(cluster);
					writer.println(hit.toString());
				}
			}
		} catch (Throwable error) {
			// TODO: report error
			return false;
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (Throwable error) {}
		}
		return true;
	}
}
