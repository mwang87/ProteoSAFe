package edu.ucsd.livesearch.parameter.processors;

import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;

public class FilterProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		// process filter parameter
		String filter = builder.getFirstParameterValue("filter.filter");
		if (filter == null)
			return null;
		else if (filter.equals("FDR")) {
			builder.removeParameter("PepFDR.PepFDR");
			builder.removeParameter("FPR.FPR");
			builder.setParameterValue("FPR.FPR", "1.0");
			builder.removeParameter("ModFDR.ModFDR");
		} else if (filter.equals("PepFDR")) {
			builder.removeParameter("FDR.FDR");
			builder.removeParameter("FPR.FPR");
			builder.setParameterValue("FPR.FPR", "1.0");
			builder.removeParameter("ModFDR.ModFDR");
		} else if (filter.equals("FPR")) {
			builder.removeParameter("FDR.FDR");
			builder.setParameterValue("FDR.FDR", "1.0");
			builder.removeParameter("PepFDR.PepFDR");
			builder.removeParameter("ModFDR.ModFDR");
		} else if (filter.equals("ModFDR")) {
			builder.removeParameter("FDR.FDR");
			builder.removeParameter("PepFDR.PepFDR");
			builder.removeParameter("FPR.FPR");
		}
		return null;
	}
}
