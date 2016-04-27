package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.Task;

public class SlicedTabularResult
extends GroupedTabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private final Logger logger =
		LoggerFactory.getLogger(SlicedTabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected String sliceBy;
	protected List<ResultHit> slice;
	protected int cursor;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public SlicedTabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public SlicedTabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// initialize slice variables
		setSliceBy(null);
		slice = null;
		cursor = 0;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
	
	public String getSliceBy() {
		return sliceBy;
	}
	
	public void setSliceBy(String sliceBy) {
		this.sliceBy = sliceBy;
		// if no slice-by field was specified, then no parsing can happen
		if (sliceBy == null)
			parsed = null;
		// otherwise, set target for sliced result
		// file to indicate slice-by field
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
			String slicedBase = FilenameUtils.getBaseName(source.getName());
			String slicedPrefix = block + "_";
			if (slicedBase.startsWith(slicedPrefix) == false)
				slicedBase = slicedPrefix + slicedBase;
			// determine proper suffix for this file, to
			// indicate that it has been sliced
			String slicedSuffix = "." + sliceBy + "_sliced";
			// only append the suffix if the source file has not
			// already been sorted with the specified parameters
			if (slicedBase.endsWith(slicedSuffix) == false)
				slicedBase = slicedBase + slicedSuffix;
			parsed = task.getPath(sourceDirectory + slicedBase + ".tsv");
		}
	}
	
	/*========================================================================
	 * Iterator methods
	 *========================================================================*/
	@Override
	public boolean hasNext() {
		if (slice == null)
			return super.hasNext();
		else return cursor < slice.size();
	}
	
	@Override
	public ResultHit next()
	throws NoSuchElementException {
		if (sliceBy == null)
			throw new NoSuchElementException("A valid non-null " +
				"slice-by value must be provided to retrieve a " +
				"sliced result hit.");
		// retrieve this slice of the result data
		while (slice == null) {
			ResultHit groupedHit = super.next();
			if (groupedHit == null)
				return null;
			String value = groupedHit.getAttribute(groupBy);
			if (value != null && value.equals(sliceBy))
				slice = ((GroupedHit)groupedHit).getMemberHits();
		}
		// if the slice is empty, or the cursor has advanced beyond
		// the end of the slice, then there is no next element
		if (slice.isEmpty() || cursor >= slice.size())
			throw new NoSuchElementException();
		else {
			ResultHit hit = slice.get(cursor);
			cursor++;
			return hit;
		}
	}
}
