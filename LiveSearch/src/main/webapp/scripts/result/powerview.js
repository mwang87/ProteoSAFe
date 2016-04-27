var powerview_global_object = null;

/**
 * File stream result view block implementation
 */
// constructor
function ResultViewFilePowerView(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
    
    this.pairs_data = null;
    this.cluster_data = null;
    
}

// initialize block from XML specification
ResultViewFilePowerView.prototype.init = function(blockXML) {
}

// render the streamed file
ResultViewFilePowerView.prototype.render = function(div, index) {
    this.renderdiv = div;
    
    //Adding Massive Information On Top
    massive_info_div = document.createElement("div");
    massive_info_div.id = "PowerViewerDiv";
    
    plot_area_div = document.createElement("div");
    plot_area_div.style.height = "350px";
    
    plot_display_first = document.createElement("div");
    plot_display_first.id = "PlotDisplay_1";
    plot_display_first.style.width = "450px";
    plot_display_first.style.height = "360px";
    plot_display_first.style.float = "left";
    plot_display_second = document.createElement("div");
    plot_display_second.id = "PlotDisplay_2";
    plot_display_second.style.width = "450px";
    plot_display_second.style.height = "360px";
    plot_display_second.style.float = "right";
    
    
    plot_area_div.appendChild(plot_display_first);
    plot_area_div.appendChild(plot_display_second);
    massive_info_div.appendChild(plot_area_div);
    
    //Creating input boxes
    var filename_input_box_1 = document.createElement("INPUT");
    filename_input_box_1.id = "filename_input_box_1";
    var filename_input_box_2 = document.createElement("INPUT");
    filename_input_box_2.id = "filename_input_box_2";
    var peptide_input_box_1 = document.createElement("INPUT");
    peptide_input_box_1.id = "peptide_input_box_1";
    var peptide_input_box_2 = document.createElement("INPUT");
    peptide_input_box_2.id = "peptide_input_box_2";
    var scan_input_box_1 = document.createElement("INPUT");
    scan_input_box_1.id = "scan_input_box_1";
    var scan_input_box_2 = document.createElement("INPUT");
    scan_input_box_2.id = "scan_input_box_2";
    
    var render_button_1 = document.createElement("button");
    var render_button_2 = document.createElement("button");
    
    render_button_1.onclick = function(){
        return plot_spectrum_parameter_construction("1");
    };
    render_button_2.onclick = function(){
        return plot_spectrum_parameter_construction("2");
    };
    
    render_button_1.innerHTML = "Plot1";
    render_button_2.innerHTML = "Plot2";
    
    massive_info_div.appendChild(filename_input_box_1);
    massive_info_div.appendChild(peptide_input_box_1);
    massive_info_div.appendChild(scan_input_box_1);
    massive_info_div.appendChild(render_button_1);
    
    massive_info_div.appendChild(filename_input_box_2);
    massive_info_div.appendChild(peptide_input_box_2);
    massive_info_div.appendChild(scan_input_box_2);
    massive_info_div.appendChild(render_button_2);
    
    //Buttons for Switching Left Panel
    var left_panel_pairs_button = document.createElement("button");
    left_panel_pairs_button.innerHTML = "Cosine Pairs";
    left_panel_pairs_button.onclick = function(){
        show_pairs_table();
    }
    massive_info_div.appendChild(left_panel_pairs_button);
    
    var left_panel_clusters_button = document.createElement("button");
    left_panel_clusters_button.innerHTML = "Clusters";
    left_panel_clusters_button.onclick = function(){
        show_allclusters_table();
    };
    massive_info_div.appendChild(left_panel_clusters_button);
    
    //Making Left Panel
    left_content_div = document.createElement("div");
    left_content_div.style.width = "450px";
    left_content_div.style.float = "left";
    left_content_div.id = "leftcontentdiv";
    
    //Pairs Data Table Render
    network_pairs_div = document.createElement("div");
    network_pairs_div.style.width = "450px";
    //network_pairs_div.style.float = "left";
    network_pairs_div.id = "NetworkPairsDiv";
    
    all_clusters_div = document.createElement("div");
    all_clusters_div.style.width = "450px";
    //all_clusters_div.style.float = "left";
    all_clusters_div.id = "AllClustersDiv";
    
    //Cluster Details
    cluster_details = document.createElement("div");
    cluster_details.style.width = "450px";
    //cluster_details.style.float = "left";
    cluster_details.id = "ClusterDetails";
    left_content_div.appendChild(cluster_details);
    
    left_content_div.appendChild(network_pairs_div);
    left_content_div.appendChild(all_clusters_div);
    
    
    //Making a right div to hold stuff
    right_content_div = document.createElement("div");
    right_content_div.style.width = "500px";
    right_content_div.style.float = "right";
    right_content_div.id = "rightcontentdiv";
    
    //DEBUG=================================
//     var debug_msgf_div = document.createElement("div");
//     var msgf_result_input = document.createElement("INPUT");
//     msgf_result_input.id = "msgf_result_input";
//     var msgf_get_result_button = document.createElement("button");
//     msgf_get_result_button.innerHTML = "GetMSGFResult";
//     msgf_get_result_button.onclick = function(){
//         //Constructing URL
//         var result_url = '/ProteoSAFe/result.jsp'
//         var task_id = $("#msgf_result_input")[0].value;
//         $.ajax({
//             url: result_url,
//             data: { task: task_id, view: 'group_by_spectrum' },
//             cache: false,
//             success: get_msgf_result_callback_gen(this)
//         });
//     };
//     
//     debug_msgf_div.appendChild(msgf_result_input);
//     debug_msgf_div.appendChild(msgf_get_result_button);
//     
//     this.renderdiv.appendChild(debug_msgf_div);
//     
    
    this.renderdiv.appendChild(massive_info_div);
    this.renderdiv.appendChild(left_content_div);
    this.renderdiv.appendChild(right_content_div);
    
    this.getOtherViewData();
}
    
