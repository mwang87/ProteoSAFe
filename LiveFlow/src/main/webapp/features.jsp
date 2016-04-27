<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
	import="java.util.Map"
	import="java.util.HashMap"    
    import="org.w3c.dom.*"
    import="org.w3c.dom.bootstrap.DOMImplementationRegistry"
%><%!
	static{
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
	}

/** DOM Level 2 Modules */
private static Map<String, String> module2Map;

/** DOM Level 3 Modules */
private static Map<String, String> module3Map;

private static void loadModules(  ) {
	module2Map = new HashMap<String, String>();
	module3Map = new HashMap<String, String>();
	
	// DOM Level 2
	module2Map.put("XML", "DOM Level 2 Core");
	module2Map.put("Views", "DOM Level 2 Views");
	module2Map.put("Events", "DOM Level 2 Events");
	module2Map.put("CSS", "DOM Level 2 CSS");
	module2Map.put("Traversal", "DOM Level 2 Traversal");
	module2Map.put("Range", "DOM Level 2 Range");
	module2Map.put("HTML", "DOM Level 2 HTML");
	
	// DOM Level 3
	module3Map.put("XML", "DOM Level 3 Core");
	module3Map.put("LS", "DOM Level 3 Load & Save");
	module3Map.put("Validation", "DOM Level 3 Validation");
}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title>Fetch Web application folder via class loader</title>
</head>
<body>
	
<%
	DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
	loadModules();

	  registry = DOMImplementationRegistry.newInstance();
	  DOMImplementation impl = registry.getDOMImplementation("XML 3.0");

	  	// Check for DOM Level 2 Features
%>
<h2>2.0 features</h2>
<ul>
<%
		for (String name: module2Map.keySet()) {
			String description = module2Map.get(name);
%>
	<li>
	The <%= description %> module is <%= impl.hasFeature(name, "2.0")? "supported" : "not supported" %>.
	</li>
<%
		}
%>
</ul>
<h2>3.0 features</h2>
<ul>
<%
	// Check for DOM Level 3 Features
		for (String name: module3Map.keySet()) {
			String description = (String)module3Map.get(name);
%>
	<li>
	The <%= description %> module is <%= impl.hasFeature(name, "3.0")? "supported" : "not supported" %>.
	</li>
<%
		}
%>
</ul>
</body>
</html>
