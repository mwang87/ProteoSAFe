package edu.ucsd.livesearch.result.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.result.processors.ResultProcessor;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;

public class MzTabResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(MzTabResult.class);
	private static final Pattern FILE_URI_PATTERN =
		Pattern.compile("file:(?:/{1,2})?(.*)");
	private static final Pattern FILE_REFERENCE_PATTERN =
		Pattern.compile("ms_run\\[(\\d+)\\]");
	private static final Pattern FILE_LINE_PATTERN =
		Pattern.compile("^MTD\\s+ms_run\\[(\\d+)\\]-location\\s+(.+)$");
	private static final Pattern SEARCH_ENGINE_SCORE_PATTERN =
		Pattern.compile("^MTD\\s+psm_search_engine_score\\[(\\d+)\\]\\s+(.+)$");
	private static final Pattern SEARCH_ENGINE_SCORE_PSM_PATTERN =
		Pattern.compile("^search_engine_score\\[(\\d+)\\]$");
	private static final Pattern CV_TERM_PATTERN = Pattern.compile(
		"^\\[([^,]*),\\s*([^,]*),\\s*\"?([^\"]*)\"?,\\s*([^,]*)\\]$");
	private static final Pattern MODIFICATION_PATTERN = Pattern.compile(
		"(\\d+.*?(?:\\|\\d+.*?)*)-" +
		"((?:\\[[^,]*,\\s*[^,]*,\\s*\"?[^\"]*\"?,\\s*[^,]*\\])|" +	// CV param
		"(?:[^,]*))");
	public static final String DYNAMIC_COLUMN_PREFIX = "_dyn_#";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected Map<Integer, String> spectrumFiles;
	protected Map<Integer, String> searchEngineScores;
	protected File parsed;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MzTabResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public MzTabResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		spectrumFiles = new TreeMap<Integer, String>();
		searchEngineScores = new TreeMap<Integer, String>();
		// determine parsed output file path
		String taskRoot = task.getPath("").getAbsolutePath();
		String sourceDirectory = resultFile.getParent();
		// of the file doesn't come from the task, then
		// it probably comes from the associated dataset
		if (sourceDirectory.startsWith(taskRoot))
			sourceDirectory = sourceDirectory.substring(taskRoot.length());
		if (sourceDirectory.startsWith("/"))
			sourceDirectory = sourceDirectory.substring(1);
		if (sourceDirectory.endsWith("/") == false)
			sourceDirectory += "/";
		// only create a new nested directory under temp/ if the source
		// file was not already itself from a temporary directory
		if (sourceDirectory.startsWith("temp/") == false)
			sourceDirectory = "temp/" + sourceDirectory;
		// determine base filename of parsed file
		String parsedBase = FilenameUtils.getBaseName(resultFile.getName());
		String parsedPrefix = block + "_";
		if (parsedBase.startsWith(parsedPrefix) == false)
			parsedBase = parsedPrefix + parsedBase;
		// determine proper suffix for this file
		String parsedSuffix = ".mztab";
		if (parsedBase.endsWith(parsedSuffix) == false)
			parsedBase = parsedBase + parsedSuffix;
		parsed = task.getPath(sourceDirectory + parsedBase + ".tsv");
		// write parsed output file in advance
		// the sorted result file must be written in advance
		if (OnDemandLoader.load(this))
			close();
		else throw new IOException(String.format(
			"Could not parse mzTab file [%s] into a TSV representation.",
			resultFile.getAbsolutePath()));
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void load()
	throws IOException, IllegalArgumentException {
		// close down the parser, in case it was open before
		close();
		// initialize buffered result file reader
		try {
			resultReader =
				new BufferedReader(new FileReader(resultFile), 50000);
		} catch (IOException error) {
			String errorMessage = String.format(
				"Error accessing result file [%s].",
				resultFile.getAbsolutePath());
			logger.error(errorMessage, error);
			throw error;
		}
		// get all relevant metadata from the top of the file
		int lineCount = 0;
		String line = null;
		while (true) try {
			resultReader.mark(50000);
			line = resultReader.readLine();
			if (line == null)
				break;
			lineCount++;
			// if we're already at the PSM section, rewind
			if (line.startsWith("PSH") || line.startsWith("PSM")) {
				resultReader.reset();
				lineCount--;
				break;
			}
			// don't keep reading if we're past the metadata section
			else if (line.startsWith("MTD") == false &&
				line.startsWith("COM") == false &&
				line.trim().equals("") == false)
				break;
			// get spectrum file data, if this is a file location line
			Matcher matcher = FILE_LINE_PATTERN.matcher(line);
			if (matcher.matches()) try {
				spectrumFiles.put(Integer.parseInt(matcher.group(1)),
					resolveFilename(matcher.group(2)));
			} catch (NumberFormatException error) {
				// it should be impossible to reach this catch block, since
				// the regular expression capture ensures that it's an integer
				throw new IllegalStateException();
			}
			// get search engine score data, if this is a score line
			matcher = SEARCH_ENGINE_SCORE_PATTERN.matcher(line);
			if (matcher.matches()) try {
				searchEngineScores.put(Integer.parseInt(matcher.group(1)),
					getCVTermName(matcher.group(2)));
			} catch (NumberFormatException error) {
				// it should be impossible to reach this catch block, since
				// the regular expression capture ensures that it's an integer
				throw new IllegalStateException();
			}
		} catch (Throwable error) {
			String errorMessage = String.format("Error parsing spectrum " +
				"filenames from mzTab result file [%s] (line %d): %s",
				resultFile.getAbsolutePath(), lineCount, error.getMessage());
			logger.error(errorMessage, error);
			throw new IOException(errorMessage, error);
		}
		// set field names and their indices
		while (true) try {
			line = resultReader.readLine();
			if (line == null)
				break;
			lineCount++;
			// the PSH row should be before any PSM rows
			if (line.startsWith("PSM"))
				throw new ParseException("Found a \"PSM\" row before finding " +
					"the \"PSH\" (header) row", lineCount);
			else if (line.startsWith("PSH")) {
				line = line.substring(3).trim();
				String[] columns = line.split(getEscapedDelimiter());
				if (columns == null || columns.length < 1)
					throw new ParseException("Did not find any columns " +
						"in the \"PSH\" (header) row", lineCount);
				// update jmzTab score columns to be dynamic columns
				for (int i=0; i<columns.length; i++) {
					Matcher matcher =
						SEARCH_ENGINE_SCORE_PSM_PATTERN.matcher(columns[i]);
					if (matcher.matches()) try {
						String score = searchEngineScores.get(
							Integer.parseInt(matcher.group(1)));
						if (score != null)
							columns[i] = String.format(
								"%s%s", DYNAMIC_COLUMN_PREFIX, score);
					} catch (Throwable error) {}
				}
				fieldNames = Arrays.asList(columns);
				loaded = true;
				break;
			}
		} catch (Throwable error) {
			String errorMessage = String.format("Error parsing PSH " +
				"column header line from mzTab result file [%s] " +
				"(line %d): %s", resultFile.getAbsolutePath(),
				lineCount, error.getMessage());
			logger.error(errorMessage, error);
			throw new IOException(errorMessage, error);
		}
		// ensure that field names were found
		if (fieldNames == null || fieldNames.isEmpty()) {
			String errorMessage = String.format("Error parsing PSH " +
				"column header line from mzTab result file [%s] " +
				"(line %d): this line was not found.",
				resultFile.getAbsolutePath(), lineCount);
			logger.error(errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}
	}
	
	/*========================================================================
	 * Iterator methods
	 *========================================================================*/
	@Override
	public boolean hasNext() {
		try {
			if (isLoaded() == false)
				load();
			// first check the basic conditions for tabular results
			if (isLoaded() && resultReader != null &&
				resultReader.ready()) {
				// then peek at upcoming lines to see if there's a PSM line
				String line = null;
				while (true) {
					resultReader.mark(50000);
					line = resultReader.readLine();
					if (line == null)
						return false;
					// if there's another PSM line, rewind to get it next
					else if (line.startsWith("PSM")) {
						resultReader.reset();
						return true;
					}
					// if the next line is a comment or is blank, skip it
					else if (line.startsWith("COM") || line.trim().equals(""))
						continue;
					// otherwise, the next meaningful line is not a PSM
					else return false;
				}
			} else return false;
		} catch (IOException error) {
			return false;
		}
	}
	
	@Override
	public ResultHit next()
	throws NoSuchElementException {
		if (hasNext() == false)
			throw new NoSuchElementException();
		// read the next line from the result file
		String line = null;
		try {
			line = resultReader.readLine();
		} catch (IOException error) {
			logger.error(
				String.format(
					"Error reading next line from result file \"%s\".",
					resultFile.getAbsolutePath()),
				error);
			return null;
		}
		if (line == null)
			throw new NoSuchElementException();
		// it should be impossible for this line to not be a "PSM" line, since
		// the hasNext() method only returns true if the next line is one
		if (line.startsWith("PSM") == false)
			throw new IllegalStateException();
		// first parse out the leading mzTab-format-mandated
		// "PSM" string and whitespace
		line = line.substring(3).trim();
		// build hit from the fields in the parsed line
		ResultHit hit = null;
		try {
			List<String> fieldValues =
				Arrays.asList(line.split(getEscapedDelimiter()));
			hit = new TabularResultHit(this, fieldValues);
		} catch (Exception error) {
			logger.error(
				String.format(
					"Error parsing next hit from result file \"%s\".",
					resultFile.getAbsolutePath()),
				error);
			return null;
		}
		// add dereferenced filename as a special hit attribute
		String spectraRef = hit.getFieldValue("spectra_ref");
		if (spectraRef != null) {
			// each "spectra_ref" column value should
			// be a string with the following format:
			// ms_run[<index>]:<nativeID-formatted identifier string>
			String[] tokens = spectraRef.split(":");
			if (tokens != null && tokens.length > 0) {
				String filename = null;
				Matcher matcher = FILE_REFERENCE_PATTERN.matcher(tokens[0]);
				if (matcher.matches()) try {
					filename =
						spectrumFiles.get(Integer.parseInt(matcher.group(1)));
				} catch (NumberFormatException error) {
					// it should be impossible to reach this
					// catch block, since the regular expression
					// capture ensures that it's an integer
					throw new IllegalStateException();
				}
				if (filename != null)
					hit.setAttribute("#SpecFile", filename);
				// add nativeID string value as another special hit attribute
				if (tokens.length > 1)
					hit.setAttribute("nativeID", tokens[1]);
			}
		}
		// process "modifications" column to figure out exactly which mods
		// are present, and where they are in the peptide sequence
		String mods = hit.getFieldValue("modifications");
		Collection<String> unpositionedMods = null;
		if (mods != null && mods.trim().equals("") == false &&
			mods.trim().equalsIgnoreCase("null") == false) {
			// build a map of mass offsets to insert
			// into the modified peptide string
			unpositionedMods = new LinkedHashSet<String>();
			Matcher matcher = MODIFICATION_PATTERN.matcher(mods);
			while (matcher.find())
				unpositionedMods.add(matcher.group(2));
		}
		// add unique set of mods (regardless of position) as a hit attribute
		if (unpositionedMods != null && unpositionedMods.isEmpty() == false) {
			StringBuffer modSet = new StringBuffer();
			for (String mod : unpositionedMods) {
				if (modSet.length() > 0)
					modSet.append(",");
				modSet.append(mod);
			}
			hit.setAttribute("mod_set", modSet.toString());
		} else hit.setAttribute("mod_set", "null");
		// concatenate nativeID attribute and sequence and modifications
		// field values to form a legitimate PSM ID field
		String nativeID = hit.getAttribute("nativeID");
		String sequence = hit.getFieldValue("sequence");
		hit.setAttribute("unique_PSM_ID", String.format("%s_%s_%s",
			nativeID == null ? "null" : nativeID,
			sequence == null ? "null" : sequence,
			mods == null ? "null" : mods));
		// process the hit
		hit.setDelimiter(getFieldDelimiter());
		for (ResultProcessor processor : getProcessors()) {
			processor.processHit(hit, this);
		}
		return hit;
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean execute() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(parsed);
			if (isLoaded() == false)
				load();
			// the header line cannot be written until the first row has been
			// generated, since the generation of a row necessarily determines
			// any processing attributes that need to be added to each hit, and
			// therefore also to the set of column headers
			if (hasNext()) {
				ResultHit hit = next();
				writer.println(getHeaderLine());
				writer.println(((TabularResultHit)hit).toRowLine());
			}
			// write the rest of the rows
			while (hasNext()) {
				writer.println(((TabularResultHit)next()).toRowLine());
			}
			return true;
		} catch (Throwable error) {
			logger.error("Could not write parsed mzTab to TSV file.", error);
			return false;
		} finally {
			try { writer.close(); }
			catch (Throwable error) {}
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
			resultFile == null || resultFile.exists() == false)
			return false;
		else return parsed.lastModified() < resultFile.lastModified(); 
	}
	
	@Override
	public String getResourceName() {
		return parsed.getAbsolutePath();
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		return parsed;
	}
	
	public String getSearchEngineScore(int index) {
		if (searchEngineScores == null || searchEngineScores.isEmpty())
			return null;
		else return searchEngineScores.get(index);
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String resolveFilename(String filename) {
		if (filename == null)
			return null;
		// account for buggy mzidentml-lib implementation
		Pattern pattern = Pattern.compile("^[^:/]+:/{2,3}([^:/]+://.*)$");
		Matcher matcher = pattern.matcher(filename);
		if (matcher.matches())
			filename = matcher.group(1);
		// if this is a file URI, clean it
		matcher = FILE_URI_PATTERN.matcher(filename);
		if (matcher.matches())
			filename = matcher.group(1);
		return filename;
	}
	
	private static String getCVTermName(String cvTerm) {
		if (cvTerm == null)
			return null;
		// a CV term string should be a square bracket-enclosed ("[]") tuple
		Matcher matcher = CV_TERM_PATTERN.matcher(cvTerm);
		if (matcher.matches() == false)
			return null;
		else return matcher.group(3);
	}
}
