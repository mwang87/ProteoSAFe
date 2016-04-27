<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.FileManager"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.livesearch.task.WorkflowUtils"
	import="edu.ucsd.saint.commons.WebAppProps"
	import="java.util.logging.Level"
	import="org.apache.commons.lang3.StringEscapeUtils"
	import="org.slf4j.Logger"
	import="org.slf4j.LoggerFactory"
%><%
	Logger logger = LoggerFactory.getLogger(this.getClass()); 
	boolean authenticated = ServletUtils.isAuthenticated(session);
	boolean	isAdmin = ServletUtils.isAdministrator(session);
	if(authenticated && isAdmin){
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>A flawed workflow for testing purpose</title>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="../images/favicon.ico" type="../image/icon" />		
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
</head>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<div id="textWrapper">
<br />
<%
		Task task = null;
		try{		
			task = TaskManager.createTask("guest", WebAppProps.get("livesearch.site.name"));
			String flowname ="TEST_1"; 
			task.setFlowName(flowname);
			response.sendRedirect("../status.jsp?task=" + task.getID());
			TaskManager.setRunning(task);
			WorkflowUtils.launchWorkflow(task);
			String msg = String.format("Test workflow instance [%s] launched", flowname);
%><h2><%=msg%></h2><%			
			logger.info(msg);
		}catch(Exception e){
			logger.info("Failed to launch test workflow", e);
%><h2>Failed to launch test workflow</h2><%			
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
%>
</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>

<% } %>
