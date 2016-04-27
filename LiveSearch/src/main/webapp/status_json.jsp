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
	Collection<InputCollection> collections =
		DownloadWorkflowInterface.getCollections(taskID, identity);

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

	String status_string = "";
	switch (status) {
                case DONE:
                        status_string = "DONE";
                        break;
                case LAUNCHING:
                        status_string = "LAUNCHING";
                        break;
                case RUNNING:
                        status_string = "RUNNING";
                        break;
                case UPLOADING:
                        status_string = "UPLOADING";
                        break;
                case QUEUED:
                        status_string = "QUEUED";
                        break;
                case FAILED:
                        status_string = "FAILED";
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
%>
<%
	if (status != TaskStatus.NONEXIST) {
%>
        {
        <% if (status == TaskStatus.DONE) {
        %>
            "status":"DONE",
        <% }

        %>
        "description":"<%=desc%>",
        "workflow":"<%= task.getFlowName() %>",
        "status":"<%= status_string %>",
        "user":"<%= task.getUser() %>"
        }

	<%
	} else { %>
	{error:  "Invalid Task"}
	<% } %>
