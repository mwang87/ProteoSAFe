<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.dataset.Dataset"
	import="edu.ucsd.livesearch.dataset.DatasetManager"
	import="edu.ucsd.livesearch.dataset.DatasetPublisher"
	import="edu.ucsd.livesearch.publication.Publication"
	import="edu.ucsd.livesearch.result.ResultFactory"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
	import="edu.ucsd.livesearch.task.WorkflowUtils"
	import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface"
	import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface.InputCollection"
	import="edu.ucsd.livesearch.servlet.ManageParameters"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.UploadManager.PendingUpload"
	import="edu.ucsd.livesearch.util.FormatUtils"
	import="edu.ucsd.livesearch.util.WorkflowParameterUtils"
	import="edu.ucsd.saint.commons.WebAppProps"
	import="java.util.Collection"
	import="java.util.Iterator"
	import="java.util.LinkedList"
	import="java.util.Map"
	import="org.apache.commons.lang3.time.DurationFormatUtils"
	import="org.apache.commons.lang3.StringEscapeUtils"

%><%
	String      identity		= (String)session.getAttribute(
									"livesearch.user");
	String		taskID			= request.getParameter("task");
	Task		task			= TaskManager.queryTask(taskID);
	TaskStatus	status			= task.getStatus();
	String		desc			= task.getDescription();
	boolean		hasAccess		= ServletUtils.isAdministrator(session) ||
									ServletUtils.sameIdentity(
										session, task.getUser());
	boolean		isDone			= false;
	boolean		needRefreshing	= false;
	int			refreshTime		= 5;

	// compile task user's details into a display string
	StringBuffer userDetails = new StringBuffer(task.getUser());
	Map<String, String> profile =
		AccountManager.getInstance().getProfile(task.getUser());
	if (profile != null) {
		String email = profile.get("email");
		if (email != null && email.trim().isEmpty() == false &&
			email.trim().equalsIgnoreCase("N/A") == false) {
			userDetails.append(" (");
			userDetails.append(email);
			userDetails.append(")");
		}
		String organization = profile.get("organization");
		if (organization != null && organization.trim().isEmpty() == false &&
			organization.trim().equalsIgnoreCase("N/A") == false) {
			userDetails.append(", ");
			userDetails.append(organization);
		}
	}

	// uploaded file display
	Collection<InputCollection> collections = null;
	// calling this method may result in an error, if the task has been deleted
	try {
		collections =
			DownloadWorkflowInterface.getCollections(taskID, identity);
	} catch (Throwable error) {}

	// TODO: do the same for system resources as we did for input collections

	switch (status) {
		case DONE:
			isDone = true;
			break;
		case LAUNCHING:
		case RUNNING:
		case UPLOADING:
		case QUEUED:
			needRefreshing = true;
			refreshTime = 30;
			break;
	}

	// if this is a dataset task, fetch the dataset
	Dataset dataset = null;
	boolean isDataset = false;
	String datasetID = null;
	String datasetComments = null;
	Collection<Publication> publications = null;
	boolean hasPublications = false;
	String ftpURL = "";
	String pxID = null;
	if (task.getFlowName().toUpperCase().startsWith("MASSIVE"))
		dataset = DatasetManager.queryDatasetByTaskID(task.getID());
	if (dataset != null) {
		isDataset = true;
		datasetID = dataset.getDatasetIDString();
		datasetComments = dataset.getDescription();
		// query DB for this dataset's associated publications
		dataset = DatasetManager.queryDatasetPublications(dataset);
		publications = dataset.getPublications();
		if (publications != null && publications.isEmpty() == false)
			hasPublications = true;
		// determine this dataset's FTP URL
		ftpURL = DatasetPublisher.getDatasetFTPURL(dataset);
		pxID = dataset.getAnnotation("px_accession");
		if (pxID == null)
			pxID = "N/A";
	}
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>UCSD Computational Mass Spectrometry Website</title>
	<link href="styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon" />
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/render.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/fileprogress.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
		function init() {
		 	<%= ServletUtils.JSLogoBlock("logo", request, session) %>
		}
	</script>
<%
	if (needRefreshing) {
%>
	<meta http-equiv="refresh" content="<%= refreshTime %>" />
<%
	}
