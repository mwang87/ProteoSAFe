/**
 * Widget for Displaying Ratings
 *
 * @param {rating}
 * @param {maxrating}
 */
function raty_display_rating_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }

        rating = parseFloat(invokeParameters["rating"])
        maxrating = parseInt(invokeParameters["maxrating"])
        rating_div = document.createElement("div");
        rating_div.style.width = "100px"
        div_id_name = "rating" + rowId + "_" + columnId + "_" + index + "_" + makeRandomString(5)
        rating_div.id = div_id_name

        td.appendChild(rating_div);

        $("#" + div_id_name).raty({path: '/ProteoSAFe/images/plugins', numberMax: maxrating, score: rating, half: true, readOnly: true})
    }
}

var ratydisplayratingColumnHandler = {
    render: raty_display_rating_renderstream,
    sort: plainSorter
};

columnHandlers["ratydisplayrating"] = ratydisplayratingColumnHandler;



//Widget to Count the number of Total Compounds For Continuous ID
//Parameters: task_id, view_name
function display_continuous_identifications_summary_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        invokeParameters = resolve_parameters_to_map(attributes.parameters, record)

        task_id = (invokeParameters["task_id"])
        view_name = (invokeParameters["view_name"])

        summary_div = document.createElement("div");

        result_url = '/ProteoSAFe/result.jsp';

        $.ajax({
            url: result_url,
            data: { task: task_id, view: view_name, show: 'true'},
            cache: false,
            success: continuous_all_results_summary_callback_gen(summary_div)
        });

        td.appendChild(summary_div)

    }
}

function continuous_all_results_summary_callback_gen(result_object){
    return function(html){
        results_data = get_block_data_from_page(html);
        result_object.textContent = results_data.length
    }
}

var displayContinuousTaskSummaryColumnHandler = {
    render: display_continuous_identifications_summary_renderstream,
    sort: plainSorter
};

columnHandlers["displaycontinuoustasksummary"] = displayContinuousTaskSummaryColumnHandler;

//Widget for Generic URL Generation
function renderGenericURLGenerator(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        //Getting out the URL Prefix
        url_parameter_prefix = "URLBASE"
        parameter_prefix = "REQUESTPARAMETER="
        urlhash_prefix = "HASHPARAMTER="
        label_prefix = "LABEL"
        usetask_prefix = "USETASK"
        ignore_value_prefix = "IGNORE_VALUE"
        value_to_consider_prefix = "VALUE_TO_CONSIDER"
        open_same_page_prefix = "DISABLE_NEW_TAB"

        values_to_ignore = new Array();
        for (var parameter in parameters){
            if(parameter == ignore_value_prefix){
                values_to_ignore.push( parameters[parameter] )
            }
        }


        url = ""
        //Looking for url base
        for (var parameter in parameters){
            if(parameter == url_parameter_prefix){
                url += parameters[parameter]
                break;
            }
        }

        //Adding on request parameters
        url += "?"
        for (var parameter in parameters){
            if(parameter.indexOf(parameter_prefix) == 0){
                url += parameter.slice(parameter_prefix.length) + "=" + parameters[parameter] + "&"
            }
        }

        //Looking if we should include task
        for (var parameter in parameters){
            if(parameter == usetask_prefix){
                url += "task=" + get_taskid();
                break;
            }
        }

        //Adding Hash parameters
        url += "#"
        hash_parameter = "{"
        hash_parameter_count = 0
        for (var parameter in parameters){
            if(parameter.indexOf(urlhash_prefix) == 0){
                hash_parameter_count += 1
                if(hash_parameter_count == 1){
                    hash_parameter += "\"" + parameter.slice(urlhash_prefix.length) + "\"" + ":" + "\"" + parameters[parameter] + "\""
                }
                else{
                    hash_parameter += ",\"" + parameter.slice(urlhash_prefix.length) + "\"" + ":" + "\"" + parameters[parameter] + "\""
                }
            }
        }
        hash_parameter += "}"
        url += encodeURIComponent(hash_parameter)


        //Looking for url base
        label = "LABEL"
        for (var parameter in parameters){
            if(parameter == label_prefix){
                label = parameters[parameter]
                break;
            }
        }

        if(parameters[value_to_consider_prefix] != null){
            if(values_to_ignore.indexOf(parameters[value_to_consider_prefix]) != -1){
                label = "";
            }
        }

        //Checking if we should open same tab or new tab
        open_new_tab = true
        if(parameters[open_same_page_prefix] != null){
            open_new_tab = false
        }


        var json_link = document.createElement("a");
        json_link.href = url;
        json_link.innerHTML = label;
        if(open_new_tab == true){
            json_link.target = "_blank"
        }
        td.appendChild(json_link);
    }
}

