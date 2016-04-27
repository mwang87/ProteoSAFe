package edu.ucsd.saint.commons.http;

public interface RetryPolicy {
	public boolean reset();
	public boolean keepTrying();
	public long waitingPeriod();
}
