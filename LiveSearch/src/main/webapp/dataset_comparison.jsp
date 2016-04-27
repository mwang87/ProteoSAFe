<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.dataset.Dataset"
%>

<%
    String dataset_1 = request.getParameter("dataset1");
    String dataset_2 = request.getParameter("dataset2");
    
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
    <script src="scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>

    
    <!-- Third-party utility scripts -->
    <script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/arbor.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />
    
    
    <script language="javascript" type="text/javascript">
        var dataset_1 = "<%= dataset_1 %>";
        var dataset_2 = "<%= dataset_2 %>";
        
        
        
        function init() {
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>
            
            table_header = document.createElement("h2");
            table_header.innerHTML = "Dataset Shared Compounds";
            $("#maindisplay").append(table_header);
            
            table_div = document.createElement("div");
            table_div.id = "table_div";
            $("#maindisplay").append(table_div);
            
            process_dataset_similarity(dataset_1, dataset_2, "table_div")
        }
        
        function process_dataset_similarity(datasetid_1, datasetid_2, div_name){
            console.log("doing shit");
            context = new Object();
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/ContinuousIDServlet",
                data: { massiveid: datasetid_1},
                cache: false,
                async: false,
                success: function(context){
                    return function(json){
                        continuous_info = JSON.parse(json)
                        for(i = 0; i < continuous_info.jobs.length; i++){
                            if(continuous_info.jobs[i].reported == 1){
                                context.dataset1_job = continuous_info.jobs[i].task;
                                break;
                            }
                        }
                    }
                }(context)
            });
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/ContinuousIDServlet",
                data: { massiveid: datasetid_2},
                cache: false,
                async: false,
                success: function(context){
                    return function(json){
                        continuous_info = JSON.parse(json)
                        for(i = 0; i < continuous_info.jobs.length; i++){
                            if(continuous_info.jobs[i].reported == 1){
                                context.dataset2_job = continuous_info.jobs[i].task;
                                break;
                            }
                        }
                    }
                }(context)
            });
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/result.jsp",
                data: { task: context.dataset1_job, view: "group_by_spectrum_all", show: "true"},
                cache: false,
                async: false,
                success: function(context){
                    return function(html){
                        results_data = get_block_data_from_page(html);
                        context.dataset1_data = results_data;
                    }
                }(context)
            });
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/result.jsp",
                data: { task: context.dataset2_job, view: "group_by_spectrum_all", show: "true"},
                cache: false,
                async: false,
                success: function(context){
                    return function(html){
                        results_data = get_block_data_from_page(html);
                        context.dataset2_data = results_data;
                    }
                }(context)
            });
            
            //Now we have all the data. 
            dataset1_compounds = $.unique(context.dataset1_data.map(function(element){return element.Compound_Name}))
            dataset2_compounds = $.unique(context.dataset2_data.map(function(element){return element.Compound_Name}))
            
            //Intersect the two lists to see size
            intersected_values = $(dataset1_compounds).filter(dataset2_compounds);
            
            display_data = new Array();
            for(i = 0; i < intersected_values.length; i++){
                obj = new Object();
                obj.compoundname = intersected_values[i]
                display_data.push(obj)
            }
            
            child_table = document.createElement("div");
            $("#" + div_name).append(child_table);
            
            var task = new Object();
            task.id = "1234";
            task.workflow = "Dataset Intersection";
            task.description = "Dataset Intersection";
            var generic_table = new ResultViewTable(get_dataset_intersection_table(), "main", task);
            generic_table.setData(display_data);
            generic_table.render(child_table, 0);
            
        }
        
        function get_dataset_intersection_table(){
            var tableXML_str = '<block id="dataset_intersection" type="table"> \
                                    <row>  \
                                        <column field="compoundname" label="Compound Name" type="text" width="5"/> \
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