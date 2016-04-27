package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.Task;

public class RepresentativeTabularResult
extends GroupedTabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	@SuppressWarnings("unused")
	private static final Logger logger =
		LoggerFactory.getLogger(RepresentativeTabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected String selectBy;
	protected String operator;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public RepresentativeTabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public RepresentativeTabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// initialize selection variables
		setSelectBy(null);
		setOperator("max");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void preSort()
	throws IOException, IllegalArgumentException {
		super.preSort();
		// reset selectBy, to ensure that filenames are properly updated
		setSelectBy(selectBy);
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}
	
	public String getSelectBy() {
		return selectBy;
	}
	
	public void setSelectBy(String selectBy) {
		this.selectBy = selectBy;
		String operator = getOperator();
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
		// determine base filename of representative-filtered file
		String selectedBase = FilenameUtils.getBaseName(resultFile.getName());
		String selectedPrefix = block + "_";
		if (selectedBase.startsWith(selectedPrefix) == false)
			selectedBase = selectedPrefix + selectedBase;
		// determine proper suffix for this file, to
		// indicate that it has been filtered
		String selectedSuffix = ".";
		// if no select-by field or operator were specified,
		// then just pick the first row found for each group
		if (selectBy == null)
			selectedSuffix += "first";
		else {
			selectedSuffix += selectBy;
			if (operator != null)
				selectedSuffix += "_" + operator;
		}
		// only append the suffix if the source file has not
		// already been parsed with the specified parameters
		if (selectedBase.endsWith(selectedSuffix) == false)
			selectedBase = selectedBase + selectedSuffix;
		parsed = task.getPath(sourceDirectory + selectedBase + ".tsv");
	}
	
	public String getOperator() {
		return operator;
	}
	
	public void setOperator(String operator)
	throws NullPointerException, IllegalArgumentException {
		if (operator == null)
			throw new NullPointerException("");
		else if (operator.equals("min") || operator.equals("max"))
			this.operator = operator;
		else throw new IllegalArgumentException("");
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean resourceExists() {
		// pre-load the sorted source file, to ensure that the parent
		// grouped result has updated the resource name properly
		try { preSort(); } catch (Throwable error) {}
		// then check if the (now properly named) resource exists,
		// using the parent grouped result's procedure
		return super.resourceExists();
	}
	
	/*========================================================================
	 * Iterator methods
	 *========================================================================*/
	@Override
	public ResultHit next()
	throws NoSuchElementException {
		ResultHit groupedHit = super.next();
		if (groupedHit == null)
			return null;
		List<ResultHit> slice = ((GroupedHit)groupedHit).getMemberHits();
		// if the slice is empty, then there is no next element
		if (slice.isEmpty())
			throw new NoSuchElementException();
		// otherwise iterate over the slice and select the best representative
		ResultHit representative = null;
		for (ResultHit hit : slice) {
			if (representative == null) {
				representative = hit;
				// if no selection values have been provided,
				// just return the first hit found
				if (selectBy == null || operator == null)
					break;
			} else {
				String hitValue = hit.getFirstFieldValue(selectBy);
				String bestValueSoFar =
					representative.getFirstFieldValue(selectBy);
				if (hitValue == null || bestValueSoFar == null)
					continue;
				int difference = compare(hitValue, bestValueSoFar);
				if ((operator.equals("min") && difference < 0) ||
					(operator.equals("max") && difference > 0))
					representative = hit;
			}
		}
		return representative;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private Integer compare(String value1, String value2) {
		if (value1 == null || value2 == null)
			return null;
		// try converting to numbers first
		try {
			double number1 = Double.parseDouble(value1);
			double number2 = Double.parseDouble(value2);
			double difference = number1 - number2;
			if (difference < 0.0)
				return -1;
			else if (difference > 0.0)
				return 1;
			else return 0;
		} catch (Throwable error) {
			// if the values are not numbers, perform string comparison
			return value1.compareTo(value2);
		}
	}
}