function show_pairs_table(){
    $("#AllClustersDiv").hide();
    $("#NetworkPairsDiv").show();
}

function show_allclusters_table(){
    $("#NetworkPairsDiv").hide();
    $("#AllClustersDiv").show();
}


function get_networkpairs_tableXML(){
    var tableXML_str = '<block id="networkpairs" type="table"> \
                            <row>  \
                                <column type="powerview_plotbuttons" label="1"> \
                                    <parameter name="Scan" value="[Node1]"/> \
                                    <parameter name="FileName" value="[FileName]"/> \
                                    <parameter name="peptide" value="*..*"/> \
                                </column> \
                                <column field="Node1" label="Node" type="text" width="3"/> \
                                <column type="powerview_plotbuttons" label="2"> \
                                    <parameter name="Scan" value="[Node2]"/> \
                                    <parameter name="FileName" value="[FileName]"/> \
                                    <parameter name="peptide" value="*..*"/> \
                                </column> \
                                <column field="Node2" label="Node" type="text" width="3"/> \
                                <column field="Cos_Score" label="Cos" type="float" precision="2"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}


function get_allclusters_tableXML(){
    var tableXML_str = '<block id="allclusters" type="table"> \
                            <row>  \
                                <column type="powerview_plotbuttons" label="1"> \
                                    <parameter name="Scan" value="[cluster index]"/> \
                                    <parameter name="FileName" value="spectra/specs_ms.pklbin"/> \
                                    <parameter name="peptide" value="[LibraryID]"/> \
                                </column> \
                                <column field="cluster index" label="Cluster" type="text" width="3"/> \
                                <column field="number of spectra" label="Count" type="integer" width="3"/> \
                                <column field="LibraryID" label="ID" type="text" width="3"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_clusterinfo_tableXML(){
    var tableXML_str = '<block id="clusterinfo" type="table"> \
                            <row>  \
                                <column type="powerview_rightpanel_plotbuttons" label="1"> \
                                    <parameter name="Scan" value="[ScanNumber]"/> \
                                    <parameter name="FileName" value="[ProteosafeFilePath]"/> \
                                    <parameter name="peptide" value="*..*"/> \
                                </column> \
                                <column field="AllFiles" label="Filename" type="text" width="10"/> \
                                <column field="ScanNumber" label="Scan" type="text" width="10"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_networkneighbors_tableXML(){
    var tableXML_str = '<block id="clusterinfo" type="table"> \
                            <row>  \
                                <column type="powerview_rightpanel_plotbuttons" label="1"> \
                                    <parameter name="Scan" value="[Node2]"/> \
                                    <parameter name="FileName" value="[FileName]"/> \
                                    <parameter name="peptide" value="*..*"/> \
                                </column> \
                                <column field="Node2" label="Index" type="text" width="10"/> \
                                <column field="Cos_Score" label="Cos" type="float" precision="2"/> \
                                <column field="MzDiff" label="MzDiff" type="float" precision="2"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}

function plot_spectrum_call_backend(parameters, plotter) {
    var task = get_taskid();
    var type = "invoke";
    //var source  = "annotatedSpectrumImageThumbnail";
    var source  = "annotatedSpectrumImage";
    var contentType = "image/png";
    var block = 0;
    
    if(plotter == 1)
        display_specplot_image_powerview($("#PlotDisplay_1")[0], task, type, source, parameters, contentType, block);
    if(plotter == 2)
        display_specplot_image_powerview($("#PlotDisplay_2")[0], task, type, source, parameters, contentType, block);
}

function plot_spectrum_setparameters(filename, scan, peptide, plotter){
    filename_box_name = "#filename_input_box_" + plotter;
    peptide_box_name = "#peptide_input_box_" + plotter;
    scan_box_name = "#scan_input_box_" + plotter;
    
    $(filename_box_name).val(filename);
    $(peptide_box_name).val(peptide);
    $(scan_box_name).val(scan);
    
    plot_spectrum_parameter_construction(plotter);
}

function plot_spectrum_parameter_construction(plotter){
    var invokeParameters = {};
    filename_box_name = "#filename_input_box_" + plotter;
    peptide_box_name = "#peptide_input_box_" + plotter;
    scan_box_name = "#scan_input_box_" + plotter;
    
    invokeParameters["file"] = "FILE->" + $(filename_box_name).val();;
    invokeParameters["scan"] = + $(scan_box_name).val();;
    invokeParameters["peptide"] = $(peptide_box_name).val();;
    invokeParameters["force"] = "true";
    
    plot_spectrum_call_backend(invokeParameters, plotter);
}

ResultViewFilePowerView.prototype.updaterender = function() {
    
    

}

ResultViewFilePowerView.prototype.getOtherViewData = function() {
    var task_id = get_taskid();
    //Getting Pairs Info Page
    
    //Constructing URL
    var result_url = '/ProteoSAFe/result.jsp'
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_network_pairs', show: 'true'},
        cache: false,
        success: view_network_pairs_data_callback_gen(this)
    });
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_all_clusters_withID', show: 'true'},
        cache: false,
        success: view_all_clusters_data_callback_gen(this)
    });
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_all_annotations_DB', show: 'true'},
        cache: false,
        success: view_all_annotations_DB_callback_gen(this)
    });
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'overall_group_by_spectrum', show: 'true' },
        cache: false,
        success: get_proteomics_result_callback_gen(this)
    });
}



