package edu.ucsd.livesearch.parameter.processors;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import edu.ucsd.livesearch.parameter.ParameterProcessor;
import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.util.AminoAcidUtils;

public class PtmProcessor
implements ParameterProcessor
{
	/*========================================================================
	 * Public interface methods
	 *========================================================================*/
	/**
	 * Validates all custom and fixed PTM parameters submitted by the user
	 * from the CCMS ProteoSAFe web application input form.
	 * 
	 * @param builder	an {@link TaskBuilder} object representing the building
	 * 					state of the task whose parameters are to be processed
	 * 
	 * @return			the {@link List} of error messages encountered
	 * 					during processing,
	 * 					null if processing completed successfully
	 */
	public List<String> processParameters(TaskBuilder builder) {
		if (builder == null)
			return null;
		// remove superfluous PTM parameters
		builder.removeParameter("ptm_mass");
		builder.removeParameter("ptm_residue");
		builder.removeParameter("ptm_type");
		// validate PTMs
		Collection<String> errors =
			AminoAcidUtils.validatePTMs(builder.getParameters());
		if (errors == null || errors.isEmpty())
			return null;
		else return new Vector<String>(errors);
	}
}
