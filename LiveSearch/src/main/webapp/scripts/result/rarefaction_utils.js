function generate_clustering_rarefaction(cluster_data){
    //Determining file list
    file_map = new Object()
    
    for(var i in cluster_data){
        AllFiles_string = cluster_data[i].AllFiles
        individual_files = AllFiles_string.split("###")
        for(var j in individual_files){
            if(individual_files[j].length < 2){
                continue;
            }
            filename = individual_files[j].split(":")[0]
            
            if(!(filename in file_map)){
                file_map[filename] = new Array()
            }
            
            file_map[filename].push(cluster_data[i]["cluster index"])
        }
    }
    
    //Generating File List
    file_list = new Array();
    
    for(key in file_map){
        file_list.push(key);
    }
    
    all_diversity_lists = new Array();
    
    for(var iterations = 0; iterations < 20; iterations++){
        //Randomizing list
        file_list.sort(function() {
            return .5 - Math.random();
        });
        
        
        //Test Generation of curve
        diversity_list = generate_rarefaction_curve(file_list, file_map)
        all_diversity_lists.push(diversity_list)
    }
    
    return generate_average_error_bar_curve(all_diversity_lists, file_list.length)
}


function generate_rarefaction_curve(file_ordering, file_to_item_mapping){
    var groups_touched = new Object();
    
    diversity_list = new Array()
    
    for(var i in file_ordering){
        //iterating through all the files
        groups_effected = file_to_item_mapping[file_ordering[i]]
       
        for(var j in groups_effected){
           groups_touched[groups_effected[j]] = 1
        }
       
       //Getting Size
        var size = 0, key;
        for (key in groups_touched) {
            if (groups_touched.hasOwnProperty(key)) size++;
        }
    
       
       diversity_list.push(size);
    }
    
    return diversity_list;
}

function generate_average_error_bar_curve(diversity_lists, input_file_size){
    summarizing_curve = new Array();
    
    for(i = 0; i < input_file_size; i++){
        sum = 0;
        for(j = 0; j < diversity_lists.length; j++){
            sum += diversity_lists[j][i]
        }
        mean = sum/diversity_lists.length
        variance = 20.0
        
        summarizing_curve.push([mean, variance])
    }
    return summarizing_curve;
}
