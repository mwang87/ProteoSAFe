<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Register a grid account</title>
</head>
<body>
	<form action="RegisterGridAccount" method="get">
		<table>
			<tr><td>Account owner</td>
				<td><input name="owner" type="text" /><br/></td></tr>
			<tr><td>Target grid</td>
				<td><input name="grid-name" type="text" /><br/></td></tr>
			<tr><td>On-grid account</td>
				<td><input name="grid-account" type="text" /><br/></td></tr>
			<tr><td/>
				<td><input name="Submit" type="submit" /><br/></td></tr>
		</table>
	</form>
</body>
</html>