%>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<div id="textWrapper">
<%
	if (status != TaskStatus.NONEXIST) {
%>
	<h4><a href="index.jsp">Back to main page</a></h4><hr />

	<table class="mainform" border="0" cellspacing="0" cellpadding="2" align="center" width="100%">
		<tr><th>Job Status</th> </tr>
		<tr><td><table border="1" cellspacing="1" cellpadding="4" class="sched" width="100%">
					<tr><th>Workflow</th><td><%= task.getFlowName() %></td></tr>
					<tr><th>Status</th>
						<td style="background-color:<%= needRefreshing? "lightblue" : "lightgreen" %>;">
							<%= ServletUtils.HTMLStatusBlock(
									task, request, session, true) %>
							<div style="clear: both; padding-top: 1px">
							<% if (status == TaskStatus.DONE) {
								String navigator =
									ResultFactory.getHTMLViewNavigator(task);
								if (navigator == null)
									navigator = "";
							%>
								<%= navigator %>
							<% }
								String msgs = task.getMessage();
								if (msgs != null && status != TaskStatus.RUNNING && status != TaskStatus.DONE) {
									out.println("<hr style='width: 100%; height: 0px; border: 1px solid #5570B2;'>");
									for(String msg: msgs.split("\\n"))
										out.println(StringEscapeUtils.escapeHtml4(msg) + " <br/>");
								}
							%>
							</div>
						</td></tr>
					<% if (isDataset) { %>
						<tr>
							<th>MassIVE Accession</th>
							<td>
								<%= datasetID %>
							</td>
						</tr>
						<% if (pxID.equals("N/A") == false ||
							task.getFlowName().toUpperCase().equals("MASSIVE-COMPLETE")) { %>
						<tr>
							<th>ProteomeXchange Accession</th>
							<td><%= pxID %></td>
						</tr>
						<% } if (isDone) { %>
						<tr>
							<th>FTP Download</th>
							<td>
								<div style="float:left;padding-right:10px;">
									<a style="vertical-align:middle;"
										href="<%= ftpURL %>">
										<%= ftpURL %>
									</a>
								</div>
								<!--
								<div style="font-size:70%">
									Note: FTP access to this dataset using
									Chrome may require you to explicitly log on
									<br/>
									with username "<%= datasetID %>" and
									password "a" (do not enter the quotes).
								</div>
								-->
							</td>
						</tr>
						<% }
					} %>
					<tr><th>User</th><td><%= userDetails.toString() %></td></tr>
					<tr><th>Title</th>
						<td><div style="overflow-x: auto;">
								<%= desc == null ? "N/A" : desc %>
							</div>
						</td>
					</tr>
					<%
					if (datasetComments != null) {
						%><tr>
							<th>Description</th>
							<td><%= datasetComments %></td>
						</tr><%
					}
					%>
					<tr>
						<th>Date Created</th>
						<td><%= task.getCreateTime() %></td>
					</tr>
					<% if (needRefreshing || isDataset == false) { %>
					<tr>
						<th>Execution Time</th>
						<td><%= DurationFormatUtils.formatDurationWords(task.getElapsedTime(), true, true) %></td>
					</tr>
					<% }
					if (isDataset == false || status == TaskStatus.RUNNING) { %>
					<tr><th>Progress</th>
					<% String url = (status == TaskStatus.RUNNING) ?
							"/LiveFlow/" : "";
					%>
						<td><embed src="<%= url %>layout.jsp?task=<%= taskID %>"/>
						</td>
					</tr>
					<% } %>
					<% if (isDataset && isDone) { %>
					<tr>
						<th>Files Uploaded</th>
						<td>
							<%= dataset.getFileCountString() %>
							(<%= dataset.getFileSizeString() %>)
						</td>
					</tr>
					<% }
					if (collections != null && collections.isEmpty() == false) {
						for (InputCollection collection : collections) {
					%>
					<tr>
						<th><%= collection.getLabel() %></th>
						<td style="padding-top: 20px; padding-bottom: 20px;">
						<div style="display: table-cell; vertical-align: middle; padding-right: 100px;">
						<%
							int count = 0;
							Iterator<String> iterator = collection.getFiles().iterator();
							while (iterator.hasNext() && (count++) < 20) {
								out.println(iterator.next() + "<br/>");
							}
							if (collection.getFiles().size() >= 20) {
						%>
						<br />
						<div id='more<%= collection.getName() %>'>
							<a href="#" onclick="toggleDiv('all<%= collection.getName() %>'); toggleDiv('more<%= collection.getName() %>');">
								More available
							</a>
						</div>
						<div id='all<%= collection.getName() %>' style='display: none;'>
						<%
							while (iterator.hasNext()) {
								out.println(iterator.next() + "<br/>");
							}
						%>
						<br />
						<a href="#" onclick="toggleDiv('all<%= collection.getName() %>'); toggleDiv('more<%= collection.getName() %>');">
							Show less
						</a>
						</div>
						<% } %>
						</div>
						<% if (TaskStatus.UPLOADING.equals(status)) { %>
						<jsp:include page="pendingUploads.jsp">
							<jsp:param name="task" value="<%= taskID %>"/>
							<jsp:param name="purpose"
								value="<%= collection.getPurpose() %>"/>
							<jsp:param name="legend"
								value="Pending <%= collection.getLabel() %>"/>
						</jsp:include>
						<% } %>
						</td>
					</tr>
					<% } }
					if (hasPublications) { %>
					<tr>
						<th>Publications</th>
						<td style="padding-top: 20px; padding-bottom: 20px;">
						<%
							boolean first = true;
							for (Publication publication : publications) {
								if (first == false)
									out.println("<br/><br/>");
								out.println(publication
									.getHTMLFormattedReferenceString());
								first = false;
							}
						%>
						</td>
					</tr>
					<% } %>
				</table>
			</td>
		</tr>
		<tr><td class="bottomline">&nbsp;</td></tr>
	</table>
	<%
	} else { %>
	<h1>This task does not exist.  How did you get here?</h1>
	<% } %>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
