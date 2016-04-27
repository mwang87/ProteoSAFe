<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link href="../images/favicon.ico" rel="shortcut icon" type="image/icon" />
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
	function init() {
		<% if (ServletUtils.isAuthenticated(session))
				out.print("window.location.replace('../index.jsp')");
			else out.print(
				ServletUtils.JSLogoBlock("logo", request, session)); %>
	}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>Welcome to ProteoSAFe</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="../index.jsp"><div id="logo"></div></a>
	<br/>
	<div id="textWrapper" style="text-align: justify;">
		<h3 style="text-align: center">
			Please log in to access the workflows installed on this server.
		</h3>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
