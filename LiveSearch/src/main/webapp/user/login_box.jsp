<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
</head>
<body>
	<div class="loginframe">
		<form method="post" action="login.jsp" target="_top">
			User: <input type="text" name="user" size="8"/>
			Pass: <input type="password" name="password" size="8"/>
			<input type="submit" name="login" value="Sign in"/>
			<br/>
			<input type="hidden" name="url" value="<%= request.getParameter("url") %>"/>
		</form>
	</div>
</body>
</html>
