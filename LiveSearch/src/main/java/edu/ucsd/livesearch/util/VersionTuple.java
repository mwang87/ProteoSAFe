package edu.ucsd.livesearch.util;

public class VersionTuple
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private int    major, minor, revision;
	private String cache;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public VersionTuple(String text) {
		this();
		String[] sections = text.split(":");
		if (sections == null || sections.length < 1)
			return;
		else if (sections.length >= 2)
			cache = sections[1];
		String[] tokens = sections[0].split("\\.");
		if (tokens == null || tokens.length < 1)
			return;
		else try {
			major = Integer.parseInt(tokens[0]);
			if (tokens.length >= 2)
				minor = Integer.parseInt(tokens[1]);
			if (tokens.length >= 3)
				revision = Integer.parseInt(tokens[2]);
		} catch (NumberFormatException error) {}
	}
	
	public VersionTuple() {
		major = minor = revision = -1;
		cache = null;
	}
	
	public VersionTuple(int major, int minor, int revision) {
		this(major, minor, revision, null);
	}
	
	public VersionTuple(int major, int minor, int revision, String cache) {
		this.major = major;
		this.minor = minor;
		this.revision = revision;
		this.cache = cache;
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public int getMajor() {
		return major;
	}
	
	public int getMinor() {
		return minor;
	}
	
	public int getRevision() {
		return revision;
	}
	
	public String getCache() {
		return cache;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean compatibleWith(VersionTuple target) {
		return major > target.major ||
		    (major == target.major &&
		    	(minor > target.minor ||
		    		(minor == target.minor && revision >= target.revision)));
	}
	
	@Override
	public String toString() {
		StringBuffer version = new StringBuffer().append(major);
		if (minor >= 0) {
			version.append(".").append(minor);
			if (revision >= 0)
				version.append(".").append(revision);
		}
		return version.toString();
	}
}
