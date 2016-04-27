<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Dumping request parameters</title>
</head>
<body>
<table>
<tr><th>Attribute</th> <th>Value</th></tr>
<tr><td>AuthType</td><td><%= request.getAuthType() %></td></tr>
<tr><td>CharacterEncoding</td><td><%= request.getCharacterEncoding() %></td></tr>
<tr><td>ContentLength</td><td><%= request.getContentLength() %></td></tr>
<tr><td>ContentType</td><td><%= request.getContentType() %></td></tr>
<tr><td>ContextPath</td><td><%= request.getContextPath() %></td></tr>
<tr><td>LocalAddr</td><td><%= request.getLocalAddr() %></td></tr>
<tr><td>LocalName</td><td><%= request.getLocalName() %></td></tr>
<tr><td>LocalPort</td><td><%= request.getLocalPort() %></td></tr>
<tr><td>Method</td><td><%= request.getMethod() %></td></tr>
<tr><td>PathInfo</td><td><%= request.getPathInfo() %></td></tr>
<tr><td>PathTranslated</td><td><%= request.getPathTranslated() %></td></tr>
<tr><td>Protocol</td><td><%= request.getProtocol() %></td></tr>
<tr><td>QueryString</td><td><%= request.getQueryString() %></td></tr>
<tr><td>RemoteAddr</td><td><%= request.getRemoteAddr() %></td></tr>
<tr><td>RemoteHost</td><td><%= request.getRemoteHost() %></td></tr>
<tr><td>RemotePort</td><td><%= request.getRemotePort() %></td></tr>
<tr><td>RemoteUser</td><td><%= request.getRemoteUser() %></td></tr>
<tr><td>RequestSessionId</td><td><%= request.getRequestedSessionId() %></td></tr>
<tr><td>RequestURI</td><td><%= request.getRequestURI() %></td></tr>
<tr><td>RequestURL</td><td><%= request.getRequestURL() %></td></tr>
<tr><td>Scheme</td><td><%= request.getScheme() %></td></tr>
<tr><td>ServerName</td><td><%= request.getServerName() %></td></tr>
<tr><td>ServerPort</td><td><%= request.getServerPort() %></td></tr>
<tr><td>ServletPath</td><td><%= request.getServletPath() %></td></tr>
<tr><td>Session</td><td><%= request.getSession() %></td></tr>
</table>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
