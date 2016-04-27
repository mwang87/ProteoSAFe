<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.dataset.Dataset"
	import="edu.ucsd.livesearch.dataset.DatasetManager"
	import="edu.ucsd.livesearch.parameter.ResourceManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.task.NullTask"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="java.util.Map"
	import="java.util.LinkedHashMap"
	import="org.apache.commons.lang3.StringEscapeUtils"
%><%
	// prevent caching
	response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	response.addHeader("Cache-Control", "post-check=0, pre-check=0");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);
	// dataset update operations must be based on an existing dataset task
	String taskID = request.getParameter("task");
	if (taskID == null || taskID.isEmpty()) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			"You must specify the ID of a valid dataset task to update it.");
		return;
	}
	Task task = TaskManager.queryTask(taskID);
	if (task == null || task instanceof NullTask) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			String.format("No task could be found with ID [%s].", taskID));
		return;
	}
	Dataset dataset = DatasetManager.queryDatasetByTaskID(taskID);
	if (dataset == null) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			String.format(
				"No associated dataset could be found for task with ID [%s].",
				taskID));
		return;
	}
	// the user must have permission to update the dataset
	boolean hasAccess = ServletUtils.isAdministrator(session) ||
		ServletUtils.sameIdentity(session, task.getUser());
	// try to look up all metadata values to get their labels
	Map<String, String> speciesMap = new LinkedHashMap<String, String>();
	String species = dataset.getSpecies();
	if (species != null) 
		ResourceManager.getCVResourceLabels(species, "species", speciesMap);
	Map<String, String> instrumentMap = new LinkedHashMap<String, String>();
	String instrument = dataset.getInstrument();
	if (instrument != null) 
		ResourceManager.getCVResourceLabels(
			instrument, "instrument", instrumentMap);
	Map<String, String> modificationMap = new LinkedHashMap<String, String>();
	String modification = dataset.getModification();
	if (modification != null) 
		ResourceManager.getCVResourceLabels(
			modification, "modification", modificationMap);
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<% ServletUtils.printSessionInfo(out, session); %>
<head>
	<link href="styles/jquery/jquery-ui.css" rel="stylesheet" type="text/css"/>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link href="images/favicon.ico" rel="shortcut icon" type="image/icon"/>
	
	<!-- General ProteoSAFe scripts -->
	<script src="scripts/form.js?1" language="javascript" type="text/javascript"></script>
	<script src="scripts/input.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	
	<!-- Third-party utility scripts -->
	<script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/jquery/jquery-ui-1.10.4.min.js" language="javascript" type="text/javascript"></script>
	
	<!-- Third-party widget scripts -->
	<script src="scripts/tooltips/spin.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
	
	<!-- Special script code pertaining exclusively to this page -->
	<script language="javascript" type="text/javascript">
		var tooltip = createTooltip();
		var species = [
		<% boolean first = true;
		for (String term : species.split(";")) {
			String label = speciesMap.get(term);
			if (label == null)
				label = "";
			else label = StringEscapeUtils.escapeJson(label);
			if (first == false) { %>,<% }
			%>{"value":"<%= term %>","label":"<%= label %>"}<%
			first = false;
		} %>
		];
		var instrument = [
		<% first = true;
		for (String term : instrument.split(";")) {
			String label = instrumentMap.get(term);
			if (label == null)
				label = "";
			else label = StringEscapeUtils.escapeJson(label);
			if (first == false) { %>,<% }
			%>{"value":"<%= term %>","label":"<%= label %>"}<%
			first = false;
		} %>
		];
		var modification = [
		<% first = true;
		for (String term : modification.split(";")) {
			String label = modificationMap.get(term);
			if (label == null)
				label = "";
			else {
				int pipe = label.indexOf("|");
				if (pipe >= 0)
					label = label.substring(0, pipe);
				label = StringEscapeUtils.escapeJson(label);
			}
			if (first == false) { %>,<% }
			%>{"value":"<%= term %>","label":"<%= label %>"}<%
			first = false;
		} %>
		];
		function init() {
			// load page logo block
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>
			// load form with module instances
			CCMSFormUtils.loadModule("multipleSelect", "input",
				document.getElementById("species"), "dataset.species",
				{"custom_label":"Custom Species"},
				{"dataset.species":{"options":{"_SERVICE":"QuerySpecies"}}},
				function() {
					CCMSFormUtils.setFieldValue(document.forms["mainform"],
						"dataset.species", species);
				});
			CCMSFormUtils.loadModule("multipleSelect", "input",
				document.getElementById("instrument"), "dataset.instrument",
				{"custom_label":"Custom MS Instrument"},
				{"dataset.instrument":{"options":{"_RESOURCE":"instrument"}}},
				function() {
					CCMSFormUtils.setFieldValue(document.forms["mainform"],
						"dataset.instrument", instrument);
				});
			CCMSFormUtils.loadModule("multipleSelect", "input",
				document.getElementById("modification"), "dataset.modification",
				{"custom_label":"Custom PTM"},
				{"dataset.modification":{"options":{"_RESOURCE":"modification"}}},
				function() {
					CCMSFormUtils.setFieldValue(document.forms["mainform"],
						"dataset.modification", modification);
				});
			<%
			String description = dataset.getDescription();
			if (description != null) {
			%>
			// populate the dataset description here, since the "value"
			// attribute is not directly supported by the HTML tag
			CCMSFormUtils.setFieldValue(document.forms["mainform"],
				"dataset.comments",
				"<%= StringEscapeUtils.escapeJson(description) %>");
			<% } %>
		}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<meta http-equiv="PRAGMA" content="NO-CACHE"/>
	<title>CCMS MassIVE Dataset Update Form</title>
