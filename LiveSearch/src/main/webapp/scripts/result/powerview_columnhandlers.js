
function powerview_plotbutton_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        
        var scan = invokeParameters["Scan"];
        var filename = invokeParameters["FileName"];
        var peptide = invokeParameters["peptide"];
        
        if(peptide.indexOf("Peptide: ") == -1){
            peptide = "*..*";
        }
        else{
            peptide = peptide.slice(9);
            if(peptide.length < 3){
                peptide = "*..*";
            }
        }
        
        //json_link.href = invokeParameters["link"];
        var render_button_1 = document.createElement("button");
        render_button_1.innerHTML = "1";
        render_button_1.onclick = function(){
            return plot_spectrum_setparameters(filename, scan, peptide, "1");
        };
        
        var render_button_2 = document.createElement("button");
        render_button_2.innerHTML = "2";
        render_button_2.onclick = function(){
            return plot_spectrum_setparameters(filename, scan, peptide, "2");
        };
        
        var view_cluster_button = document.createElement("button");
        view_cluster_button.innerHTML = "Cluster";
        view_cluster_button.onclick = function(){
            return onclick_view_cluster_spectra(scan);
        };
        
        var view_neighbor_button = document.createElement("button");
        view_neighbor_button.innerHTML = "Neighbors";
        view_neighbor_button.onclick = function(){
            return onclick_view_neighbor_spectra(scan);
        };
        
        var view_cluster_details = document.createElement("button");
        view_cluster_details.innerHTML = "Details";
        view_cluster_details.onclick = function(){
            return onclick_clusterdetails_spectra(scan);
        };
        
        td.appendChild(render_button_1);
        td.appendChild(render_button_2);
        td.appendChild(view_cluster_button);
        td.appendChild(view_neighbor_button);
        td.appendChild(view_cluster_details);
    }
}

var powerview_plotbuttonsColumnHandler = {
    render: powerview_plotbutton_renderstream
};

columnHandlers["powerview_plotbuttons"] = powerview_plotbuttonsColumnHandler;

//Plotting for Right Column
function powerview_plotcolumn_rightpanel_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        
        var scan = invokeParameters["Scan"];
        var filename = invokeParameters["FileName"];
        var peptide = invokeParameters["peptide"];
        if(peptide.indexOf("Peptide: ") == -1){
            peptide = "*..*";
        }
        else{
            peptide = peptide.slice(9);
            if(peptide.length < 3){
                peptide = "*..*";
            }
        }
        //json_link.href = invokeParameters["link"];
        var render_button_1 = document.createElement("button");
        render_button_1.innerHTML = "1";
        render_button_1.onclick = function(){
            return plot_spectrum_setparameters(filename, scan, peptide, "1");
        };
        
        var render_button_2 = document.createElement("button");
        render_button_2.innerHTML = "2";
        render_button_2.onclick = function(){
            return plot_spectrum_setparameters(filename, scan, peptide, "2");
        };
        
        
        td.appendChild(render_button_1);
        td.appendChild(render_button_2);
    }
}

var powerview_plotbuttons_rightpanel_ColumnHandler = {
    render: powerview_plotcolumn_rightpanel_renderstream
};

columnHandlers["powerview_rightpanel_plotbuttons"] = powerview_plotbuttons_rightpanel_ColumnHandler;


//callback onclick to view clusters
function onclick_view_cluster_spectra(cluster_index){
    var task_id = get_taskid();
    var pairs_info_url = '/ProteoSAFe/result.jsp'
    $.ajax({
        url: pairs_info_url,
        data: { task: task_id, view: 'cluster_details', protein: cluster_index, proteinID: cluster_index, show: 'true' },
        cache: false,
        success: function(result){
            //$("#NetworkPairsDiv").hide();
            $("#rightcontentdiv").empty();
            
            var task = new Object();
            task.id = "1234";
            task.workflow = "Cluster Info Table";
            task.description = "Cluster Info Table";
            var generic_table = new ResultViewTable(get_clusterinfo_tableXML(), "clusterinfo_id", task, 0);
            generic_table.setData(get_block_data_from_page(result));
            generic_table.render($("#rightcontentdiv")[0], 0);
            
        }
    });    
}

function get_cluster_peptide_information(cluster_index){
    peptide = "*..*";
    if(powerview_global_object.peptide_results_data != null){
        peptide = get_proteomics_result_peptide(cluster_index, powerview_global_object.peptide_results_data);
        if(peptide == null){
            peptide = "*..*";
        }
    }
    return peptide;
}

