package edu.ucsd.livesearch.libraries;

import java.util.Comparator;

public class TimeStampComparater implements Comparator<SpectrumAnnotation> {

	@Override
	public int compare(SpectrumAnnotation arg0, SpectrumAnnotation arg1) {
		
		return arg1.getCreate_time().compareTo(arg0.getCreate_time());
	}

}
