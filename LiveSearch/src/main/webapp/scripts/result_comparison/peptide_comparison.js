function get_result_peptide_intersection_table_XML(){
    var tableXML_str = '<block id="dataset_intersection" type="table"> \
                            <row>  \
                                <column field="task1_peptide" label="task1_peptide" type="text" width="5"/> \
                                <column field="task2_peptide" label="task2_peptide" type="text" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_result_peptide_unique_table_XML(){
    var tableXML_str = '<block id="dataset_intersection" type="table"> \
                            <row>  \
                                <column field="peptide" label="peptide" type="text" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


function preprocess_task_peptide_data(task_data){
    //if("task1_map" in task_data){
    //    //Dont Do anything
    //    console.log("Don't have to preprocess")
    //    return;
    //}
    
    
    task1_map = new Object();
    for(i = 0; i < task_data.taskid1_data.length; i++){
        key = task_data.taskid1_data[i]["modified_sequence"]
        task1_map[key] = task_data.taskid1_data[i]
    }
    
    task2_map = new Object();
    for(i = 0; i < task_data.taskid2_data.length; i++){
        key = task_data.taskid2_data[i]["modified_sequence"]
        task2_map[key] = task_data.taskid2_data[i]
    }
    
    task_data.task1_map = task1_map
    task_data.task2_map = task2_map
}

function display_peptide_intersection(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_peptide_data(task_data);
    
    //Now lets do the heavy lifting
    
    for(key in task_data.task1_map){
        if(key in task_data.task2_map){
            intersection_object = new Object();
            intersection_object.task1_filename = task_data.task1_map[key]["#SpecFile"]
            intersection_object.task2_filename = task_data.task2_map[key]["#SpecFile"]
            intersection_object.task1_peptide = task_data.task1_map[key].modified_sequence
            intersection_object.task2_peptide = task_data.task2_map[key].modified_sequence
            intersection_object.task1_scan = task_data.task1_map[key]["nativeID_scan"]
            intersection_object.task1_scan = task_data.task2_map[key]["nativeID_scan"]
            
            //enrich_comparison_object_with_siteinformation(intersection_object, task_data, key)
            intersection_list.push(intersection_object)
        }
    }
    
    //creating ids for each row
    for(var i = 0; i < intersection_list.length; i++){
        intersection_list[i].id = "intersection_" + i;
    }
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Result Intersection";
    task.description = "Result Intersection";
    var generic_table = new ResultViewTableGen(get_result_peptide_intersection_table_XML(), "main_interesection_peptide", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}


function display_task1_peptide_unique(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_peptide_data(task_data);

    //Now lets do the heavy lifting
    
    for(key in task_data.task1_map){
        if(key in task_data.task2_map){
        }
        else{
            intersection_object = new Object();
            intersection_object.filename = task_data.task1_map[key]["#SpecFile"]
            intersection_object.peptide = task_data.task1_map[key].modified_sequence
            intersection_object.scan = task_data.task1_map[key]["nativeID_scan"]
            
            intersection_list.push(intersection_object)
        }
    }
    
    //creating ids for each row
    for(var i = 0; i < intersection_list.length; i++){
        intersection_list[i].id = "intersection_" + i;
    }
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Task 1 Unique Peptide";
    task.description = "Task 1 Unique Peptide";
    var generic_table = new ResultViewTableGen(get_result_peptide_unique_table_XML(), "main_unique1_peptide", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}

function display_task2_peptide_unique(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_peptide_data(task_data);

    //Now lets do the heavy lifting
    
    for(key in task_data.task2_map){
        if(key in task_data.task1_map){
        }
        else{
            intersection_object = new Object();
            intersection_object.filename = task_data.task2_map[key]["#SpecFile"]
            intersection_object.peptide = task_data.task2_map[key].modified_sequence
            intersection_object.scan = task_data.task2_map[key]["nativeID_scan"]
            
            intersection_list.push(intersection_object)
        }
    }
    
    //creating ids for each row
    for(var i = 0; i < intersection_list.length; i++){
        intersection_list[i].id = "intersection_" + i;
    }
    
    child_table = document.createElement("div");
    $("#" + div_name).append(child_table);
    
    var task = new Object();
    task.id = "1234";
    task.workflow = "Task 2 Unique Peptide";
    task.description = "Task 2 Unique Peptide";
    var generic_table = new ResultViewTableGen(get_result_peptide_unique_table_XML(), "main_unique2_peptide", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}
