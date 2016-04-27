
function get_result_intersection_table_XML(){
    var tableXML_str =
'<block id="dataset_intersection" type="table"> \
	<row>  \
    	<column field="filename"        label="Filename"        type="text" width="5"/> \
    	<column field="scan"            label="Scan"            type="text" width="5"/> \
    	<column field="index"           label="Index"           type="text" width="5"/> \
    	<column field="task1_peptide"   label="Peptide 1"       type="text" width="5"/> \
    	<column field="task2_peptide"   label="Peptide 2"       type="text" width="5"/> \
    	<column field="spectra_present" label="Spectra Present" type="text" width="2"/> \
	</row>\
	<row expander="image"> \
    	<column type="jsonp_jscolumnspectrumviewer" colspan="3" label="Spectrum" width="600" height="350"> \
    		<parameter name="file"         value="FILE->[internalFilename]"/> \
    		<parameter name="scan"         value="[scan]"/> \
    		<parameter name="index"        value="[index]"/> \
    		<parameter name="peptide"      value="[task1_peptide]"/> \
    		<parameter name="dataset"      value="[dataset]"/> \
    		<parameter name="task"         value="[taskid]"/> \
    		<parameter name="modformat"    value="inspect"/> \
    		<parameter name="remoteserver" value="[remote_url]"/> \
    	</column> \
    	<column type="jsonp_jscolumnspectrumviewer" colspan="3" label="Spectrum" width="600" height="350"> \
    		<parameter name="file"         value="FILE->[internalFilename]"/> \
    		<parameter name="scan"         value="[scan]"/> \
    		<parameter name="index"        value="[index]"/> \
    		<parameter name="peptide"      value="[task2_peptide]"/> \
    		<parameter name="dataset"      value="[dataset]"/> \
    		<parameter name="task"         value="[taskid]"/> \
    		<parameter name="modformat"    value="inspect"/> \
    		<parameter name="remoteserver" value="[remote_url]"/> \
    	</column> \
	</row> \
</block>';
    return (parseXML(tableXML_str));
}

function get_result_unique_table_XML(){
    var tableXML_str = 
'<block id="dataset_intersection" type="table"> \
	<row>  \
    	<column field="filename"        label="Filename"        type="text" width="5"/> \
    	<column field="scan"            label="Scan"            type="text" width="5"/> \
    	<column field="peptide"         label="Peptide"         type="text" width="5"/> \
    	<column field="spectra_present" label="Spectra Present" type="text" width="2"/> \
	</row>\
	<row expander="image"> \
    	<column type="jsonp_jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
    		<parameter name="file"         value="FILE->[internalFilename]"/> \
    		<parameter name="scan"         value="[scan]"/> \
    		<parameter name="index"        value="[index]"/> \
    		<parameter name="peptide"      value="[peptide]"/> \
    		<parameter name="dataset"      value="[dataset]"/> \
    		<parameter name="task"         value="[taskid]"/> \
    		<parameter name="modformat"    value="inspect"/> \
    		<parameter name="remoteserver" value="[remote_url]"/> \
    	</column> \
	</row> \
</block>';
    return (parseXML(tableXML_str));
}

function preprocess_task_data(task_data){
    //if("task1_map" in task_data){
    //    //Dont Do anything
    //    console.log("Don't have to preprocess")
    //    return;
    //}
    
    
    task1_map = new Object();
    for(i = 0; i < task_data.taskid1_data.length; i++){
        key = task_data.taskid1_data[i]["#SpecFile"] + task_data.taskid1_data[i]["nativeID_scan"]
        if(task_data.taskid1_data[i]["nativeID_scan"] == "-1"){
            key = task_data.taskid1_data[i]["#SpecFile"] + task_data.taskid1_data[i]["nativeID_index"]
        }
        
        task1_map[key] = task_data.taskid1_data[i]
    }
    
    task2_map = new Object();
    for(i = 0; i < task_data.taskid2_data.length; i++){
        key = task_data.taskid2_data[i]["#SpecFile"] + task_data.taskid2_data[i]["nativeID_scan"]
        if(task_data.taskid2_data[i]["nativeID_scan"] == "-1"){
            key = task_data.taskid2_data[i]["#SpecFile"] + task_data.taskid2_data[i]["nativeID_index"]
        }
        task2_map[key] = task_data.taskid2_data[i]
    }
    
    task_data.task1_map = task1_map
    task_data.task2_map = task2_map
}

function annotateFilename(intersection_object, task_object) {
	if (intersection_object == null || task_object == null)
		return;
	// annotate object with mangled filename
	var mangled = task_object["internalFilename"];
	if (mangled == null)
		mangled = task_object["#SpecFile"];
	if (mangled == null) {
		intersection_object.internalFilename = "";
		return;
	}
	// include proper workflow-specific prefix
	var workflow = task_object["workflow"];
	if (workflow != null) {
		// if this is a MassIVE submission workflow task,
		// then its spectrum files are stored under "peak"
		if (workflow.length >= 7 &&
			workflow.substring(0, 7).toUpperCase() == "MASSIVE")
			mangled = "peak/" + mangled;
		// if this is a MassIVE submission workflow task,
		// then its spectrum files are stored under "peak"
		else if (workflow.length >= 7 &&
			workflow.substring(0, 7).toUpperCase() == "CONVERT")
			mangled = "peak/" + mangled;
		// otherwise, for any other workflow task,
		// its spectrum files are stored under "spec"
		else mangled = "spec/" + mangled;
	}
	intersection_object.internalFilename = mangled;
}

