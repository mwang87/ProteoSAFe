<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="dapper.server.flow.*"
	import="dapper.server.ServerProcessor.FlowProxy"
	import="dapper.server.flow.LogicalNode"
	import="dapper.server.flow.LogicalNodeStatus"
	import="edu.ucsd.liveflow.FlowEngineFacade"
	import="edu.ucsd.liveflow.FlowEngineFacade.WorkflowRecord"
	import="java.util.*"
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<title>List of Running ProteoSAFe Workflows</title>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	
	<!--  jQuery stuff -->
	<link href="styles/jquery/jquery-ui.css" rel="stylesheet" type="text/css"/>
	<script src="scripts/jquery/jquery.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/jquery/jquery-ui.min.js" language="javascript" type="text/javascript"></script>
	
	<!-- Special script code pertaining exclusively to this page -->
	<script language="javascript" type="text/javascript">
		function init() {
			$( "#workflows" ).accordion({
				active: false,
				animate: 0,
				collapsible: true,
				header: "tr.accordion"
			});
		}
	</script>
</head>
<body onload="init();">
	<table id="workflows">
		<tr>
			<th style="width:30px;"/>
			<th>Name</th>
			<th>Status</th>
			<th>Pending</th>
		</tr>
		<%
		Collection<WorkflowRecord> records =
			FlowEngineFacade.getFacade().getWorkflows();
		for (WorkflowRecord record: records) {
			FlowProxy fp = record.getProxy();
			fp.refresh();
			Flow flow = fp.getFlow();
			Set<LogicalNode> nodes = flow.getNodes();
			Set<LogicalNode> executing = new HashSet<LogicalNode>();
			for (LogicalNode node : nodes)
				if (node.getStatus().isExecuting())
					executing.add(node);
		%>
		<tr class="accordion">
			<td><%= flow.toString() %></td>
			<td>
				<a href="layout.jsp?task=<%=record.getTask()%>">
					<%= flow.getStatus() %>
				</a>
			</td>
			<td style="text-align:center;"><%= fp.getPendingCount() %></td>
		</tr>
		<tr>
			<td/>
			<td colspan="3">
				Currently Executing Nodes:
		<%
			if (executing.isEmpty()) {
		%>
					None
		<%		
			} else for (LogicalNode node : executing) {
		%>
					<br/>&nbsp;&nbsp;<%= node.toString() %>
		<%
			}
		%>
			</td>
		</tr>
		<%
		}
		%>
	</table>
</body>
</html>
