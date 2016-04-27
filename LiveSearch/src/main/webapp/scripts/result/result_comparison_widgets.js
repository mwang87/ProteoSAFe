
//Widget for Dataset, selecting task
function renderTaskComparisonSelector(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)
        
        if(parameters == null){
            return;
        }
        
        var do_stuff_button = document.createElement("button");
        do_stuff_button.innerHTML = parameters.buttondisplay;
                                
        td.appendChild(do_stuff_button)
        
        do_stuff_button.onclick = function(textboxleftname, textboxrightname, task, site, all_parameters){
            return function(){
                //$("#" + textboxdivname).val(task)
                console.log(task)
                console.log(site)
                
                remote_url = ""
                if(site == "MassIVE"){
                    remote_url = "http://massive.ucsd.edu"
                }
                if(site == "ProteoSAFe-BETA"){
                    remote_url = "http://proteomics2.ucsd.edu"
                }
                
                fetch_url = remote_url + "/ProteoSAFe/result_jsonp.jsp"
                
        		// set up modal overlay div for task selector box
                $("#tab_results_per_task").empty();
        		var overlay = document.createElement("div");
        		overlay.id = "tab_results_per_task_overlay";
        		overlay.className = "overlay";
        		var spinner = document.createElement("div");
        		spinner.id = "tab_results_per_task_spinner";
        		overlay.appendChild(spinner);
                $("#tab_results_per_task").append(overlay);
            	// start progress spinner
            	enableOverlay(
            		document.getElementById("tab_results_per_task_overlay"),
            		true, true, "34px");
                
                $.ajax({
                    url: fetch_url,
                    jsonp: "callback",
                    dataType: "jsonp",
                    data: {
                        task: task,
                        view: "view_result_list"
                    },
                    success: function(textboxleftname, textboxrightname, site, task, all_parameters) {
                        location.hash = "";
                        location.hash = "#compare_selected";
                    	return function(response) {
                    		// display task/dataset header information
                            var is_dataset = false;
                            var workflow = all_parameters.workflow;
                            if (workflow.length >= 7 &&
                            	workflow.substring(0, 7).toUpperCase()
                            		== "MASSIVE")
                            	is_dataset = true;
                            var header = document.createElement("div");
                            header.style.borderTop =
                            	header.style.borderLeft =
                            	header.style.borderRight = "1px solid #5570B2";
                            header.style.padding = "10px 0 10px 0";
                            var title = "<span style=\"font-weight:bold;\">";
                            if (is_dataset)
                            	title += "MassIVE dataset " +
                            		all_parameters.dataset;
                            else title += workflow +
                            	" workflow task (ID = " + task + ")";
                            title += "</span><br/>Title = \"" +
                        		"<span style=\"font-style:italic;\">" +
                        		all_parameters.title + "</span>\"";
                            header.innerHTML = title;
                            $("#tab_results_per_task").append(header);
                            
                            // display comparison group controls
                            // for each file in this task/dataset
                            var comparison_controls =
                            	document.createElement("div");
                            comparison_controls.style.borderBottom =
                            	comparison_controls.style.borderLeft =
                            	comparison_controls.style.borderRight =
                            		"1px solid #5570B2";
                            comparison_controls.style.padding = "10px 0 10px 0";
                            var table = document.createElement("table");
                            table.style.margin = "0 auto";
                            for (var tab_file in response.blockData) {
                            	var row = table.insertRow(table.rows.length);
                                // get actual mzTab filename
                                var tab_display_name =
                                	response.blockData[tab_file]["filename"];
                                if (tab_display_name == null)
                                    tab_display_name = response
                                    	.blockData[tab_file]["Uploaded_file"];
                                // remove the path prefix
                                tab_display_name =
                                	tab_display_name.replace(/^.*[\\\/]/, '');
                                // display the filename
                                var cell = row.insertCell(row.cells.length);
                                cell.style.textAlign = "right";
                                cell.innerHTML = tab_display_name;
                                // get the correct mzTab file
                                if (tab_file.length < 2)
                                    // most likely from a dataset
                                    tab_file = response
                                    	.blockData[tab_file]["MzTab_file"];
                                // create the "Add to Group A" button
                                var compare_button =
                                	document.createElement("button");
                                compare_button.innerHTML = "Add to Group A";
                                compare_button.onclick = function(
                                	text_field_id, task, tab_file,
                                	site, workflow
                                ) {
                                    return function() {
                                        var additional_text = site + ":" +
                                        	task + ":" + tab_file + ":" +
                                        	workflow + ";\n";
                                        $("#" + text_field_id).val(
                                        	$("#" + text_field_id).val() +
                                        	additional_text);
                                        $(this).prop("disabled", "disabled");
                                    }
                                }(textboxleftname, task, tab_file,
                                	site, workflow)
                                cell = row.insertCell(row.cells.length);
                                cell.appendChild(compare_button);
                                // create the "Add to Group B" button
                                compare_button =
                                	document.createElement("button");
                                compare_button.innerHTML = "Add to Group B";
                                compare_button.onclick = function(
                                	text_field_id, task, tab_file,
                                	site, workflow
                                ) {
                                    return function() {
                                        var additional_text = site + ":" +
                                        	task + ":" + tab_file + ":" +
                                        	workflow + ";\n";
                                        $("#" + text_field_id).val(
                                        	$("#" + text_field_id).val() +
                                        	additional_text);
                                        $(this).prop("disabled", "disabled");
                                    }
                                }(textboxrightname, task, tab_file,
                                	site, workflow)
                                cell = row.insertCell(row.cells.length);
                                cell.appendChild(compare_button);
                            }
                            comparison_controls.appendChild(table);
                            $("#tab_results_per_task").append(
                            	comparison_controls);
                			// stop progress spinner
                            setTimeout(function() {
                            	enableOverlay(document.getElementById(
        						"tab_results_per_task_overlay"), false); },
        						200);
                        }
                    }(textboxleftname, textboxrightname, site, task,
                    	all_parameters),
                    error: function(response, text, error) {
            			// stop progress spinner
                        setTimeout(function() {
                        	enableOverlay(document.getElementById(
    						"tab_results_per_task_overlay"), false); },
    						200);
                		console.log("Error fetching task data to " +
                			"select for comparison: [" + text + "].");
                	}
                });
            }
        }(parameters.textboxleft, parameters.textboxright, parameters.task, parameters.site, parameters);
    }
}



var renderTaskComparisonDisplayer = {
    render: renderTaskComparisonSelector,
    sort: plainSorter
};

columnHandlers["taskcomparisonselector"] = renderTaskComparisonDisplayer;
