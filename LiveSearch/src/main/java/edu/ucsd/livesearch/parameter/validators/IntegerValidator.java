package edu.ucsd.livesearch.parameter.validators;

public class IntegerValidator
extends MaskValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String INTEGER_PATTERN = "^[\\+\\-]?\\d+$";
	private static final String NOT_INTEGER = "'%s' must be an integer.";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	IntegerRangeValidator rangeValidator;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public IntegerValidator() {
		super();
		setMessage(NOT_INTEGER);
		setRegex(INTEGER_PATTERN);
		rangeValidator = null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		String intError = super.validateParameter(value);
		if (intError != null)
			return intError;
		else if (rangeValidator != null)
			return rangeValidator.validateParameter(value);
		else return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public void setParameter(String parameter) {
		super.setParameter(parameter);
		if (rangeValidator != null)
			rangeValidator.setParameter(parameter);
	}
	
	@Override
	public void setLabel(String label) {
		super.setLabel(label);
		if (rangeValidator != null)
			rangeValidator.setLabel(label);
	}
	
	/*========================================================================
	 * Delegate property accessor methods
	 *========================================================================*/
	public String getMinimum() {
		if (rangeValidator == null)
			return null;
		else return rangeValidator.getMinimum();
	}
	
	public void setMinimum(String minimum) {
		if (rangeValidator == null) {
			rangeValidator = new IntegerRangeValidator(minimum, false, false);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMinimum(minimum);
	}
	
	public void setMinimumExclusive(String minimum) {
		if (rangeValidator == null) {
			rangeValidator = new IntegerRangeValidator(minimum, false, true);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMinimumExclusive(minimum);
	}
	
	public String getMaximum() {
		if (rangeValidator == null)
			return null;
		else return rangeValidator.getMaximum();
	}
	
	public void setMaximum(String maximum) {
		if (rangeValidator == null) {
			rangeValidator = new IntegerRangeValidator(maximum, true, false);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMaximum(maximum);
	}
	
	public void setMaximumExclusive(String maximum) {
		if (rangeValidator == null) {
			rangeValidator = new IntegerRangeValidator(maximum, true, true);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMaximumExclusive(maximum);
	}
	
	public Boolean isMinimumExclusive() {
		if (rangeValidator == null)
			return null;
		else return rangeValidator.isMinimumExclusive();
	}
	
	public Boolean isMaximumExclusive() {
		if (rangeValidator == null)
			return null;
		else return rangeValidator.isMaximumExclusive();
	}
}
