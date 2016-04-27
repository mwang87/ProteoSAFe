<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.dataset.Dataset"
	import="edu.ucsd.livesearch.dataset.DatasetManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.task.NullTask"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager"
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
		function init() {
			// load page logo block
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>
		}
		
		function fetchPubMed() {
			// retrieve pmid from the form
			var form = document.forms["mainform"];
			var pmid = CCMSFormUtils.getFieldValue(form, "publication.pmid");
			if (pmid == null || pmid == "") {
				alert("Please enter a valid PubMed ID to look it up.");
				return;
			}
			// start loading spinner
			var waiting = document.getElementById("pmid_lookup_waiting");
			if (waiting != null) {
				var img = document.createElement("img");
				img.src = "images/inProgress.gif";
				img.height = img.width = "16";
				waiting.appendChild(img);
			}
			// invoke the service to look up the pmid
			$.ajax({
				type: "GET",
				url: "ManagePublications?pmid=" + pmid,
				async: false,
				success: function(data) {
					// update all the form fields
					if (data.pmid != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.pmid", data.pmid);
					if (data.pmcid != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.pmcid", data.pmcid);
					if (data.authors != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.authors", data.authors);
					if (data.title != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.title", data.title);
					if (data.citation != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.citation", data.citation);
					if (data["abstract"] != "null")
						CCMSFormUtils.setFieldValue(
							form, "publication.abstract", data["abstract"]);
					// stop loading spinner
					if (waiting != null)
						removeChildren(waiting);
				},
				error: function(request, status, error) {
					alert("Could not find an entry in PubMed for ID [" +
						pmid + "].");
					// stop loading spinner
					if (waiting != null)
						removeChildren(waiting);
				}
		    });
		}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<meta http-equiv="PRAGMA" content="NO-CACHE"/>
	<title>CCMS MassIVE Add Publication Form</title>
</head>
<body onload="init();">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br/>
	<div id="textWrapper">
		<% if (hasAccess == false) { %>
		<h3 style="text-align: center">
			You do not have permission to add a publication to
			the selected dataset.
		</h3>
		<% } else { %>
		<h4><a href="status.jsp?task=<%= taskID %>">Back to status page</a></h4>
		<hr/>
		<!-- Publication addition form -->
		<form name="mainform" method="post" action="ManagePublications">
			<table class="mainform">
				<tr><th colspan="4">Add Dataset Publication</th></tr>
				<!-- PubMed IDs -->
				<tr>
					<td style="text-align:right">PubMed ID:</td>
					<td>
						<input name="publication.pmid" type="text"/>
						<input type="button" value="Look up in PubMed"
							onclick="fetchPubMed();"/>
						<span id="pmid_lookup_waiting">&nbsp;</span>
					</td>
					<td style="text-align:right">PubMed Central ID:</td>
					<td><input name="publication.pmcid" type="text"/></td>
				</tr>
				<!-- authors -->
				<tr>
					<td style="vertical-align:top;text-align:right">Authors:</td>
					<td colspan="3">
						<input name="publication.authors" type="text"
							style="width: 80%"/>
					</td>
				</tr>
				<!-- title -->
				<tr>
					<td style="vertical-align:top;text-align:right">Title:</td>
					<td colspan="3">
						<input name="publication.title" type="text"
							style="width: 80%"/>
					</td>
				</tr>
				<!-- citation -->
				<tr>
					<td style="vertical-align:top;text-align:right">Citation:</td>
					<td colspan="3">
						<input name="publication.citation" type="text"
							style="width: 80%"/>
					</td>
				</tr>
				<tr>
					<td/>
					<td colspan="3">
						Example citation:
						<span style="font-style:italic;">
							Nat Biotechnol. 2014 Mar;32(3):223-6.
							doi: 10.1038/nbt.2839
						</span><br/>
						Other possible values are "(submitted)"
						or "(in preparation)"
					</td>
				</tr>
				<!-- abstract -->
				<tr>
					<td style="vertical-align:top;text-align:right">Abstract:</td>
					<td colspan="3">
						<textarea name="publication.abstract"
							rows="5" cols="80"></textarea>
					</td>
				</tr>
				<!-- submit button -->
				<tr>
					<td colspan="4" class="bottomline"
						style="text-align: right">
						<input id="submit_publication" type="submit"
							value="Add Publication"/>
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
