function getSpectrumPeaks(task, parameters, function_callback){
    var type = "invoke";
    var source  = "annotatedSpectrumImageText";
    var contentType = "image/text";
    var block = 0;
    var invokeParameters = {};
    
    var url = "/ProteoSAFe/DownloadResultFile?task=" + task + "&invoke=annotatedSpectrumImageText"
    
    if (block != null)
        url += "&block=" + block;
    
    plot_library = false;
    force_from_file = "false";
    
    
    if (parameters != null){
        for (var parameter in parameters){
            if(parameter == "spectrumid"){
                plot_library = true;
            }
            if(parameter == "force"){
                if(parameters[parameter] == "true"){
                    force_from_file = "true";
                }
                continue;
            }
            else{
                url += "&" + parameter + "=" +
                    encodeURIComponent(parameters[parameter]);
            }
        }
    }
    
    $.ajax({
        url: url,
        cache: false,
        data: {force: force_from_file},
        success: function(function_callback){
            return function(response){
                peaklist = parseSpecplotPeaksToArray(response)
                if(function_callback != null){
                    function_callback(peaklist);
                }
            }
        }(function_callback),
        error: function(function_callback, url){
            return function(){
                console.log("plotting failed, we should try again with force turned on")

                $.ajax({
                    url: url,
                    cache: false,
                    data: {force: "true"},
                    success: function(function_callback){
                        return function(response){
                            peaklist = parseSpecplotPeaksToArray(response)
                            if(function_callback != null){
                                function_callback(peaklist);
                            }
                        }
                    }(function_callback),
                    error: function(){
                        console.log("plotting failed, really sucks")
                    }
                });
            }
        }(function_callback, url)
    });
}

function displayJSLibrarySpectrumViewer_Standalone(div, panelHeight, panelWidth, peaks, extraSeries, peptide){
    removeChildren(div);
    var child = document.createElement("div");
    child.id = makeRandomString(10);
    div.appendChild(child);
    
    /*render_peptide = ""
    
    if(peptide != null){
        if(peptide.length > 5){
            //Cleaning up Peptide
            render_peptide = peptide
            if(peptide.indexOf(".") != -1){
                render_peptide = peptide.substring(2, peptide.length - 2);
            }
        }
    }
    
    //Getting the mods from the specnets string
    //count number of mods
    number_of_mods = (render_peptide.match(/\(/g) || []).length;
    all_modifications = []
    for(i = 0; i < number_of_mods; i++){
        mod_start = render_peptide.indexOf("(")
        mod_end = render_peptide.indexOf(")")
        mod_mass_start = render_peptide.indexOf(",")
        mod_aminoacid = render_peptide[mod_start + 1]
        mod_mass = parseFloat(render_peptide.slice(mod_mass_start + 1, mod_end))
        all_modifications.push({index:mod_start, modMass:mod_mass, aminoAcid: mod_aminoacid});
        render_peptide = render_peptide.slice(0, mod_start) + mod_aminoacid + render_peptide.slice(mod_end + 1)
    }*/
    
    modification_struct = get_peptide_modification_list_specnets_format(peptide)
    render_peptide = modification_struct.render_peptide
    all_modifications = modification_struct.all_modifications
    nterm_mod_mass = modification_struct.nterm_mod_mass
    
    show_panels_flag = false
    
    
    if(extraSeries == null){
        $("#" + child.id).specview( {
            sequence: render_peptide, 
            peaks: peaks,
            labelImmoniumIons: false,
            width:panelWidth,
            height:panelHeight,
            showOptionsTable:show_panels_flag,
            showIonTable:show_panels_flag,
            showSequenceInfo:show_panels_flag,
            variableMods: all_modifications,
            zoomType: "xy",
            ntermMod: nterm_mod_mass
        })
    }
    else{
        $("#" + child.id).specview( {
            sequence: render_peptide, 
            peaks: peaks,
            labelImmoniumIons: false,
            width:panelWidth,
            height:panelHeight,
            showOptionsTable:show_panels_flag,
            showIonTable:show_panels_flag,
            showSequenceInfo:show_panels_flag,
            extraPeakSeries: extraSeries,
            variableMods: all_modifications,
            zoomType: "xy",
            ntermMod: nterm_mod_mass
        })
    }
}


function displayJSLibrarySpectrumViewer_Standalone_divname(div_name, panelHeight, panelWidth, peaks, extraSeries, peptide, show_panel){
    
    var child = document.createElement("div");
    child.id = makeRandomString(10);
    $("#" + div_name).append(child);
    
    
    modification_struct = get_peptide_modification_list_specnets_format(peptide)
    render_peptide = modification_struct.render_peptide
    all_modifications = modification_struct.all_modifications
    nterm_mod_mass = modification_struct.nterm_mod_mass
    
    show_panels_flag = false
    
    if(show_panel != null){
    	show_panels_flag = true
    }
    
    if(extraSeries == null){
        $("#" + child.id).specview( {
            sequence: render_peptide, 
            peaks: peaks,
            labelImmoniumIons: false,
            width:panelWidth,
            height:panelHeight,
            showOptionsTable:show_panels_flag,
            showIonTable:show_panels_flag,
            showSequenceInfo:show_panels_flag,
            variableMods: all_modifications,
            ntermMod: nterm_mod_mass
        })
    }
    else{
        $("#" + child.id).specview( {
            sequence: render_peptide, 
            peaks: peaks,
            labelImmoniumIons: false,
            width:panelWidth,
            height:panelHeight,
            showOptionsTable:show_panels_flag,
            showIonTable:show_panels_flag,
            showSequenceInfo:show_panels_flag,
            extraPeakSeries: extraSeries,
            variableMods: all_modifications,
            ntermMod: nterm_mod_mass
        })
    }
}

