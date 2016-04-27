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
    <script src="scripts/result/table_ss.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/stream.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/updateResource.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview_columnhandlers.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/misc_column_render_widgets.js?3" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/js_column_spectrum_viewer.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/jsonp_js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/network_utils.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result_comparison_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result_comparison_server/comparison_rendering.js?2" language="javascript" type="text/javascript"></script>

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
            link.href = "results_comparison_server.jsp";
            link.innerHTML = "Back to results selection";
            link.style.paddingLeft = "20px";
            table_header.appendChild(link);
            $("#maindisplay").append(table_header);
            $("#maindisplay").append(document.createElement("hr"));

            // header text
            table_header = document.createElement("h2");
            table_header.innerHTML = "Results Comparison View";
            $("#maindisplay").append(table_header);

            var container_div = document.createElement("div");

            //Getting Parameters
            url_parameters = getURLParameters()
            task_id = url_parameters["task"]
            compare_type = url_parameters["compare_type"]

            //Displaying buttons to select what to compare
            render_buttons_spectrum_serverside(container_div)
            render_buttons_peptide_serverside(container_div)
            render_buttons_protein_serverside(container_div)



            //Displaying what was compared

            comparison_tabs_header = document.createElement("h2");
            comparison_tabs_header.innerHTML = "Comparison Selection";
            comparison_tabs_header.style.textAlign = "center"
            $("#resultTablediv").append(comparison_tabs_header)

            comparison_div = document.createElement("div");
            $("#resultTablediv").append(comparison_div)
            display_comparison_selection(task_id, comparison_div)

            //Header for comparison
            results_header = document.createElement("h2");
            results_header.innerHTML = "Results Display";
            results_header.style.textAlign = "center"
            $("#resultTablediv").append(results_header)

            //Displaying the actual compare table
            table_div = document.createElement("div");
            $("#resultTablediv").append(table_div)

            render_appropriate_comparison_view(task_id, compare_type, table_div)

            $("#maindisplay").append(container_div);
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
<div id="resultTablediv">
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
