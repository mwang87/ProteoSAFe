package edu.ucsd.livesearch.parameter.processors;

import java.util.List;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskBuilder;

public class MscProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		Task task = builder.getTask();
		if (task == null)
			return null;
		String msc = builder.getFirstParameterValue("msc.msc");
		if (msc != null && msc.equals("on")) {
			String workflow = builder.getFirstParameterValue("workflow");
			// if "workflow" is not present, check "tool"
			// for backwards compatibility reasons
			if (workflow == null)
				workflow = builder.getFirstParameterValue("tool");
			if (workflow != null && workflow.equals("MSALIGN"))
				task.setFlowName("MSC-MSALIGN");
			else task.setFlowName("MSC-INSPECT");
		}
		return null;
	}
}
