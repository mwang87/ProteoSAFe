package edu.ucsd.livesearch.parameter.processors;

import java.util.ArrayList;
import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class MsgfPlusProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		List<String> errors = new ArrayList<String>();
		String c13 = builder.getFirstParameterValue("c13_nnet.c13");
		try {
			Integer.parseInt(c13);
			builder.setParameterValue("c13_nnet.msgfPlusC13", "0," + c13);
		} catch (Throwable error) {
			errors.add("Parameter \"c13_nnet.c13\" must be a valid integer.");
		}
		String nnet = builder.getFirstParameterValue("c13_nnet.nnet");
		try {
			int ntt = 2 - Integer.parseInt(nnet);
			builder.setParameterValue(
				"c13_nnet.msgfPlusNnet", Integer.toString(ntt));
		} catch (Throwable error) {
			errors.add("Parameter \"c13_nnet.nnet\" must be a valid integer.");
		}
		if (errors.isEmpty())
			return null;
		else return errors;
	}
}
