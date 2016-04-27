<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.dataset.Dataset"
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
    <link href="styles/main.css" rel="stylesheet" type="text/css" />
    <link href="images/favicon.ico" rel="shortcut icon" type="image/icon" />
    <script src="scripts/util.js" language="javascript" type="text/javascript"></script>


    <!-- Result view rendering scripts -->
    <script src="scripts/render.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/stream.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/updateResource.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview_columnhandlers.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/misc_column_render_widgets.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/jsonp_js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/network_utils.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result_comparison_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result_comparison_server/comparison_rendering.js" language="javascript" type="text/javascript"></script>

    <!-- Third-party utility scripts -->
    <script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/arbor.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />
	<script src="scripts/tooltips/spin.min.js" language="javascript" type="text/javascript"></script>

    <script type="text/javascript" src="scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="scripts/result/lorikeet/css/lorikeet.css">


    <script language="javascript" type="text/javascript">
		function init() {
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>

			// debug panel
			var debug_panel = document.createElement("div");
			debug_panel.id = "debug_panel"
			debug_panel.style.position = "absolute";
			debug_panel.style.left = "1476px";
			debug_panel.style.right = "209px";
			debug_panel.style.width = "165px";
			debug_panel.style.height = "400px";
			$("#maindisplay").append(debug_panel);
			// hide the debug panel by default
			$("#debug_panel").hide();

			// link to return to main application page
			var table_header = document.createElement("h4");
			table_header.style.textAlign = "left";
			var link = document.createElement("a");
			link.href = "/";
			link.innerHTML = "Back to main page";
			table_header.appendChild(link);

			// link to return to results selection
			link = document.createElement("a");
			link.id = "return_to_results_selection_link";
			link.href = "results_comparison.jsp";
			link.innerHTML = "Back to results selection";
			link.style.paddingLeft = "20px";
			link.style.visibility = "hidden";
			link.onclick = function() {
				show_hide_panel("container_div", "comparison_content_panel");
				return false;
			}
			table_header.appendChild(link);
			$("#maindisplay").append(table_header);
			$("#maindisplay").append(document.createElement("hr"));

			// header text
			table_header = document.createElement("h2");
			table_header.innerHTML = "Results Comparison";
			$("#maindisplay").append(table_header);

			// "Results Selection" container
			var container_div = document.createElement("div");
			container_div.id = "container_div";
			container_div.className = "comparison_content_panel";
			container_div.style.clear = "both";
			table_header = document.createElement("h3");
			table_header.innerHTML = "Select Results From Search Jobs";
			container_div.appendChild(table_header);
			import_jobs_and_display(container_div);
			container_div.appendChild(document.createElement("br"));
			table_header = document.createElement("h3");
			table_header.innerHTML = "Select Results From MassIVE Datasets";
			container_div.appendChild(table_header);
			import_datasets_and_display(container_div);
			container_div.appendChild(document.createElement("br"));
			table_header = document.createElement("h3");
			table_header.innerHTML =
				"Add Selected Results to Comparison Groups";
			container_div.appendChild(table_header);
			var anchor = document.createElement("a");
			anchor.name = "compare_selected";
			container_div.appendChild(anchor);

			// "Spectrum Level Comparison" container
			var comparison_panel = document.createElement("div");
			comparison_panel.id = "comparison_panel";
			comparison_panel.className = "comparison_content_panel";
			comparison_panel.style.clear = "both";
			comparison_panel.style.position = "relative";


			// add import boxes to the "Results Selection" div
			render_import_boxes("left_input", "right_input",
				container_div, comparison_panel)

			// add all container divs
			$("#maindisplay").append(container_div);
        }

        function get_dataset_table_XML(){
            var tableXML_str = '<block id="task_list" type="table" pagesize="10"> \
                                    <row>  \
                                        <column field="dataset" label="dataset" type="text" width="10"/> \
                                        <column field="title" label="Title" type="text" width="5"/> \
                                        <column field="pi" label="PI" type="text" width="6"/> \
                                        <column field="created" label="created" type="text" width="10"/> \
                                        <column label="Compare" type="taskcomparisonselector" width="6"> \
                                            <parameter name="textboxleft" value="left_input"/>\
                                            <parameter name="textboxright" value="right_input"/>\
                                            <parameter name="task" value="[task]"/>\
                                            <parameter name="site" value="MassIVE"/>\
                                            <parameter name="title" value="[title]"/>\
                                            <parameter name="buttondisplay" value="Compare"/>\
                                            <parameter name="workflow" value="MASSIVE-COMPLETE"/>\
                                            <parameter name="dataset" value="[dataset]"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }

        function get_task_table_XML(){
            var tableXML_str = '<block id="task_list" type="table" pagesize="10"> \
                                    <row>  \
                                        <column field="desc" label="desc" type="text" width="10"/> \
                                        <column field="site" label="Site" type="text" width="5"/> \
                                        <column field="workflow" label="workflow" type="text" width="6"/> \
                                        <column field="created" label="created" type="text" width="10"/> \
                                        <column label="Compare" type="taskcomparisonselector" width="6"> \
                                            <parameter name="textboxleft" value="left_input"/>\
                                            <parameter name="textboxright" value="right_input"/>\
                                            <parameter name="task" value="[task]"/>\
                                            <parameter name="site" value="[site]"/>\
                                            <parameter name="title" value="[desc]"/>\
                                            <parameter name="buttondisplay" value="Compare"/>\
                                            <parameter name="workflow" value="[workflow]"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }


        function decorateTable(table) {
            table.cellSpacing = "1";
            table.cellPadding = "4";
            table.className = "result";
            table.border = "0";
            table.width = "100%";
        }

    </script>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <title>Welcome to ProteoSAFe</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
    <a href="${livesearch.logo.link}"><div id="logo"></div></a>

    <div id="maindisplay"></div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
