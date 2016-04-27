package edu.ucsd.livesearch.result.processors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class NativeIDProcessor
implements ResultProcessor
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Pattern INDEX_PATTERN =
		Pattern.compile("index=(\\d+)");
	private static final Pattern SCAN_PATTERN = Pattern.compile("scan=(\\d+)");
	private static final Pattern FILE_PATTERN = Pattern.compile("file=(.+)");
	
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
		// extract nativeID field from original result file column value
		String fieldValue = hit.getFieldValue(field);
		// the nativeID value may be stored as an attribute
		if (fieldValue == null)
			fieldValue = hit.getAttribute(field);
		if (fieldValue == null)
			return;
		// extract index, scan and file IDs from the nativeID value
		String index = "-1";
		String scan = "-1";
		String[] tokens = fieldValue.split("\\s+");
		for (String token : tokens) {
			// first try to extract an index
			Matcher matcher = INDEX_PATTERN.matcher(token);
			if (matcher.matches()) {
				index = matcher.group(1);
				// According to version 1.0.0 of the official mzTab format
				// specification (section 5.2, "Use of identifiers for input
				// spectra to a search", pages 6-8), the only officially
				// supported "index=" nativeID format in mzTab is MS:1000774
				// ("multiple peak list nativeID format"). The description for
				// this format clearly states that an "index is the spectrum
				// number in the file, starting from 0." Therefore, it should
				// be assumed that all mzTab files with "index=" nativeIDs use
				// 0-based indexing, and any that do not are incorrect.
				// Consequently, since our downstream tools (mainly specplot)
				// expect 1-based indexing, it is safe and reasonable to always
				// increment the index here.
				try {
					index = Integer.toString(Integer.parseInt(index) + 1);
				} catch (NumberFormatException error) {}
				continue;
			}
			// then try to extract a scan
			matcher = SCAN_PATTERN.matcher(token);
			if (matcher.matches()) {
				scan = matcher.group(1);
				continue;
			}
			// if it's a file specifier, and no index was also specified,
			// then assume that it's just a 1-spectrum file
			matcher = FILE_PATTERN.matcher(token);
			if (matcher.matches() && index.equals("-1")) {
				index = "1";
				continue;
			}
			// finally, try to just parse the value as a plain integer and call
			// it a scan number, to account for invalid nativeID ID references
			if (index.equals("-1") && scan.equals("-1")) try {
				scan = Integer.toString(Integer.parseInt(token));
			} catch (NumberFormatException error) {}
		}
		// write index and scan values as special hit attributes
		hit.setAttribute("nativeID_index", index);
		hit.setAttribute("nativeID_scan", scan);
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
