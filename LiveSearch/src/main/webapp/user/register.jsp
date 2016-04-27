<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"	
	import="edu.ucsd.livesearch.account.AccountManager"
	import="java.util.HashMap"
	import="java.util.List"
	import="java.util.LinkedList"
	import="java.util.Map"
	import="edu.ucsd.livesearch.storage.FileManager"
	import="edu.ucsd.livesearch.util.Commons"
	import="org.apache.commons.lang3.StringEscapeUtils"
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
<%
	boolean submitting = request.getParameter("submit") != null;
	String user = request.getParameter("user");
	String password = request.getParameter("password");
	String confirm = request.getParameter("confirmation");	
	String email = request.getParameter("email");
	String realname = request.getParameter("realname");
	String organization = request.getParameter("organization");	
	if(!submitting)
		user=email=realname=organization="";
%>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<br/>

<div id="textWrapper">
<%
	boolean success = false;
	List<String> msgs = new LinkedList<String>();

	if (submitting) {
		if ((password == null || confirm == null || !password.equals(confirm)))
			msgs.add("Password and confirmation do not match");
		if (password == null || password.length() < 8)
			msgs.add("Password must contain at least 8 characters");
		if (user.equalsIgnoreCase("guest"))
			msgs.add("<i>guest</i> is not a legal username");
		if ((user == null || !user.matches("(\\w){4,32}+")))
			msgs.add("Username must consist of 4 to 32 Latin letters, digits and underscores");
		if (msgs.isEmpty()) {
			Map<String, String> profile = new HashMap<String, String>();
			profile.put("email", email);
			profile.put("realname", realname);
			profile.put("organization", organization);
			try {
				AccountManager manager = AccountManager.getInstance();
				success = manager.tryRegister(user, password, profile);
				if (success == false)
					msgs.add(String.format(
						"The username <i>%s</i> is already taken.", user));	
			} catch (Throwable error) {
				msgs.add(String.format(
					"There was an error registering your account:<br/>%s",
					error.getMessage()));	
			}		
		}
	}
	if (success) {
		FileManager.syncFTPSpace(user);
%>

<div style="text-align: center;">
	<h1>Your account is created sucessfully!<br/><br/>
		You can now log in at the <a href="../index.jsp">main page</a>.</h1>
</div>
<%
	}
	else{
		for(String msg: msgs)
			out.println("<h4>" + msg + ".</h4><br/><br/>");
%>

<form method="post" action="register.jsp">
<table class="mainform" border="0" cellspacing="0" cellpadding="2" align="center" width="100%">
<tr><td>Username</td>
	<td><input type="text" name="user" value="<%= StringEscapeUtils.escapeJson(user) %>"/></td></tr>
<tr><td>Name</td>
	<td><input type="text" name="realname" value="<%=StringEscapeUtils.escapeHtml4(realname) %>"/></td></tr>
<tr><td>Organization</td>
	<td><input type="text" name="organization" value="<%=StringEscapeUtils.escapeHtml4(organization) %>"/></td></tr>
<tr><td>Email</td>
	<td><input type="text" name="email" value="<%=StringEscapeUtils.escapeHtml4(email) %>"/></td></tr>
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
