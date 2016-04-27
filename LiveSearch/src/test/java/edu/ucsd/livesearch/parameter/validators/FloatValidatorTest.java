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

public class FloatValidatorTest
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final boolean debug = false;
	// float validator parameters
	private static final String FLOAT_PARAMETER =
		"Default.tolerance.Ion_tolerance";
	private static final String FLOAT_LABEL = "Ion tolerance";
	private static final String FLOAT_MINIMUM = "0";
	private static final String FLOAT_MAXIMUM = "1";
	private static final List<String> BAD_FLOATS =
		new Vector<String>(Arrays.asList(
			"ABC", "0", "0.0", "0.0e0", "-1", "1.1"
		));
	private static final List<String> GOOD_FLOATS =
		new Vector<String>(Arrays.asList(
			"0.1", "0.5", "15e-7", "1", "1.0", "1e0", "1.00000e-0"
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
		FloatValidator validator = new FloatValidator();
		validator.setParameter(FLOAT_PARAMETER);
		validator.setLabel(FLOAT_LABEL);
		validator.setMinimumExclusive(FLOAT_MINIMUM);
		validator.setMaximum(FLOAT_MAXIMUM);
		StringBuffer output = new StringBuffer("[Expected Errors]:");
		for (String value : BAD_FLOATS) {
			WorkflowParameterUtils.removeParameter(parameters, FLOAT_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, FLOAT_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			Assert.assertTrue("Expected to receive an error when " +
				"validating bad float value \"" + value + "\".",
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
		for (String value : GOOD_FLOATS) {
			WorkflowParameterUtils.removeParameter(parameters, FLOAT_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, FLOAT_PARAMETER, value);
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
				"validating good float value \"" + value + "\".", errors);
		}
	}
}
