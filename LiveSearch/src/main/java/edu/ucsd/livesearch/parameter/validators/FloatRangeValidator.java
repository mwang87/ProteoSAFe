package edu.ucsd.livesearch.parameter.validators;

public class FloatRangeValidator
extends ParameterValidator
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	// range parameters
	private double minimum;
	private double maximum;
	private boolean minimumExclusive;
	private boolean maximumExclusive;
	// error messages
	private String minNotReached = "'%s' must be greater than %s%s.";
	private String maxExceeded = "'%s' must be less than %s%s.";
	private String exclusiveQualifier = "or equal to ";
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public FloatRangeValidator() {
		this(null, true, null, true);
	}
	
	public FloatRangeValidator(String boundary,
		boolean maximum, boolean exclusive) {
		super();
		if (maximum) {
			setMinimumExclusive(null);
			if (exclusive)
				setMaximumExclusive(boundary);
			else setMaximum(boundary);
		} else {
			setMaximumExclusive(null);
			if (exclusive)
				setMinimumExclusive(boundary);
			else setMinimum(boundary);
		}
	}
	
	public FloatRangeValidator(String minimum, boolean minimumExclusive,
		String maximum, boolean maximumExclusive) {
		super();
		if (minimumExclusive)
			setMinimumExclusive(minimum);
		else setMinimum(minimum);
		if (maximumExclusive)
			setMaximumExclusive(maximum);
		else setMaximum(maximum);
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public String validateParameter(String value) {
		Double parsedValue = null;
		try {
			parsedValue = Double.parseDouble(value);
		} catch (Throwable error) {
			// TODO: do something
			return null;
		}
		String label = getLabel();
		// check bottom of range
		if (minimumExclusive && parsedValue <= minimum)
			return String.format(minNotReached,
				label, "", truncate(minimum));
		else if (minimumExclusive == false && parsedValue < minimum)
			return String.format(minNotReached,
				label, exclusiveQualifier, truncate(minimum));
		// check top of range
		if (maximumExclusive && parsedValue >= maximum)
			return String.format(maxExceeded,
				label, "", truncate(maximum));
		else if (maximumExclusive == false && parsedValue > maximum)
			return String.format(maxExceeded,
				label, exclusiveQualifier, truncate(maximum));
		return null;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public String getMinimum() {
		return Double.toString(minimum);
	}
	
	public void setMinimum(String minimum) {
		try {
			this.minimum = Double.parseDouble(minimum);
		} catch (Throwable error) {
			this.minimum = Double.NEGATIVE_INFINITY;
		}
		minimumExclusive = false;
	}
	
	public void setMinimumExclusive(String minimum) {
		setMinimum(minimum);
		minimumExclusive = true;
	}
	
	public String getMaximum() {
		return Double.toString(maximum);
	}
	
	public void setMaximum(String maximum) {
		try {
			this.maximum = Double.parseDouble(maximum);
		} catch (Throwable error) {
			this.maximum = Double.POSITIVE_INFINITY;
		}
		maximumExclusive = false;
	}
	
	public void setMaximumExclusive(String maximum) {
		setMaximum(maximum);
		maximumExclusive = true;
	}
	
	public boolean isMinimumExclusive() {
		return minimumExclusive;
	}
	
	public boolean isMaximumExclusive() {
		return maximumExclusive;
	}
	
	public String getMinNotReached() {
		return minNotReached;
	}
	
	public void setMinNotReached(String minNotReached) {
		this.minNotReached = minNotReached;
	}
	
	public String getMaxExceeded() {
		return maxExceeded;
	}
	
	public void setMaxExceeded(String maxExceeded) {
		this.maxExceeded = maxExceeded;
	}
	
	public String getExclusiveQualifier() {
		return exclusiveQualifier;
	}
	
	public void setExclusiveQualifier(String exclusiveQualifier) {
		this.exclusiveQualifier = exclusiveQualifier;
	}
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private String truncate(double value) {
		double floor = Math.floor(value);
		if (floor == value)
			return Integer.toString((int)value);
		else return Double.toString(value);
	}
}