function get_proteomics_result_peptide(cluster_index, proteomics_data){
    for (var i = 0; i < proteomics_data.length; i++){
        if(proteomics_data[i]['Scan'] == cluster_index){
            return proteomics_data[i]['Peptide'];
        }
    }
    return null;
}

function get_cluster_libID_information(cluster_index){
    lib_annotation_db = null;
    if(powerview_global_object.all_annotations_DB_data != null){
        lib_annotation_db = get_libdb_result_compound(cluster_index, powerview_global_object.all_annotations_DB_data);
    }
    return lib_annotation_db;
}

function get_libdb_result_compound(cluster_index, libdata_data){
    for (var i = 0; i < libdata_data.length; i++){
        if(libdata_data[i]['#Scan#'] == cluster_index){
            return libdata_data[i];
        }
    }
    return null;
}

function render_library_list_in_details(div, lib_element_data){
    if(lib_element_data == null){
        div.innerHTML = "Lib: NONE";
        return;
    }
    compound_name = lib_element_data["Compound_Name"];
    library_path = "lib/" + lib_element_data["LibraryName"];
    library_spectrumid = lib_element_data["SpectrumID"];
    peptide = get_peptide_from_compoundname(compound_name);
    
    var render_library = document.createElement("button");
    render_library.innerHTML = "RenderLib: " + compound_name;
    render_library.onclick = function(){
        return plot_library_call_backend(library_path, library_spectrumid, peptide, "2");
    };
    
    
    div.appendChild(render_library);
}

function plot_library_call_backend(library_path, library_spectrumid, peptide, plotter){
    invokeParameters = {}
    
    invokeParameters["file"] = "FILE->" + library_path;
    invokeParameters["spectrumid"] = library_spectrumid;
    invokeParameters["peptide"] = peptide;
    invokeParameters["force"] = "true";
    
    plot_spectrum_call_backend(invokeParameters, plotter);
}

function onclick_clusterdetails_spectra(cluster_index){
    //First lets find in the data
    clusters_array = powerview_global_object.allclusters_data;
    for (var i = 0; i < clusters_array.length; i++){
        if(clusters_array[i]['cluster index'] == cluster_index){
            console.log(clusters_array[i]);
            $("#ClusterDetails").empty();
            
            var library_id = get_cluster_libID_information(cluster_index);
            var peptide = "*..*";
            if(library_id != null){
                peptide = get_peptide_from_compoundname(library_id.Compound_Name);
            }
            
            //Creating Content
            var details_div = document.createElement("div");
            
            var listElement = document.createElement("ul");
            var ClusterIndex_listitem = document.createElement("li");
            ClusterIndex_listitem.innerHTML = "Index: " + cluster_index;
            var RTMean_listitem = document.createElement("li");
            RTMean_listitem.innerHTML = "RTMean: " + clusters_array[i]['RTMean'];
            var PrecursorMZ_listitem = document.createElement("li");
            PrecursorMZ_listitem.innerHTML = "MZ: " + clusters_array[i]['precursor mass'];
            var PrecursorInt_listitem = document.createElement("li");
            PrecursorInt_listitem.innerHTML = "Prec Int: " + clusters_array[i]['sum(precursor intensity)'];
            var PrecursorCharge_listitem = document.createElement("li");
            PrecursorCharge_listitem.innerHTML = "Charge: " + clusters_array[i]['precursor charge'];
            var LibraryID_listitem = document.createElement("li");
            render_library_list_in_details(LibraryID_listitem, get_cluster_libID_information(cluster_index));
            var peptide_listitem = document.createElement("li");
            peptide_listitem.innerHTML = "Peptide: " + peptide;
            
            
            
            
            //Adding Buttons
            var filename = "spectra/specs_ms.pklbin";
            var scan = cluster_index;
            //peptide = "*..*";
            var render_button_1 = document.createElement("button");
            render_button_1.innerHTML = "1";
            render_button_1.onclick = function(){
                return plot_spectrum_setparameters(filename, scan, peptide, "1");
            };
            
            var render_button_2 = document.createElement("button");
            render_button_2.innerHTML = "2";
            render_button_2.onclick = function(){
                return plot_spectrum_setparameters(filename, scan, peptide, "2");
            };
            
            var view_cluster_button = document.createElement("button");
            view_cluster_button.innerHTML = "Cluster";
            view_cluster_button.onclick = function(){
                return onclick_view_cluster_spectra(scan);
            };
            
            var view_neighbor_button = document.createElement("button");
            view_neighbor_button.innerHTML = "Neighbors";
            view_neighbor_button.onclick = function(){
                return onclick_view_neighbor_spectra(scan);
            };
            
            
            listElement.appendChild(ClusterIndex_listitem);
            listElement.appendChild(RTMean_listitem);
            listElement.appendChild(PrecursorMZ_listitem);
            listElement.appendChild(PrecursorInt_listitem);
            listElement.appendChild(PrecursorCharge_listitem);
            listElement.appendChild(LibraryID_listitem);
            listElement.appendChild(peptide_listitem);
            
            listElement.appendChild(render_button_1);
            listElement.appendChild(render_button_2);
            listElement.appendChild(view_cluster_button);
            listElement.appendChild(view_neighbor_button);
            
            
            
            
            details_div.appendChild(listElement);
            
            $("#ClusterDetails")[0].appendChild(details_div);
            
            return;
        }
    }
}

