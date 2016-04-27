<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.result.parsers.Result"
	import="edu.ucsd.livesearch.result.ResultFactory"
	import="edu.ucsd.livesearch.result.ResultViewXMLUtils"
	import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.task.NullTask"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
	import="edu.ucsd.livesearch.util.Commons"
	import="java.io.File"
	import="java.util.Enumeration"
	import="java.util.HashMap"
	import="java.util.Map"
	import="org.apache.commons.lang3.StringEscapeUtils"
	import="org.w3c.dom.Element"
	import="org.slf4j.Logger"
    import="org.slf4j.LoggerFactory"
%>
<%!
	/*========================================================================
	 * Constants
	 *========================================================================*/
	private static final Long MAX_DISPLAY_SIZE = 10000000L;	// 10M bytes
	
	private static final Logger logger =
        LoggerFactory.getLogger("Result.jsp");
	
	/*========================================================================
	 * Properties
	 *========================================================================*/
	//private Task task;
	//private String view;
	//private Map<String, Result> results;
	//private Map<String, String> parameters;
	
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	 class ProcessReturn { 
        public Task task;
        public String view;
        public Map<String, Result> results;
        public Map<String, String> parameters;
    }
	 
	@SuppressWarnings("unchecked")
	private void processRequest(HttpServletRequest request, ProcessReturn ret)
	throws ServletException, IllegalStateException {
        Task task;
        String view;
        Map<String, Result> results;
        Map<String, String> parameters;
		// extract the request parameters
		parameters = new HashMap<String, String>();
		try {
			Enumeration<String> parameterNames =
				(Enumeration<String>)request.getParameterNames();
			while (parameterNames.hasMoreElements()) {
				String parameter = parameterNames.nextElement();
				parameters.put(parameter, request.getParameter(parameter));
			}
		} catch (Throwable error) {
			parameters = null;
		}
		// get the ID of the task whose results are to be displayed
		String taskID = request.getParameter("task");
		if (taskID == null)
			throw new ServletException(
				"Please specify the ID of a valid task to see its results.");
		// retrieve the specified task, and verify that it can display results
		task = TaskManager.queryTask(taskID);
		if (task == null || task instanceof NullTask ||
			task.getStatus().equals(TaskStatus.NONEXIST))
			throw new ServletException(
				"No valid task could be found for task ID \"" + taskID + "\".");
		else if (task.getStatus().equals(TaskStatus.DONE) == false)
			throw new IllegalStateException();
		// retrieve the proper data specifications for the specified view
		view = request.getParameter("view");
		if (view == null)
			throw new ServletException("Please specify the name " +
				"of a valid result view for workflow type \"" +
				task.getFlowName() + "\" to see the results of this task.");
		Map<String, Element> dataSpecs =
			ResultViewXMLUtils.getDataSpecifications(task, view);
		if (dataSpecs == null || dataSpecs.isEmpty())
			throw new ServletException(
				"No valid data specifications could be found for view \"" +
				view + "\" of workflow type \"" + task.getFlowName() + "\".");
		// iterate over the data specifications, set up results
		results = new HashMap<String, Result>(dataSpecs.size());
		for (String block : dataSpecs.keySet()) {
			String blockID = view + "-" + block;
			Result result = ResultFactory.createResult(
				dataSpecs.get(block), task, blockID, parameters);
			if (result == null)
				throw new ServletException("There was an error retrieving " +
					"the result data for block \"" + block + "\" of " +
					"workflow type \"" + task.getFlowName() + "\".");
			else results.put(block, result);
		}
		ret.task = task;
		ret.view = view;
		ret.results = results;
		ret.parameters = parameters;
	}
%>
<%
	// process request, determine if the specified task can display results
	String displayError = null;
	Task task = null;
    String view = null;
    Map<String, Result> results = null;
    Map<String, String> parameters = null;
    ProcessReturn ret = new ProcessReturn();
    String callback_functionname = null;
	
	try {
		processRequest(request, ret);
		task = ret.task;
		view = ret.view;
		results = ret.results;
		parameters = ret.parameters;
		callback_functionname = request.getParameter("callback");
		
	} catch (ServletException error) {
		displayError = error.getMessage();
	} catch (IllegalStateException error) {
		// if the task is not done yet, redirect to the status page
		response.sendRedirect("status.jsp?task=" + request.getParameter("task"));
		return;
	}
	// determine total size of results
	long totalSize = 0L;
	for (String block : results.keySet()) {
		Long size = results.get(block).getSize();
		if (size == null) {
			displayError = "There was an error displaying the result data " +
				"for this view: could not determine the size of the result " +
				"data for block \"" + block + "\".";
			break;
		} else totalSize += size;
	}
	// determine if results are to be forcibly displayed
	String show = request.getParameter("show");
	boolean force = (show != null && show.equalsIgnoreCase("true"));
	// if result display is not forced, and total result size exceeds
	// allowable limit, report an error and do not display the results
	if (force == false && displayError == null && totalSize > MAX_DISPLAY_SIZE)
		displayError = "The result data for this view is too large " +
			"to display on an internet browser.<br/> Please use the " +
			"\"Download\" tab to download the results for further processing.";
	boolean showResult = (displayError == null);
%>

<%
for (String block : results.keySet()) {
        Result result = results.get(block);
        %><%=  callback_functionname %>({ "blockData" : <%= result.getData() %> });
        <%
}
%>