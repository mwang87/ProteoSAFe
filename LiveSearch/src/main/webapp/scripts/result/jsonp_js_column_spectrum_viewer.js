var jsonp_jsspectrumviewerColumnHandler = {
    render: renderJSONP_JSSpectrumViewer
};


columnHandlers["jsonp_jscolumnspectrumviewer"] = jsonp_jsspectrumviewerColumnHandler;


function renderJSONP_JSSpectrumViewer(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        //Adding font awesome CSS
        var link = document.createElement('link')
        link.setAttribute('rel', 'stylesheet')
        link.setAttribute('type', 'text/css')
        link.setAttribute('href', "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css")
        document.getElementsByTagName('head')[0].appendChild(link)
        
        td.id = getColumnElementId(tableId, record.id, rowId, columnId);
        if (attributes.colspan != null)
            td.colSpan = attributes.colspan;
        // get loader parameters
        var task = attributes.task.id;
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        var contentType = attributes.contentType;
        var height_str = attributes.height;
        var width_str = attributes.width;
        
        var height = 300;
        var width = 400;
        if(height_str != null){
            height = parseInt(height_str);
        }
        if(width_str != null){
            width = parseInt(width_str);
        }
        
        var label = attributes.label
        
        var block = attributes.blockId;
        // set up on-demand loader function
        var columnLoader = function(render_height, render_width, call_parameters, call_block, div, render_task, label_display) {
            return function(){
                removeChildren(td);
                
                if(label_display != null){
                    header = document.createElement("h3")
                    header.innerText = label_display
                    header.style.textAlign = "center"
                    header.style.marginTop = "3px"
                    header.style.marginBottom = "3px"
                    div.appendChild(header)
                }
                
                lorikeet_div = document.createElement("div")
                div.appendChild(lorikeet_div)
            
                displayJSONP_JSSpectrumViewer(
                    lorikeet_div, render_task, call_parameters, render_height , render_width, call_block);
            }
        }(height, width, invokeParameters, block, td, task, label);
        // if this column is already loaded, just invoke the loader function
        if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
            columnLoader();
        // otherwise, assign the loader function to this record,
        // so that it can be invoked when the column is loaded
        else tableManager.setColumnLoader(
            tableId, record.id, rowId, columnId, columnLoader);
    }
}