var genericURLGeneratorColumnHandler = {
    render: renderGenericURLGenerator,
    sort: plainSorter,
    filter: plainFilter
};

columnHandlers["genericurlgenerator"] = genericURLGeneratorColumnHandler;



//Widget for showing structures
function renderStructureImageDisplayer(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        smiles_structure_paramname = "smilesstructure"

        smiles_structure = parameters[smiles_structure_paramname]

        width = "350"
        height = "250"
        cache_flag = "2"

        if(smiles_structure != null){
            if(smiles_structure != "N/A" && smiles_structure.length > 3){
                image_element = document.createElement("img")
                structure_url_prefix = "http://ccms-support.ucsd.edu:5000/smilesstructure?smiles=" + encodeURIComponent(smiles_structure) + "&width=" + width + "&height=" + height + "cache=" + cache_flag
                image_element.src = structure_url_prefix
                td.appendChild(image_element)
            }
        }
    }
}



var structureImageDisplayer = {
    render: renderStructureImageDisplayer,
    sort: plainSorter
};

columnHandlers["structuredisplayer"] = structureImageDisplayer;


//Widget for linking out to twitter to tweet dataset
function renderTwitterDataset(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        task_name = parameters["TASK"]
        dataset_id = parameters["DATASET"]
        url_base = parameters["URLBASE"]
        default_tweet_text = parameters["TWEETTEXT"]

        url = url_base + "?task=" + task_name + "&view=group_all_annotations"

        tweet_text = default_tweet_text + " " + dataset_id

        var json_link = document.createElement("div");
        div_id_name = "twitter_" + makeRandomString(5)
        json_link.id = div_id_name;
        json_link.style.display = "inline-block"
        json_link.style.width = "54px"

        td.appendChild(json_link);

        twttr.widgets.createShareButton(
            url,
            document.getElementById(div_id_name),
            {
                count: 'none',
                text: tweet_text,
                hashtag: "GNPS",
                url: url
            }).then(function (el) {
                console.log("Button created.")
            });

        var facebook_hyperlink = document.createElement("a")
        var facebook_link = document.createElement("img");
        facebook_link.src = '/ProteoSAFe/images/plugins/facebook/sharebutton.png'
        facebook_link.style.height = "21px"
        facebook_link.style.marginLeft = "3px"

        facebook_hyperlink.href = 'https://www.facebook.com/sharer/sharer.php?u=' + encodeURIComponent(url)
        facebook_hyperlink.target = "_blank"
        facebook_hyperlink.appendChild(facebook_link)

        td.appendChild(facebook_hyperlink)

        td.style.width = "120px"
    }
}



var twitterDatasetDisplayer = {
    render: renderTwitterDataset,
    sort: plainSorter
};

columnHandlers["twitterdataset"] = twitterDatasetDisplayer;



