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
		
		Please select the following demo tasks to browse their results:
		<ul>
			<li><a href="status.jsp?task=956a4b0f68ed4ceeb100b16a8e1150e6">InsPecT</a></li>
			<li><a href="status.jsp?task=289e5a473fb7473fb04b2bfccd24b803">MSCluster + InsPecT</a></li>
			<li><a href="status.jsp?task=0e6b4f17045a48f8bb0fba2cf417a1c2">MS-Alignment</a></li>
			<li><a href="status.jsp?task=448dff241c154ba2a53c930da48f6d73">MSCluster + MS-Alignment</a></li>
			<li><a href="status.jsp?task=fd59a65c90224514a61b56b84be11508">PepNovo</a></li>
			<li><a href="status.jsp?task=17bff34d3b0f4ed787d99d6f57c77d98">Proteogenomics</a></li>
			<li><a href="status.jsp?task=bfc38056622541139d7b9aed9b97c5a3">Spectral Archives</a></li>
		</ul>		
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
