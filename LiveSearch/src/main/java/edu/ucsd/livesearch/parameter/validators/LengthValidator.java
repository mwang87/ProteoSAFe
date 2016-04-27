package edu.ucsd.livesearch.parameter.validators;

public class LengthValidator
extends IntegerRangeValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String MIN_NOT_REACHED =
		"'%s' cannot contain fewer than %s%s characters.";
	private static final String MAX_EXCEEDED =
		"'%s' cannot contain more than %s%s characters.";
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public LengthValidator() {
		this(Integer.toString(Integer.MAX_VALUE));
	}
	
	public LengthValidator(String maximum) {
		super("0", false, maximum, false);
		setMinNotReached(MIN_NOT_REACHED);
		setMaxExceeded(MAX_EXCEEDED);
		setExclusiveQualifier("");
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		String length = "0";
		if (value != null)
			length = Integer.toString(value.length());
		return super.validateParameter(length);
	}
}
