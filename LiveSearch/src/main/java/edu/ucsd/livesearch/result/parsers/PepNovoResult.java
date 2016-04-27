package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.util.FileIOUtils;

public class PepNovoResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(PepNovoResult.class);
	private static final Pattern HEADER_PATTERN = Pattern.compile(
		">>\\s+([+-]?\\d+)\\s+([+-]?\\d+)\\s+([+-]?\\d+)\\s+(.*)");
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private File source;
	private File parsed;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public PepNovoResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public PepNovoResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// set source file to original result file, before parsing
		source = resultFile;
		// set parsed result file path
		String taskRoot = task.getPath("").getAbsolutePath();
		String sourceDirectory = source.getParent();
		if (sourceDirectory.startsWith(taskRoot) == false)
			throw new IllegalArgumentException(
				String.format("Result file \"%s\" must reside somewhere " +
					"underneath task directory \"%s\".",
					source.getAbsolutePath(), taskRoot));
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
		String parsedSuffix = ".pepnovo";
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
			"could not be transformed from PepNovo result file \"%s\".",
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
		RandomAccessFile file = null;
		PrintWriter writer = null;
		if (source == null)
			return false;
		else try {
			writer = new PrintWriter(parsed);
			file = new RandomAccessFile(source, "r");
			String[] fields = null;
			while (true) {
				String line = file.readLine();
				if (line == null)
					break;
				else if (line.trim().equals(""))
					continue;
				// parse out first header line
				Matcher matcher = HEADER_PATTERN.matcher(line);
				// keep searching for the beginning of the next valid block
				if (matcher.matches() == false)
					continue;
				// determine spectrum filename from header file index
				int fileIndex = Integer.parseInt(matcher.group(1));
				File spectrumFolder = task.getPath("spec/");
				FileIOUtils.validateDirectory(spectrumFolder);
				File[] spectrumFiles = spectrumFolder.listFiles();
				if (spectrumFiles == null ||
					spectrumFiles.length <= fileIndex) {
					// TODO: report error
					return false;
				}
				File spectrumFile = spectrumFiles[fileIndex];
				FileIOUtils.validateReadableFile(spectrumFile);
				String spectrumFilename = spectrumFile.getName();
				// get remaining header properties
				String index = matcher.group(2);
				String scan = matcher.group(3);
				String title = matcher.group(4);
				// parse out second header line
				line = file.readLine();
				// the second header should be a comment, starting with "#";
				// otherwise, it's an invalid block
				if (line == null || line.startsWith("#") == false)
					continue;
				// verify second header, it should be a list of column headers
				String[] currentFields = line.split("\\t");
				// if the list of column headers consists of less than two
				// elements, then this is an invalid block
				if (currentFields == null || currentFields.length < 2)
					continue;
				else if (fields != null) {
					// TODO: verify each header line to ensure consistency
				}
				// if this is the first set of header fields encountered,
				// write it out to the parsed result file
				else {
					fields = currentFields;
					StringBuffer header = new StringBuffer("SpectrumFile");
					header.append(Character.toString(getDelimiter()));
					header.append("Index");
					header.append(Character.toString(getDelimiter()));
					header.append("Scan");
					header.append(Character.toString(getDelimiter()));
					header.append("Title");
					for (String field : fields) {
						header.append(Character.toString(getDelimiter()));
						header.append(field);
					}
					writer.println(header.toString());
				}
				// parse out each result line
				while (true) {
					line = file.readLine();
					String[] values = null;
					if (line == null || line.trim().equals(""))
						break;
					else {
						values = line.split("\\t");
						if (values == null || values.length != fields.length) {
							// TODO: report error
							return false;
						} else {
							// TODO: verify line values against header
							// fields to ensure consistency
						}
					}
					StringBuffer hit = new StringBuffer(spectrumFilename);
					hit.append(Character.toString(getDelimiter()));
					hit.append(index);
					hit.append(Character.toString(getDelimiter()));
					hit.append(scan);
					hit.append(Character.toString(getDelimiter()));
					hit.append(title);
					for (String value : values) {
						hit.append(Character.toString(getDelimiter()));
						hit.append(value);
					}
					writer.println(hit.toString());
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
			if (file != null) try {
				file.close();
			} catch (Throwable error) {}
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
	
	public static void main(String[] args) {
		String line = ">> 0 0 -1	Cmpd 1, +MSn(590.33), 5.0 min";
		Matcher matcher = HEADER_PATTERN.matcher(line);
		if (matcher.matches())
			System.out.println("Matches.");
		else System.out.println("Does not match.");
	}
}
