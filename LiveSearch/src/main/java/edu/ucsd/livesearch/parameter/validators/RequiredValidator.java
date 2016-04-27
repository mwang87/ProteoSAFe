package edu.ucsd.livesearch.parameter.validators;

public class RequiredValidator
extends ParameterValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String REQUIRED = "'%s' is required.";
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public RequiredValidator() {
		super();
		setMessage(REQUIRED);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		if (value == null || value.equals(""))
			return String.format(getMessage(), getLabel());
		else return null;
	}
}
