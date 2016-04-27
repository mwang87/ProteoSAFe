var comparison_hoist_state = new Object();

function show_hide_panel(show_panel_id, hide_panel_class) {
	// hide the return link if this is the main panel
	if (show_panel_id == "container_div")
		$("#return_to_results_selection_link").css("visibility", "hidden");
	// otherwise, show the return link
	else $("#return_to_results_selection_link").css("visibility", "visible");
	// hide all panels
    $("div." + hide_panel_class).hide();
    // show the current panel
    $("div#" + show_panel_id).show();
}

function render_import_boxes(
	left_div_name, right_div_name, parent_div, comparison_div,
	comparison_peptide_div, comparison_protein_div
) {
	// mzTab file selection display and controls
	var tab_results = document.createElement("div");
	tab_results.id = "tab_results_per_task";
	tab_results.style.position = "relative";
	parent_div.appendChild(tab_results);
	
	// "Group A" set of tasks/datasets to be compared
	var div = document.createElement("div");
	div.style.float = "left";
	div.style.paddingLeft = "85px";
	var title = document.createElement("h4");
	title.innerHTML = "Group A";
	div.appendChild(title);
	var input_box = document.createElement("textarea");
	input_box.id = left_div_name;
	input_box.style.width = "400px";
	input_box.style.height = "300px";
	div.appendChild(input_box);
	parent_div.appendChild(div);
	
	// "Group B" set of tasks/datasets to be compared
	div = document.createElement("div");
	div.style.float = "right";
	div.style.paddingRight = "85px";
	title = document.createElement("h4");
	title.innerHTML = "Group B";
	div.appendChild(title);
	input_box = document.createElement("textarea");
	input_box.id = right_div_name;
	input_box.style.width = "400px"
	input_box.style.height = "300px"
	div.appendChild(input_box);
	parent_div.appendChild(div);
	
	// comparison controls
	parent_div.appendChild(document.createElement("br"));
	div = document.createElement("div");
	div.style.clear = "both";
	var compare_button = document.createElement("button");
	compare_button.innerHTML = "Compare Spectrum Level";
	div.appendChild(compare_button);
	var compare_peptide_button = document.createElement("button");
	compare_peptide_button.innerHTML = "Compare Peptide Level";
	div.appendChild(compare_peptide_button);
	var compare_protein_button = document.createElement("button");
	compare_protein_button.innerHTML = "Compare Protein Level";
	div.appendChild(compare_protein_button);
	parent_div.appendChild(div);
    
    compare_button.onclick = function(comparison_div){
        return function(){
            show_hide_panel(comparison_div.id, "comparison_content_panel");
            
        	// set up modal overlay div for comparison panel
            $("#" + comparison_div.id).empty();
            var overlayDivID = comparison_div.id + "_overlay";
    		var overlay = document.createElement("div");
    		overlay.id = overlayDivID;
    		overlay.className = "overlay";
    		var spinner = document.createElement("div");
    		spinner.id = comparison_div.id + "_spinner";
    		overlay.appendChild(spinner);
            $("#" + comparison_div.id).append(overlay);
        	// start progress spinner
        	enableOverlay(
        		document.getElementById(overlayDivID), true, true, "14px");
            
            //task_id1 = $("#" + left_div_name).val();
            //task_id2 = $("#" + right_div_name).val();
            
            left_task_string = $("#" + left_div_name).val();
            right_task_string = $("#" + right_div_name).val();
            
            left_tab_objects = parse_comparison_string_to_array(left_task_string)
            right_tab_objects = parse_comparison_string_to_array(right_task_string)
            
            task_data = new Object()
            task_data.taskid1_data = new Array();
            task_data.taskid2_data = new Array();
            comparison_hoist_state.loaded = 0;
            get_tab_data_list(left_tab_objects, task_data,
            	"group_by_spectrum", "A", overlayDivID);
            get_tab_data_list(right_tab_objects, task_data,
            	"group_by_spectrum", "B", overlayDivID);
            
            
            //get_task_data(task_id1, task_id2, task_data)
            
            //Adding an additional div for the table
            table_div = document.createElement("div");
            table_div.id = "table_div_spectrum";
            
            render_buttons_spectrum(comparison_div.id, task_data, table_div.id)
            comparison_div.appendChild(table_div)
        }
    }(comparison_div)
    
    

    
    compare_peptide_button.onclick = function(comparison_div){
        return function(){
            show_hide_panel(comparison_div.id, "comparison_content_panel")
            
        	// set up modal overlay div for peptide comparison panel
            $("#" + comparison_div.id).empty();
            var overlayDivID = comparison_div.id + "_overlay";
    		var overlay = document.createElement("div");
    		overlay.id = overlayDivID;
    		overlay.className = "overlay";
    		var spinner = document.createElement("div");
    		spinner.id = comparison_div.id + "_spinner";
    		overlay.appendChild(spinner);
            $("#" + comparison_div.id).append(overlay);
        	// start progress spinner
        	enableOverlay(
        		document.getElementById(overlayDivID), true, true, "14px");
            
            left_task_string = $("#" + left_div_name).val();
            right_task_string = $("#" + right_div_name).val();
            
            left_tab_objects = parse_comparison_string_to_array(left_task_string)
            right_tab_objects = parse_comparison_string_to_array(right_task_string)
            
            task_data = new Object()
            task_data.taskid1_data = new Array();
            task_data.taskid2_data = new Array();
            comparison_hoist_state.loaded = 0;
            get_tab_data_list(left_tab_objects, task_data,
            	"group_by_peptide_derived", "A", overlayDivID);
            get_tab_data_list(right_tab_objects, task_data,
            	"group_by_peptide_derived", "B", overlayDivID);
            
            
            //get_task_data(task_id1, task_id2, task_data)
            
            //Adding an additional div for the table
            table_div = document.createElement("div");
            table_div.id = "table_div_peptide";
            
            render_buttons_peptide(comparison_div.id, task_data, table_div.id)
            comparison_div.appendChild(table_div)
        }
    }(comparison_peptide_div)
    
    
    compare_protein_button.onclick = function(comparison_div){
        return function(){
            show_hide_panel(comparison_div.id, "comparison_content_panel")
            
        	// set up modal overlay div for protein comparison panel
            $("#" + comparison_div.id).empty();
            var overlayDivID = comparison_div.id + "_overlay";
    		var overlay = document.createElement("div");
    		overlay.id = overlayDivID;
    		overlay.className = "overlay";
    		var spinner = document.createElement("div");
    		spinner.id = comparison_div.id + "_spinner";
    		overlay.appendChild(spinner);
            $("#" + comparison_div.id).append(overlay);
        	// start progress spinner
        	enableOverlay(
        		document.getElementById(overlayDivID), true, true, "14px");
            
            left_task_string = $("#" + left_div_name).val();
            right_task_string = $("#" + right_div_name).val();
            
            left_tab_objects = parse_comparison_string_to_array(left_task_string)
            right_tab_objects = parse_comparison_string_to_array(right_task_string)
            
            task_data = new Object()
            task_data.taskid1_data = new Array();
            task_data.taskid2_data = new Array();
            comparison_hoist_state.loaded = 0;
            get_tab_data_list(left_tab_objects, task_data,
            	"group_by_protein", "A", overlayDivID);
            get_tab_data_list(right_tab_objects, task_data,
            	"group_by_protein", "B", overlayDivID);
            
            
            //get_task_data(task_id1, task_id2, task_data)
            
            //Adding an additional div for the table
            table_div = document.createElement("div");
            table_div.id = "table_div_protein";
            
            render_buttons_protein(comparison_div.id, task_data, table_div.id)
            comparison_div.appendChild(table_div)
        }
    }(comparison_protein_div)
}

