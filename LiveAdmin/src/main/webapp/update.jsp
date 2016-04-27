<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.liveadmin.UpdateSequences"
	import="edu.ucsd.liveadmin.SequenceRepository"
%>

<html>
<body>
<%
	UpdateSequences.forceUpdate();
%>
<h2>Update sequences</h2>
</body>
</html>
