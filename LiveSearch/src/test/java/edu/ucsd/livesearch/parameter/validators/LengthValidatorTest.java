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

public class LengthValidatorTest
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final boolean debug = false;
	// length validator parameters
	private static final String LENGTH_PARAMETER = "desc";
	private static final String LENGTH_LABEL = "Description";
	private static final String LENGTH_MAXIMUM = "10";
	private static final List<String> BAD_LENGTHS =
		new Vector<String>(Arrays.asList(
			"ABCDEFGHIJK", "This is a long description"
		));
	private static final List<String> GOOD_LENGTHS =
		new Vector<String>(Arrays.asList(
			"ABCDEFGHIJ", "Short"
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
		LengthValidator validator = new LengthValidator();
		validator.setParameter(LENGTH_PARAMETER);
		validator.setLabel(LENGTH_LABEL);
		validator.setMaximum(LENGTH_MAXIMUM);
		StringBuffer output = new StringBuffer("[Expected Errors]:");
		for (String value : BAD_LENGTHS) {
			WorkflowParameterUtils.removeParameter(
				parameters, LENGTH_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, LENGTH_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			Assert.assertTrue("Expected to receive an error when " +
				"validating bad length value \"" + value + "\".",
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
		for (String value : GOOD_LENGTHS) {
			WorkflowParameterUtils.removeParameter(
				parameters, LENGTH_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, LENGTH_PARAMETER, value);
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
				"validating good length value \"" + value + "\".", errors);
		}
	}
}
