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

public class EmailValidatorTest
{
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final boolean debug = false;
	// email validator parameters
	private static final String EMAIL_PARAMETER = "email";
	private static final String EMAIL_LABEL = "Email me at";
	private static final List<String> BAD_EMAILS =
		new Vector<String>(Arrays.asList(
			"12345"
		));
	private static final List<String> GOOD_EMAILS =
		new Vector<String>(Arrays.asList(
			"ccms.web@gmail.com"
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
		EmailValidator validator = new EmailValidator();
		validator.setParameter(EMAIL_PARAMETER);
		validator.setLabel(EMAIL_LABEL);
		StringBuffer output = new StringBuffer("[Expected Errors]:");
		for (String value : BAD_EMAILS) {
			WorkflowParameterUtils.removeParameter(parameters, EMAIL_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, EMAIL_PARAMETER, value);
			List<String> errors = validator.processParameters(builder);
			Assert.assertTrue("Expected to receive an error when " +
				"validating bad email address \"" + value + "\".",
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
		for (String value : GOOD_EMAILS) {
			WorkflowParameterUtils.removeParameter(parameters, EMAIL_PARAMETER);
			WorkflowParameterUtils.setParameterValue(
				parameters, EMAIL_PARAMETER, value);
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
				"validating good email address \"" + value + "\".", errors);
		}
	}
}