function get_peptide_modification_list_specnets_format(peptide){
    return_value = new Object()
    
    render_peptide = ""
    if(peptide != null){
        if(peptide.length > 5){
            //Cleaning up Peptide
            render_peptide = peptide
            if(peptide[1] == '.' && peptide[peptide.length - 2] == '.'){
                render_peptide = peptide.substring(2, peptide.length - 2);
            }
        }
    }
    
    nterm_mod_mass = 0.0
    //Looking for n-term mod
    if(render_peptide[0] == "["){
        //Looking for ending bracket
        for(i = 0; i < render_peptide.length; i++){
            if(render_peptide[i] == "]"){
                nterm_mod_mass = parseFloat(render_peptide.slice(1, i))
                render_peptide = render_peptide.slice(i+1)
                break;
            }
        }
    }
    
    //Getting the mods from the specnets string
    //count number of mods
    number_of_mods = (render_peptide.match(/\(/g) || []).length;
    all_modifications = []
    for(i = 0; i < number_of_mods; i++){
        mod_start = render_peptide.indexOf("(")
        mod_end = render_peptide.indexOf(")")
        mod_mass_start = render_peptide.indexOf(",")
        mod_aminoacid = render_peptide[mod_start + 1]
        mod_mass = parseFloat(render_peptide.slice(mod_mass_start + 1, mod_end))
        all_modifications.push({index:mod_start+1, modMass:mod_mass, aminoAcid: mod_aminoacid});
        render_peptide = render_peptide.slice(0, mod_start) + mod_aminoacid + render_peptide.slice(mod_end + 1)
    }
    
    
    
    return_value.render_peptide = render_peptide
    return_value.all_modifications = all_modifications
    return_value.nterm_mod_mass = nterm_mod_mass
    
    return return_value
}

function get_peptide_modification_list_inspect_format(peptide){
    return_value = new Object()
    
    render_peptide = ""
    if(peptide != null){
        if(peptide.length > 5){
            //Cleaning up Peptide
            render_peptide = peptide
            if(peptide.indexOf(".") == 1){
                render_peptide = peptide.substring(2, peptide.length - 2);
            }
        }
    }
    
    //Getting the mods from the specnets string
    //count number of mods
    //number_of_mods = (render_peptide.match(/\+/g) || render_peptide.match(/\-/g) || []).length;
    number_of_mods = 0
    for(i = 0; i < peptide.length; i++){
    	if(peptide[i] == "+" || peptide[i] == "-"){
    		number_of_mods++;
    	}
    }
    
    all_modifications = []
    nterm_mod_mass = 0.0
    for(i = 0; i < number_of_mods; i++){
        mod_start_plus = render_peptide.indexOf("+") - 1
        mod_start_minus = render_peptide.indexOf("-") - 1
        
        mod_start = mod_start_plus
        
        if(mod_start_plus == -2){
        	mod_start = mod_start_minus
        }
        
        if(mod_start_minus != -2 && mod_start_minus < mod_start_plus){
            mod_start = mod_start_minus
        }
        
        
        
        
        mod_end = render_peptide.length
        for(k = mod_start+1; k < render_peptide.length; k++){
            if(render_peptide[k].match(/[A-Z]/i) != null){
                mod_end = k
                break
            }
        }
        
        if(mod_start == -1){
        	//nterm mod
        	mod_mass_start = mod_start + 1
        	nterm_mod_mass = parseFloat(render_peptide.slice(mod_mass_start, mod_end))
        	render_peptide = render_peptide.slice(mod_end)
        	continue;
        }
        
        mod_mass_start = mod_start + 1
        mod_aminoacid = render_peptide[mod_start]
        mod_mass = parseFloat(render_peptide.slice(mod_mass_start, mod_end))
        all_modifications.push({index:mod_start+1, modMass:mod_mass, aminoAcid: mod_aminoacid});
        render_peptide = render_peptide.slice(0, mod_start) + mod_aminoacid + render_peptide.slice(mod_end)
    }
    
    return_value.render_peptide = render_peptide
    return_value.all_modifications = all_modifications
    return_value.nterm_mod_mass = nterm_mod_mass
    
    return return_value
}

function calculateStructureSimilarity(structure1, structure2, callbackfunction){
    $.ajax({
        url: "http://ccms-support.ucsd.edu:5000/structure_similarity/smiles.jsonp",
    
        // The name of the callback parameter, as specified by the YQL service
        jsonp: "callback",
    
        // Tell jQuery we're expecting JSONP
        dataType: "jsonp",
    
        // Tell YQL what we want and that we want JSON
        data: {
            structure1: (structure1),
            structure2: (structure2)
        },
    
        // Work with the response
        success: function( response ) {
            callbackfunction(response)
        }
    });
}
