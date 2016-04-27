<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
%><% 
	String task = request.getParameter("task");	
%>
<html>
<head><meta http-equiv="refresh" content="5" /></head>
<body><embed src="layout.jsp?task=<%=task%>"/></body>
</html>
