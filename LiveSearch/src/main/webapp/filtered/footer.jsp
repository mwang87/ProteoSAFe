<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="java.io.File"
%><%
	String path =
		pageContext.getServletContext().getRealPath(request.getServletPath());
	File jspFile = new File(path);
	long lastModified = jspFile.lastModified();
	String version = "${livesearch.version}";
	if (version.trim().equals("") == false) {
		String build = "${livesearch.build}";
		if (build.trim().equals("") == false)
			version += "-" + build;
	}
%><div style="font-size: 7pt; color: gray;">
	Copyright &copy; <%= String.format("%tY", lastModified) %>.
	Last modified: <%= String.format("%tF", lastModified) %>.
	<% if (version.trim().equals("") == false) { %>
		Version <%= version %>.
	<% } %>
</div>
