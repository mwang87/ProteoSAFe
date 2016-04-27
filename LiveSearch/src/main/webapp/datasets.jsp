<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.dataset.Dataset"
	import="edu.ucsd.livesearch.dataset.DatasetManager"
	import="edu.ucsd.livesearch.parameter.ResourceManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
	import="edu.ucsd.livesearch.task.Task"
	import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
	import="edu.ucsd.livesearch.task.TaskManager"
	import="edu.ucsd.livesearch.util.Commons"
	import="edu.ucsd.livesearch.util.FormatUtils"
	import="edu.ucsd.livesearch.subscription.SubscriptionManager"
	import="java.util.*"	
	import="org.json.simple.JSONObject"
%>
<%!
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String resolveCVLabels(
		String list, String resource, Map<String, String> cache
	) {
		if (list == null)
			return "";
		StringBuffer resolved = new StringBuffer("");
		Collection<String> labels =
			ResourceManager.getCVResourceLabels(list, resource, cache);
		boolean first = true;
		for (String label : labels) {
			// prepend <hr> tag if this isn't the first item
			if (first == false)
				resolved.append("<hr class='separator'/>");
			resolved.append(label);
			first = false;
		}
		return resolved.toString();
	}
%>
<%
	Map<Task, Dataset> datasets;
	// determine the identity of the requesting user and the request type
	String identity = (String)session.getAttribute("livesearch.user");
	boolean isAdmin =
		AccountManager.getInstance().checkRole(identity, "administrator");
	String target = request.getParameter("user");
	// fetch datasets accordingly
	if (isAdmin && target != null && target.isEmpty() == false)
		datasets = (target.equals("all")) ?
			DatasetManager.queryAllDatasets() :
			DatasetManager.queryOwnedDatasets(target);
	else datasets = DatasetManager.queryDatasetsByPrivacy(false);
	// obtain dataset metadata CV resource maps
	Map<String, String> species = new LinkedHashMap<String, String>();
	Map<String, String> instrument = new LinkedHashMap<String, String>();
	Map<String, String> modification = new LinkedHashMap<String, String>();
	// update CV fields appropriately
	for (Dataset dataset : datasets.values()) {
		dataset.setSpecies(JSONObject.escape(resolveCVLabels(
			dataset.getSpecies(), "species", species)));
		dataset.setInstrument(JSONObject.escape(resolveCVLabels(
			dataset.getInstrument(), "instrument", instrument)));
		dataset.setModification(JSONObject.escape(resolveCVLabels(
			dataset.getModification(), "modification", modification)));
		if (dataset.getPI() == null)
			dataset.setPI("");
	}
	
	List<Integer> dataset_subscriptions = new ArrayList<Integer>();
    String dataset_subscription_string = "[";
    if(identity != null){
            dataset_subscriptions = SubscriptionManager.get_all_user_subscriptions(identity);
            for(Integer dataset : dataset_subscriptions){
                    dataset_subscription_string += "\"" + dataset + "\",";
            }
    }
    dataset_subscription_string += "]";
    
    Map<Integer, List<String> > all_subscriptions = SubscriptionManager.get_all_subscriptions();
    String all_dataset_sub_json = "{";
    for(Integer dataset : all_subscriptions.keySet()){
        all_dataset_sub_json += "\"" + dataset + "\" : " + "\"" + all_subscriptions.get(dataset).size() + "\",";
    }
    all_dataset_sub_json += "}";

