<%@ page language="java" contentType="text/xml; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" 
	import="java.io.*"
	import="edu.ucsd.livesearch.util.Commons"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.FileManager"
%><%
	String path = request.getParameter("path");
	String identity = (String)session.getAttribute("livesearch.user");
	String folders = request.getParameter("folders");
	boolean foldersOnly =
		(folders != null && folders.trim().equalsIgnoreCase("true"));
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<entries>
<%
	if(path == null || "".equals(path)){
/*		for(String group: AccountManager.queryGroups(identity))
			out.println(String.format(
				"<folder display='%s'>%s</folder>", group, "g." + group));*/
		out.println(String.format(
				"<folder display='%s'>f.%s</folder>", identity, identity));
	}
	else{
		File folder = FileManager.getFile(path);
		if(folder != null && folder.exists() && folder.isDirectory())
			for(File item: folder.listFiles()){
				if(item.isHidden()) continue;
				if(item.isDirectory())
					out.println(String.format(
						"<folder>%s</folder>", item.getName()));
				else if(item.isFile() && foldersOnly == false)
					out.println(String.format(
						"<file>%s</file>", item.getName()));
			}
	}
%>
</entries>