function build_intersection_list(task_data) {
	preprocess_task_data(task_data);
	intersection_list = new Array();
	// now lets do the heavy lifting
	for (key in task_data.task1_map) {
		if (key in task_data.task2_map) {
			intersection_object = new Object();
            intersection_object.taskid = task_data.task1_map[key].taskid;
            intersection_object.site = task_data.task1_map[key].site;
			intersection_object.filename =
				task_data.task1_map[key]["#SpecFile"];
			annotateFilename(intersection_object, task_data.task1_map[key]);
			intersection_object.task1_peptide =
				task_data.task1_map[key].modified_sequence;
			intersection_object.task2_peptide =
				task_data.task2_map[key].modified_sequence;
			intersection_object.scan =
				task_data.task1_map[key]["nativeID_scan"];
			intersection_object.index =
				task_data.task1_map[key]["nativeID_index"];
			// determine which site to pull stuff from
			enrich_comparison_object_with_siteinformation(
				intersection_object, task_data, key);
			intersection_list.push(intersection_object);
		}
	}
	// create ids for each row
	for (var i=0; i<intersection_list.length; i++) {
		intersection_list[i].id = "intersection_" + i;
	}
	return intersection_list;
}

function display_intersection(task_data, div_name){
	var intersection_list = comparison_hoist_state.intersection_list;
	if (intersection_list == null)
		intersection_list = comparison_hoist_state.intersection_list =
			build_intersection_list(task_data);
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Result Intersection";
    task.description = "Result Intersection";
    var generic_table = new ResultViewTableGen(
    	get_result_intersection_table_XML(), "intersection", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
    
}

function enrich_comparison_object_with_siteinformation(intersection_object, task_data, key){
    if(task_data.task1_map[key].workflow == "MASSIVE-COMPLETE" || task_data.task2_map[key].workflow == "MASSIVE-COMPLETE"){
        if(task_data.task1_map[key].workflow == "MASSIVE-COMPLETE"){
            intersection_object.dataset = task_data.task1_map[key].datasetName
            intersection_object.spectra_present = "1"
            site = task_data.task1_map[key].site
        }
        else{
            intersection_object.dataset = task_data.task2_map[key].datasetName
            intersection_object.spectra_present = "1"
            site = task_data.task2_map[key].site
        }
    }
    else{
        if(task_data.task1_map[key].workflow == "MSGFDB"){
            intersection_object.spectra_present = "1"
            site = task_data.task1_map[key].site
        }
        else{
            if(task_data.task2_map[key].workflow == "MSGFDB"){
                intersection_object.spectra_present = "1"
                site = task_data.task2_map[key].site
            }
            else{
                if(task_data.task2_map[key].workflow == "CONVERT-TSV"){
                    //Its Convert TSV
                    intersection_object.spectra_present = "0"
                    site = task_data.task2_map[key].site
                }
            }
        }
    }
    remote_url = ""
    if(site == "MassIVE"){
        remote_url = "http://massive.ucsd.edu"
    }
    if(site == "ProteoSAFe-BETA"){
        remote_url = "http://proteomics2.ucsd.edu"
    }
    
    intersection_object.remote_url = remote_url
}

function build_intersected_differences_list(task_data) {
    intersection_list = new Array();
    preprocess_task_data(task_data);
    // now lets do the heavy lifting
    for (key in task_data.task1_map) {
        if (key in task_data.task2_map) {
            if (task_data.task1_map[key].sequence !=
            	task_data.task2_map[key].sequence) {
                intersection_object = new Object();
                intersection_object.taskid = task_data.task1_map[key].taskid;
                intersection_object.site = task_data.task1_map[key].site;
    			intersection_object.filename =
    				task_data.task1_map[key]["#SpecFile"];
    			annotateFilename(intersection_object, task_data.task1_map[key]);
                intersection_object.task1_peptide =
                	task_data.task1_map[key].modified_sequence;
                intersection_object.task2_peptide =
                	task_data.task2_map[key].modified_sequence;
                intersection_object.scan =
                	task_data.task2_map[key]["nativeID_scan"];
                intersection_object.index =
                	task_data.task2_map[key]["nativeID_index"];
                enrich_comparison_object_with_siteinformation(
                	intersection_object, task_data, key);
                intersection_list.push(intersection_object);
            }
        }
    }
    // create ids for each row
    for (var i=0; i<intersection_list.length; i++) {
        intersection_list[i].id = "intersection_" + i;
    }
    return intersection_list;
}

function display_intersected_differences(task_data, div_name){
	var intersection_list = comparison_hoist_state.intersected_differences_list;
	if (intersection_list == null)
		intersection_list =
		comparison_hoist_state.intersected_differences_list =
			build_intersected_differences_list(task_data);
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Result Intersection Different";
    task.description = "Result Intersection Different";
    var generic_table = new ResultViewTableGen(
    	get_result_intersection_table_XML(), "main2", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}

function build_task1_unique_list(task_data) {
    intersection_list = new Array();
    preprocess_task_data(task_data);
    // now lets do the heavy lifting
    for (key in task_data.task1_map) {
        if (key in task_data.task2_map) {
        	
        } else {
            intersection_object = new Object();
            intersection_object.taskid = task_data.task1_map[key].taskid;
            intersection_object.site = task_data.task1_map[key].site;
			intersection_object.filename =
				task_data.task1_map[key]["#SpecFile"];
			annotateFilename(intersection_object, task_data.task1_map[key]);
            intersection_object.peptide =
            	task_data.task1_map[key].modified_sequence;
            intersection_object.scan =
            	task_data.task1_map[key]["nativeID_scan"];
            intersection_object.index =
            	task_data.task1_map[key]["nativeID_index"];
            intersection_object.dataset =
            	task_data.task1_map[key].datasetName;
            intersection_object.taskid =
            	task_data.task1_map[key].taskid;
            intersection_object.site =
            	task_data.task1_map[key].site;
            if (task_data.task1_map[key].workflow == "MASSIVE-COMPLETE" ||
            	task_data.task1_map[key].workflow == "MSGFDB") {
                intersection_object.spectra_present = "1"
            }
            if (task_data.task1_map[key].workflow == "CONVERT-TSV") {
                intersection_object.spectra_present = "0"
            }
            remote_url = ""
            if (intersection_object.site == "MassIVE") {
                remote_url = "http://massive.ucsd.edu"
            }
            if (intersection_object.site == "ProteoSAFe-BETA") {
                remote_url = "http://proteomics2.ucsd.edu"
            }
            intersection_object.remote_url = remote_url;
            intersection_list.push(intersection_object);
        }
    }
    // create ids for each row
    for (var i=0; i<intersection_list.length; i++) {
        intersection_list[i].id = "intersection_" + i;
    }
    return intersection_list;
}

function display_task1_unique(task_data, div_name) {
	var intersection_list = comparison_hoist_state.task1_unique_list;
	if (intersection_list == null)
		intersection_list = comparison_hoist_state.task1_unique_list =
			build_task1_unique_list(task_data);
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Task 1 Unique";
    task.description = "Task 1 Unique";
    var generic_table = new ResultViewTableGen(
    	get_result_unique_table_XML(), "main2", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}

function build_task2_unique_list(task_data) {
    intersection_list = new Array();
    preprocess_task_data(task_data);
    // now lets do the heavy lifting
    for (key in task_data.task2_map) {
        if (key in task_data.task1_map) {
        	
        } else {
            intersection_object = new Object();
            intersection_object.taskid = task_data.task2_map[key].taskid;
            intersection_object.site = task_data.task2_map[key].site;
			intersection_object.filename =
				task_data.task2_map[key]["#SpecFile"];
			annotateFilename(intersection_object, task_data.task2_map[key]);
            intersection_object.peptide =
            	task_data.task2_map[key].modified_sequence;
            intersection_object.scan =
            	task_data.task2_map[key]["nativeID_scan"];
            intersection_object.index =
            	task_data.task2_map[key]["nativeID_index"];
            intersection_object.dataset =
            	task_data.task2_map[key].datasetName;
            intersection_object.taskid =
            	task_data.task2_map[key].taskid;
            intersection_object.site =
            	task_data.task2_map[key].site;
            if (task_data.task2_map[key].workflow == "MASSIVE-COMPLETE" ||
            	task_data.task2_map[key].workflow == "MSGFDB") {
                intersection_object.spectra_present = "1"
            }
            if (task_data.task2_map[key].workflow == "CONVERT-TSV") {
                intersection_object.spectra_present = "0"
            }
            remote_url = ""
            if (intersection_object.site == "MassIVE") {
                remote_url = "http://massive.ucsd.edu"
            }
            if (intersection_object.site == "ProteoSAFe-BETA") {
                remote_url = "http://proteomics2.ucsd.edu"
            }
            intersection_object.remote_url = remote_url;
            intersection_list.push(intersection_object);
        }
    }
    // create ids for each row
    for (var i=0; i<intersection_list.length; i++) {
        intersection_list[i].id = "intersection_" + i;
    }
    return intersection_list;
}

function display_task2_unique(task_data, div_name) {
	var intersection_list = comparison_hoist_state.task2_unique_list;
	if (intersection_list == null)
		intersection_list = comparison_hoist_state.task2_unique_list =
			build_task2_unique_list(task_data);
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Task 2 Unique";
    task.description = "Task 2 Unique";
    var generic_table = new ResultViewTableGen(
    	get_result_unique_table_XML(), "main2", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}