function parse_comparison_string_to_array(comparison_task_string){
    return_list = new Array()
    lines = comparison_task_string.split(";")
    for( i = 0; i < lines.length; i++){
        if(lines[i].length > 2){
            console.log(lines[i])
            
            site = lines[i].split(":")[0]
            tab_name = lines[i].split(":")[2]
            task = lines[i].split(":")[1]
            workflow = lines[i].split(":")[3]
            
            tab_object = new Object();
            tab_object.site = site
            tab_object.tab_name = tab_name
            tab_object.task = task
            tab_object.workflow = workflow
            
            return_list.push(tab_object)
        }
    }
    return return_list
}

function import_jobs_and_display(div){
    //Getting all of my tasks
    child_table = document.createElement("div");
    child_table.id = "jobs_div"
    div.appendChild(child_table);
    
    $.ajax({
        url: "/ProteoSAFe/QueryTaskList",
        cache: false,
        success: function(child_table){
            return function(json){
                all_tasks = JSON.parse(json);
                if(all_tasks.status == "success"){
                    my_tasks = all_tasks.tasks;
                    
                    filtered_tasks = new Array();
                    for (var i=0; i<my_tasks.length; i++) {
                    	// reject any tasks that did not complete successfully
                    	if (my_tasks[i].status != "DONE")
                    		continue;
                    	// only include tasks with an mzTab result view
                    	var workflow = my_tasks[i].workflow;
                        if (workflow == "CONVERT-TSV")
                            filtered_tasks.push(my_tasks[i]);
                        else if (workflow == "MSGFDB" ||
                        	workflow == "MSGF_PLUS" ||
                        	workflow == "MSPLIT" ||
                        	workflow == "MODA") {
                        	var version = my_tasks[i].version;
                        	// only version 1.2.8 and greater of these workflows
                        	// includes an mzTab conversion step
                        	// TODO: do a proper comparison of version numbers
                        	if (version == "1.2.8")
                                filtered_tasks.push(my_tasks[i]);
                        }
                    }
                    
                    var task = new Object();
                    task.id = "12345";
                    task.workflow = "My Tasks";
                    task.description = "";
                    var generic_table = new ResultViewTableGen(get_task_table_XML(), "jobs", task);
                    generic_table.setData(filtered_tasks);
                    generic_table.render(child_table, 0);
                }
            }
        }(child_table)
    });
}

