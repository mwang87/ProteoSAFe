package edu.ucsd.livesearch.result.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.result.processors.ResultProcessor;
import edu.ucsd.livesearch.task.Task;

public class MzTabProteinResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(MzTabProteinResult.class);
	private final Pattern PSM_COUNT_PATTERN =
		Pattern.compile("num_psms_ms_run\\[(\\d+)\\]");
	private final Pattern PEPTIDE_COUNT_PATTERN =
		Pattern.compile("num_peptides_distinct_ms_run\\[(\\d+)\\]");
	private final Pattern UNIQUE_PEPTIDE_COUNT_PATTERN =
		Pattern.compile("num_peptides_unique_ms_run\\[(\\d+)\\]");
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MzTabProteinResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public MzTabProteinResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
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
		// set field names and their indices
		int lineCount = 0;
		String line = null;
		while (true) try {
			line = resultReader.readLine();
			if (line == null)
				break;
			lineCount++;
			// the PRH row should be before any PRT rows
			if (line.startsWith("PRT"))
				throw new ParseException("Found a \"PRT\" row before finding " +
					"the \"PRH\" (header) row", lineCount);
			else if (line.startsWith("PRH")) {
				line = line.substring(3).trim();
				String[] columns = line.split(getEscapedDelimiter());
				if (columns == null || columns.length < 1)
					throw new ParseException("Did not find any columns " +
						"in the \"PRH\" (header) row", lineCount);
				else {
					fieldNames = Arrays.asList(columns);
					loaded = true;
					break;
				}
			}
		} catch (Throwable error) {
			String errorMessage = String.format("Error parsing PRH " +
				"column header line from mzTab result file [%s] " +
				"(line %d): %s", resultFile.getAbsolutePath(),
				lineCount, error.getMessage());
			logger.error(errorMessage, error);
			throw new IOException(errorMessage, error);
		}
		// ensure that field names were found
		if (fieldNames == null || fieldNames.isEmpty()) {
			String errorMessage = String.format("Error parsing PRH " +
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
				// then peek at upcoming lines to see if there's a PRT line
				String line = null;
				while (true) {
					resultReader.mark(10000);
					line = resultReader.readLine();
					if (line == null)
						return false;
					// if there's another PRT line, rewind to get it next
					else if (line.startsWith("PRT")) {
						resultReader.reset();
						return true;
					}
					// if the next line is a comment or is blank, skip it
					else if (line.startsWith("COM") || line.trim().equals(""))
						continue;
					// otherwise, the next meaningful line is not a PRT
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
					"Error reading next line from result file [%s].",
					resultFile.getAbsolutePath()),
				error);
			return null;
		}
		if (line == null)
			throw new NoSuchElementException();
		// it should be impossible for this line to not be a "PRT" line, since
		// the hasNext() method only returns true if the next line is one
		if (line.startsWith("PRT") == false)
			throw new IllegalStateException();
		// first parse out the leading mzTab-format-mandated
		// "PRT" string and whitespace
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
					"Error parsing next hit from result file [%s].",
					resultFile.getAbsolutePath()),
				error);
			return null;
		}
		// accumulate PSM and peptide counts
		int psms = 0;
		int peptides = 0;
		int uniquePeptides = 0;
		for (String field : fieldNames) {
			// extract value, move on if it's null or not an integer
			String value = hit.getFieldValue(field);
			if (value == null)
				continue;
			int count = 0;
			try { count = Integer.parseInt(value); }
			catch (NumberFormatException error) { continue; }
			// increment the proper count depending on the column name
			if (PSM_COUNT_PATTERN.matcher(field).matches())
				psms += count;
			else if (PEPTIDE_COUNT_PATTERN.matcher(field).matches())
				peptides += count;
			else if (UNIQUE_PEPTIDE_COUNT_PATTERN.matcher(field).matches())
				uniquePeptides += count;
		}
		// add counts as special hit attributes
		hit.setAttribute("Hits", Integer.toString(psms));
		hit.setAttribute("Peptides", Integer.toString(peptides));
		hit.setAttribute("UniquePeptides", Integer.toString(uniquePeptides));
		// process the hit
		hit.setDelimiter(getFieldDelimiter());
		for (ResultProcessor processor : getProcessors()) {
			processor.processHit(hit, this);
		}
		return hit;
	}
}
