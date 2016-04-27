package edu.ucsd.livesearch.parameter.validators;

import java.util.regex.Pattern;

public class MaskValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String DEFAULT_REGEX = "^.*$";
	private static final String INVALID_VALUE = "'%s' is invalid.";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private String regex;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public MaskValidator() {
		super();
		setMessage(INVALID_VALUE);
		setRegex(DEFAULT_REGEX);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		if (value == null)
			value = "";
		if (Pattern.matches(regex, value) == false)
			return String.format(getMessage(), getLabel());
		else return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getRegex() {
		return regex;
	}
	
	public void setRegex(String regex) {
		this.regex = regex;
	}
}