function import_datasets_and_display(div) {
	// dataset table div
	var child_table = document.createElement("div");
	child_table.id = "datasets_div";
	div.appendChild(child_table);
	
	$.ajax({
        url: "/ProteoSAFe/datasets_json.jsp",
        cache: false,
        success: function(child_table){
            return function(json){
                all_datasets = JSON.parse(json).datasets;
                
                filtered_datasets = new Array();
                for(i = 0; i < all_datasets.length; i++){
                    if(all_datasets[i].flowname == "MASSIVE-COMPLETE") {
                        filtered_datasets.push(all_datasets[i])
                    }
                }
                
                var task = new Object();
                task.id = "12345";
                task.workflow = "Datasets";
                task.description = "";
                var generic_table = new ResultViewTableGen(get_dataset_table_XML(), "datasets", task, 0);
                generic_table.setData(filtered_datasets);
                generic_table.render(child_table, 0);
            }
        }(child_table)
    });
}

function render_buttons_spectrum(main_div_name, data, table_name) {
	// display header for this section
	var header = document.createElement("h3");
	header.innerHTML = "Spectrum Level Comparison";
	$("#" + main_div_name).append(header);
	
	// display tabs for this section
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_intersection(data, table_name)
        }
    }(data, table_name);
    
    
    intersection_different_button = document.createElement("button");
    intersection_different_button.innerHTML = "Intersection Mismatch";
    intersection_different_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_intersected_differences(data, table_name)
        }
    }(data, table_name);
    
    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_unique(data, table_name)
        }
    }(data, table_name);
    
    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_unique(data, table_name)
        }
    }(data, table_name);
    
    
    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(intersection_different_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}


function render_buttons_peptide(main_div_name, data, table_name){
	// display header for this section
	var header = document.createElement("h3");
	header.innerHTML = "Peptide Level Comparison";
	$("#" + main_div_name).append(header);
	
	// display tabs for this section
    console.log("Peptide Comparison Render");
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_peptide_intersection(data, table_name)
        }
    }(data, table_name);
    
    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_peptide_unique(data, table_name)
        }
    }(data, table_name);
    
    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_peptide_unique(data, table_name)
        }
    }(data, table_name);
    
    
    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}

