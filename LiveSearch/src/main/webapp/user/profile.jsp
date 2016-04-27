<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.*"
	import="edu.ucsd.livesearch.servlet.ManageSharing"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.FileManager"	
	import="java.io.*, java.util.*"
	import="org.apache.commons.lang3.StringEscapeUtils"
%><%
	boolean authenticated = ServletUtils.isAuthenticated(session);
	boolean submitting = request.getParameter("submitting") != null, unchanged = false;
	String url = request.getParameter("url");
	String identity = (String)session.getAttribute("livesearch.user");
	String password, confirm, email, realname, organization;
	if(url == null || url.equals("")) url = "/";

	if(submitting){
		password = request.getParameter("password");
		confirm = request.getParameter("confirmation");	
		email = request.getParameter("email");
		realname = request.getParameter("realname");
		organization = request.getParameter("organization");
		if(confirm == null) confirm = "";
		if(password == null) password = "";
		if(confirm.equals("") && !password.equals(""))
			unchanged = true;
	}
	else{
		AccountManager manager = AccountManager.getInstance();		
		Map<String, String> profile = manager.getProfile(identity);
		email = profile.get("email");
		realname = profile.get("realname");
		organization = profile.get("organization");
		password = confirm = "";
	}
	if(authenticated){
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>UCSD Computational Mass Spectrometry Website</title>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="../images/favicon.ico" type="../image/icon" />
	<script src="../scripts/invoke.js" language="javascript" type="text/javascript"></script>
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
	
	<!-- TODO: This should probably be on another page -->
	<script src="../scripts/admin.js" language="javascript" type="text/javascript"></script>
</head>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<div id="textWrapper">
<br />
<%
	boolean success = false;
	if(submitting){
		boolean passwordValid = true;
		if(!unchanged && !password.equals(confirm)){
			out.println("<h4>Password and confirmation do not match</h4>");
			passwordValid = false;
		}
		if(!unchanged && !password.equals("") && password.length() < 8){
			out.println("<h4>Passowrd must at least of eight characters</h4>");
			passwordValid = false;			
		}
		if(passwordValid){
			Map<String, String> profile = new HashMap<String, String>();
			profile.put("email", email);
			profile.put("realname", realname);
			profile.put("organization", organization);
			AccountManager manager = AccountManager.getInstance();			
			success = manager.updateProfile(identity, password, profile);
			if(!success)
				out.println("<h4>Failed to update profile due to reasons unknown</h4>");			
		}
	}
	if(success){
		session.setAttribute("livesearch.email", email);
%>
<div style="text-align: center;">
	<h2>Your account is updated sucessfully<br/>
<%if(unchanged){%>
		(Password unchanged due to empty confirmation)<br/>
<% } %>
		<a href="<%= url %>">Back to where you are</a>
	</h2>
</div>
<%
	}
	else{
%>
<form method="post" action="profile.jsp">
	<table class="mainform" border="0" cellspacing="0" cellpadding="2" align="center" width="100%">
		<tr>
			<th colspan="2">User Profile</th>
		</tr>
		<tr><td>Username</td>
			<td><%= identity %></td></tr>
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
			<td><input type="submit" name="Submit" value="Submit"/></td></tr>
	</table>
	<input type="hidden" name="url" value="<%= url %>"/>
	<input type="hidden" name="submitting" value="true"/>
</form>

<!-- TODO: This should probably be on another page -->
<!--
<hr/>
<table>
	<tr>
		<td>
			<input value="Cleanup Label Directories" type="button"
				onclick="cleanupLabel();" />
		</td>
		<td>
			<div>
				<div id="cleanLabelProgress"
					style="float: left; height: 48px; margin-top: 32px; display: none">
					<img src="../images/inProgress.gif"/>
				</div>
				<div id="cleanLabelResult"
					style="float: right; height: 80px"></div>
			</div>
		</td>
	</tr>
	<tr>
		<td>
			<input value="Generate Masses Files" type="button"
				onclick="generateMasses();" />
		</td>
		<td>
			<div>
				<div id="generateMassesProgress"
					style="float: left; height: 48px; margin-top: 32px; display: none">
					<img src="../images/inProgress.gif"/>
				</div>
				<div id="generateMassesResult"
					style="float: right; height: 80px"></div>
			</div>
		</td>
	</tr>
</table>
-->
<% } %>
</div>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>

<% } %>
