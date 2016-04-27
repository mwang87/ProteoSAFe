package edu.ucsd.livesearch.parameter.validators;

public class IntegerRangeValidator
extends FloatRangeValidator
{
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public IntegerRangeValidator() {
		this(null, true, null, true);
	}
	
	public IntegerRangeValidator(String boundary,
		boolean maximum, boolean exclusive) {
		super(boundary, maximum, exclusive);
	}
	
	public IntegerRangeValidator(String minimum, boolean minimumExclusive,
		String maximum, boolean maximumExclusive) {
		super(minimum, minimumExclusive, maximum, maximumExclusive);
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public void setMinimum(String minimum) {
		int min;
		try {
			min = Integer.parseInt(minimum);
		} catch (Throwable error) {
			min = Integer.MIN_VALUE;
		}
		super.setMinimum(Integer.toString(min));
	}

	@Override
	public void setMaximum(String maximum) {
		int max;
		try {
			max = Integer.parseInt(maximum);
		} catch (Throwable error) {
			max = Integer.MAX_VALUE;
		}
		super.setMaximum(Integer.toString(max));
	}
}
