
/**
 * Result view table column handler for streaming peaks to replace the specplot viewer
 */
var jsspectrumviewerColumnHandler = {
    render: renderJSSpectrumViewer
};


columnHandlers["jscolumnspectrumviewer"] = jsspectrumviewerColumnHandler;



function renderJSSpectrumViewer(tableId, rowId, columnId, attributes) {
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

                displayJSSpectrumViewer(
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


function displayJSSpectrumViewer(div, task, parameters, panelHeight, panelWidth, block){
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

    plot_library = false;
    force_from_file = false;
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



    if(plot_library && !force_from_file){
        var library_url = "/ProteoSAFe/SpectrumCommentServlet?SpectrumID=" + parameters.spectrumid
        $.ajax({
            url: library_url,
            cache: true,
            success: function(render_div, width, height){
                return function(response){
                    removeChildren(render_div);
                    var child = document.createElement("div");
                    child.id = makeRandomString(10);
                    render_div.appendChild(child);

                    spectrum_object = JSON.parse(response)
                    spectrum_info = spectrum_object.spectruminfo
                    peaks_str = spectrum_info.peaks_json

                    peaklist = JSON.parse(peaks_str)



                    $("#" + child.id).specview( {
                                sequence: "",
                                peaks: peaklist,
                                labelImmoniumIons: false,
                                width:width,
                                height:height,
                                showOptionsTable:false,
                                showIonTable:false,
                                showSequenceInfo:false
                            })

                }
            }(div, panelWidth, panelHeight),
            failure: function(div, panelWidth, panelHeight, url){
                return function(){
                    $.ajax({
                        url: url,
                        cache: true,
                        success: function(render_div, width, height){
                            return function(response){
                                removeChildren(render_div);
                                var child = document.createElement("div");
                                child.id = makeRandomString(10);
                                div.appendChild(child);

                                peaklist = parseSpecplotPeaksToArray(response)

                                $("#" + child.id).specview( {
                                            sequence: "",
                                            peaks: peaklist,
                                            labelImmoniumIons: false,
                                            width:width,
                                            height:height,
                                            showOptionsTable:false,
                                            showIonTable:false,
                                            showSequenceInfo:false
                                        })
                            }
                        }(div, panelWidth, panelHeight)
                    });
                }
            }(div, panelWidth, panelHeight, url)
        });
    }
    else{

        success_function = function(render_div, width, height, peptide_current){
            return function(response){
                removeChildren(render_div);
                var child = document.createElement("div");
                child.id = makeRandomString(10);

                peaklist = parseSpecplotPeaksToArray(response)

                //Sorting peak list to make sure masses are in ascending order
                peaklist.sort(function(a, b){return a[0]-b[0]});


                if(render_peptide.length > 0){
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
                    text_label.style.left = "50px"
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
                else{
                    div.appendChild(child);

                    $("#" + child.id).specview( {
                                sequence: "",
                                peaks: peaklist,
                                labelImmoniumIons: false,
                                width:width,
                                height:height,
                                showOptionsTable:false,
                                showIonTable:false,
                                showSequenceInfo:false
                            })
                }
            }
        }(div, panelWidth, panelHeight, render_peptide)

        error_function = function(render_div){
            return function(){
                removeChildren(render_div);
                text_label = document.createElement("p");
                text_label.innerHTML = "Error Showing Spectrum";
                render_div.appendChild(text_label);
            }
        }(div)

        retry_error_function = function(render_div, url, success_function, error_function){
            return function(){

                retry_function = function(render_div, url, success_function, error_function){
                    return function(){
                        new_url = url + "&force=true"
                        $.ajax({
                            url: new_url,
                            cache: true,
                            success: success_function,
                            error: error_function
                        });
                    }
                }(div, url, success_function, error_function)

                setTimeout(retry_function, 200)

            }
        }(div, url, success_function, error_function)

        if(parameters["jsonp"] == "1"){
            remote_server = parameters["remoteserver"]
            remote_url = remote_server + url


            success_jsonp_function = function(render_div, width, height, peptide_current){
                return function(response){
                    removeChildren(render_div);
                    var child = document.createElement("div");
                    child.id = makeRandomString(10);


                    peaklist = parseSpecplotPeaksToArray(response.data)

                    if(render_peptide.length > 0){
                        text_box = document.createElement("input")
                        text_box.id = makeRandomString(10)
                        text_box.size = 70
                        text_box.value = peptide_current
                        text_box.placeholder = "Render Peptide"
                        text_box.style.marginLeft = "30%"
                        text_box.style.position = "relative"
                        text_box.style.top = "-25px"

                        refresh_button = document.createElement("i");
                        refresh_button.className = "fa fa-refresh"
                        refresh_button.title = "Update Peptide"
                        refresh_button.id = makeRandomString(10)
                        refresh_button.style.fontSize = "1.5em"
                        refresh_button.style.cursor =  "pointer";
                        refresh_button.style.marginLeft = "13px";
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
                    else{
                        div.appendChild(child);

                        $("#" + child.id).specview( {
                                    sequence: "",
                                    peaks: peaklist,
                                    labelImmoniumIons: false,
                                    width:width,
                                    height:height,
                                    showOptionsTable:false,
                                    showIonTable:false,
                                    showSequenceInfo:false
                                })
                    }
                }
            }(div, panelWidth, panelHeight, render_peptide)

            $.ajax({
                url: remote_url,
                cache: true,
                jsonp: "callback",
                dataType: "jsonp",
                success: success_jsonp_function,
                error: error_function
            });
        }
        else{
            $.ajax({
                url: url,
                cache: true,
                success: success_function,
                error: retry_error_function
            });
        }


    }
}

/**
 * Result view table column handler for streaming peaks from the database
 */
var jsspectrumviewerLibraryColumnHandler = {
    render: renderJSLibrarySpectrumViewer
};


columnHandlers["jscolumnspectrumviewer_fromlibrary"] = jsspectrumviewerLibraryColumnHandler;

function renderJSLibrarySpectrumViewer(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
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
        var panelHeight = this.panelHeight;
        var block = attributes.blockId;
        // set up on-demand loader function
        var columnLoader = function() {
            removeChildren(td);


            displayJSLibrarySpectrumViewer(
                td, task, invokeParameters, 300 , 450, block);
        };
        // if this column is already loaded, just invoke the loader function
        if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
            columnLoader();
        // otherwise, assign the loader function to this record,
        // so that it can be invoked when the column is loaded
        else tableManager.setColumnLoader(
            tableId, record.id, rowId, columnId, columnLoader);
    }
}



function displayJSLibrarySpectrumViewer(div, task, parameters, panelHeight, panelWidth, block){
    var url = "/ProteoSAFe/SpectrumCommentServlet?SpectrumID=" + parameters.spectrumid

    removeChildren(div);
    var child = document.createElement("img");
    child.src = "/ProteoSAFe/images/inProgress.gif";
    div.appendChild(child);

    $.ajax({
            url: url,
            cache: true,
            success: function(render_div, width, height){
                return function(response){
                    removeChildren(render_div);
                    var child = document.createElement("div");
                    child.id = makeRandomString(10);
                    render_div.appendChild(child);

                    spectrum_object = JSON.parse(response)
                    spectrum_info = spectrum_object.spectruminfo
                    peaks_str = spectrum_info.peaks_json

                    peaklist = JSON.parse(peaks_str)

                    $("#" + child.id).specview( {
                                sequence: "",
                                peaks: peaklist,
                                labelImmoniumIons: false,
                                width:width,
                                height:height,
                                showOptionsTable:false,
                                showIonTable:false,
                                showSequenceInfo:false
                            })
                }
            }(div, panelWidth, panelHeight)
        });
}




/**
 * Result view table column handler for streaming peaks from the file and database to show comparison of peaks
 */
var jsspectrumviewerSpectrumComparisonColumnHandler = {
    render: renderJSSpectrumComparisonViewer
};


columnHandlers["jscolumnspectrumviewer_spectrum_comparison"] = jsspectrumviewerSpectrumComparisonColumnHandler;

function renderJSSpectrumComparisonViewer(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
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
        var panelHeight = this.panelHeight;
        var block = attributes.blockId;
        // set up on-demand loader function
        var columnLoader = function() {
            removeChildren(td);

            if(invokeParameters.show != null){
            	if(invokeParameters.show == "false"){
            		return;
            	}
            }

            var compare_spectra_button = document.createElement('input');
            compare_spectra_button.setAttribute('type','button');
            compare_spectra_button.setAttribute('name','Compare Spectra (Mirror Plot)');
            compare_spectra_button.setAttribute('value','Compare Spectra (Mirror Plot)');

            compare_spectra_button.onclick = function(td, task, invokeParameters, block){
                return function(){
                    displayJSSpectrumComparisonViewer(td, task, invokeParameters, 500 , 800, block);
                }
            }(td, task, invokeParameters, block)

            td.appendChild(compare_spectra_button)



        };
        // if this column is already loaded, just invoke the loader function
        if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
            columnLoader();
        // otherwise, assign the loader function to this record,
        // so that it can be invoked when the column is loaded
        else tableManager.setColumnLoader(
            tableId, record.id, rowId, columnId, columnLoader);
    }
}


/**
 * Parameters:
 *              spectrum1_type - lib or flatfile
 *              spectrum2_type - lib or flatfile
 *              spectrum1_file - <file path>
 *              spectrum2_file - <file path>
 *              spectrum1_scan - <scan number>
 *              spectrum2_scan - <scan number>
 *              spectrum1_spectrumid - <spectrumid>
 *              spectrum2_spectrumid - <spectrumid>
 *              spectrum1_optionalparam_PARAMNAME - <param value>
 *              spectrum2_optionalparam_PARAMNAME - <param value>
 *
    <column type="jscolumnspectrumviewer_spectrum_comparison" colspan="5">
            <parameter name="spectrum1_type"    value="flatfile"/>
            <parameter name="spectrum1_file"    file="spec/[internalFilename]"/>>
            <parameter name="spectrum1_scan"    value="[Scan#]"/>
            <parameter name="spectrum2_type"    value="flatfile"/>
            <parameter name="spectrum2_file"    file="specLib/mergedLib.mgf"/>>
            <parameter name="spectrum2_index"    value="[libIndex1]"/>
            <parameter name="display_peptide"    value="[Annotation]"/>
    </column>
 */
function displayJSSpectrumComparisonViewer(div, task, parameters, panelHeight, panelWidth, block){
    removeChildren(div);
    var child = document.createElement("img");
    child.src = "/ProteoSAFe/images/inProgress.gif";
    div.appendChild(child);

    spectrum1_peaks = new Object()
    spectrum2_peaks = new Object()

    data_ready_object = new Object()
    data_ready_object.spectrum1_ready = false
    data_ready_object.spectrum2_ready = false
    data_ready_object.display_peptide = parameters.display_peptide

    //Function to render once both pieces of data are available
    render_comparision_func = function(div, data_ready_object){
        removeChildren(div);
        var child = document.createElement("div");
        child.id = makeRandomString(10);
        div.appendChild(child);

        width = panelWidth
        height = panelHeight

        display_lorikeet_comparison_spectra(child.id, width, height, data_ready_object.spectrum1_peaks, data_ready_object.spectrum2_peaks, data_ready_object.display_peptide)
    }


    if(parameters.spectrum1_type == "lib"){
        url = "/ProteoSAFe/SpectrumCommentServlet?SpectrumID=" + parameters.spectrum1_spectrumid
    }
    else{
        url = "/ProteoSAFe/DownloadResultFile?invoke=annotatedSpectrumImageText"

        success_function = function(div, data_ready_object, render_comparision_func){
            return function(response){
                data_ready_object.spectrum1_ready = true
                peaklist = parseSpecplotPeaksToArray(response)
                data_ready_object.spectrum1_peaks = peaklist
                if(data_ready_object.spectrum2_ready == true){
                    render_comparision_func(div, data_ready_object)
                }

                console.log(peaklist);
            }
        }(div, data_ready_object, render_comparision_func)

        //Determining scan or index
        use_scan = true
        if(parameters.spectrum1_scan == null || parameters.spectrum1_scan == "-1"){
            use_scan = false
        }

        if(use_scan){
            $.ajax({
                url: url,
                cache: true,
                data: {
                    file: parameters.spectrum1_file,
                    scan: parameters.spectrum1_scan,
                    task: task,
                    block: 0,
                    peptide: "*..*"
                },
                success: success_function
            });
        }
        else{
            $.ajax({
                url: url,
                cache: true,
                data: {
                    file: parameters.spectrum1_file,
                    index: parameters.spectrum1_index,
                    task: task,
                    block: 0,
                    peptide: "*..*"
                },
                success: success_function
            });
        }
    }

    if(parameters.spectrum2_type == "lib"){
        url = "/ProteoSAFe/SpectrumCommentServlet?SpectrumID=" + parameters.spectrum2_spectrumid

        success_function = function(div, data_ready_object,render_comparision_func){
            return function(response){
                data_ready_object.spectrum2_ready = true
                spectrum_object = JSON.parse(response)
                spectrum_info = spectrum_object.spectruminfo
                peaks_str = spectrum_info.peaks_json
                peaklist = JSON.parse(peaks_str)
                data_ready_object.spectrum2_peaks = peaklist
                if(data_ready_object.spectrum1_ready == true){
                    render_comparision_func(div, data_ready_object)
                }

                console.log(peaklist);
            }
        }(div, data_ready_object, render_comparision_func)

        $.ajax({
            url: url,
            cache: true,
            data: {
                SpectrumID: parameters.spectrum2_spectrumid
            },
            success: success_function
        })

    }
    else{
        url = "/ProteoSAFe/DownloadResultFile?invoke=annotatedSpectrumImageText"

        success_function = function(div, data_ready_object, render_comparision_func){
            return function(response){
                data_ready_object.spectrum2_ready = true
                peaklist = parseSpecplotPeaksToArray(response)
                data_ready_object.spectrum2_peaks = peaklist
                if(data_ready_object.spectrum1_ready == true){
                    render_comparision_func(div, data_ready_object)
                }
            }
        }(div, data_ready_object, render_comparision_func)

        //Determining scan or index
        use_scan = true
        if(parameters.spectrum2_scan == null || parameters.spectrum2_scan == "-1"){
            use_scan = false
        }

        if(use_scan){
            $.ajax({
                url: url,
                cache: true,
                data: {
                    file: parameters.spectrum2_file,
                    scan: parameters.spectrum2_scan,
                    task: task,
                    block: 0,
                    peptide: "*..*"
                },
                success: success_function
            });
        }
        else{
            $.ajax({
                url: url,
                cache: true,
                data: {
                    file: parameters.spectrum2_file,
                    index: parameters.spectrum2_index,
                    task: task,
                    block: 0,
                    peptide: "*..*"
                },
                success: success_function
            });
        }
    }
}

function display_lorikeet_comparison_spectra(div_name, width, height, peaklist_primary, peaklist_secondary, display_peptide){
    max_peak_in_spectrum1 = 0
    for(i in peaklist_primary){
        if(peaklist_primary[i][1] > max_peak_in_spectrum1){
            max_peak_in_spectrum1 = peaklist_primary[i][1]
        }
    }

    max_peak_in_spectrum2 = 0
    for(i in peaklist_secondary){
        if(peaklist_secondary[i][1] > max_peak_in_spectrum2){
            max_peak_in_spectrum2 = peaklist_secondary[i][1]
        }
    }

    primary_peaks_rescaled = new Array()
    for(i in peaklist_primary){
        primary_peaks_rescaled.push([peaklist_primary[i][0], (peaklist_primary[i][1] / max_peak_in_spectrum1)])
    }

    comparison_peaks = new Array()
    for(i in peaklist_secondary){
        comparison_peaks.push([peaklist_secondary[i][0], (peaklist_secondary[i][1] / max_peak_in_spectrum2)*-1])
    }



    stripped_peptide = ""
    all_modifications = new Array()
    nterm_mod_mass = 0.0
    if(display_peptide != null){
        modification_struct = get_peptide_modification_list_specnets_format(display_peptide)
        stripped_peptide = modification_struct.render_peptide
        all_modifications = modification_struct.all_modifications
        nterm_mod_mass = modification_struct.nterm_mod_mass
    }

    lorikeet_parameters =  {
        sequence: stripped_peptide,
        peaks: primary_peaks_rescaled,
        labelImmoniumIons: false,
        width:width,
        height:height,
        showOptionsTable:false,
        showIonTable:false,
        showSequenceInfo:false,
        extraPeakSeries: [{data: comparison_peaks, color: "#009900",labelType: ''}],
        variableMods: all_modifications,
        ntermMod: nterm_mod_mass
    }

    if(display_peptide != null){
        lorikeet_parameters.showIonTable = true
        lorikeet_parameters.showSequenceInfo = true
        lorikeet_parameters.showOptionsTable = true
    }



    $("#" + div_name).specview(lorikeet_parameters)
}
