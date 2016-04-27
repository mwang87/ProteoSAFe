package edu.ucsd.livesearch.subscription;

import java.util.Comparator;

public class CIJobTimeStampComparater implements Comparator<ContinuousIDJob> {

	@Override
	public int compare(ContinuousIDJob arg0, ContinuousIDJob arg1) {
		
		return arg1.getTimestamp().compareTo(arg0.getTimestamp());
	}

}
