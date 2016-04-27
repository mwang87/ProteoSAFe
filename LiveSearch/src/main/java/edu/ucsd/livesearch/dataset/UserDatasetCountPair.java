package edu.ucsd.livesearch.dataset;


public class UserDatasetCountPair implements Comparable<UserDatasetCountPair>{
	public String username;
	public Integer count;
	
	public UserDatasetCountPair(String username, Integer count) {
		super();
		this.username = username;
		this.count = count;
	}

	@Override
	public int compareTo(UserDatasetCountPair arg0) {
		return count - arg0.count;
	}
	
};