function onclick_view_neighbor_spectra(cluster_index){
    var task_id = get_taskid();
    var pairs_info_url = '/ProteoSAFe/DownloadBlock'
    $.ajax({
        url: pairs_info_url,
        data: { task: task_id, block: 'related_network_list_specnets', spectraid: cluster_index, show: 'true' },
        cache: false,
        success: function(result){
            //$("#NetworkPairsDiv").hide();
            $("#rightcontentdiv").empty();
            
            var task = new Object();
            task.id = "1234";
            task.workflow = "Cluster Info Table";
            task.description = "Cluster Info Table";
            var generic_table = new ResultViewTable(get_networkneighbors_tableXML(), "neighbors_id", task, 0);
            generic_table.setData(get_block_data_from_xmlspec(result));
            generic_table.render($("#rightcontentdiv")[0], 0);
            
        }
    });    
}

function get_peptide_from_compoundname(compound_name){
    peptide = "*..*";
    if(compound_name != null){
        if(compound_name.indexOf("Peptide: ") == -1){
            peptide = "*..*";
        }
        else{
            peptide = compound_name.slice(9);
            if(peptide.length < 3){
                peptide = "*..*";
            }
        }
    }
    
    return peptide;
}

/**
 * Helper function to stream and render workflow result files
 */
function display_specplot_image_powerview(
    div, task, type, source, parameters, contentType, block
) {
    if (div == null || task == null || type == null || source == null)
        return;
    // set up URL to download result file
    var url = "DownloadResultFile?task=" + task + "&" + type + "=" + source;
    if (block != null)
        url += "&block=" + block;
    if (parameters != null)
        for (var parameter in parameters)
            url += "&" + parameter + "=" +
                encodeURIComponent(parameters[parameter]);
    
    // show "in-progress" download spinner
    removeChildren(div);
    var child = document.createElement("img");
    child.src = "images/inProgress.gif";
    div.appendChild(child);
    
    // create and submit AJAX request for the file data
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            // remove "in-progress" download spinner
            removeChildren(div);
            if (request.status == 200) {
                // display text file content appropriately based on type
                if (contentType != null && contentType.indexOf("image") >= 0) {
                    child = document.createElement("img");
                    child.style.border = "0px";
                    child.style.margin = "0px";
                    child.style.padding = "0px";
                    child.style.maxWidth = "100%";
                    // servlet should write image content
                    // as a properly formatted data URL
                    child.src = "data:" + contentType + ";base64," +
                        request.responseText;
                } else if (contentType == "text/plain") {
                    child = document.createElement("pre");
                    child.innerHTML = request.responseText;
                } else if (contentType == "text/html") {
                    child = document.createElement("div");
                    child.innerHTML = request.responseText;
                    injectScripts(request.responseText);
                } else {
                    child = document.createElement("div");
                    child.innerHTML = request.responseText;
                }
                div.appendChild(child);
            } else if (request.status == 410) {
                alert("The input file associated with this request could not " +
                    "be found. This file was probably deleted by its owner " +
                    "after this workflow task completed.");
            } else {
                alert("Could not download result artifact of type \"" + type +
                    "\" and value \"" + source + "\", belonging to task " +
                    task + ".");
            }
        }
    }
    request.setRequestHeader("If-Modified-Since",
        "Sat, 1 Jan 2000 00:00:00 GMT");    
    request.send(null);
}