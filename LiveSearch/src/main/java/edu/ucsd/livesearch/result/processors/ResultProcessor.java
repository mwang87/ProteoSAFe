package edu.ucsd.livesearch.result.processors;

import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public interface ResultProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void processHit(ResultHit hit, Result result);
}