function render_buttons_protein(main_div_name, data, table_name){
	// display header for this section
	var header = document.createElement("h3");
	header.innerHTML = "Protein Level Comparison";
	$("#" + main_div_name).append(header);
	
	// display tabs for this section
    console.log("Protein Comparison Render");
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_protein_intersection(data, table_name)
        }
    }(data, table_name);
    
    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_protein_unique(data, table_name)
        }
    }(data, table_name);
    
    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_protein_unique(data, table_name)
        }
    }(data, table_name);
    
    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}


function get_tab_data_list(tab_list, task_data, view, group, overlayDivID) {
	data_retreval_object = new Object();
	data_retreval_object.retreived_count = 0;
	data_retreval_object.expected = tab_list.length;
    
    for(i = 0; i < tab_list.length; i++){
        task = tab_list[i].task
        tab_name = tab_list[i].tab_name
        site = tab_list[i].site
        workflow = tab_list[i].workflow
        
        remote_url = "http://massive.ucsd.edu"
        
        if (site == "MassIVE")
            remote_url = "http://massive.ucsd.edu";
        else if (site == "ProteoSAFe-BETA")
            remote_url = "http://proteomics2.ucsd.edu";
        else if (site == "ProteoSAFe-ALPHA")
        	remote_url = "http://ccms-internal.ucsd.edu";
    
        fetch_url = remote_url + "/ProteoSAFe/result_jsonp.jsp"
        
        $.ajax({
            url: fetch_url,
            jsonp: "callback",
            async: false,
            dataType: "jsonp",
            data: {
                task: task,
                view: view,
                file: tab_name
            },
            success: function(
            	task_data, group, taskid, site, workflow, data_retreval_object
            ) {
                return function(response) {
                	if (isNaN(comparison_hoist_state.loaded))
                		comparison_hoist_state.loaded = 0;
                	comparison_hoist_state.loaded++;
                    data_retreval_object.retreived_count++;
                    if (data_retreval_object.retreived_count ==
                    	data_retreval_object.expected)
                        log_debug_message("Data Hoisted Successfully");
                    else console.log("Count = " +
                    	data_retreval_object.retreived_count +
                    	" , Expected = " + data_retreval_object.expected);
                    for (var i=0; i<response.blockData.length; i++)
                        response.blockData[i].taskid = taskid
                    // get the data group as specified by the function argument;
                    // this is necessary to do here since we need the whole
                    // "task_data" object to retrieve data in advance for
                    // populating the link list view
                	var data_group = null;
                	if (group.toUpperCase() == "A")
                		data_group = task_data.taskid1_data;
                	else data_group = task_data.taskid2_data;
                    response.blockData.forEach(function(v) {
                        v.site = site
                        v.workflow = workflow
                        data_group.push(v)
                    }, data_group);
                    // if both comparison groups have been loaded,
                    // collect the stats for the various comparisons
                    if (comparison_hoist_state.loaded >= 2) {
                    	if (view == "group_by_spectrum") {
                    		comparison_hoist_state.intersection_list =
                        		build_intersection_list(task_data);
                        	console.log("Intersection Match = " +
                        		comparison_hoist_state.intersection_list.length);
                        	comparison_hoist_state.intersected_differences_list =
                        		build_intersected_differences_list(task_data);
                        	console.log("Intersection Mismatch = " +
                        		comparison_hoist_state.intersected_differences_list.length);
                        	comparison_hoist_state.task1_unique_list =
                        		build_task1_unique_list(task_data);
                        	console.log("Group A Unique = " +
                        		comparison_hoist_state.task1_unique_list.length);
                        	comparison_hoist_state.task2_unique_list =
                        		build_task2_unique_list(task_data);
                        	console.log("Group B Unique = " +
                        		comparison_hoist_state.task2_unique_list.length);
                    	}
            			// stop progress spinner
                        setTimeout(function() {
                        	enableOverlay(document.getElementById(overlayDivID),
                        	false); }, 200);
                    }
                }
            }(task_data, group, task, site, workflow, data_retreval_object),
            error: function(response, text, error) {
    			// stop progress spinner
                setTimeout(function() {
                	enableOverlay(document.getElementById(overlayDivID),
                	false); }, 200);
    			console.log(
    				"Error hoisting result data to compare: [" + text + "].");
    		}
        })
    }
}

function log_debug_message(output_message){
    $("#debug_panel").append(output_message + "<br>");
}