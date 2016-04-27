package edu.ucsd.livesearch.parameter.validators;

import java.util.HashSet;
import java.util.Set;

public class SetValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String INVALID_VALUE = "Invalid value for '%s': %s";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Set<String> options;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public SetValidator() {
		super();
		setMessage(INVALID_VALUE);
		options = new HashSet<String>();
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		if (options.contains(value) == false) {
			if (value == null)
				value = "null";
			else value = "\"" + value + "\"";
			return String.format(getMessage(), getLabel(), value);
		}
		else return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public Set<String> getOptions() {
		return options;
	}
	
	public void setOption(String value) {
		if (value != null)
			options.add(value);
	}
}