%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<title>UCSD/CCMS - MassIVE Datasets - Mass Spectrometry Repository Dataset List</title>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>
	
	<!-- General ProteoSAFe scripts -->
	<script src="scripts/form.js" language="javascript" type="text/javascript"></script>
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
		var datasets = <%= DatasetManager.getDatasetTaskListJSON(datasets) %>;
        var subscribed_datasets = <%= dataset_subscription_string %>;
        var all_dataset_subs = <%=all_dataset_sub_json%>;
        
		var tooltip = createTooltip();
		
		function init() {
            add_subscription_info(datasets, subscribed_datasets);
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>
			var task = {id:"N/A", description:"Submitted MassIVE Datasets"}
			var table =
				new ResultViewTable(getDatasetsTableXML(), "main", task);
			table.setData(datasets);
			table.render(document.getElementById("datasetTable"), 0);
			// hack to automatically hide certain columns
			tableManager.hideColumn("main", "hash");
			tableManager.getBlock("main").rebuildTable();
		}
		
		// custom XML layout for this table
		function getDatasetsTableXML() {
			var tableXML = "<block id='datasets' type='table'>\n" +
				"\t<row>\n" +
				"\t\t<column type='text' field='title' label='Title' " +
					"width='18' tooltip='Dataset title'/>\n" +
				"\t\t<column type='dataset' field='dataset' " +
					"label='MassIVE ID' width='12' " +
					"tooltip='MassIVE dataset ID'/>\n" +
				"\t\t<column type='px' field='px' " +
					"label='ProteomeXchange ID' width='9' " +
					"tooltip='ProteomeXchange Accession'/>\n" +
				"\t\t<column type='text' field='status' " +
					"label='Submission Type' width='5' tooltip='A dataset is " +
					"considered a complete submission if its results can be " +
					"parsed and visualized by MassIVE.'/>\n" +
				"\t\t<column type='text' field='user' label='Uploaded By' " +
					"width='12' tooltip='ProteoSAFe user who uploaded this " +
					"dataset'/>\n" +
				"\t\t<column type='time' field='created' label='Upload Date' " +
					"width='12' tooltip='Dataset upload date'/>\n" +
				"\t\t<column type='integer' field='fileCount' " +
					"label='# Files' width='3' tooltip='Number of files " +
					"uploaded'/>\n" +
				"\t\t<column type='fileSize' field='fileSizeKB' " +
					"label='Total Size (KB)' width='5' tooltip='Total size " +
					"of all files uploaded (in KB)'/>\n" +
                "\t\t<column type='text' field='subscribed' " +
                    "label='My Sub' width='1'/>\n" +
                "\t\t<column type='text' field='total_subs' " +
                    "label='Subs' width='1'/>\n" +
				"\t\t<column type='text' field='pi' label='PI' width='8' " +
					"tooltip='Principal Investigator'/>\n" +
				"\t\t<column type='expandable' field='species' " +
					"label='Species' width='8' tooltip='Species'/>\n" +
				"\t\t<column type='expandable' field='instrument' " +
					"label='Instrument' width='8' tooltip='Instrument'/>\n" +
				"\t\t<column type='expandable' field='modification' " +
					"label='PTMs' width='8' " +
					"tooltip='Post-Translational Modifications'/>\n" +
				"\t\t<column type='text' field='hash' label='Tranche Hash' " +
					"width='40' tooltip='Tranche hash'/>\n" +
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
        var datasetColumnHandler = {
            render: function(tableId, rowId, columnId, attributes) {
                var format = function(value, record) {
                    // set up dataset status page link
                    var link = document.createElement("a");
                    link.href = "result.jsp?task=" + record.task +
                    	"&view=advanced_view";
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
            sort: function(sg, caption, fieldname, tooltipText) {
                return numberSorter(sg, caption, (fieldname + "Num"),
                	tooltipText);
            },
            filter: plainFilter
        };
        columnHandlers["dataset"] = datasetColumnHandler;
        
        var pxColumnHandler = {
        	render: function(tableId, rowId, columnId, attributes) {
        		var format = function(value, record) {
        			if (value != null) {
                    	// set up PX dataset page link
                        var link = document.createElement("a");
                        link.href =
                        	"http://proteomecentral.proteomexchange.org" +
                        	"/cgi/GetDataset?ID=" + value;
                        link.target = "_blank";
                        link.textContent = value;
                        // give link the usual task status page link style
                        link.style.backgroundColor = "lightgreen";
                        link.style.textDecoration = "underlined";
                        link.style.color = "black";
                        link.style.fontSize = "10pt";
                        return link;
        			}
                };
                return renderCell(format, tableId, rowId, columnId, attributes);
            },
            sort: plainSorter,
            filter: plainFilter
        };
        columnHandlers["px"] = pxColumnHandler;
		
		var timeColumnHandler = {
			render: renderPlain,
			sort: function(sg, caption, fieldname, tooltipText) {
				return numberSorter(sg, caption, (fieldname + "Millis"),
					tooltipText);
			},
			filter: plainFilter
		};
		columnHandlers["time"] = timeColumnHandler;
		
		var commaSeparatedIntegerColumnHandler = {
			render: function(tableId, rowId, columnId, attributes) {
				var format = function(value, record) {
					if (typeof(value) != "number")
						value = parseInt(value);
					if (value == null || isNaN(value))
						return "N/A";
					// add comma separators to numerical value
					value = value.toString();
					var separated = "";
					for (var i=value.length-1; i>=0; i--) {
						if (i != (value.length - 1) &&
							(value.length - 1 - i) % 3 == 0)
							separated = "," + separated;
						separated = value[i] + separated;
					}
					return separated;
				};
				return renderCell(format, tableId, rowId, columnId, attributes);
			},
			sort: numberSorter,
			filter: rangeFilter
		};
		columnHandlers["fileCount"] = commaSeparatedIntegerColumnHandler;
		columnHandlers["fileSize"] = commaSeparatedIntegerColumnHandler;
	</script>
	<script language="javascript" type="text/javascript">
        //Adding column to datasets with the subscription info
        function add_subscription_info(datasets, subscription_lists){
                for(var i = 0; i < datasets.length; i++){
                    dataset_num = datasets[i]["datasetNum"]
                    if(subscribed_datasets.indexOf(dataset_num) >= 0){
                            datasets[i]["subscribed"] = "1";
                    }
                    else{
                            datasets[i]["subscribed"] = "0";
                    }
                    
                    if(dataset_num in all_dataset_subs){
                        datasets[i]["total_subs"] = all_dataset_subs[dataset_num];
                    }
                    else{
                        datasets[i]["total_subs"] = "0";
                    }
                }
                return 0;
        }
    </script>

</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<div id="textWrapper">
		<h4><a href="index.jsp">Back to main page</a>&nbsp;</h4>
	</div>
</div>

<div id="datasetTable" style="margin-left: 10px"></div>

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