//Widget for linking to a dataset, and displaying things
function renderDatasetFieldWithMassiveID(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        dataset_id = parameters["DATASET"]
        displayfield = parameters["DISPLAYFIELD"]


        get_datasetinformation_url = "/ProteoSAFe/MassiveServlet"

        $.ajax({
            url: get_datasetinformation_url,
            data: { massiveid: dataset_id, function: "massiveinformation"},
            cache: false,
            success: function(div, field){
                return function(result){
                    datasetinformation = JSON.parse(result);
                    url_base = "/ProteoSAFe/dataset_id_redirect.jsp"
                    var linkout = document.createElement("a")
                    linkout.target = "_blank"
                    linkout.href = url_base + "?massiveid=" + datasetinformation.dataset_id
                    linkout.textContent = datasetinformation[field]
                    div.appendChild(linkout)
                }
            }(td, displayfield)
        });

        td.style.width = "600px"


//         var json_link = document.createElement("div");
//         div_id_name = "twitter_" + makeRandomString(5)
//         json_link.id = div_id_name;
//         json_link.style.display = "inline-block"
//         json_link.style.width = "54px"
//
//         td.appendChild(json_link);
//
//         twttr.widgets.createShareButton(
//             url,
//             document.getElementById(div_id_name),
//             {
//                 count: 'none',
//                 text: tweet_text,
//                 hashtag: "GNPS",
//                 url: url
//             }).then(function (el) {
//                 console.log("Button created.")
//             });
//
//         var facebook_hyperlink = document.createElement("a")
//         var facebook_link = document.createElement("img");
//         facebook_link.src = '/ProteoSAFe/images/plugins/facebook/sharebutton.png'
//         facebook_link.style.height = "21px"
//         facebook_link.style.marginLeft = "3px"
//
//         facebook_hyperlink.href = 'https://www.facebook.com/sharer/sharer.php?u=' + encodeURIComponent(url)
//         facebook_hyperlink.target = "_blank"
//         facebook_hyperlink.appendChild(facebook_link)
//
//         td.appendChild(facebook_hyperlink)
//
//         td.style.width = "120px"
    }
}



var datasetFieldWithMassiveIDDisplayer = {
    render: renderDatasetFieldWithMassiveID,
    sort: plainSorter
};

columnHandlers["datasetFieldWithMassiveID"] = datasetFieldWithMassiveIDDisplayer;




//Widget for linking out to twitter to tweet dataset
function renderPeptideExplorerComponentLinkout(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        peptide = parameters.peptide
        variants = JSON.parse(parameters.variants)
        components = JSON.parse(parameters.components)

        taskid = get_taskid()

        url = "/ProteoSAFe/result.jsp?"
        url += "&task=" + taskid
        url += "&view=" + "network_components_peptide"

        if(components[0] != -1){
            hash_parameter = "#{\"AllPeptides_input\":\"" + variants.join("||") + "\"}"

            url_link = document.createElement("a")
            url_link.target = "_blank"
            url_link.href = url + hash_parameter
            url_link.innerHTML = peptide



            td.appendChild(url_link)



        }
    }
}



var peptideExplorerComponentLinkoutDisplayer = {
    render: renderPeptideExplorerComponentLinkout,
    sort: plainSorter
};

columnHandlers["peptide_explorer_component_linkout"] = peptideExplorerComponentLinkoutDisplayer;





//Widget for linking out to pubmed


//Widget for removing spectrum tag
function renderRemoveSpectrumTag(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        parameters = resolve_parameters_to_map(attributes.parameters, record)

        if(parameters == null){
            return;
        }

        task_id = parameters["task_id"]

        close_dialog_button = document.createElement("img")
        close_dialog_button.src = "/ProteoSAFe/images/hide.png"
        close_dialog_button.onclick = function(task_id){
            return function(){
                remove_spectrum_tag_url = "/ProteoSAFe/LibraryServlet"

                $.ajax({
                    type: "POST",
                    url: remove_spectrum_tag_url,
                    data: { task_id: task_id, action: "removespectrumtag"},
                    cache: false,
                    success: function(){
                        return function(result){
                            location.reload();
                        }
                    }(),
                    failure: function(){
                        return function(result){
                            alert("Error");
                            location.reload();
                        }
                    }()
                });
            }
        }(task_id)

        td.appendChild(close_dialog_button)
        td.style.width = "50px"
    }
}



var removeSpectrumTagDisplayer = {
    render: renderRemoveSpectrumTag,
    sort: plainSorter
};

columnHandlers["removespectrumtag"] = removeSpectrumTagDisplayer;



//Widget for linking out to CAS


//Utils for Widgets
function resolve_parameters_to_map(parameters, record){
    var invokeParameters = null;
    if (parameters != null) {
        invokeParameters = {};
        for (var parameter in parameters)
            invokeParameters[parameter] =
                resolveFields(parameters[parameter], record);
    }
    return invokeParameters;
}
