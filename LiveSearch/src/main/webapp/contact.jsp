<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
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
	<title>CCMS ProteoSAFe</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br/>
	<div id="textWrapper" style="text-align: justify;">

<h2>Contact Information</h2>
<p>
  For general questions, comments, and bug reports related to the ProteoSAFe
  software system, please contact either
  <a href="mailto:ccms-web@proteomics.ucsd.edu">ccms-web@proteomics.ucsd.edu</a>
  or <a href="mailto:ccms-web@cs.ucsd.edu">ccms-web@cs.ucsd.edu</a>.
</p>
<p>
  For inquiries related to specific tools and components of the system, please
  <a href="http://proteomics.ucsd.edu/People.html">
    contact the developer or maintainer directly
  <a/>.
</p>

</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
