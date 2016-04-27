function display_unique_cluster_filenames(tableId, rowId, columnId, attributes){
    return function(td, record, index) {
        invokeParameters = resolve_parameters_to_map(attributes.parameters, record)
        
        all_files_string = (invokeParameters["all_files"])
        tokens = all_files_string.split("###");
        
        unique_filename = new Object()
        
        for(i in tokens){
            if(tokens[i].length < 2){
                continue
            }
            unique_filename[tokens[i].split(":")[0]] = 1
        }
        
        filenames_array = Object.keys(unique_filename).sort()
        
        filenames_div = document.createElement("div");
        filenames_div.id = makeRandomString(10)
        filenames_div.innerHTML =  filenames_array.join([separator = '<br>'])
        filenames_div.style.display = "none"
        
        td.appendChild(filenames_div)
        
        show_btn = document.createElement("BUTTON");
        show_btn.id = makeRandomString(10)
        show_btn.innerHTML = "Show Files"
        show_btn.style.width = "100px"
        
        show_btn.onclick = function(show_div_id, button_id){
            return function(){
                $("#" + show_div_id).show()
                $("#" + button_id).hide()
            }
        }(filenames_div.id, show_btn.id)
        
        td.appendChild(show_btn)
    }
}

var displayUniqueClusterFilenamesColumnHandler = {
    render: display_unique_cluster_filenames
};

columnHandlers["network_displayuniqueclusterfilenames"] = displayUniqueClusterFilenamesColumnHandler;