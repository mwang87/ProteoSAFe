<%@ page language="java" contentType="image/svg+xml; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
	import="java.util.*"
	import="java.io.*"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.saint.commons.IOUtils"
%><%
	Task task = TaskManager.queryTask(request.getParameter("task"));
	File svg = task.getPath(".info/layout.svg");
	if(svg.isFile() && svg.exists()){
		Scanner scanner = null;
		try{
			scanner = new Scanner(svg);			
			while(scanner.hasNextLine())
				out.println(scanner.nextLine());
		}
		finally{
			scanner.close();
		}
	}
	else{
%><?xml version="1.0" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" 
  "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg width="4cm" height="4cm" viewBox="0 0 400 400"
     xmlns="http://www.w3.org/2000/svg" version="1.1">
</svg><%
	}
%>
