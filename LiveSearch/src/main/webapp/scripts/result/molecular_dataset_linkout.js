/**
 * File stream result view block implementation
 */
// constructor
function MolecularDatasetLinkoutClass(blockXML, id, task) {
        // properties
        this.id = id;
        this.task = task;
        
        // set up the file retrieval
        this.init(blockXML);
}

// initialize block from XML specification
MolecularDatasetLinkoutClass.prototype.init = function(blockXML) {
        
}

// render the streamed file
MolecularDatasetLinkoutClass.prototype.render = function(div, index) {
}

// set data to the file streamer
MolecularDatasetLinkoutClass.prototype.setData = function(data) {
        this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["molecular_dataset_linkout"] = ParameterLinkClass;

function molecular_dataset_linkout_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        //Setting the width
        if(attributes.width == null){
            td.style.minWidth = "200px";
        }
        else{
            td.style.minWidth = attributes.width;
        }
        
        
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        var compound_name = ""
        var json_string = ""
        if (parameters != null){
            json_string = invokeParameters["datasets_json"];
            compound_name = invokeParameters["compoundname"].replace("FILE->", "").replace(/"/g, '\\"');
        }
        
        var datasets_array = JSON.parse(json_string);
        
        for ( i = 0; i < datasets_array.length; i++ ) {
            var unidentified_precursors = datasets_array[i]["unique_unidentified_precursor_neighbors_in_dataset"];
            
            
            var search_task_id = datasets_array[i]["search_task_id"]
            var dataset_name = datasets_array[i]["dataset_name"]
            var dataset_display_name = datasets_array[i]["dataset_display_name"]
            var candidate_scans_str = datasets_array[i]["unique_unidentified_precursor_neighbors_in_dataset_candidate_scans"];
            
            var link_path = "/ProteoSAFe/result.jsp?show=true&task=" + search_task_id + "&view=view_all_clusters_withID"
            
            filter_string = '#{"LibraryID_input":"' + compound_name + '"}';
            
            link_path += filter_string;
            
            var dataset_search_link = document.createElement("a");
            dataset_search_link.href = link_path;
            dataset_search_link.innerHTML = dataset_name;
            dataset_search_link.title = dataset_display_name;
            dataset_search_link.target = "_blank";
            
            dataset_exact_match_button = document.createElement("input")
            dataset_exact_match_button.setAttribute('type','button');
            dataset_exact_match_button.setAttribute('name', dataset_name + " IDs");
            dataset_exact_match_button.setAttribute('value',dataset_name + " IDs");
            
            dataset_exact_match_button.onclick = function(target_url){
                return function(){
                    var win = window.open(target_url, '_blank');
                    win.focus();
                }
            }(link_path);
            

            
            //Link to analogs
            var candidate_scans_array = JSON.parse(candidate_scans_str);
            filter_string = '#{"cluster index_input":"' + candidate_scans_array.join("||") + "||EXACT" +  '"}';
            analog_match_url = "/ProteoSAFe/result.jsp?show=true&task=" + search_task_id + "&view=view_all_clusters_withID" + filter_string;
            
            var dataset_analogs_link = document.createElement("a");
            dataset_analogs_link.innerHTML = ":View " + unidentified_precursors + " Neighbors";
            dataset_analogs_link.href = analog_match_url
            dataset_analogs_link.target = "_blank";
            
            
            dataset_analogs_match_button = document.createElement("input")
            dataset_analogs_match_button.setAttribute('type','button');
            dataset_analogs_match_button.setAttribute('name', "View " + unidentified_precursors + " Neighbors");
            dataset_analogs_match_button.setAttribute('value',"View " + unidentified_precursors + " Neighbors");
            
            dataset_analogs_match_button.onclick = function(target_url){
                return function(){
                    var win = window.open(target_url, '_blank');
                    win.focus();
                }
            }(dataset_analogs_link);
            
            dataset_analogs_match_button.style.float = "right"
            
            
            //td.appendChild(dataset_search_link);
            //td.appendChild(dataset_analogs_link);
            td.appendChild(dataset_exact_match_button);
            td.appendChild(dataset_analogs_match_button);
            td.appendChild(document.createElement("br"));
        }
    }
}

var molecular_dataset_linkout_ColumnHandler = {
    render: molecular_dataset_linkout_renderstream,
    sort: plainSorter
};



columnHandlers["molecular_dataset_linkout"] = molecular_dataset_linkout_ColumnHandler;