package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.task.OnDemandLoader;
import edu.ucsd.livesearch.task.Task;

public class UnrolledTabularResult
extends TabularResult
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private final Logger logger =
		LoggerFactory.getLogger(UnrolledTabularResult.class);
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected File   source;
	protected File   unrolled;
	protected String unrollBy;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public UnrolledTabularResult(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(resultFile, task, block);
	}
	
	public UnrolledTabularResult(Result result, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super(result, block);
	}
	
	@Override
	protected void init(File resultFile, Task task, String block)
	throws NullPointerException, IllegalArgumentException, IOException {
		super.init(resultFile, task, block);
		// set source file to original result file, before unrolling
		source = resultFile;
		// set default unroll field (null)
		setUnrollBy(null);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public void load()
	throws IOException, IllegalArgumentException {
		// the unrolled result file must be written in advance
		if (OnDemandLoader.load(this))
			resultFile = unrolled;
		else throw new IOException("A valid result file unrolled " +
			"by the specified field could not be generated.");
		// once the proper unrolled file is assigned, load normally
		super.load();
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		return unrolled;
	}
	
	public String getUnrollBy() {
		return unrollBy;
	}
	
	public void setUnrollBy(String unrollBy) {
		this.unrollBy = unrollBy;
		// if no unroll field was specified, then no parsing is required
		if (unrollBy == null)
			unrolled = source;
		// otherwise, set target for unrolled result file
		// to indicate unroll field
		else {
			String taskRoot = task.getPath("").getAbsolutePath();
			// get source directory path relative to task root
			String sourceDirectory = source.getParent();
			if (sourceDirectory.startsWith(taskRoot) == false)
				throw new IllegalArgumentException(
					String.format("Result file [%s] must reside somewhere " +
						"underneath task directory [%s].",
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
			// determine base filename of unrolled file
			String unrolledBase = FilenameUtils.getBaseName(source.getName());
			String unrolledPrefix = block + "_";
			if (unrolledBase.startsWith(unrolledPrefix) == false)
				unrolledBase = unrolledPrefix + unrolledBase;
			// determine proper suffix for this file, to
			// indicate that it has been unrolled
			String unrolledSuffix = "." + unrollBy + "_unrolled";
			// only append the suffix if the source file has not
			// already been unrolled with the specified parameters
			if (unrolledBase.endsWith(unrolledSuffix) == false)
				unrolledBase = unrolledBase + unrolledSuffix;
			unrolled = task.getPath(sourceDirectory + unrolledBase + ".tsv");
		}
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
		// if no unroll field is specified, then no parsing is required
		else if (unrollBy == null)
			return resourceExists();
		// otherwise, parse the source file, unroll each packed row, and
		// then write the unrolled contents to the destination file
		else try {
			file = new RandomAccessFile(source, "r");
			// get header, find index of unroll field
			String header = file.readLine();
			if (header == null)
				return false;
			int unrollFieldIndex = -1;
			String[] fields = header.split(getEscapedDelimiter());
			if (fields == null || fields.length < 1)
				return false;
			for (int i=0; i<fields.length; i++) {
				String field = fields[i];
				if (field != null && field.equals(unrollBy)) {
					unrollFieldIndex = i;
					break;
				}
			}
			if (unrollFieldIndex < 0)
				return false;
			// set up output file, write header to it
			writer = new PrintWriter(unrolled);
			writer.println(header);
			while (true) {
				String line = file.readLine();
				if (line == null)
					break;
				String tokens[] = line.split(getEscapedDelimiter());
				String fieldValue = tokens[unrollFieldIndex];
				// extract all the values in this hit
				String values[] = fieldValue.split(getEscapedFieldDelimiter());
				// don't attempt to unroll this line if
				// the unroll field wasn't even found
				if (values == null || values.length < 1)
					writer.println(line);
				// otherwise, unroll the row by writing one line for each
				// one of its unroll field values
				else for (String value : values) {
					StringBuffer unrolledLine = new StringBuffer();
					for (int i=0; i<tokens.length; i++) {
						if (i == unrollFieldIndex)
							unrolledLine.append(value).append(getDelimiter());
						else unrolledLine.append(tokens[i])
							.append(getDelimiter());
					}
					// chomp trailing delimiter
					if (unrolledLine.charAt(unrolledLine.length() - 1)
						== getDelimiter())
						unrolledLine.setLength(unrolledLine.length() - 1);
					// write the unrolled line to the output file
					writer.println(unrolledLine.toString());
				}
			}
			return true;
		} catch (Exception error) {
			logger.error(String.format(
				"Failed to write unrolled result file [%s]",
				unrolled.getAbsolutePath()), error);			
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
		if (unrolled == null)
			return false;
		else return unrolled.exists();
	}
	
	@Override
	public boolean resourceDated() {
		if (unrolled == null || unrolled.exists() == false ||
			source == null || source.exists() == false)
			return false;
		else return unrolled.lastModified() < source.lastModified(); 
	}
	
	@Override
	public String getResourceName() {
		return unrolled.getAbsolutePath();
	}
}
