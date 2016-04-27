package edu.ucsd.livesearch.result.processors;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class InternalFilenameProcessor
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
		String fieldValue = hit.getFirstFieldValue(field);
		// the filename value may be stored as an attribute,
		// if this is a grouped hit
		if (fieldValue == null)
			fieldValue = hit.getAttribute(field);
		if (fieldValue == null)
			return;
		// add internal filename as a special hit attribute
		String uploadName = result.getTask().queryInternalName(fieldValue);
		if (uploadName != null)
			hit.setAttribute("internalFilename", uploadName);
		else hit.setAttribute("internalFilename", fieldValue);
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
