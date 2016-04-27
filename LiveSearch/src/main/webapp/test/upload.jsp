<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Test uploading a .tgz file to the server</title>
</head>
<body>

<form action="../Upload" method="post" enctype="multipart/form-data">
	Task: <input name="task" type="text"/> <br/>
	File: <input name="content" type="file"/> <br/>
	Resource: <input name="resource" /> <br/>
	<button type="submit">Submit</button> <br/>
</form>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>