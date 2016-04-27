<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.saint.commons.WebAppProps"
    import="org.apache.commons.lang3.math.NumberUtils"    
%>
<%
	AccountManager manager = AccountManager.getInstance();
	String identity = (String)session.getAttribute("livesearch.user");
%>

<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>UCSD Computational Mass Spectrometry Website</title>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="../images/favicon.ico" type="image/icon" />		
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
	function init(){
		<%= ServletUtils.JSLogoBlock("logo", request, session) %>
	}
	</script>
	<title>Monthly statistics</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="../index.jsp"><div id="logo"></div></a>
	<div id="textWrapper">
		<h4><a href="../index.jsp">Back to main page</a>&nbsp;</h4>
		<hr />
<%
	if(manager.checkRole(identity, "administrator")){
		String site = WebAppProps.get("livesearch.site.name");
		String period = request.getParameter("period");
		int days = NumberUtils.toInt(period, 30); 
%>

<h2>
In the past <%= days %> days: <br/>

<%= TaskManager.queryNumSubmittedTasks(site, days) %> tasks are submitted <br/>
<%= TaskManager.queryNumCompletedTasks(site, days) %> tasks are completed <br/>
<%= TaskManager.queryNumFailedTasks(site, days) %> tasks have failed <br/>

<%
	}
%>
</h2>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
