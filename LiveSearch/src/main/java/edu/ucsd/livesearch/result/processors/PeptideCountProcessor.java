package edu.ucsd.livesearch.result.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.ucsd.livesearch.result.parsers.GroupedHit;
import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class PeptideCountProcessor
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
		// peptide count processors can only operate on grouped hits
		GroupedHit groupedHit = null;
		try {
			groupedHit = (GroupedHit)hit;
		} catch (ClassCastException error) {
			return;
		}
		// iterate over the hits associated with this group, and count peptides
		List<ResultHit> memberHits = groupedHit.getMemberHits();
		if (memberHits == null || memberHits.isEmpty())
			return;
		Set<String> uniquePeptides = new TreeSet<String>();
		Set<String> uniqueModifiedPeptides = new TreeSet<String>();
		for (ResultHit memberHit : memberHits) {
			// first check field values
			List<String> values = memberHit.getFieldValues(getField());
			// if not found there, check attributes
			if (values == null || values.isEmpty()) {
				String value = memberHit.getAttribute(getField());
				if (value != null) {
					values = new ArrayList<String>(1);
					values.add(value);
				}
			}
			// if not found at all, move on
			if (values == null || values.isEmpty())
				continue;
			// otherwise, process all found values
			else for (String value : values) {
				String peptide = clean(value);
				String plainPeptide = unmodify(peptide);
				if (plainPeptide != null) {
					uniquePeptides.add(plainPeptide);
					if (peptide != null &&
						peptide.equals(plainPeptide) == false)
						uniqueModifiedPeptides.add(peptide);
				}
			}
		}
		// add peptide counts as special hit attributes
		hit.setAttribute("Peptides", Integer.toString(uniquePeptides.size()));
		hit.setAttribute(
			"Modified", Integer.toString(uniqueModifiedPeptides.size()));
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
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String clean(String peptide) {
		if (peptide != null) {
			// remove enclosing dots and bordering residues
			int start = peptide.indexOf('.');
			// it's apparently legal for a peptide sequence to start with "[."
			if (start == 1 && peptide.charAt(0) != '[')
				peptide = peptide.substring(start + 1);
			int end = peptide.lastIndexOf('.');
			if (end == peptide.length() - 2)
				peptide = peptide.substring(0, end);
			// remove all whitespace in the sequence
			peptide = peptide.replaceAll("[ \t]+", "");
		}
		return peptide;
	}
	
	private String unmodify(String peptide) {
		if (peptide != null)
			// remove all modifications, which are defined to be all
			// sequences of characters that are not upper-case letters
			peptide = peptide.replaceAll("[^A-Z]+", "");
		return peptide;
	}
}
