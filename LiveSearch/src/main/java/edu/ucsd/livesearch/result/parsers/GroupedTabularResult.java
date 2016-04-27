package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.result.processors.ResultProcessor;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;

public class GroupedTabularResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(GroupedTabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected File source;
	protected File parsed;
	protected String groupBy;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public GroupedTabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public GroupedTabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// set source file to original result file, before sorting
		source = resultFile;
		// set default group-by field (null)
		setGroupBy(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void load()
	throws IOException, IllegalArgumentException {
		// a result file sorted by the group-by field will need to be used
		preSort();
		// once the proper sorted file is assigned, load normally
		super.load();
	}
	
	public void preSort()
	throws IOException, IllegalArgumentException {
		if (groupBy == null)
			throw new IllegalArgumentException("A valid non-null " +
				"group-by field must be provided to load a grouped result.");
		// a result file sorted by the group-by field will need to be used
		try {
			SortedTabularResult sortedLoader =
				new SortedTabularResult(source, task, block);
			sortedLoader.setSortBy(groupBy);
			if (OnDemandLoader.load(sortedLoader)) {
				resultFile = sortedLoader.getFile();
				// reset groupBy, to ensure that filenames are properly updated
				setGroupBy(groupBy);
			} else throw new IOException("A valid result file sorted by " +
				"the specified group-by field could not be generated.");
		} catch (Throwable error) {
			logger.error(
				String.format(
					"Error sorting result file \"%s\" by field \"%s\".",
					resultFile.getAbsolutePath(), groupBy),
				error);
			throw new IOException(error);
		}
	}
	
	@Override
	public String getHeaderLine() {
		StringBuffer header = new StringBuffer();
		// print attribute names
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
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getGroupBy() {
		return groupBy;
	}
	
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
		// if no group-by field was specified, then no parsing can happen
		if (groupBy == null)
			parsed = null;
		// otherwise, set target for grouped result
		// file to indicate group-by field
		else {
			String taskRoot = task.getPath("").getAbsolutePath();
			// get source directory path relative to task root
			String sourceDirectory = resultFile.getParent();
			if (sourceDirectory.startsWith(taskRoot) == false)
				throw new IllegalArgumentException(
					String.format("Result file \"%s\" must reside somewhere " +
						"underneath task directory \"%s\".",
						resultFile.getAbsolutePath(), taskRoot));
			sourceDirectory = sourceDirectory.substring(taskRoot.length());
			if (sourceDirectory.startsWith("/"))
				sourceDirectory = sourceDirectory.substring(1);
			if (sourceDirectory.endsWith("/") == false)
				sourceDirectory += "/";
			// only create a new nested directory under temp/ if the source
			// file was not already itself from a temporary directory
			if (sourceDirectory.startsWith("temp/") == false)
				sourceDirectory = "temp/" + sourceDirectory;
			// determine base filename of grouped file
			String groupedBase =
				FilenameUtils.getBaseName(resultFile.getName());
			String groupedPrefix = block + "_";
			if (groupedBase.startsWith(groupedPrefix) == false)
				groupedBase = groupedPrefix + groupedBase;
			// determine proper suffix for this file, to
			// indicate that it has been grouped
			String groupedSuffix = "." + groupBy + "_grouped";
			// only append the suffix if the source file has not
			// already been grouped with the specified parameters
			if (groupedBase.endsWith(groupedSuffix) == false)
				groupedBase = groupedBase + groupedSuffix;
			parsed = task.getPath(sourceDirectory + groupedBase + ".tsv");
		}
	}
	
	@Override
	public File getFile() {
		return parsed;
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean execute() {
		// if no valid parsed file destination could be found,
		// then the file cannot be written
		if (parsed == null)
			return false;
		// ensure that the result is loaded from scratch
		if (isLoaded())
			close();
		try {
			load();
		} catch (Throwable error) {
			logger.error("Error loading result", error);
			return false;
		}
		// write the freshly loaded result to its output file
		PrintWriter writer = null;
		try {
			// attempt to open the output file for writing
			writer = new PrintWriter(parsed);
			// the header line cannot be written until the first row has been
			// generated, since the generation of a row necessarily determines
			// any processing attributes that need to be added to each hit, and
			// therefore also to the set of column headers
			if (hasNext()) {
				ResultHit hit = next();
				if (hit instanceof TabularResultHit)
					writer.println(((TabularResultHit)hit).getHeaderLine());
				else if (hit instanceof GroupedHit)
					writer.println(((GroupedHit)hit).getHeaderLine());
				else writer.println(getHeaderLine());
				writer.println(hit.toString());
			}
			// write the rest of the rows
			while (hasNext()) {
				writer.println(next().toString());
			}
			return true;
		} catch (Throwable error) {
			logger.error("Error writing parsed result file", error);
			return false;
		} finally {
			// since all of this result's hits have been looped over, reload it 
			try {
				close();
				load();
			} catch (Throwable error) {}
			// close output file writer
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
		if (parsed == null)
			return null;
		return parsed.getAbsolutePath();
	}
	
	/*========================================================================
	 * Iterator methods
	 *========================================================================*/
	@Override
	public ResultHit next()
	throws NoSuchElementException {
		if (hasNext() == false)
			throw new NoSuchElementException();
		// this will be a grouped hit, consisting of all contiguous
		// hits with the same value for this field
		GroupedHit groupedHit = new GroupedHit(this);
		String groupByValue = null;
		while (true) {
			// read the next line from the result file
			String line = null;
			try {
				resultReader.mark(500000);
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
				break;
			// build hit from the fields in the parsed line
			ResultHit memberHit = null;
			try {
				List<String> fieldValues =
					Arrays.asList(line.split(getEscapedDelimiter()));
				memberHit = new TabularResultHit(this, fieldValues);
			} catch (Exception error) {
				logger.error(
					String.format(
						"Error parsing next hit from result file \"%s\".",
						resultFile.getAbsolutePath()),
					error);
				return null;
			}
			// process the hit
			for (ResultProcessor processor : getProcessors()) {
				processor.processHit(memberHit, this);
			}
			// get this hit's group-by field value
			String value = memberHit.getFirstFieldValue(groupBy);
			if (value == null)
				continue;
			// if this is the first hit read for this value, then note it
			if (groupByValue == null) {
				groupByValue = value;
				groupedHit.setAttribute(groupBy, groupByValue);
			}
			// if this hit has the same group-by value as
			// the rest of the group so far, add it
			if (value.equals(groupByValue))
				groupedHit.addMemberHit(memberHit);
			// otherwise, we've advanced beyond the end of this group,
			// so we need to roll back to the last line
			else try {
				//resultReader.seek(position);
				resultReader.reset();
				break;
			} catch (IOException error) {
				logger.error(
					String.format(
						"Error rolling back result file reader to " +
						"the previous line of result file [%s].",
						resultFile.getAbsolutePath()),
					error);
				return null;
			}
		}
		// verify that at least one valid member hit was found and added
		List<ResultHit> memberHits = groupedHit.getMemberHits();
		if (memberHits == null || memberHits.isEmpty())
			throw new NoSuchElementException();
		else groupedHit.setAttribute("Hits",
			Integer.toString(memberHits.size()));
		// process the hit
		for (ResultProcessor processor : getProcessors()) {
			processor.processHit(groupedHit, this);
		}
		return groupedHit;
	}
}