</head>
<body onload="init();">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br/>
	<div id="textWrapper">
		<% if (hasAccess == false) { %>
		<h3 style="text-align: center">
			You do not have permission to update the selected dataset.
		</h3>
		<% } else { %>
		<h4><a href="status.jsp?task=<%= taskID %>">Back to status page</a></h4>
		<hr/>
		<!-- Dataset update form -->
		<form name="mainform" method="post" action="UpdateDataset">
			<table class="mainform">
				<tr><th colspan="2">Update Dataset</th></tr>
				<!-- title -->
				<tr>
					<td style="vertical-align:top;text-align:right">Title:</td>
					<td>
						<input name="desc" type="text" style="width: 80%"
							value="<%= task.getDescription() %>"/>
					</td>
				</tr>
				<!-- species -->
				<tr>
					<td style="vertical-align:top;text-align:right">Species:</td>
					<td id="species"></td>
				</tr>
				<!-- instrument -->
				<tr>
					<td style="vertical-align:top;text-align:right">Instrument:</td>
					<td id="instrument"></td>
				</tr>
				<!-- modification -->
				<tr>
					<td style="vertical-align:top;text-align:right">Modification:</td>
					<td id="modification"></td>
				</tr>
				<!-- principal investigator -->
				<tr>
					<td style="vertical-align:top;text-align:right">Principal Investigator:</td>
					<td>
						<input type="text" name="dataset.pi" size="50"
							value="<%= dataset.getPI() %>"/>
					</td>
				</tr>
				<!-- description -->
				<tr>
					<td style="vertical-align:top;text-align:right">Description:</td>
					<td>
						<textarea name="dataset.comments" rows="5" cols="80"></textarea>
					</td>
				</tr>
				<!-- submit button -->
				<tr>
					<td colspan="2" class="bottomline"
						style="text-align: right">
						<input id="submit_update" type="submit" value="Update"/>
					</td>
				</tr>
			</table>
			<input type="hidden" name="dataset"
				value="<%= dataset.getDatasetIDString() %>"/>
		</form>
		<br/>
		<% } %>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
