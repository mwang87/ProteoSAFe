<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
        import="edu.ucsd.livesearch.servlet.ServletUtils"
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<title>ProteoSAFe Maintenance</title>
</head>
<body>
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br/>
	<div id="textWrapper" style="text-align: justify;">
		<h1 style="text-align: center">
			ProteoSAFe Down for Scheduled Maintenance
		</h1>
		<h3>
			ProteoSAFe is currently unavailable due to a scheduled maintenance
			downtime. We apologize for any inconvenience this may cause.
			<br/><br/>
			
			ProteoSAFe is expected to be available again on
			<span style="font-style: italic; font-size: 120%; color: darkgreen">
			DATE</span>.
			<br/><br/>
			
			Thank you for using ProteoSAFe.
		</h3>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
