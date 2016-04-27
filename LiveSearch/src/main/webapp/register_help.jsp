<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.saint.commons.WebAppProps"
	import="edu.ucsd.saint.commons.xml.XmlUtils"
	import="edu.ucsd.saint.commons.http.HttpGetAgent"
	import="edu.ucsd.saint.commons.http.SteadyRetry"
	import="java.util.Collection"
	import="java.util.LinkedList"
	import="org.apache.http.HttpEntity"
	import="org.slf4j.Logger"
	import="org.slf4j.LoggerFactory"
	import="org.w3c.dom.Document"
	import="org.w3c.dom.Element"
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<%!
	Logger logger = LoggerFactory.getLogger(this.getClass());
%>
<!DOCTYPE html>
<html>
<head>
	<link href="styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon" />
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
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
		<% if(ServletUtils.isAuthenticated(session)){ %>
		<h2>Register your grid account for running tasks</h2>
		If you have an account on a supported grid, you can register it to allow the system to run
		jobs on it.  The following grids are supported by the system:<br/><br/>
		<table class="tabular" style="margin-left:auto; margin-right:auto">
		<tr><th>Grid Name</th><th>Address</th><th>Registration Script</th></tr>
		<%
			String site = WebAppProps.get("livegrid.service.url");
			String query = WebAppProps.get("livegrid.service.queryTargetGrids");
			String register = WebAppProps.get("livegrid.service.registerGridAccount");
			String url = site + "/" + query;
			logger.info("Query URL: [{}]", url);
			HttpGetAgent agent = new HttpGetAgent(new SteadyRetry(5000, 3));
			try{
			    HttpEntity entity = agent.execute(url);
				Document doc = XmlUtils.parseXML(entity.getContent());
				for(Element grid: XmlUtils.getElements(doc, "target-grid")){
					Element eName = XmlUtils.getElement(grid, "name");
					Element eAddr = XmlUtils.getElement(grid, "address");
					Element eScript = XmlUtils.getElement(grid, "script-path");
					String name = (eName != null) ? eName.getTextContent() : "N/A";
					String addr = (eAddr != null) ? eAddr.getTextContent() : "N/A";
					String script = (eScript != null) ? eScript.getTextContent() : "N/A";
		%>
			<tr><td><%= name %></td>
				<td><%= addr %></td>
				<td><%= script %></td></tr>
		<%
				}
			}
			catch(Exception e){
				logger.info("Failed to query target grids information", e);
			}
			finally{
				agent.close();
			}
		%>
		</table><br/>
		To register your grid account, log on a supported grid which is listed above, and execute the registration 
		script shown in the "Registration Script" column. 
		<% } %>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
