<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.*"
	import="java.io.*, java.util.*"
	import="org.apache.commons.lang3.StringEscapeUtils"
%><%
	boolean checking = request.getParameter("login") != null;
	String user = request.getParameter("user");
	String password = request.getParameter("password");
	String url = request.getParameter("url");
	boolean authenticated = false;
	AccountManager manager = AccountManager.getInstance();
	if(checking) authenticated = manager.authenticate(user, password);
	else user="";
	if(url == null || url.equals("")) url = "../index.jsp";
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>UCSD Computational Mass Spectrometry Website</title>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="../images/favicon.ico" type="../image/icon" />		
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
<% if(authenticated){ %>
	<meta http-equiv="refresh" content="2;URL=<%= url %>" />
<%	} %>
</head>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<div id="textWrapper">
<br />
<%
	if(authenticated){
		Map<String, String> credentials = manager.getProfile(user);
		session.setAttribute("livesearch.email", credentials.get("email"));
		session.setAttribute("livesearch.authenticated", "true");
		session.setAttribute("livesearch.user", user);
%>
<div style="text-align: center;"><h1>Login successfully!</h1></div>
<%
	}
	else{
		if(checking)
			out.println("<h2>Incorrect username or password.</h2>");
%>
<form method="post" action="login.jsp">
<table class="mainform" border="0" cellspacing="0" cellpadding="2" align="center" width="100%">
<tr><td>Username</td>
	<td><input type="text" name="user" value="<%=StringEscapeUtils.escapeHtml4(user) %>"/></td></tr>
<tr><td>Password</td>
	<td><input type="password" name="password"/></td></tr>
<tr><td></td>
	<td><input type="submit" name="login" value="login"/></td></tr>
</table>
<input type="hidden" name="url" value="<%= url %>"/>
</form>

<%	} %>
</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
