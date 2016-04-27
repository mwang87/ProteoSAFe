<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.storage.FileManager"
%><%
	// prevent caching
	response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	response.addHeader("Cache-Control", "post-check=0, pre-check=0");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader ("Expires", 0);
	// determine whether or not an authenticated user is currently logged in
	boolean authenticated = false;
	String user = (String)session.getAttribute("livesearch.user");
	if (user == null)
		user = ServletUtils.registerGuestUser(session);
	if (FileManager.syncFTPSpace(user))
		authenticated = true;
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<% ServletUtils.printSessionInfo(out, session); %>
<head>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link href="images/favicon.ico" rel="shortcut icon" type="image/icon"/>
	
	<% if (authenticated) { %>
	<!-- Dojo tree widget styles -->
	<link rel="stylesheet" type="text/css" href="scripts/dojo/dijit/themes/tundra/tundra.css"/>
	<% } %>

	<link rel="stylesheet" href="dragdrop/assets/css/styles.css" />


	<!-- General ProteoSAFe scripts -->
	<script src="scripts/resource.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/form.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/input.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	
	<!-- Third-party utility scripts -->
	<script src="scripts/yepnope.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
	
	<!-- Third-party widget scripts -->
	<script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
	
	<% if (authenticated) { %>
	<!-- SWFUpload scripts -->
	<script src="scripts/swfupload.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/swfupload.cookies.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/swfupload.queue.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/fileprogress.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/upload_handlers.js" language="javascript" type="text/javascript"></script>
	
	<!-- Dojo tree widget scripts -->
	<script src="scripts/dojo/dojo/dojo.js" language="javascript" type="text/javascript" djConfig="parseOnLoad: true"></script>
	<script src="scripts/fileManager.js" language="javascript" type="text/javascript"></script>

	<script src="dragdrop/assets/js/jquery.filedrop.js"></script>
	<% } %>
	
	<!-- Special script code pertaining exclusively to this page -->
	<script language="javascript" type="text/javascript">
		var tooltip = createTooltip();
		
		function init(){
			setMode("select", "treeSelector");
		}
		
		function setMode(mode, module) {
			if (mode == null || module == null)
				return;
			if (mode == "upload" && module == "uploader"){
				document.getElementById("dropbox").style.visibility = "visible";
			}
			else{
				
				document.getElementById("dropbox").style.visibility = "hidden";
			}
			// deactivate all tabs except the selected one
			var tabs = document.getElementsByTagName("a");
			for (var i=0; i<tabs.length; i++) {
				var tab = tabs[i];
				if (tab.id == mode + "_tab")
					tab.className = "current";
				else if (tab.className == "current")
					tab.className = "";
			}
			// disable all tab divs except the current one
			var content = document.getElementById("tabContent");
			for (var i=0; i<content.childNodes.length; i++) {
				var child = content.childNodes[i];
				if (child.id == mode)
					child.style.display = "block";
				else if (child.nodeName.toUpperCase() == "DIV")
					child.style.display = "none";
			}
			// load this mode's div if necessary
			var div = document.getElementById(mode);
			if (div == null)
				CCMSFormUtils.loadModule(module, "input", content, mode);
			// otherwise, try to refresh the module
			var instance = CCMSFormUtils.getModuleInstance(mode);
			if (instance != null && instance.refresh != null)
				instance.refresh();
		}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<meta http-equiv="PRAGMA" content="NO-CACHE"/>
	<title>CCMS ProteoSAFe File/Resource Manager</title>
</head>
<body class="tundra" onload="init();">
	<div id="tabs" class="navMain">
		<ul>
			<li>
				<a id="select_tab" onclick="setMode('select', 'treeSelector');">
					<span>Select Input Files</span>
				</a>
			</li>
			<li>
				<a id="upload_tab" onclick="setMode('upload', 'uploader');">
					<span>Upload Files</span>
				</a>
			</li>
			<li>
				<a id="sharing_tab" onclick="setMode('sharing', 'fileSharer');">
					<span>Share Files</span>
				</a>
			</li>
		</ul>
	</div>
	
	<div id="tabContent" style="position:relative;"></div>
	<div id="dropbox" style="position:absolute;left:430px;top:0px;visibility:hidden">
		<span class="messagedropbox"><font color=#F0F8FF>Drop Files Here To Upload</font></span>
	</div>
	        <script src="dragdrop/assets/js/script.js"></script>

	<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
