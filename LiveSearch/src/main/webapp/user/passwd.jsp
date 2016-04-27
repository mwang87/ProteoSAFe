<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.*"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.FileManager"	
	import="java.io.*, java.util.*"
	import="org.apache.commons.lang3.StringEscapeUtils"
%><%
	boolean isAdmin = ServletUtils.isAdministrator(session);
	boolean submitting = request.getParameter("submit") != null, unchanged = false;
	String identity = request.getParameter("identity");
	String password = request.getParameter("password");
	String confirm = request.getParameter("confirmation");
	if(identity == null) identity = "";
	if(confirm == null) confirm = "";
	if(password == null) password = "";

	if(isAdmin){
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
</head>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<div id="textWrapper">
<br />
<%
	boolean success = false;
	if(submitting){
		boolean canProceed= true;
		if(identity.isEmpty()){
			out.println("<h4>Empty target username.</h4>");
			canProceed = false;			
		}
		if(!password.equals(confirm)){
			out.println("<h4>Passowrd and confirmation do not match.</h4>");
			canProceed = false;
		}
		if(!password.equals("") && password.length() < 8){
			out.println("<h4>Passowrd must at least of eight characters.</h4>");
			canProceed = false;			
		}
		if(canProceed){
			AccountManager manager = AccountManager.getInstance();
			success = manager.updatePassword(identity, password);
			if(!success)
				out.println("<h4>Failed to update password due to reasons unknown.</h4>");			
		}
	}
	if(success){
%>
<div style="text-align: center;">
	<h2>The password is updated sucessfully for [<%= identity %>]<br/>
		<a href="/%>">Back to the main page</a>
	</h2>
</div>
<%
	}
	else{
%>
<form method="post" action="profile.jsp">
	<table class="mainform" border="0" cellspacing="0" cellpadding="2" align="center" width="100%">
		<tr><td>Username</td>
			<td><input type="text" name="identity" value="<%=StringEscapeUtils.escapeHtml4(identity) %>"/></td></tr>
		<tr><td>Password</td>
			<td><input type="password" name="password"/></td></tr>
		<tr><td></td>
			<td><input type="password" name="confirmation"/> (confirmation)</td></tr>
		<tr><td></td>
			<td><input type="submit" name="submit" value="submit"/></td></tr>
	</table>
</form>

<%	} %>
</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>

<% } %>
