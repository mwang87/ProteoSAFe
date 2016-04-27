<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
        import="edu.ucsd.livesearch.parameter.ResourceManager"
        import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface"
        import="edu.ucsd.livesearch.servlet.ManageParameters"
        import="edu.ucsd.livesearch.servlet.ServletUtils"
        import="edu.ucsd.livesearch.storage.FileManager"
        import="edu.ucsd.livesearch.storage.SequenceRepository"
        import="edu.ucsd.livesearch.storage.SequenceFile"
        import="edu.ucsd.livesearch.task.Task"
        import="edu.ucsd.livesearch.task.TaskManager"
        import="edu.ucsd.livesearch.util.AminoAcidUtils.PTM"
        import="edu.ucsd.livesearch.util.AminoAcidUtils.PTMType"
        import="edu.ucsd.livesearch.util.Commons"
        import="edu.ucsd.livesearch.account.AccountManager"
%>
<%
    String user = (String)session.getAttribute("livesearch.user");


%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
    <link href="/ProteoSAFe/styles/main.css" rel="stylesheet" type="text/css" />
    <link href="/ProteoSAFe/images/favicon.ico" rel="shortcut icon" type="image/icon" />
    <script src="/ProteoSAFe/scripts/util.js" language="javascript" type="text/javascript"></script>
    
    
    <!-- Result view rendering scripts -->
    <script src="/ProteoSAFe/scripts/render.js?2" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/stream.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/updateResource.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/massivepage.js?5" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/powerview_columnhandlers.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/parameterlink.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/absolutelink.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/molecular_dataset_linkout.js?2" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/fileViewLinkList.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/continuous_id_rating.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/continuous_id_rating_column_display.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/network_displayer.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>

    
    <!-- Third-party utility scripts -->
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/cytoscape.js/cytoscape.min.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/cytoscape.js/arbor.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="/ProteoSAFe/styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />
    
    
    <script language="javascript" type="text/javascript">
        function init() {
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>
            
            
            //Javascript For Summary
            table_header = document.createElement("h2");
            table_header.innerHTML = "Dataset Continuous Identification Ratings Per User";
            $("#maindisplay").append(table_header);
            
            table_div = document.createElement("div");
            $("#maindisplay").append(table_div);
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/ContinuousIDRatingSummaryServlet",
                data: { summary_type: "per_all"},
                cache: false,
                success: get_all_ratings_gen(table_div)
            });
        }
        
        function get_all_ratings_gen(div){
            return function(json){
                obj = JSON.parse(json);

                //Creating another key that uniquely identifies a match
                for( i in obj.ratings){
                    obj.ratings[i].spectrum_match_key = obj.ratings[i].dataset_id + "_" + obj.ratings[i].scan
                }
                
                display_per_match_ratings(obj.ratings, div)
                display_per_person_ratings(obj.ratings, div)
                display_per_spectrum_ratings(obj.ratings, div)
            }
        }

        //Groups ratings by matches
        function display_per_match_ratings(ratings_input, render_div){
            map_output = format_ratings_into_groupby(ratings_input, "spectrum_match_key")
            output_render_allratings = map_output.allratings
            output_render_groupratings = map_output.groupratings

            var task = new Object();
            task.id = "12345";
            task.workflow = "Match Ratings";
            task.description = "Match Ratings";
            var generic_table = new ResultViewTableGen(get_adminpage_per_match_ratings_tableXML(), "match_ratings", task, 0, 0);
            
            generic_table.setData(output_render_groupratings);
            table_div = document.createElement("div");
            render_div.appendChild(table_div)
            generic_table.render(table_div, 0);
        }
        
        //Groups ratings by person
        function display_per_person_ratings(ratings_input, render_div){
            map_output = format_ratings_into_groupby(ratings_input, "user_id")
            output_render_allratings = map_output.allratings
            output_render_groupratings = map_output.groupratings
            
            var task = new Object();
            task.id = "12345";
            task.workflow = "Spectrum Ratings";
            task.description = "Spectrum Ratings";
            var column_handler_second = function(block, parameters){
                return get_constituent_ratings_per_person_XML(output_render_allratings, parameters);
            };
            var generic_table = new ResultViewTableGen(get_adminpage_ratings_tableXML(), "spectrum_ratings", task, 0, column_handler_second);
            
            generic_table.setData(output_render_groupratings);
            table_div = document.createElement("div");
            render_div.appendChild(table_div)
            generic_table.render(table_div, 0);
        }
        
        function display_per_spectrum_ratings(ratings_input, render_div){
            map_output = format_ratings_into_groupby(ratings_input, "spectrum_id")
            output_render_allratings = map_output.allratings
            output_render_groupratings = map_output.groupratings
            
            
            spectrum_grab_status = new Object()
            spectrum_grab_status.grabbed = 0
            spectrum_grab_status.total = output_render_groupratings.length
            //Grabbing spectrum info per spectrum
            for(i in output_render_groupratings){
                $.ajax({
                    type: "GET",
                    url: "/ProteoSAFe/SpectrumCommentServlet",
                    data: { SpectrumID: output_render_groupratings[i].grouping_attribute},
                    cache: true,
                    async: true,
                    success: function(output_render_groupratings, output_render_allratings, spectrum_grab_status, render_div, i){
                        return function(json){
                            spectrum_object = JSON.parse(json)
                            output_render_groupratings[i].libraryname = spectrum_object.spectruminfo.library_membership
                            output_render_groupratings[i].libraryclass = spectrum_object.annotations[0].Library_Class
                            
                            spectrum_grab_status.grabbed++;
                            console.log(spectrum_grab_status.grabbed + " of " + spectrum_grab_status.total);
                            if(spectrum_grab_status.grabbed == spectrum_grab_status.total){
                                var task = new Object();
                                task.id = "12345";
                                task.workflow = "Spectrum Ratings";
                                task.description = "Spectrum Ratings";
                                var column_handler_second = function(block, parameters){
                                    return get_constituent_ratings_per_person_XML(output_render_allratings, parameters);
                                };
                                var generic_table = new ResultViewTableGen(get_adminpage_perlibrary_ratings_tableXML(), "libraryspectrum_ratings", task, 0, column_handler_second);
                                
                                generic_table.setData(output_render_groupratings);
                                table_div = document.createElement("div");
                                render_div.appendChild(table_div)
                                generic_table.render(table_div, 0);
                            }
                        }
                    }(output_render_groupratings, output_render_allratings, spectrum_grab_status, render_div, i )
                });
            }
        }
        
        function format_ratings_into_groupby(ratings_input, grouping_attribute){
            //Grouping by Dataset Scan Number
            mygroups = new Array();
            for(var i in ratings_input){
                dataset_id = ratings_input[i].dataset_id
                scan_number = parseInt(ratings_input[i].scan)
                unique_key =  ratings_input[i][grouping_attribute]
                ratings_input[i]["unique_key"] = unique_key
                ratings_input[i]["id"] = i
                if(!(unique_key in mygroups)){
                    mygroups[unique_key] = []
                    mygroups[unique_key]["allratings"] = []
                    mygroups[unique_key]["sumratings"] = 0
                    mygroups[unique_key]["countratings"] = 0
                }
                mygroups[unique_key]["allratings"].push(ratings_input[i])
                mygroups[unique_key]["sumratings"] += parseInt(ratings_input[i].rating)
                mygroups[unique_key]["countratings"] += 1
            }
            
            //Finding average Ratings
            allgroups_render = []
            for(var i in mygroups){
                rating_item = new Array()
                rating_item["grouping_attribute"] = mygroups[i]["allratings"][0][grouping_attribute]
                rating_item["averagerating"] = (mygroups[i]["sumratings"] / mygroups[i]["countratings"]).toString()
                rating_item["totalratings"] = mygroups[i]["allratings"].length
                rating_item["unique_key"] = i
                rating_item["id"] = i
                allgroups_render.push(rating_item)
            }
            return {
                allratings: ratings_input,
                groupratings: allgroups_render
            }; 
        }
        
        function get_adminpage_perlibrary_ratings_tableXML(){
            var tableXML_str = '<block id="spectrum_ratings" type="table"> \
                                    <row>  \
                                        <column label="Avg Rating" type="ratydisplayrating"> \
                                            <parameter name="rating" value="[averagerating]"/>\
                                            <parameter name="maxrating" value="4"/>\
                                        </column>\
                                        <column field="grouping_attribute" label="SpectrumID" type="text" width="5"/> \
                                        <column field="averagerating" label="Avg Rating" type="float" precision="1"/> \
                                        <column field="totalratings" label="Total Ratings" type="integer" width="5"/> \
                                        <column field="libraryname" label="Library Name" type="text" width="5"/> \
                                        <column field="libraryclass" label="Library Class" type="integer" width="5"/> \
                                    </row>\
                                    <row expander="down:up">\
                                        <column type="callbackblock" block="constituent_ratings" colspan="7">\
                                            <parameter name="unique_key" value="[unique_key]"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }
        
        function get_adminpage_ratings_tableXML(){
            var tableXML_str = '<block id="spectrum_ratings" type="table"> \
                                    <row>  \
                                        <column label="Avg Rating" type="ratydisplayrating"> \
                                            <parameter name="rating" value="[averagerating]"/>\
                                            <parameter name="maxrating" value="4"/>\
                                        </column>\
                                        <column field="grouping_attribute" label="User ID" type="text" width="5"/> \
                                        <column field="totalratings" label="Total Ratings" type="integer" width="5"/> \
                                    </row>\
                                    <row expander="down:up">\
                                        <column type="callbackblock" block="constituent_ratings" colspan="7">\
                                            <parameter name="unique_key" value="[unique_key]"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }

        function get_adminpage_per_match_ratings_tableXML(){
            var tableXML_str = '<block id="spectrum_ratings" type="table"> \
                                    <row>  \
                                        <column label="Avg Rating" type="ratydisplayrating"> \
                                            <parameter name="rating" value="[averagerating]"/>\
                                            <parameter name="maxrating" value="4"/>\
                                        </column>\
                                        <column field="unique_key" label="unique_key" type="text" width="5"/> \
                                        <column field="totalratings" label="totalratings" type="integer" width="5"/> \
                                        <column field="averagerating" label="Avg Rating" type="float" precision="1"/> \
                                    </row>\
                                    <row expander="down:up">\
                                        <column type="callbackblock" block="constituent_ratings" colspan="7">\
                                            <parameter name="unique_key" value="[unique_key]"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }
        
        function get_constituent_ratings_per_person_XML(input_data, parameters){
            input_data_filtered = [];
            
            
            for(i = 0; i < input_data.length; i++){
                var keep = true;
                for(parameter in parameters){
                    if(parameters[parameter] != input_data[i][parameter]){
                        keep = false;
                        break;
                    }
                }
                if ( keep == true ){
                    input_data_filtered.push(input_data[i]);
                }
            }
            
            var tableXML_str = '<blockInstance> \
                                    <block id="constituent_ratings" type="table"> \
                                        <row> \
                                            <column field="user_id" label="User" type="text" width="5"/> \
                                            <column label="Rating" type="ratydisplayrating"> \
                                                <parameter name="rating" value="[rating]"/>\
                                                <parameter name="maxrating" value="4"/>\
                                            </column>\
                                            <column field="rating_comment" label="Comment" type="text" width="5"/> \
                                            <column field="dataset_id" label="Dataset" type="text" width="5"/> \
                                            <column field="scan" label="Scan" type="text" width="5"/> \
                                            <column field="spectrum_id" label="SpectrumID" type="text" width="5"/> \
                                            <column label="Link" type="genericurlgenerator"> \
                                                <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                                <parameter name="REQUESTPARAMETER=task" value="[task_id]"/>\
                                                <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum_all_beta"/>\
                                                <parameter name="HASHPARAMTER=#Scan#_lowerinput" value="[scan]"/>\
                                                <parameter name="HASHPARAMTER=#Scan#_upperinput" value="[scan]"/>\
                                                <parameter name="LABEL" value="Link"/>\
                                            </column>\
                                        </row> \
                                    </block>\n \
                                <blockData>\n';
            tableXML_str += JSON.stringify(input_data_filtered);
            tableXML_str += '\n</blockData></blockInstance>\n';
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
