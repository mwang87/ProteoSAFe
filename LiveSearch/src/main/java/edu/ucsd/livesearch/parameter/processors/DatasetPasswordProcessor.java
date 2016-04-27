package edu.ucsd.livesearch.parameter.processors;

import java.util.List;

import edu.ucsd.livesearch.dataset.DatasetManager;
import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class DatasetPasswordProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		// ensure that password is non-empty
		String password = builder.getFirstParameterValue("dataset.password");
		if (password == null || password.isEmpty()) {
			password = DatasetManager.PUBLIC_DATASET_PASSWORD;
			builder.setParameterValue("dataset.password", password);
		}
		return null;
	}
}
