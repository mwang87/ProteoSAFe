package edu.ucsd.livesearch.result.processors;

import org.apache.commons.io.FilenameUtils;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class UploadFilenameProcessor
implements ResultProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String field;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void processHit(ResultHit hit, Result result) {
		if (hit == null || result == null)
			return;
		String field = getField();
		if (field == null)
			return;
		// extract filename from original result file column value
		boolean isField = true;
		String fieldValue = hit.getFirstFieldValue(field);
		// the filename value may be stored as an attribute,
		// if this is a grouped hit
		if (fieldValue == null) {
			fieldValue = hit.getAttribute(field);
			isField = false;
		}
		if (fieldValue == null)
			return;
		String filename = FilenameUtils.getName(fieldValue);
		if (filename == null)
			return;
		// determine original upload (de-mangled) filename
		String uploadName = result.getTask().queryOriginalName(filename);
		if (uploadName == null)
			uploadName = filename;
		else uploadName = FilenameUtils.getName(uploadName);
		// if this is an original column, update it
		if (isField)
			hit.setFieldValue(field, uploadName);
		// otherwise update it as an attribute
		else hit.setAttribute(field, uploadName);
		// add internal filename as a special hit attribute
		hit.setAttribute("internalFilename", filename);
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getField() {
		return field;
	}
	
	public void setField(String field) {
		this.field = field;
	}
}
