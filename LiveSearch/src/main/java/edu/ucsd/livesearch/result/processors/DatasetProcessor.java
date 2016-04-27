package edu.ucsd.livesearch.result.processors;

import edu.ucsd.livesearch.dataset.Dataset;
import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.result.parsers.Result;
import edu.ucsd.livesearch.result.parsers.ResultHit;

public class DatasetProcessor
implements ResultProcessor
{
	/*========================================================================
	 * Properties
	 *========================================================================*/
	Dataset dataset;
	boolean checked = false;
	
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public void processHit(ResultHit hit, Result result) {
		if (hit == null || result == null)
			return;
		// only check for a dataset if we haven't checked yet
		if (dataset == null && checked == false) {
			// try to get the dataset associated with this task
			dataset =
				DatasetManager.queryDatasetByTaskID(result.getTask().getID());
			// set this processor as having already checked for this dataset
			checked = true;
		}
		// if an associated dataset was found, write its
		// ID and string name as special hit attributes
		if (dataset != null) {
			hit.setAttribute(
				"datasetID", Integer.toString(dataset.getDatasetID()));
			hit.setAttribute("datasetName", dataset.getDatasetIDString());
		}
	}
}