function displayJSONP_JSSpectrumViewer(div, task, parameters, panelHeight, panelWidth, block){
    var type = "invoke";
    //var source  = "annotatedSpectrumImageThumbnail";
    var source  = "annotatedSpectrumImageText";
    var contentType = "image/text";
    var block = 0;
    var invokeParameters = {};
    
    var url = "/ProteoSAFe/DownloadResultFile?invoke=annotatedSpectrumImageText"
    
    if(task.length > 5){
        url += "&task=" + task
    }
    
    if (block != null)
        url += "&block=" + block;
    
    
    url += "&jsonp=1"
    
    mod_format = "specnets"
    fixed_mods = new Array();
    
    render_peptide = "";
    
    if (parameters != null){
        for (var parameter in parameters){
            if(parameter == "spectrumid"){
                plot_library = true;
            }
            if(parameter == "forcefromfile"){
                force_from_file = true;
            }
            if(parameter == "peptide"){
                if(parameters["peptide"].length > 5){
                    render_peptide = parameters["peptide"]
                }
            }
            if(parameter == "modformat"){
                mod_format = parameters["modformat"]
                continue
            }
            if(parameter == "FIXEDMOD"){
                fixed_mod = parameters["FIXEDMOD"]
                aminoacid = fixed_mod[0]
                mass = parseFloat(fixed_mod.split(":")[1])
                
                mod_object = new Object()
                mod_object.modMass = mass
                mod_object.aminoAcid = aminoacid
                fixed_mods.push(mod_object);
                
                continue;
            }
            
            if(parameter == "dataset"){
                if(parameters["dataset"].length < 2){
                    continue
                }
            }
            
                
            if(parameter == "compoundname"){
                compound_name = parameters[parameter];
                if(compound_name.indexOf("Peptide: ") == 0){
                    peptide_sequence = compound_name.slice(9)
                    console.log("Peptide: " + peptide_sequence);
                    if(peptide_sequence.length < 3){
                        peptide_sequence = "*..*";
                    }
                    url += "&" + "peptide" + "=" + "*..*";
                }
                else{
                    if(parameters["peptide"] == null){
                        url += "&" + "peptide" + "=" + "*..*";
                    }
                }
            }
            else{
                if(parameter == "peptide"){
                    url += "&" + parameter + "=" + "*..*"
                    continue
                }
                
                url += "&" + parameter + "=" +
                    encodeURIComponent(parameters[parameter]);
            }
        }
    }
    
    removeChildren(div);
    var child = document.createElement("img");
    child.src = "/ProteoSAFe/images/inProgress.gif";
    div.appendChild(child);
    

    error_function = function(render_div){
        return function(){
            removeChildren(render_div);
            text_label = document.createElement("p");
            text_label.innerHTML = "Error Showing Spectrum";
            render_div.appendChild(text_label);
        }
    }(div)
    

    remote_server = parameters["remoteserver"]
    remote_url = remote_server + url
    
    
    success_jsonp_function = function(render_div, width, height, peptide_current){
        return function(response){
            removeChildren(render_div);
            var child = document.createElement("div");
            child.id = makeRandomString(10);
            
            peaklist = parseSpecplotPeaksToArray(response.data)
            
            text_box = document.createElement("input")
            text_box.id = makeRandomString(10)
            text_box.size = 70
            text_box.value = peptide_current
            text_box.placeholder = "Render Peptide"
            text_box.style.marginLeft = "162px"
            text_box.style.position = "relative"
            text_box.style.top = "-25px"
            
            //refresh_button = document.createElement("i");
            refresh_button = document.createElement("Button");
            //refresh_button.className = "fa fa-refresh"
            refresh_button.innerHTML = "Update Peptide"
            refresh_button.title = "Update Peptide"
            refresh_button.id = makeRandomString(10)
            //refresh_button.style.fontSize = "1.5em"
            //refresh_button.style.cursor =  "pointer"
            //refresh_button.style.marginLeft = "13px"
            refresh_button.style.position = "relative"
            refresh_button.style.top = "-24px"
            
            text_label = document.createElement("p");
            text_label.innerHTML = "Rendered Peptide:";
            text_label.style.left = "180px"
            text_label.style.position = "relative"
            text_label.style.top = "10px"
            
            refresh_button.onclick = function(peaklist, peptide_box_id, lorikeet_div_id){
                return function(){
                    $("#" + lorikeet_div_id).empty();
                    
                    render_peptide = $("#" + peptide_box_id)[0].value
                    
                    modification_struct = new Object();
                    if(mod_format == "specnets"){
                        modification_struct = get_peptide_modification_list_specnets_format(render_peptide)
                    }
                    if(mod_format == "inspect"){
                        modification_struct = get_peptide_modification_list_inspect_format(render_peptide)
                    }
                    stripped_peptide = modification_struct.render_peptide
                    all_modifications = modification_struct.all_modifications
                    nterm_mod_mass = modification_struct.nterm_mod_mass
                    
                    $("#" + lorikeet_div_id).specview( {
                        sequence: stripped_peptide, 
                        peaks: peaklist,
                        labelImmoniumIons: false,
                        width:width,
                        height:height,
                        showOptionsTable:true,
                        showIonTable:true,
                        showSequenceInfo:true,
                        variableMods: all_modifications,
                        staticMods: fixed_mods,
                        ntermMod: nterm_mod_mass
                    })
                }
            }(peaklist, text_box.id, child.id);
            
            top_panel_box = document.createElement("div")
            top_panel_box.id = makeRandomString(10)
            top_panel_box.style.width = "100%"
            top_panel_box.style.height = "30px"
            top_panel_box.style.backgroundColor = "#ADD8E6"
            
            top_panel_box.appendChild(text_label);
            top_panel_box.appendChild(text_box)
            top_panel_box.appendChild(refresh_button)
            div.appendChild(top_panel_box);
            div.appendChild(child);
            
            modification_struct = new Object();
            if(mod_format == "specnets"){
                modification_struct = get_peptide_modification_list_specnets_format(peptide_current)
            }
            if(mod_format == "inspect"){
                modification_struct = get_peptide_modification_list_inspect_format(peptide_current)
            }
            stripped_peptide = modification_struct.render_peptide
            all_modifications = modification_struct.all_modifications
            nterm_mod_mass = modification_struct.nterm_mod_mass
            
            $("#" + child.id).specview( {
                sequence: stripped_peptide, 
                peaks: peaklist,
                labelImmoniumIons: false,
                width:width,
                height:height,
                showOptionsTable:true,
                showIonTable:true,
                showSequenceInfo:true,
                variableMods: all_modifications,
                staticMods: fixed_mods,
                ntermMod: nterm_mod_mass
            })
        
        }
    }(div, panelWidth, panelHeight, render_peptide)
    
    $.ajax({
        url: remote_url,
        cache: true,
        timeout: 90000,
        jsonp: "callback",
        dataType: "jsonp",
        success: success_jsonp_function,
        error: error_function 
    });

}