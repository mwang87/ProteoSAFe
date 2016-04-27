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
		
		Additionally you can browse the demo results for:
		<ul>
			<li><a href="status.jsp?task=5cdf72782b644953903936d8ee043cc4">InsPecT</a></li>
			<li><a href="status.jsp?task=45295338cd40436285fdd97d350cc2a9">MSCluster + InsPecT</a></li>
			<li><a href="status.jsp?task=f4d2a24e45394c19a18324fc04dcc400">PepNovo</a></li>
			<li><a href="status.jsp?task=c286bd3ddd57480f9e27998137c0c150">MS-Alignment</a></li>
			<li><a href="status.jsp?task=e9d40ef4c7e9469c801ee89536eeb00f">MSCluster + MS-Alignment</a></li>
		</ul>		
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
