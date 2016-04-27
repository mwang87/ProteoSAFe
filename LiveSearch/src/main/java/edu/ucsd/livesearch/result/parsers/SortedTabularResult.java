package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.result.ResultFieldComparator;
import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;

public class SortedTabularResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(SortedTabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected File source;
	protected File sorted;
	protected String sortBy;
	protected boolean ascending;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public SortedTabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public SortedTabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// set source file to original result file, before sorting
		source = resultFile;
		// set default sort field (null)
		setSortBy(null);
		// set default sort operator (ascending)
		setOperator("ascending");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void load()
	throws IOException, IllegalArgumentException {
		// the sorted result file must be written in advance
		if (OnDemandLoader.load(this))
			resultFile = sorted;
		else throw new IOException("A valid result file sorted by " +
			"the specified sort field could not be generated.");
		// once the proper sorted file is assigned, load normally
		super.load();
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		return sorted;
	}
	
	public String getSortBy() {
		return sortBy;
	}
	
	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
		// if no sort field was specified, then no parsing is required
		if (sortBy == null)
			sorted = source;
		// otherwise, set target for sorted result file to indicate sort field
		else {
			String taskRoot = task.getPath("").getAbsolutePath();
			// get source directory path relative to task root
			String sourceDirectory = source.getParent();
			if (sourceDirectory.startsWith(taskRoot) == false)
				throw new IllegalArgumentException(
					String.format("Result file \"%s\" must reside somewhere " +
						"underneath task directory \"%s\".",
						source.getAbsolutePath(), taskRoot));
			sourceDirectory = sourceDirectory.substring(taskRoot.length());
			if (sourceDirectory.startsWith("/"))
				sourceDirectory = sourceDirectory.substring(1);
			if (sourceDirectory.endsWith("/") == false)
				sourceDirectory += "/";
			// only create a new nested directory under temp/ if the source
			// file was not already itself from a temporary directory
			if (sourceDirectory.startsWith("temp/") == false)
				sourceDirectory = "temp/" + sourceDirectory;
			// determine base filename of sorted file
			String sortedBase = FilenameUtils.getBaseName(source.getName());
			String sortedPrefix = block + "_";
			if (sortedBase.startsWith(sortedPrefix) == false)
				sortedBase = sortedPrefix + sortedBase;
			// determine proper suffix for this file, to
			// indicate that it has been sorted
			String sortedSuffix = "." + sortBy + "_";
			if (ascending)
				sortedSuffix += "ascending";
			else sortedSuffix += "descending";
			// only append the suffix if the source file has not
			// already been sorted with the specified parameters
			if (sortedBase.endsWith(sortedSuffix) == false)
				sortedBase = sortedBase + sortedSuffix;
			sorted = task.getPath(sourceDirectory + sortedBase + ".tsv");
		}
	}
	
	public String getOperator() {
		if (ascending)
			return "ascending";
		else return "descending";
	}
	
	public void setOperator(String operator)
	throws NullPointerException, IllegalArgumentException {
		if (operator == null)
			throw new NullPointerException("Sort method cannot be null.");
		else if (operator.toLowerCase().equals("ascending"))
			ascending = true;
		else if (operator.toLowerCase().equals("descending"))
			ascending = false;
		else throw new IllegalArgumentException("Sort method must be either " +
			"\"ascending\" or \"descending\".");
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean execute() {
		RandomAccessFile file = null;
		PrintWriter writer = null;
		if (source == null)
			return false;
		// if no sort field is specified, then no parsing is required
		else if (sortBy == null)
			return resourceExists();
		// otherwise, parse the source file, sort its contents, and
		// then write the sorted contents to the destination file
		else try {
			Map<String, SortedSet<Long>> pointers =
				new HashMap<String, SortedSet<Long>>();
			file = new RandomAccessFile(source, "r");
			// get header, find index of sort field
			String header = file.readLine();
			if (header == null)
				return false;
			int sortFieldIndex = -1;
			String[] fields = header.split(getEscapedDelimiter());
			if (fields == null || fields.length < 1)
				return false;
			for (int i=0; i<fields.length; i++) {
				String field = fields[i];
				if (field != null && field.equals(sortBy)) {
					sortFieldIndex = i;
					break;
				}
			}
			if (sortFieldIndex < 0)
				return false;
			while (true) {
				long pointer = file.getFilePointer();
				String line = file.readLine();
				if (line == null)
					break;
				String tokens[] = line.split(getEscapedDelimiter());
				String fieldValue = tokens[sortFieldIndex];
				// extract the first value, if there are many in this hit
				String values[] = fieldValue.split("!");
				if (values != null && values.length > 0)
					fieldValue = values[0];
				SortedSet<Long> valuePointers = pointers.get(fieldValue);
				if (valuePointers == null)
					valuePointers = new TreeSet<Long>();
				valuePointers.add(pointer);
				pointers.put(fieldValue, valuePointers);
			}
			writer = new PrintWriter(sorted);
			writer.println(header);
			// write the sorted result file lines in the correct
			// order, based on the specified sorting parameters
			SortedSet<String> fieldValues =
				new TreeSet<String>(new ResultFieldComparator(getOperator()));
			fieldValues.addAll(pointers.keySet());
			for (String field : fieldValues) {
				SortedSet<Long> positions = pointers.get(field);
				if (positions == null)
					continue;
				for (long position : positions) {
					file.seek(position);
					writer.println(file.readLine());
				}
			}
			return true;
		} catch (Exception error) {
			logger.error(
				String.format("Failed to write result file \"%s\"",
					sorted.getAbsolutePath()),
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
		if (sorted == null)
			return false;
		else return sorted.exists();
	}
	
	@Override
	public boolean resourceDated() {
		if (sorted == null || sorted.exists() == false ||
			source == null || source.exists() == false)
			return false;
		else return sorted.lastModified() < source.lastModified(); 
	}
	
	@Override
	public String getResourceName() {
		return sorted.getAbsolutePath();
	}
}
