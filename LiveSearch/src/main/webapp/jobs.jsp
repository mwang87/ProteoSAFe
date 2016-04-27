<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.livesearch.util.Commons"
	import="edu.ucsd.livesearch.util.FormatUtils"
	import="java.util.*"	
%>
<%
	Collection<Task> tasks;
	// determine the identity of the requesting user and the request type
	String identity = (String)session.getAttribute("livesearch.user");
	boolean isAdmin =
		AccountManager.getInstance().checkRole(identity, "administrator");
	String target = request.getParameter("user");
	// fetch tasks accordingly
	if (identity == null || identity.equalsIgnoreCase("guest"))
		tasks = new LinkedList<Task>();
	else if (isAdmin && target != null && target.isEmpty() == false){
        if(target.equals("all")){
            String filter_type = request.getParameter("filtertype");
            if(filter_type != null && filter_type.equals("time")){
                tasks = TaskManager.queryRecentTasks();
            }
            else{
                int recent_count = 1000;
                String entries = request.getParameter("entries");
                if(entries != null){
                    recent_count = Integer.parseInt(entries);
                }
                tasks = TaskManager.queryRecentNumberTasks(recent_count);
            }
            
            String show_system_jobs = request.getParameter("showsystemjobs");
            if(show_system_jobs != null && show_system_jobs.equals("false")){
                //Filter out continuous and converter
                Collection<Task> filtered_tasks = new LinkedList<Task>();
                for(Task task : tasks){
                    String task_user = task.getUser();
                    if(task_user.equals("continuous") || task_user.equals("converter")){
                    }
                    else{
                        filtered_tasks.add(task);
                    }
                    
                }
                
                tasks = filtered_tasks;
            }
            else{
                if(show_system_jobs != null && show_system_jobs.equals("true")){
                    //Filter in continuous and converter
                    Collection<Task> filtered_tasks = new LinkedList<Task>();
                    for(Task task : tasks){
                        String task_user = task.getUser();
                        if(task_user.equals("continuous") || task_user.equals("converter")){
                            filtered_tasks.add(task);
                        }
                    }
                    
                    tasks = filtered_tasks;
                }
                else{
                    //Show everything
                }
            }
            
            
        }
        else{
            tasks = TaskManager.queryOwnedTasks(target);
        }
    }
	else{
        tasks = TaskManager.queryOwnedTasks(identity);
    }
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<title>UCSD/CCMS - ProteoSAFe Tasks - Mass Spectrometry Workflow Task List</title>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>
	
	<!-- General ProteoSAFe scripts -->
	<script src="scripts/render.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/result/result.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/result/table.js" language="javascript" type="text/javascript"></script>
	
	<!-- Help text tooltip scripts -->
	<script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
	
	<!-- Special script code pertaining exclusively to this page -->
	<script language="javascript" type="text/javascript">
		var tasks = <%= TaskManager.getTaskListJSON(tasks) %>;
		var tooltip = createTooltip();
		
		function init() {
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>
			var task = {id:"N/A", description:"ProteoSAFe Workflow Tasks"}
			var table = new ResultViewTable(getTasksTableXML(), "main", task);
			table.setData(tasks);
			table.render(document.getElementById("tasksTable"), 0);
		}
		
		// custom XML layout for this table
		function getTasksTableXML() {
			var tableXML = "<block id='tasks' type='table'>\n" +
				"\t<row>\n" +
				"\t\t<column type='task' field='desc' label='Description' width='18' " +
					"tooltip='Workflow task description'/>\n" +
				"\t\t<column type='text' field='user' label='User' width='12' " +
					"tooltip='ProteoSAFe user who launched this task'/>\n" +
				"\t\t<column type='text' field='workflow' label='Workflow' width='12' " +
					"tooltip='Workflow executed by this task'/>\n" +
				"\t\t<column type='text' field='site' label='Site' width='12' " +
					"tooltip='ProteoSAFe server from which this task was launched'/>\n" +
				"\t\t<column type='status' field='status' label='Status' width='4' " +
					"tooltip='Click here to view the details of this task'/>\n" +
				"\t\t<column type='time' field='created' label='Create Time' width='12' " +
					"tooltip='Workflow task launch date'/>\n" +
				"\t\t<column type='time' field='elapsed' label='Elapsed Time' width='12' " +
					"tooltip='Total execution time for this task'/>\n" +
				"\t</row>\n" +
				"</block>";
	    	return (parseXML(tableXML));
		}
		
		// custom decorator for this table
		function decorateTable(table) {
			table.cellSpacing = "1";
			table.cellPadding = "4";
			table.className = "result";
			table.border = "0";	
			table.width = "100%";
		}
		
		// custom column handlers for this table
		var taskColumnHandler = {
			render: function(tableId, rowId, columnId, attributes) {
				var format = function(value, record) {
					// set up task description element
					var div = document.createElement("div");
					// set element's text content
					if (value == null)
						value = "N/A";
					div.appendChild(document.createTextNode(value));
					// add task ID
					div.appendChild(document.createElement("br"));
					var span = document.createElement("span");
					span.textContent = "ID=" + record.task;
					span.style.color = "#003399";
					div.appendChild(span);
					return div;
				};
				return renderCell(format, tableId, rowId, columnId, attributes);
			},
			sort: plainSorter,
			filter: plainFilter
		};
		columnHandlers["task"] = taskColumnHandler;
		
		var statusColumnHandler = {
			render: function(tableId, rowId, columnId, attributes) {
				var format = function(value, record) {
					// set up task status page link
					var link = document.createElement("a");
					link.href = "status.jsp?task=" + record.task;
					// set link's text content
					if (value == null)
						link.textContent = "N/A";
					else link.textContent = value;
					// give link the usual task status page link style
					link.style.backgroundColor = "lightgreen";
					link.style.textDecoration = "underlined";
					link.style.color = "black";
					link.style.fontSize = "10pt";
					return link;
				};
				return renderCell(format, tableId, rowId, columnId, attributes);
			},
			sort: plainSorter,
			filter: plainFilter
		};
		columnHandlers["status"] = statusColumnHandler;
		
		var timeColumnHandler = {
			render: renderPlain,
			sort: function(sg, caption, fieldname, tooltipText) {
				return numberSorter(sg, caption, (fieldname + "Millis"), tooltipText);
			},
			filter: plainFilter
		};
		columnHandlers["time"] = timeColumnHandler;
	</script>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<div id="textWrapper">
		<h4><a href="index.jsp">Back to main page</a>&nbsp;</h4>
		<div id="tasksTable"></div>
	</div>
</div>

<!-- Column selector form -->
<div class="helpbox" id="hColumnSelector" style="left:-5000px;">
	<form name="columnSelector" method="get" action="">
		<table id="columnSelector">
			<tr>
				<td/>
				<td>
					<input value="Submit" type="button"
						onclick="evaluateColumns(this.form);"/>
				</td>
			</tr>
		</table>
	</form>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
