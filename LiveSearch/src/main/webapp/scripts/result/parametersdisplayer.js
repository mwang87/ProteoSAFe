/**
 * File stream result view block implementation
 */
// constructor
function ResultViewParametersDisplayer(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}


// initialize block from XML specification
ResultViewParametersDisplayer.prototype.init = function(blockXML) {
}


// render the streamed file
ResultViewParametersDisplayer.prototype.render = function(div, index) {
    if (div != null)
        this.div = div;
    if (this.div == null) {
        alert("No div was provided under which to render this result block.");
        return;
    }
    
    result_url = "/ProteoSAFe/result.jsp"
    task_id = get_taskid()
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'parameters_xml_grabber', show: 'true'},
        cache: false,
        success: function(render_div){
            return function(html){
                //Get the actual URL to query the file
                var parsed_data =  $.parseHTML(html);
                var dom = $(html);
                var javascript_element_with_block_data  = dom.filter("#renderjsdata")[0];
                var lines_array = javascript_element_with_block_data.text.split("\n");
                
                var lines_array_truncated = lines_array[2];
                
                
                data = lines_array_truncated.split("=")[1]
                
                data = data.replace("\"", "")
                data = data.replace(" ", "")
                data = data.replace(";", "")
                data = data.replace("\"", "")
                
                streamed_url = "/ProteoSAFe/DownloadResultFile"
                
                $.ajax({
                    url: streamed_url,
                    data: { task: task_id, file: data, block: 'main'},
                    cache: false,
                    success: function(render_div){
                        return function(value){
                            all_parameters = parseXML(value).getElementsByTagName("parameter")
                            
                            var tableXML_str = '<block id="random" type="table"> \
                                    <row> \
                                        <column field="Parameter" label="Parameter" type="text" width="5"/> \
                                        <column field="Value" label="Value" type="text" width="5"/> \
                                    </row> \
                                </block>' ;
                            var tableXML = (new window.DOMParser() ).parseFromString(tableXML_str, "text/xml");
                            console.log(all_parameters);
                            parameters_map = new Object();
                            
                            for(var i = 0; i < all_parameters.length; i++){
                                console.log(all_parameters[i])
                                parameter_name = all_parameters[i].attributes.name.value
                                if(parameter_name != null){
                                    if( !(parameter_name in parameters_map)){
                                        parameters_map[parameter_name] = [];
                                    }
                                    parameters_map[parameter_name].push(all_parameters[i].childNodes[0].textContent)
                                }
                            }
                            
                            ///Writing Table Data
                            parameters_to_view_list = ["PAIRS_MIN_COSINE", "ANALOG_SEARCH", "tolerance.PM_tolerance", "tolerance.Ion_tolerance", "MIN_MATCHED_PEAKS", "TOPK", "CLUSTER_MIN_SIZE", "MAXIMUM_COMPONENT_SIZE", "MIN_PEAK_INT", "FILTER_STDDEV_PEAK_INT", 
                                                            "RUN_MSCLUSTER", "FILTER_PRECURSOR_WINDOW", "FILTER_LIBRARY", "WINDOW_FILTER", "SCORE_THRESHOLD", "MIN_MATCHED_PEAKS_SEARCH", "MAX_SHIFT_MASS"]
                            var parameters_data = [];
                            for(i = 0; i < parameters_to_view_list.length; i++){
                                parameters_data.push( {"Parameter" : parameters_to_view_list[i], "Value":parameters_map[parameters_to_view_list[i]][0]})
                            }
                      
                            
                            //parameters_data.push({"Parameter": "PAIRS_MIN_COSINE", "Value":"Value"})
                            
                            var task = new Object();
                            task.id = "1234";
                            task.workflow = "Parameters Table";
                            task.description = "Parameters Table";
                            var generic_table = new ResultViewTableGen(tableXML, "parameters", task, 0);
                            generic_table.setData(parameters_data);
                            generic_table.render(render_div, 0);
                            
                            
                            networking_written_div = document.createElement("div");
                            written_networking = "A molecular network was created using the online workflow at GNPS. "
                            
                            if( parseFloat(parameters_map["FILTER_PRECURSOR_WINDOW"][0]) > 0){
                                written_networking += "The data was filtered by removing all MS/MS peaks within +/- 17 Da of the precursor m/z. "
                            }
                            if( parseFloat(parameters_map["WINDOW_FILTER"][0]) > 0){
                                written_networking += "MS/MS spectra were window filtered by choosing only the top 6 peaks in the +/- 50Da window throughout the spectrum. "
                            }
                            if( parameters_map["RUN_MSCLUSTER"][0] == "on" ){
                                written_networking += "The data was then clustered with MS-Cluster with a parent mass tolerance of " +
                                                    parameters_map["tolerance.PM_tolerance"][0] + " Da and a MS/MS fragment ion tolerance of " + parameters_map["tolerance.Ion_tolerance"][0] +
                                                    " Da to create consensus spectra . Further, concensus spectra that contained less than " + parameters_map["CLUSTER_MIN_SIZE"][0] + " spectra" + 
                                                    " were discarded. " 
                            }
                            
                            written_networking += "A network was then created where edges were filtered to have a cosine score above " + parameters_map["PAIRS_MIN_COSINE"][0] + " and more than " + parameters_map["MIN_MATCHED_PEAKS"][0] +
                                                    " matched peaks. Further edges between two nodes were kept in the network if and only if each of the nodes appeared in each other's respective top " + parameters_map["TOPK"][0] + 
                                                    " most similar nodes. "
                                                    
                            
                            written_networking += "The spectra in the network were then searched against GNPS's spectral libraries. "
                            if( parameters_map["FILTER_LIBRARY"][0] == "1" ){
                                written_networking += " The library spectra were filtered in the same manner as the input data. "
                            }
                            
                            written_networking += " All matches kept between network spectra and library spectra were required to have a score above " + parameters_map["SCORE_THRESHOLD"][0] + " and at least " + parameters_map["MIN_MATCHED_PEAKS_SEARCH"][0] +
                                                    " matched peaks. "
                                                    
                            if( parameters_map["ANALOG_SEARCH"][0] == "1" ){
                                written_networking += " Analog search was enabled against the librar with a maximum mass shift of  " + parameters_map["MAX_SHIFT_MASS"][0] + " Da. "
                            }
                            
                            networking_written_div.innerHTML = written_networking
                            
                            render_div.appendChild(document.createElement("br"))
                            render_div.appendChild(document.createElement("br"))
                            
                            
                            render_div.appendChild(networking_written_div)
                            
                        }
                    }(render_div)
                })
                    
                
            }
        }(div)
    });
    
}


ResultViewParametersDisplayer.prototype.renderParameters = function(div){
    
}

// set data to the file streamer
ResultViewParametersDisplayer.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["parametersdisplayer"] = ResultViewParametersDisplayer;