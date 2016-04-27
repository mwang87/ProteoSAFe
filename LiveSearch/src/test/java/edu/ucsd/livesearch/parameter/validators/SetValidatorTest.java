package edu.ucsd.livesearch.parameter.validators;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.ucsd.livesearch.task.TaskBuilder;
import edu.ucsd.livesearch.util.WorkflowParameterUtils;

public class SetValidatorTest
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final boolean debug = false;
	// length validator parameters
	private static final String SET_PARAMETER = "Default.instrument.instrument";
	private static final String SET_LABEL = "Instrument";
	private static final List<String> SET_OPTIONS =
		new Vector<String>(Arrays.asList(
			"ESI-ION-TRAP", "QTOF", "FT-HYBRID"
		));;
	private static final List<String> BAD_OPTIONS =
		new Vector<String>(Arrays.asList(
			"ABC", "DOOMSDAY_MACHINE"
		));
	private static final List<String> GOOD_OPTIONS =
		new Vector<String>(Arrays.asList(
				"ESI-ION-TRAP", "QTOF", "FT-HYBRID"
		));
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	private Map<String, Collection<String>> parameters;
	private TaskBuilder builder;
	
	/*========================================================================
	 * Fixture methods
	 *========================================================================*/
	@Before
	public final void setUp() {
		parameters = new LinkedHashMap<String, Collection<String>>();
		builder = new TaskBuilder(null, parameters);
	}
	
	/*========================================================================
	 * Test methods
	 *========================================================================*/
	@Test
	public final void testEmailValidator() {
		SetValidator validator = new SetValidator();
		validator.setParameter(SET_PARAMETER);
		validator.setLabel(SET_LABEL);
		for (String option : SET_OPTIONS)
			validator.setOption(option);
		StringBuffer output = new StringBuffer("[Expected Errors]:");
		for (String value : BAD_OPTIONS) {
			WorkflowParameterUtils.removeParameter(parameters, SET_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, SET_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			Assert.assertTrue("Expected to receive an error when " +
				"validating bad set option \"" + value + "\".",
				(errors != null && errors.size() > 0));
			for (String error : errors) {
				output.append("\n  [");
				output.append(value);
				output.append("] = ");
				output.append(error);
			}
		}
		if (debug)
			System.out.println(output.toString());
		output = new StringBuffer();
		for (String value : GOOD_OPTIONS) {
			WorkflowParameterUtils.removeParameter(parameters, SET_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, SET_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			if (errors != null && errors.size() > 0)  {
				for (String error : errors) {
					output.append("\n  [");
					output.append(value);
					output.append("] = ");
					output.append(error);
				}
			}
			if (output.length() > 0)
				System.err.println("Unexpected Errors]:" + output.toString());
			Assert.assertNull("Expected to receive no error when " +
				"validating good set option \"" + value + "\".", errors);
		}
	}
}
