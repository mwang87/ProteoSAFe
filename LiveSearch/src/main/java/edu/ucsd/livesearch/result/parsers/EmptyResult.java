package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONObject;

import edu.ucsd.livesearch.task.Task;

public class EmptyResult
implements Result
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	protected String value;
	
	/*========================================================================
	 * Constructors
	 *========================================================================*/
	public EmptyResult() {
		value = "";
	}
	
	/*========================================================================
	 * OnDemandOperation methods
	 *========================================================================*/
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean resourceExists() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean resourceDated() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getResourceName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	@Override
	public boolean isLoaded() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void load() throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
	}
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	@Override
	public File getFile() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getData() {
		return String.format("\"%s\"", JSONObject.escape(value));
	}
	
	public void setData(String value) {
		if (value == null)
			this.value = "";
		else this.value = value;
	}
	
	@Override
	public Long getSize() {
		return (long)value.length();
	}
	
	@Override
	public Task getTask() {
		// TODO Auto-generated method stub
		return null;
	}
}
