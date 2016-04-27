package edu.ucsd.livesearch.result.parsers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.simple.JSONObject;


public class GroupedHit
implements ResultHit
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private IterableResult result;
	private List<ResultHit> memberHits;
	private Map<String, String> attributes;
	private char delimiter;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public GroupedHit(IterableResult result) {
		// set result
		if (result == null)
			throw new NullPointerException("\"result\" cannot be null.");
		this.result = result;
		// set default output file delimiter (tab character)
		setDelimiter('\t');
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public final String toString() {
		return toRowLine();
	}
	
	public final String toJSON() {
		StringBuffer output = new StringBuffer("{");
		// print this hit's attributes
		List<String> attributeNames = getAttributeNames();
		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				output.append("\"");
				output.append(JSONObject.escape(attributeName));
				output.append("\":");
				String attributeValue = getAttribute(attributeName);
				if (attributeValue == null)
					output.append("null");
				else {
					output.append("\"");
					output.append(JSONObject.escape(attributeValue));
					output.append("\"");
				}
				output.append(",");
			}
		}
		// truncate trailing comma, if necessary
		if (output.charAt(output.length() - 1) == ',')
			output.setLength(output.length() - 1);
		output.append("}");
		return output.toString();
	}
	
	public final String toRowLine() {
		StringBuffer output = new StringBuffer();
		// print this hit's attributes
		List<String> attributeNames = getAttributeNames();
		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				String attributeValue = getAttribute(attributeName);
				if (attributeValue == null)
					output.append("null");
				else output.append(attributeValue);
				output.append(getDelimiter());
			}
		}
		// truncate trailing delimiter character
		if (output.charAt(output.length() - 1) == getDelimiter())
			output.setLength(output.length() - 1);
		return output.toString();
	}
	
	public final String getHeaderLine() {
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
	public final List<ResultHit> getMemberHits() {
		return memberHits;
	}
	
	public final void addMemberHit(ResultHit hit) {
		if (hit == null)
			return;
		else if (memberHits == null)
			memberHits = new Vector<ResultHit>();
		memberHits.add(hit);
	}
	
	public final List<String> getFieldNames() {
		return null;
	}
	
	public final String getFieldValue(String name) {
		return null;
	}
	
	public final List<String> getFieldValues(String name) {
		return null;
	}
	
	public final String getFirstFieldValue(String name) {
		return null;
	}
	
	public final void setFieldValue(String name, String value)
	throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	public char getDelimiter() {
		return delimiter;
	}
	
	public void setDelimiter(char delimiter) {
		this.delimiter = delimiter;
	}
	
	public final List<String> getAttributeNames() {
		if (attributes == null || attributes.isEmpty())
			return null;
		else return new Vector<String>(attributes.keySet());
	}
	
	public final String getAttribute(String name) {
		if (name == null || attributes == null)
			return null;
		else return attributes.get(name);
	}
	
	public final void setAttribute(String name, String value) {
		if (name == null)
			return;
		if (attributes == null)
			attributes = new LinkedHashMap<String, String>();
		attributes.put(name, value);
		result.addAttributeName(name);
	}
}
