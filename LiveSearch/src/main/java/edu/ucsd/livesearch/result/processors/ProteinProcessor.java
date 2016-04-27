package edu.ucsd.livesearch.result.processors;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;
import edu.ucsd.livesearch.util.Commons;

public class ProteinProcessor
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
		// extract protein from original result file column value
		String protein = hit.getFirstFieldValue(field);
		// the protein value may be stored as an attribute,
		// if this is a grouped hit
		if (protein == null)
			protein = hit.getAttribute(field);
		if (protein == null)
			return;
		String id = protein;
		// split the protein string into parts, separated by pipe ("|")
		String start = null;
		String parts[] = protein.split("\\|");
		if (parts == null || parts.length < 1)
			start = protein;
		// TODO: remove the below clause when IPI ceases to be important
		else for (String part : parts) {
			if (part.startsWith("IPI:")) {
				start = part;
				break;
			}
		}
		if (start == null)
			start = parts[0];
		// if the first part contains a colon, then we assume that
		// the token before the colon is the database ID, and the
		// token after it is the ID of the protein itself
		if (start.contains(":")) {				
			parts = start.split(":");
			if (parts != null && parts.length > 1)
				id = parts[1];
		}
		// if the first part is the string "gi" or the string "pdb",
		// then we assume that the identifier is the next part
		else if ((start.equals("gi") || start.equals("pdb")) &&
				parts != null && parts.length > 1)
			id = start + "|" + parts[1];
		// attempt to look up the protein description
		String description = Commons.getDescription(id);
		if (description == null)
			description = protein;
		// add protein ID and description as special hit attributes
		hit.setAttribute("ProteinID", id);
		hit.setAttribute("Comment", description);
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
