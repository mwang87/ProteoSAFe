package edu.ucsd.livesearch.result.parsers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;


public class TabularResultHit
implements ResultHit
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private TabularResult result;
	private List<String> fieldNames;
	private List<String> fieldValues;
	private Map<String, String> attributes;
	private char delimiter;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public TabularResultHit(
		TabularResult result, List<String> fieldValues
	) throws NullPointerException, IllegalArgumentException {
		// set result
		if (result == null)
			throw new NullPointerException("\"result\" cannot be null.");
		this.result = result;
		// set field names
		List<String> fieldNames = result.getFieldNames();
		if (fieldNames == null)
			throw new NullPointerException("\"fieldNames\" cannot be null.");
		this.fieldNames = fieldNames;
		// set field values
		if (fieldValues == null)
			throw new NullPointerException("\"fieldValues\" cannot be null.");
		else if (fieldValues.size() != fieldNames.size())
			throw new IllegalArgumentException(
				String.format("The number of items in \"fieldValues\" (%d) " +
					"must match the number of items in \"fieldNames\" (%d).",
					fieldValues.size(), fieldNames.size()));
		this.fieldValues = fieldValues;
		// set default field delimiter (exclamation mark character)
		setDelimiter('!');
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
		// print fields from result file first
		List<String> fieldNames = getFieldNames();
		if (fieldNames != null) {
			for (String fieldName : fieldNames) {
				output.append("\"");
				//output.append(StringEscapeUtils.escapeJson(fieldName));
				output.append(JSONObject.escape(fieldName));
				output.append("\":");
				String fieldValue = getFieldValue(fieldName);
				if (fieldValue == null)
					output.append("null");
				else {
					output.append("\"");
					//output.append(
					//	StringEscapeUtils.escapeJson(fieldValue));
					output.append(JSONObject.escape(fieldValue));
					output.append("\"");
				}
				output.append(",");
			}
		}
		// then print any special attributes that may
		// have been generated during processing
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
		// truncate trailing comma
		if (output.charAt(output.length() - 1) == ',')
			output.setLength(output.length() - 1);
		output.append("}");
		return output.toString();
	}
	
	public final String toRowLine() {
		char delimiter = result.getDelimiter();
		StringBuffer output = new StringBuffer();
		// print fields from result file first
		List<String> fieldNames = getFieldNames();
		if (fieldNames != null) {
			for (String fieldName : fieldNames) {
				String fieldValue = getFieldValue(fieldName);
				if (fieldValue == null)
					output.append("null");
				else output.append(fieldValue);
				output.append(delimiter);
			}
		}
		// then print any special attribute names that
		// may have been generated during processing
		List<String> attributeNames = getAttributeNames();
		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				String attributeValue = getAttribute(attributeName);
				if (attributeValue == null)
					output.append("null");
				else output.append(attributeValue);
				output.append(delimiter);
			}
		}
		// truncate trailing delimiter character
		if (output.charAt(output.length() - 1) == delimiter)
			output.setLength(output.length() - 1);
		return output.toString();
	}
	
	public final String getHeaderLine() {
		char delimiter = result.getDelimiter();
		StringBuffer header = new StringBuffer();
		// print field names from result file first
		List<String> fieldNames = getFieldNames();
		if (fieldNames != null) {
			for (String fieldName : fieldNames) {
				header.append(fieldName);
				header.append(delimiter);
			}
		}
		// then print any attribute names that may
		// have been generated during processing
		List<String> attributeNames = getAttributeNames();
		if (attributeNames != null) {
			for (String attributeName : attributeNames) {
				header.append(attributeName);
				header.append(delimiter);
			}
		}
		// truncate trailing delimiter, if necessary
		if (header.charAt(header.length() - 1) == delimiter)
			header.setLength(header.length() - 1);
		return header.toString();
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public final List<String> getFieldNames() {
		return new Vector<String>(fieldNames);
	}
	
	public final String getFieldValue(String name) {
		if (name == null)
			return null;
		int index = fieldNames.indexOf(name);
		if (index < 0)
			return null;
		else return fieldValues.get(index);
	}
	
	public final List<String> getFieldValues(String name) {
		if (name == null)
			return null;
		int index = fieldNames.indexOf(name);
		if (index < 0)
			return null;
		else {
			String value = fieldValues.get(index);
			if (value == null)
				return null;
			// split value into multiple values
			String[] values = value.split(getEscapedDelimiter());
			if (values == null || values.length < 1)
				values = new String[]{value};
			return Arrays.asList(values);
		}
	}
	
	public final String getFirstFieldValue(String name) {
		List<String> fieldValues = getFieldValues(name);
		if (fieldValues == null || fieldValues.isEmpty())
			return getFieldValue(name);
		else return fieldValues.get(0);
	}
	
	public final void setFieldValue(String name, String value) {
		if (name == null)
			return;
		int index = fieldNames.indexOf(name);
		if (index < 0)
			return;
		else fieldValues.set(index, value);
	}
	
	public final char getDelimiter() {
		return delimiter;
	}
	
	public final String getEscapedDelimiter() {
		return StringEscapeUtils.escapeJava(Character.toString(delimiter));
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
