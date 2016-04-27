package edu.ucsd.livesearch.result;

import java.util.Comparator;

public class ResultFieldComparator
implements Comparator<String>
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private boolean ascending;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public ResultFieldComparator() {
		this("ascending");
	}
	
	public ResultFieldComparator(String sortMethod) {
		setSortMethod(sortMethod);
	}
	
	/*========================================================================
	 * Comparator Methods
	 *========================================================================*/
	public int compare(String field1, String field2) {
		int comparison;
		if (field1 == field2)
			comparison = 0;
		else if (field1 == null)
			comparison = -1;
		else if (field2 == null)
			comparison = 1;
		else try {
			double difference =
				Double.parseDouble(field1) - Double.parseDouble(field2);
			if (difference < 0)
				comparison = -1;
			else if (difference > 0)
				comparison = 1;
			else comparison = 0;
		} catch (NumberFormatException error) {
			comparison = field1.compareTo(field2);
		}
		// if the sort method is descending, then reverse the natural ordering
		if (ascending == false)
			comparison *= -1;
		return comparison;
	}
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public String getSortMethod() {
		if (ascending)
			return "ascending";
		else return "descending";
	}
	
	public void setSortMethod(String sortMethod)
	throws NullPointerException, IllegalArgumentException {
		if (sortMethod == null)
			throw new NullPointerException("Sort method cannot be null.");
		else if (sortMethod.toLowerCase().equals("ascending"))
			ascending = true;
		else if (sortMethod.toLowerCase().equals("descending"))
			ascending = false;
		else throw new IllegalArgumentException("Sort method must be either " +
			"\"ascending\" or \"descending\".");
	}
}
