package edu.ucsd.livesearch.result.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.result.processors.ResultProcessor;
import edu.ucsd.livesearch.task.NullTask;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager.TaskStatus;

public class TabularResult
implements IterableResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Logger logger =
		LoggerFactory.getLogger(TabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected File resultFile;
	protected Task task;
	protected String block;
	protected BufferedReader resultReader;
	protected List<String> fieldNames;
	protected Set<String> attributeNames;
	protected List<ResultProcessor> processors;
	protected boolean loaded;
	protected char delimiter;
	protected char fieldDelimiter;
	protected Collection<Result> previous;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public TabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		init(resultFile, task, block);
	}
	
	public TabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		// validate result
		if (result == null)
			throw new NullPointerException("Previous result cannot be null.");
		// ensure that previous result file is written
		else if (OnDemandLoader.load(result) == false)
			throw new IllegalArgumentException(
				"Previous result file could not be written.");
		// add previous result's stack to this stack
		previous = new ArrayList<Result>();
		if (result instanceof TabularResult) {
			Collection<Result> results = ((TabularResult)result).previous;
			if (results != null && results.isEmpty() == false)
				previous.addAll(results);
		}
		previous.add(result);
		// initialize this result
		init(result.getFile(), result.getTask(), block);
	}
	
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		// set result file
		if (resultFile == null)
			throw new NullPointerException("Result file cannot be null.");
		else if (resultFile.canRead() == false)
			throw new IllegalArgumentException(
				String.format("Result file \"%s\" must be readable.",
					resultFile.getAbsolutePath()));
		this.resultFile = resultFile;
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
		// set block name
		this.block = block;
		// initialize processor list
		processors = new ArrayList<ResultProcessor>();
		// set default delimiter (tab character)
		setDelimiter('\t');
		// set default field delimiter (exclamation mark character)
		setFieldDelimiter('!');
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean isLoaded() {
		return loaded;
	}
	
	public void load()
	throws IOException, IllegalArgumentException {
		// close down the parser, in case it was open before
		close();
		// initialize buffered result file reader
		try { 
			resultReader =
				new BufferedReader(new FileReader(resultFile), 50000);
		} catch (IOException error) {
			logger.error(
				String.format("Error accessing result file \"%s\".",
					resultFile.getAbsolutePath()),
				error);
			throw error;
		}
		// set field names and their indices
		String header = null;
		try {
			header = resultReader.readLine();
		} catch (IOException error) {
			logger.error(
				String.format(
					"Error reading header line of result file \"%s\".",
					resultFile.getAbsolutePath()),
				error);
			throw error;
		}
		String[] splitHeader = null;
		if (header != null)
			splitHeader = header.split(getEscapedDelimiter());
		if (splitHeader == null || splitHeader.length < 1) {
			String error = String.format("Error parsing result file \"%s\": " +
				"the file must contain a valid header line consisting of " +
				"one or more non-empty field names.",
				resultFile.getAbsolutePath());
			logger.error(error);
			throw new IllegalArgumentException(error);
		} else {
			fieldNames = Arrays.asList(splitHeader);
			loaded = true;
		}
	}
	
	public void close() {
		if (resultReader != null) try {
			resultReader.close();
			resultReader = null;
		} catch (Throwable error) {}
		fieldNames = null;
		loaded = false;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public File getFile() {
		return resultFile;
	}
	
	public String getData() {
		int counter = 0;
		StringBuffer hits = new StringBuffer("[");
		while (hasNext()) try {
			ResultHit hit = next();
			if (hit == null)
				throw new NoSuchElementException();
			hits.append("\n\t");
			hits.append(hit.toJSON());
			// add special fields to this hit
			if (hits.charAt(hits.length() - 1) == '}')
				hits.setLength(hits.length() - 1);
			// add "id" field
			hits.append(",\"id\":\"");
			hits.append(counter++);
			hits.append("\"},");
		} catch (NoSuchElementException error) {
			return null;
		}
		// truncate trailing comma
		if (hits.charAt(hits.length() - 1) == ',')
			hits.setLength(hits.length() - 1);
		hits.append("\n]");
		return hits.toString();
	}
	
	public Long getSize() {
		if (resultReader == null)
			return null;
		else return resultFile.length();
	}
	
	public final Task getTask() {
		return task;
	}
	
	public final List<String> getFieldNames() {
		if (fieldNames == null)
			return null;
		else return new ArrayList<String>(fieldNames);
	}
	
	public final List<String> getAttributeNames() {
		if (attributeNames == null)
			return null;
		else return new ArrayList<String>(attributeNames);
	}
	
	public final void addAttributeName(String name) {
		if (name == null)
			return;
		else if (attributeNames == null)
			attributeNames = new LinkedHashSet<String>();
		attributeNames.add(name);
	}
	
	public String getHeaderLine() {
		StringBuffer header = new StringBuffer();
		// print field names from result file first
		List<String> fieldNames = getFieldNames();
		if (fieldNames != null) {
			for (String fieldName : fieldNames) {
				header.append(fieldName);
				header.append(getDelimiter());
			}
		}
		// then print any attribute names that may
		// have been generated during processing
		List<String> attributeNames = getAttributeNames();
		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				header.append(attributeName);
				header.append(getDelimiter());
			}
		}
		// truncate trailing delimiter, if necessary
		if (header.charAt(header.length() - 1) == getDelimiter())
			header.setLength(header.length() - 1);
		return header.toString();
	}
	
	public final char getDelimiter() {
		return delimiter;
	}
	
	public final String getEscapedDelimiter() {
		return StringEscapeUtils.escapeJava(Character.toString(delimiter));
	}
	
	public final void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}
	
	public final void setDelimiter(String delimiter) {
		if (delimiter.length() != 1)
			throw new IllegalArgumentException("Tabular result delimiters " +
				"must consist of only a single character.");
		else this.delimiter = delimiter.charAt(0);
	}
	
	public final char getFieldDelimiter() {
		return fieldDelimiter;
	}
	
	public final String getEscapedFieldDelimiter() {
		return StringEscapeUtils.escapeJava(Character.toString(fieldDelimiter));
	}
	
	public final void setFieldDelimiter(char fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
	}
	
	public final void setFieldDelimiter(String fieldDelimiter) {
		if (fieldDelimiter.length() != 1)
			throw new IllegalArgumentException("Tabular result field " +
				"delimiters must consist of only a single character.");
		else this.fieldDelimiter = fieldDelimiter.charAt(0);
	}
	
	public final Collection<Result> getPreviousResults() {
		if (previous == null)
			previous = new ArrayList<Result>();
		return new ArrayList<Result>(previous);
	}
	
	/*========================================================================
	 * Processor methods
	 *========================================================================*/
	public final List<ResultProcessor> getProcessors() {
		return processors;
	}
	
	public final void addProcessor(ResultProcessor processor) {
		if (processor == null)
			return;
		processors.add(processor);
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	public boolean execute() {
		return resultFile.canRead();
	}
	
	public boolean resourceExists() {
		return resultFile.exists();
	}
	
	public boolean resourceDated() {
		return false;
	}
	
	public String getResourceName() {
		return resultFile.getAbsolutePath();
	}
	
	/*========================================================================
	 * Iterator methods
	 *========================================================================*/
	public boolean hasNext() {
		try {
			if (isLoaded() == false)
				load();
			return isLoaded() && resultReader != null && resultReader.ready();
		} catch (IOException error) {
			return false;
		}
	}
	
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
		// process the hit
		hit.setDelimiter(getFieldDelimiter());
		for (ResultProcessor processor : getProcessors()) {
			processor.processHit(hit, this);
		}
		return hit;
	}
	
	public void remove()
	throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/*========================================================================
	 * Iterable methods
	 *========================================================================*/
	public Iterator<ResultHit> iterator() {
		// force a reload to reset the iterator
		try {
			load();
		} catch (Exception error) {
			return null;
		}
		return this;
	}
}
