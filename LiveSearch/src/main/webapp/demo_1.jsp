<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.saint.commons.WebAppProps"
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon" />
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/invoke.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
	function init(){
		<%= ServletUtils.JSLogoBlock("logo", request, session) %>
	}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>Proteomics Search</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br />
	<div id="textWrapper" style="text-align: justify;">
		<h2>Demo</h2>
		
		A video demo for the InsPecT tool is available in
		<a href="http://<%= WebAppProps.get("livesearch.host.address") %>/videos/BasicSearchDemo.avi">AVI (60MB)</a> and 
		<a href="http://<%= WebAppProps.get("livesearch.host.address") %>/videos/BasicSearchDemo.divx">DIVX (5.1MB)</a> formats. <br/>
		
		Additionally you can browse the demo results yourself 
		<a href="status.jsp?task=257fe01b-d96a-4ffb-9255-fd95c238f4e1"> here </a>.
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
