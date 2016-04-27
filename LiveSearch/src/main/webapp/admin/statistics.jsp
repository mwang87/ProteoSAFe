<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
	import="edu.ucsd.saint.commons.WebAppProps"
    import="org.apache.commons.lang3.math.NumberUtils"    
    import="java.util.List"
    import="java.util.Map"
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
	<title>Site statistics</title>
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
		List<Map<String, String>> users = TaskManager.queryActiveUsers(site);
%>

	<h2>
		<%= TaskManager.queryNumRunningTasks(site) %> tasks are running <br/>
		<%= TaskManager.queryNumSuspendedTasks(site) %> tasks are suspended <br/>
		<%= TaskManager.queryNumLongRunningTasks(site, 7) %> tasks are running over 1 week <br/>
		<%= users.size() %> users are active <br/>
	</h2>	

	<% if(!users.isEmpty()){ %>
	
	<table class="sched">
		<tr><th>User ID</th>
			<th>email</th>
			<th>Running jobs</th>
			<th>Name</th>
			<th>Organization</th></tr>
		<%
			for(Map<String, String> user: users){
		%>	
		<tr><td><%= user.get("id") %></td>
			<td><%= user.get("notification") %></td>
			<td><%= user.get("count") %></td>
			<td><%= user.get("realname") %></td>
			<td><%= user.get("organization") %></td>
		</tr>
	</table>
		<%
			}
		}
	%>
<%
	}
%>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
