<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%><%
	String url = request.getParameter("url");
	if(url == null || url.equals("")) url = "../index.jsp";
	session.setAttribute("livesearch.authenticated", "false");
	session.removeAttribute("livesearch.user");
	session.removeAttribute("livesearch.email");
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
	<meta http-equiv="refresh" content="2;URL=<%= url %>" />
</head>
<body>
<div id="bodyWrapper">
<a href="../index.jsp"><div id="logo"></div></a>
<div id="textWrapper">
<br />
<div style="text-align: center;"><h1>Successful logout</h1></div>

</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
