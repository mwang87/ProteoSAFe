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

public class IntegerValidatorTest
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final boolean debug = false;
	// integer validator parameters
	private static final String INTEGER_PARAMETER = "Default.ptm.mods";
	private static final String INTEGER_LABEL =
		"Maximum number of PTMs permitted in a single peptide";
	private static final String INTEGER_MINIMUM = "0";
	private static final String INTEGER_MAXIMUM = "3";
	private static final List<String> BAD_INTEGERS =
		new Vector<String>(Arrays.asList(
			"ABC", "-0.1", "0.0", "1E0", "2.5", "-1", "4"
		));
	private static final List<String> GOOD_INTEGERS =
		new Vector<String>(Arrays.asList(
			"0", "1", "2", "3"
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
		IntegerValidator validator = new IntegerValidator();
		validator.setParameter(INTEGER_PARAMETER);
		validator.setLabel(INTEGER_LABEL);
		validator.setMinimum(INTEGER_MINIMUM);
		validator.setMaximum(INTEGER_MAXIMUM);
		StringBuffer output = new StringBuffer("[Expected Errors]:");
		for (String value : BAD_INTEGERS) {
			WorkflowParameterUtils.removeParameter(
				parameters, INTEGER_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, INTEGER_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			Assert.assertTrue("Expected to receive an error when " +
				"validating bad integer value \"" + value + "\".",
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
		for (String value : GOOD_INTEGERS) {
			WorkflowParameterUtils.removeParameter(
				parameters, INTEGER_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, INTEGER_PARAMETER, value);
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
				"validating good integer value \"" + value + "\".", errors);
		}
	}
}
