package edu.ucsd.saint.commons.http;

public class SteadyRetry implements RetryPolicy {

	private long period;
	private int max, retries;
	
	public SteadyRetry(long period, int max){
		this.period = period;
		this.max = max;
		this.retries = 0;
	}


	@Override
	public boolean reset() {
		retries = 0;
		return true;
	}


	@Override
	public boolean keepTrying() {
		if(retries > max)
			return false;
		retries++;
		return true;
	}
	
	@Override
	public long waitingPeriod() {
		return period;
	}

}
