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
	
	try {
		processRequest(request, ret);
		task = ret.task;
		view = ret.view;
		results = ret.results;
		parameters = ret.parameters;
		
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
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>UCSD Computational Mass Spectrometry Website</title>
	<link href="styles/main.css" rel="stylesheet" type="text/css" />
	<link href="styles/networkdisplayer.css?2" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="/images/favicon.ico" type="image/icon" />
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/download.js" language="javascript" type="text/javascript"></script>
	
	<!-- Result view rendering scripts -->
    <script src="scripts/render.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/stream.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/updateResource.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/massivepage.js?5" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview_columnhandlers.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/parameterlink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/absolutelink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/molecular_dataset_linkout.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/fileViewLinkList.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/continuous_id_rating.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/continuous_id_rating_column_display.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/network_displayer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/network_utils.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/alignment_utils.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/task_reuse_resultview.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/chartdisplayer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/rarefaction_utils.js" language="javascript" type="text/javascript"></script>

    
    <!-- Third-party utility scripts -->
    <script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.js-panzoom.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/arbor.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />


    <script type="text/javascript" src="scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="scripts/result/lorikeet/css/lorikeet.css">

    <script type="text/javascript" src="scripts/result/highcharts/highstock.js"></script>
    <script type="text/javascript" src="scripts/result/highcharts/highcharts-more.js"></script>
    <script type="text/javascript" src="scripts/result/highcharts/exporting.js"></script>
    <script type="text/javascript" src="scripts/result/highcharts/csv_exporting.js"></script>
    
    <!-- Help text tooltip scripts -->
    <script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
	<%
		// if this result is being displayed, write block data to page
		if (showResult) {
			%>
			<script language="javascript" type="text/javascript" id="renderjsdata">
				var blockData = {};
			<%
			for (String block : results.keySet()) {
				Result result = results.get(block);
				%>blockData.<%= block %> = <%= result.getData() %>;
				<%
			}
			%></script><%
		}
	%>
	<script language="javascript" type="text/javascript">
	/* <![CDATA[ */
		var showResult = <%= showResult %>;
		var task = {
			id: "<%= task.getID() %>",
			workflow: "<%= task.getFlowName() %>",
			description: "<%= StringEscapeUtils.escapeJson(task.getDescription()) %>"
		};
		var resultView = null;
		var tooltip = createTooltip();
		
		function init() {
			<% if (showResult) { %>
				buildResultView(task, "<%= view %>", renderResults);
			<% } %>
		}
		
		function renderResults(result) {
			resultView = result;
			if (resultView == null)
				alert("Failed to build result view.");
			else if (blockData != null) {
				for (var block in blockData)
					resultView.setData(block, blockData[block]);
				resultView.render(document.getElementById("resultViewBlocks"));
			}
		}
		
		function reloadBlock(block, data) {
			if (block == null)
				alert("Failed to reload block: no block name was specified.");
			else {
				if (data != null)
					resultView.setData(block, data);
				resultView.renderBlock(block);
			}
		}
		
		// TODO: fix or move all below functions
		function decorateTable(table) {
			table.cellSpacing = "1";
			table.cellPadding = "4";
			table.className = "result";
			table.border = "0";	
			table.width = "100%";
		}
		
		
		function forceDisplay() {
			var force = confirm("Are you sure? This result file is " +
				"<%= totalSize / 1048576L %> MB in size. Loading these data " +
				"may result in significantly reduced browser performance.");
			if (force)
				window.location.assign(window.location.href + "&show=true");
		}
	/* ]]> */		
	</script>
</head>

<body onload="init()">
<div id="bodyWrapper" style="width: 100%; text-align: left">
		<div id="resultViewBlocks"></div>
		
		<%
			if (showResult == false) {
				%><h2><%= displayError %></h2><%
				// if the error is that the file is too large,
				// let the user load it anyway
				if (totalSize > MAX_DISPLAY_SIZE) {
					%>
					<form name="forceResultDisplay" method="get" action="">
						<input value="Show Results" type="button"
							onclick="forceDisplay();"/>
					</form>
					<%
				}
			}
		%>
	</div>  <!--  textWrapper  -->
</div>  <!--  bodyWrapper  -->

<div id="popup_mask" style="display: none;"></div>
<img id="popup_img" alt="" src=""/>
<br/><br/>

<!-- Column selector form -->
<div class="helpbox" id="hColumnSelector" style="left:-5000px;">
	<form name="columnSelector" method="get" action="">
		<table id="columnSelector">
			<tr>
				<td/>
				<td>
					<input value="Submit" type="button"
						onclick="evaluateColumns(this.form);"/>
				</td>
			</tr>
		</table>
	</form>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
