<%@ page language="java" contentType="image/svg+xml; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
	import="edu.ucsd.liveflow.FlowEngineFacade"
	import="edu.ucsd.saint.commons.WebAppProps"
	import="java.util.*"
	import="java.io.*"
%><%
	String task = request.getParameter("task");
	File folder = new File(WebAppProps.getPath("liveflow.temp.path"), task);
	File svg = new File(folder, "layout.svg");
	if(svg.isFile()){
		Scanner scanner = new Scanner(svg);
		try{
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
