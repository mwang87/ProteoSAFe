<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.Date"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.VersionTuple"
    %>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Hello ProteoSAFe!</title>
</head>
<body>
Hello ProteoSAFe! <br/>

It is now <%= new Date() %> <br/>

<% VersionTuple ver = Commons.getVersion();%>

Version: <%=ver.getMajor()%>.<%=ver.getMinor() %>.<%=ver.getRevision() %>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
