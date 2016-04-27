function get_result_protein_intersection_table_XML(){
    var tableXML_str = '<block id="dataset_intersection" type="table"> \
                            <row>  \
                                <column field="protein" label="protein" type="text" width="5"/> \
                                <column field="task1_total_spectra" label="task1_total_spectra" type="text" width="5"/> \
                                <column field="task2_total_spectra" label="task2_total_spectra" type="text" width="5"/> \
                                <column field="hits_ratio" label="hits_ratio" type="float" precision="2"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_result_protein_unique_table_XML(){
    var tableXML_str = '<block id="dataset_intersection" type="table"> \
                            <row>  \
                                <column field="protein" label="protein" type="text" width="5"/> \
                                <column field="unmod_peptides" label="unmod_peptides" type="text" width="5"/> \
                                <column field="mod_peptides" label="mod_peptides" type="text" width="5"/> \
                                <column field="total_spectra" label="total_spectra" type="text" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}



function preprocess_task_protein_data(task_data){
    //if("task1_map" in task_data){
    //    //Dont Do anything
    //    console.log("Don't have to preprocess")
    //    return;
    //}
    
    
    task1_map = new Object();
    for(i = 0; i < task_data.taskid1_data.length; i++){
        key = task_data.taskid1_data[i]["ProteinID"]
        task1_map[key] = task_data.taskid1_data[i]
    }
    
    task2_map = new Object();
    for(i = 0; i < task_data.taskid2_data.length; i++){
        key = task_data.taskid2_data[i]["ProteinID"]
        task2_map[key] = task_data.taskid2_data[i]
    }
    
    task_data.task1_map = task1_map
    task_data.task2_map = task2_map
}

function display_protein_intersection(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_protein_data(task_data);
    
    //Now lets do the heavy lifting
    
    for(key in task_data.task1_map){
        if(key in task_data.task2_map){
            intersection_object = new Object();
            intersection_object.protein = task_data.task1_map[key]["ProteinID"]
            intersection_object.task1_unmod_peptides = task_data.task1_map[key]["Peptides"]
            intersection_object.task2_unmod_peptides = task_data.task2_map[key]["Peptides"]
            intersection_object.task1_mod_peptides = task_data.task1_map[key]["Modified"]
            intersection_object.task2_mod_peptides = task_data.task2_map[key]["Modified"]
            intersection_object.task1_total_spectra = task_data.task1_map[key]["Hits"]
            intersection_object.task2_total_spectra = task_data.task2_map[key]["Hits"]
            
            intersection_object.hits_ratio = parseFloat(intersection_object.task1_total_spectra) / parseFloat(intersection_object.task2_total_spectra).toString()
            
            
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
    task.workflow = "Protein Intersection";
    task.description = "Protein Intersection";
    var generic_table = new ResultViewTableGen(get_result_protein_intersection_table_XML(), "main_interesection_protein", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}


function display_task1_protein_unique(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_protein_data(task_data);

    //Now lets do the heavy lifting
    
    for(key in task_data.task1_map){
        if(key in task_data.task2_map){
        }
        else{
            intersection_object = new Object();
            intersection_object.protein = task_data.task1_map[key]["ProteinID"]
            intersection_object.unmod_peptides = task_data.task1_map[key]["Peptides"]
            intersection_object.mod_peptides = task_data.task1_map[key]["Modified"]
            intersection_object.total_spectra = task_data.task1_map[key]["Hits"]
            
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
    task.workflow = "Task 1 Unique Proteins";
    task.description = "Task 1 Unique Proteins";
    var generic_table = new ResultViewTableGen(get_result_protein_unique_table_XML(), "main_unique1_proteins", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}

function display_task2_protein_unique(task_data, div_name){
    intersection_list = new Array();
    
    preprocess_task_protein_data(task_data);

    //Now lets do the heavy lifting
    
    for(key in task_data.task2_map){
        if(key in task_data.task1_map){
        }
        else{
            intersection_object = new Object();
            intersection_object.protein = task_data.task2_map[key]["ProteinID"]
            intersection_object.unmod_peptides = task_data.task2_map[key]["Peptides"]
            intersection_object.mod_peptides = task_data.task2_map[key]["Modified"]
            intersection_object.total_spectra = task_data.task2_map[key]["Hits"]
            
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
    task.workflow = "Task 1 Unique Proteins";
    task.description = "Task 1 Unique Proteins";
    var generic_table = new ResultViewTableGen(get_result_protein_unique_table_XML(), "main_unique1_proteins", task, 0);
    generic_table.setData(intersection_list);
    generic_table.render(child_table, 0);
}