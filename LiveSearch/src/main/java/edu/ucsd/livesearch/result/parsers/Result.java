package edu.ucsd.livesearch.result.parsers;

import java.io.File;
import java.io.IOException;

import edu.ucsd.livesearch.task.OnDemandOperation;
import edu.ucsd.livesearch.task.Task;

/**
 * Interface for generically processing workflow result files for display
 * in the CCMS ProteoSAFe web application result view.
 * 
 * @author Jeremy Carver
 */
public interface Result
extends OnDemandOperation
{
	/**
	 * Implementations of this interface are assumed to expose two public
	 * constructors, with the following signatures:
	 * 
	 * public Result(File resultFile, Task task)
	 * throws NullPointerException, IllegalArgumentException, IOException
	 * 
	 * public Result(Result result)
	 * throws NullPointerException, IllegalArgumentException, IOException
	 */
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public boolean isLoaded();
	
	public void load()
	throws IOException;
	
	public void close();
	
	/*========================================================================
	 * Property accessor methods
	 *========================================================================*/
	public File getFile();
	
	public String getData();
	
	public Long getSize();
	
	public Task getTask();
}
