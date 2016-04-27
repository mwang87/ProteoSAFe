package edu.ucsd.livesearch.parameter.validators;

public class EmailValidator
extends MaskValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String EMAIL_PATTERN =
		"^[\\w\\.%+-]+@[a-zA-Z\\d\\.-]+\\.[a-zA-Z]{2,4}$";
	private static final String NOT_EMAIL =
		"'%s' must be a valid email address.";
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public EmailValidator() {
		super();
		setMessage(NOT_EMAIL);
		setRegex(EMAIL_PATTERN);
	}
}
