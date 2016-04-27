package edu.ucsd.livesearch.parameter.validators;

public class FloatValidator
extends MaskValidator
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final String FLOAT_PATTERN = 
		"^[\\+\\-]?(\\d+(\\.\\d*)?|\\.?\\d+)([eE][\\+\\-]?\\d+)?$";
	private static final String NOT_FLOAT = "'%s' must be a real number.";
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	FloatRangeValidator rangeValidator;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public FloatValidator() {
		super();
		setMessage(NOT_FLOAT);
		setRegex(FLOAT_PATTERN);
		rangeValidator = null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		String floatError = super.validateParameter(value);
		if (floatError != null)
			return floatError;
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
			rangeValidator = new FloatRangeValidator(minimum, false, false);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMinimum(minimum);
	}
	
	public void setMinimumExclusive(String minimum) {
		if (rangeValidator == null) {
			rangeValidator = new FloatRangeValidator(minimum, false, true);
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
			rangeValidator = new FloatRangeValidator(maximum, true, false);
			rangeValidator.setParameter(getParameter());
			rangeValidator.setLabel(getLabel());
		} else rangeValidator.setMaximum(maximum);
	}
	
	public void setMaximumExclusive(String maximum) {
		if (rangeValidator == null) {
			rangeValidator = new FloatRangeValidator(maximum, true, true);
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