function view_network_pairs_data_callback_gen(view_object){
    return function(html){
        pairs_data = get_block_data_from_page(html);
        if(powerview_global_object == null){
            view_object.pairs_data = pairs_data;
            powerview_global_object = view_object;
        }
        else{
            powerview_global_object.pairs_data = pairs_data;
        }
        
        
        var task = new Object();
        task.id = "1234";
        task.workflow = "Pairs Table";
        task.description = "Pairs Table";
        var generic_table = new ResultViewTable(get_networkpairs_tableXML(), "network_pairs_id", task, 0);
        generic_table.setData(view_object.pairs_data);
        generic_table.render($("#NetworkPairsDiv")[0], 0);
    }
}

function view_all_clusters_data_callback_gen(view_object){
    return function(html){
        cluster_data = get_block_data_from_page(html);
        if(powerview_global_object == null){
            view_object.allclusters_data = cluster_data;
            powerview_global_object = view_object;
        }
        else{
            powerview_global_object.allclusters_data = cluster_data;
        }
        
        var task = new Object();
        task.id = "1234";
        task.workflow = "Clusters";
        task.description = "Clusters";
        var generic_table = new ResultViewTable(get_allclusters_tableXML(), "all_clusters_id", task, 0);
        generic_table.setData(cluster_data);
        generic_table.render($("#AllClustersDiv")[0], 0);
        $("#AllClustersDiv").hide();
    }
}

function view_all_annotations_DB_callback_gen(view_object){
    return function(html){
        all_annotations_DB_data = get_block_data_from_page(html);
        if(powerview_global_object == null){
            view_object.all_annotations_DB_data = all_annotations_DB_data;
            powerview_global_object = view_object;
        }
        else{
            powerview_global_object.all_annotations_DB_data = all_annotations_DB_data;
        }
    }
}

function get_proteomics_result_callback_gen(view_object){
    return function(html){
        peptide_results_data = get_block_data_from_page(html);
        if(powerview_global_object == null){
            view_object.peptide_results_data = peptide_results_data
            powerview_global_object = view_object;
        }
        else{
            powerview_global_object.peptide_results_data = peptide_results_data;
        }
    }
}





function get_block_data_from_page(html){
    var parsed_data =  $.parseHTML(html);
    var dom = $(html);
    var javascript_element_with_block_data  = dom.filter("#renderjsdata")[0];
    var lines_array = javascript_element_with_block_data.text.split("\n");
    
    var lines_array_truncated = lines_array.slice(3);
    var json_data = lines_array_truncated.join("");
    json_data = "[" + json_data;
    json_data = json_data.trim();
    json_data = json_data.substring(0, json_data.length - 1) ;
    j_object_datay = JSON.parse(json_data);
    
    return j_object_datay;
}

function get_block_data_from_xmlspec(xml){
    json_data = xml.getElementsByTagName("blockData")[0].childNodes[0].nodeValue;
    j_object_datay = JSON.parse(json_data);
    
    return j_object_datay;
}

// set data to the file streamer
ResultViewFilePowerView.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["powerview"] = ResultViewFilePowerView;