//####################################################
//Rendering the selection
//####################################################

function render_import_boxes(
    left_div_name, right_div_name, parent_div, comparison_div)
    {
    // mzTab file selection display and controls
    var tab_results = document.createElement("div");
    tab_results.id = "tab_results_per_task";
    tab_results.style.position = "relative";
    parent_div.appendChild(tab_results);

    // "Group A" set of tasks/datasets to be compared
    var div = document.createElement("div");
    div.style.float = "left";
    div.style.paddingLeft = "85px";
    var title = document.createElement("h4");
    title.innerHTML = "Group A";
    div.appendChild(title);
    var input_box = document.createElement("textarea");
    input_box.id = left_div_name;
    input_box.style.width = "400px";
    input_box.style.height = "300px";
    div.appendChild(input_box);
    parent_div.appendChild(div);

    // "Group B" set of tasks/datasets to be compared
    div = document.createElement("div");
    div.style.float = "right";
    div.style.paddingRight = "85px";
    title = document.createElement("h4");
    title.innerHTML = "Group B";
    div.appendChild(title);
    input_box = document.createElement("textarea");
    input_box.id = right_div_name;
    input_box.style.width = "400px"
    input_box.style.height = "300px"
    div.appendChild(input_box);
    parent_div.appendChild(div);

    // Box to Say the Description
    parent_div.appendChild(document.createElement("br"));
    parent_div.appendChild(document.createElement("br"));
    var description_input = document.createElement("input");
    description_input.type = "text";
    description_input.placeholder = "Comparison Description"
    description_input.id = "description_input";
    description_input.size = 100
    parent_div.appendChild(description_input);

    // comparison controls
    parent_div.appendChild(document.createElement("br"));
    div = document.createElement("div");
    div.style.clear = "both";
    var compare_button = document.createElement("button");
    compare_button.innerHTML = "Compare Results";
    div.appendChild(compare_button);

    parent_div.appendChild(div);

    compare_button.onclick = function(comparison_div){
        return function(){
            show_hide_panel(comparison_div.id, "comparison_content_panel");

            // set up modal overlay div for comparison panel
            $("#" + comparison_div.id).empty();
            var overlayDivID = comparison_div.id + "_overlay";
            var overlay = document.createElement("div");
            overlay.id = overlayDivID;
            overlay.className = "overlay";
            var spinner = document.createElement("div");
            spinner.id = comparison_div.id + "_spinner";
            overlay.appendChild(spinner);
            $("#" + comparison_div.id).append(overlay);
            // start progress spinner
            enableOverlay(
                document.getElementById(overlayDivID), true, true, "14px");


            left_task_string = $("#" + left_div_name).val();
            right_task_string = $("#" + right_div_name).val();

            left_tab_objects = parse_comparison_string_to_array(left_task_string)
            right_tab_objects = parse_comparison_string_to_array(right_task_string)

            task_data = new Object()
            task_data.taskid1_data = new Array();
            task_data.taskid2_data = new Array();
            comparison_hoist_state.loaded = 0;
            //Making sure spectrum page is working
            make_sure_views_generated(left_tab_objects, "group_by_spectrum");
            make_sure_views_generated(right_tab_objects, "group_by_spectrum");

            //Making sure peptide page is working
            make_sure_views_generated(left_tab_objects, "group_by_peptide_derived");
            make_sure_views_generated(right_tab_objects, "group_by_peptide_derived");

            //Making sure protein page is working
            make_sure_views_generated(left_tab_objects, "group_by_protein");
            make_sure_views_generated(right_tab_objects, "group_by_protein");

            description = $("#description_input").val()
            query_server_make_comparison_session(left_tab_objects, right_tab_objects, description)
        }
    }(comparison_div)
}

function make_sure_views_generated(tab_list, view){
    //Doing the proper thing, we are going to hit all
    //the urls in order in order to make sure the sqlite file is present
    for(i = 0; i < tab_list.length; i++){
        task = tab_list[i].task
        tab_name = tab_list[i].tab_name
        site = tab_list[i].site
        workflow = tab_list[i].workflow

        fetch_url = "/ProteoSAFe/result_json.jsp"
        console.log(fetch_url);
        $.ajax({
            url: fetch_url,
            async: false,
            data: {
                task: task,
                view: view,
                file: tab_name
            },
            success: function(response){
                console.log("Got reponse for sqlite thing")
            }
        });
    }
    return;
}


function query_server_make_comparison_session(left_tab_objects, right_tab_objects, description){
    //OK LETS DO THIS
    compare_parameters = new Object()
    compare_parameters["left_compare"] = left_tab_objects
    compare_parameters["right_compare"] = right_tab_objects
    var json_data = JSON.stringify(compare_parameters);
    console.log(json_data)


    //Instead lets invoke a workflow
    post_url = "/ProteoSAFe/InvokeTools"
    invoke_parameters = new Object()
    invoke_parameters["workflow"] = "SERVERSIDE_RESULTS_COMPARISON"
    invoke_parameters["uuid"] = "1DCE40EE-81C0-0001-CDE5-815FA4CD1889"
    invoke_parameters["left_comparison"] = JSON.stringify(left_tab_objects)
    invoke_parameters["right_comparison"] = JSON.stringify(right_tab_objects)
    invoke_parameters["email"] = "miw023@ucsd.edu"
    invoke_parameters["uuid"] = "1DCE40EE-81C0-0001-CDE5-815FA4CD1889"
    invoke_parameters["desc"] = description

    $.ajax({
        type: "POST",
        url: post_url,
        data: invoke_parameters,
        success: function(response){
            redirect_url = "/ProteoSAFe/status.jsp?task=" + response
            window.location.href = redirect_url
        }
    });

    return;
}


function parse_comparison_string_to_array(comparison_task_string){
    return_list = new Array()
    lines = comparison_task_string.split(";")
    for( i = 0; i < lines.length; i++){
        if(lines[i].length > 2){
            console.log(lines[i])

            site = lines[i].split(":")[0]
            tab_name = lines[i].split(":")[2]
            task = lines[i].split(":")[1]
            workflow = lines[i].split(":")[3]

            tab_object = new Object();
            tab_object.site = site
            tab_object.tab_name = tab_name
            tab_object.task = task
            tab_object.workflow = workflow

            return_list.push(tab_object)
        }
    }
    return return_list
}

function import_jobs_and_display(div){
    //Getting all of my tasks
    child_table = document.createElement("div");
    child_table.id = "jobs_div"
    div.appendChild(child_table);

    $.ajax({
        url: "/ProteoSAFe/QueryTaskList",
        cache: false,
        success: function(child_table){
            return function(json){
                all_tasks = JSON.parse(json);
                if(all_tasks.status == "success"){
                    my_tasks = all_tasks.tasks;

                    filtered_tasks = new Array();
                    for (var i=0; i<my_tasks.length; i++) {
                        // reject any tasks that did not complete successfully
                        if (my_tasks[i].status != "DONE")
                            continue;
                        // only include tasks with an mzTab result view
                        var workflow = my_tasks[i].workflow;
                        if (workflow == "CONVERT-TSV")
                            filtered_tasks.push(my_tasks[i]);
                        else if (workflow == "MSGFDB" ||
                            workflow == "MSGF_PLUS" ||
                            workflow == "MSPLIT" ||
                            workflow == "MODA") {
                            var version = my_tasks[i].version;
                            // only version 1.2.8 and greater of these workflows
                            // includes an mzTab conversion step
                            // TODO: do a proper comparison of version numbers
                            //if (version == "1.2.8")
                            filtered_tasks.push(my_tasks[i]);
                        }
                    }

                    var task = new Object();
                    task.id = "12345";
                    task.workflow = "My Tasks";
                    task.description = "";
                    var generic_table = new ResultViewTableGen(get_task_table_XML(), "jobs", task);
                    generic_table.setData(filtered_tasks);
                    generic_table.render(child_table, 0);
                }
            }
        }(child_table)
    });
}

function import_datasets_and_display(div) {
    // dataset table div
    var child_table = document.createElement("div");
    child_table.id = "datasets_div";
    div.appendChild(child_table);

    $.ajax({
        url: "/ProteoSAFe/datasets_json.jsp",
        cache: false,
        success: function(child_table){
            return function(json){
                all_datasets = JSON.parse(json).datasets;

                filtered_datasets = new Array();
                for(i = 0; i < all_datasets.length; i++){
                    if(all_datasets[i].flowname == "MASSIVE-COMPLETE") {
                        filtered_datasets.push(all_datasets[i])
                    }
                }

                var task = new Object();
                task.id = "12345";
                task.workflow = "Datasets";
                task.description = "";
                var generic_table = new ResultViewTableGen(get_dataset_table_XML(), "datasets", task, 0);
                generic_table.setData(filtered_datasets);
                generic_table.render(child_table, 0);
            }
        }(child_table)
    });
}



//####################################################
//Rendering the Display
//####################################################

function render_appropriate_comparison_view(task_id, compare_type, table_div){
    render_XML_function = display_spectrum_match_XML
    switch(compare_type){
        case "spectrum_all":
            render_XML_function = display_spectrum_all_XML
            break;
        case "spectrum_match":
            render_XML_function = display_spectrum_match_XML
            break;
        case "spectrum_mismatch":
            render_XML_function = display_spectrum_mismatch_XML
            break;
        case "spectrum_unique_first":
            render_XML_function = display_spectrum_first_unique_XML
            break;
        case "spectrum_unique_second":
            render_XML_function = display_spectrum_second_unique_XML
            break;
        case "peptide_all":
            render_XML_function = display_peptide_all_XML
            break;
        case "peptide_match":
            render_XML_function = display_peptide_match_XML
            break;
        case "peptide_unique_first":
            render_XML_function = display_peptide_first_unique_XML
            break;
        case "peptide_unique_second":
            render_XML_function = display_peptide_second_unique_XML
            break;
        case "protein_all":
            render_XML_function = display_protein_all_XML
            break;
        case "protein_match":
            render_XML_function = display_protein_match_XML
            break;
        case "protein_unique_first":
            render_XML_function = display_protein_first_unique_XML
            break;
        case "protein_unique_second":
            render_XML_function = display_protein_second_unique_XML
            break;
    }

    var task = new Object();
    task.id = String(Math.floor((Math.random() * 10000) + 1))
    task.workflow = "Results Comparison";
    task.description = "";
    var generic_table = new ServerSideResultViewTable(render_XML_function(task_id), "main", task, 0);
    generic_table.render(table_div, 0);
}


function display_protein_all_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=protein_all">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column field="Hits" label="Hits" type="integer" width="5"/> \
                            <column label="View Peptides" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="peptide_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View Peptides"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column label="View PSMs" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column field="ComparePartition" label="CompareSide" type="text" width="2"/> \
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_protein_first_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=protein_unique_first">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column field="Hits" label="Hits" type="integer" width="5"/> \
                            <column label="View Peptides" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="peptide_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View Peptides"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column label="View PSMs" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_protein_second_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=protein_unique_second">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column field="Hits" label="Hits" type="integer" width="5"/> \
                            <column label="View Peptides" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="peptide_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View Peptides"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column label="View PSMs" type="genericurlgenerator" width="10" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_protein_match_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=protein_match">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column field="Hits" label="Hits A" type="integer" width="5"/> \
                            <column field="Hits:1" label="Hits B" type="integer" width="5"/> \
                            <column label="View Peptides" type="genericurlgenerator" width="2" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="peptide_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View Peptides"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column label="View PSMs" type="genericurlgenerator" width="2" field="accession"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=accession_input" value="[accession]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}


function display_peptide_all_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=peptide_all">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="modified_sequence" label="Mod Sequence" type="text" width="20"/> \
                            <column field="sequence" label="Sequence" type="text" width="20"/> \
                            <column field="modifications" label="Mods" type="text" width="10"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column label="View PSMs" type="genericurlgenerator" width="2" field="modified_sequence"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                            <column field="ComparePartition" label="CompareSide" type="text" width="10"/> \
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_peptide_first_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=peptide_unique_first">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="modified_sequence" label="Mod Sequence" type="text" width="20"/> \
                            <column field="sequence" label="Sequence" type="text" width="20"/> \
                            <column field="modifications" label="Mods" type="text" width="10"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column label="View PSMs" type="genericurlgenerator" width="2" field="modified_sequence"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_peptide_second_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=peptide_unique_second">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="modified_sequence" label="Mod Sequence" type="text" width="20"/> \
                            <column field="sequence" label="Sequence" type="text" width="20"/> \
                            <column field="modifications" label="Mods" type="text" width="10"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column label="View PSMs" type="genericurlgenerator" width="2" field="modified_sequence"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_peptide_match_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=peptide_match">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="modified_sequence" label="Mod Sequence" type="text" width="20"/> \
                            <column field="sequence" label="Sequence" type="text" width="20"/> \
                            <column field="modifications" label="Mods" type="text" width="10"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column label="View PSMs" type="genericurlgenerator" width="2" field="modified_sequence"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/results_comparison_view.jsp"/>\
                                <parameter name="REQUESTPARAMETER=compare_type" value="spectrum_all"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence]"/>\
                                <parameter name="LABEL" value="View PSMs"/>\
                                <parameter name="USETASK" value="True"/>\
                                <parameter name="DISABLE_NEW_TAB" value="True"/>\
                            </column>\
                        </row>\
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_spectrum_all_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=spectrum_all">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="#SpecFile" label="FileName" type="text" width="20"/> \
                            <column field="modified_sequence" label="Sequence" type="text" width="20"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="nativeID_scan" label="Scan" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column field="ComparePartition" label="CompareSide" type="text" width="10"/> \
                        </row>\
                        <row expander="image"> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename]"/> \
                                <parameter name="scan"         value="[nativeID_scan]"/> \
                                <parameter name="index"        value="[nativeID_index]"/> \
                                <parameter name="peptide"      value="[modified_sequence]"/> \
                                <parameter name="task"         value="[taskid]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                        </row> \
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_spectrum_match_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=spectrum_match">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="#SpecFile" label="FileName" type="text" width="20"/> \
                            <column field="modified_sequence" label="Sequence" type="text" width="20"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="nativeID_scan" label="Scan" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                        </row>\
                        <row expander="image"> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename]"/> \
                                <parameter name="scan"         value="[nativeID_scan]"/> \
                                <parameter name="index"        value="[nativeID_index]"/> \
                                <parameter name="peptide"      value="[modified_sequence]"/> \
                                <parameter name="task"         value="[taskid]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                        </row> \
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_spectrum_mismatch_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=spectrum_mismatch">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="#SpecFile" label="FileName" type="text" width="20"/> \
                            <column field="modified_sequence" label="Sequence A" type="text" width="20"/> \
                            <column field="modified_sequence:1" label="Sequence B" type="text" width="20"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="nativeID_scan" label="Scan" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                            <column label="View A" type="genericurlgenerator" width="10" field="modified_sequence"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum"/>\
                                <parameter name="REQUESTPARAMETER=file" value="[tabid]"/>\
                                <parameter name="REQUESTPARAMETER=task" value="[taskid]"/>\
                                <parameter name="HASHPARAMTER=nativeID_scan_lowerinput" value="[nativeID_scan]"/>\
                                <parameter name="HASHPARAMTER=nativeID_scan_upperinput" value="[nativeID_scan]"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence]"/>\
                                <parameter name="LABEL" value="View A"/>\
                            </column>\
                            <column label="View B" type="genericurlgenerator" width="10" field="modified_sequence:1"> \
                                <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum"/>\
                                <parameter name="REQUESTPARAMETER=file" value="[tabid:1]"/>\
                                <parameter name="REQUESTPARAMETER=task" value="[taskid:1]"/>\
                                <parameter name="HASHPARAMTER=nativeID_scan_lowerinput" value="[nativeID_scan]"/>\
                                <parameter name="HASHPARAMTER=nativeID_scan_upperinput" value="[nativeID_scan]"/>\
                                <parameter name="HASHPARAMTER=modified_sequence_input" value="[modified_sequence:1]"/>\
                                <parameter name="LABEL" value="View B"/>\
                            </column>\
                        </row>\
                        <row expander="image"> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename]"/> \
                                <parameter name="scan"         value="[nativeID_scan]"/> \
                                <parameter name="index"        value="[nativeID_index]"/> \
                                <parameter name="peptide"      value="[modified_sequence]"/> \
                                <parameter name="task"         value="[taskid]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename:1]"/> \
                                <parameter name="scan"         value="[nativeID_scan:1]"/> \
                                <parameter name="index"        value="[nativeID_index:1]"/> \
                                <parameter name="peptide"      value="[modified_sequence:1]"/> \
                                <parameter name="task"         value="[taskid:1]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                        </row> \
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_spectrum_first_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=spectrum_unique_first">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="#SpecFile" label="FileName" type="text" width="20"/> \
                            <column field="modified_sequence" label="Sequence" type="text" width="20"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="nativeID_scan" label="Scan" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                        </row>\
                        <row expander="image"> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename]"/> \
                                <parameter name="scan"         value="[nativeID_scan]"/> \
                                <parameter name="index"        value="[nativeID_index]"/> \
                                <parameter name="peptide"      value="[modified_sequence]"/> \
                                <parameter name="task"         value="[taskid]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                        </row> \
                    </block>' ;
    return (parseXML(tableXML_str));
}

function display_spectrum_second_unique_XML(task_id){
    var tableXML_str = '<block id="task_list" type="table_ss" pagesize="30" '
    tableXML_str += 'server_URL="/ProteoSAFe/ResultCompareSession?task=' + task_id + '&amp;' + 'compare_type=spectrum_unique_second">'
    tableXML_str += '<parsers> \
                            <parser type="mzTab"/> \
                        <parser type="SQLite"/> \
                    </parsers> \
                        <row>  \
                            <column field="#SpecFile" label="FileName" type="text" width="20"/> \
                            <column field="modified_sequence" label="Sequence" type="text" width="20"/> \
                            <column field="charge" label="Charge" type="integer" width="10"/> \
                            <column field="nativeID_scan" label="Scan" type="integer" width="10"/> \
                            <column field="nativeID_index" label="Index" type="integer" width="10"/> \
                            <column field="accession" label="Protein" type="text" width="20"/> \
                        </row>\
                        <row expander="image"> \
                            <column type="jscolumnspectrumviewer" colspan="4" label="Spectrum" width="600" height="350"> \
                                <parameter name="file"         value="FILE->spec/[internalFilename]"/> \
                                <parameter name="scan"         value="[nativeID_scan]"/> \
                                <parameter name="index"        value="[nativeID_index]"/> \
                                <parameter name="peptide"      value="[modified_sequence]"/> \
                                <parameter name="task"         value="[taskid]"/> \
                                <parameter name="modformat"    value="inspect"/> \
                                <parameter name="remoteserver" value="/"/> \
                            </column> \
                        </row> \
                    </block>' ;
    return (parseXML(tableXML_str));
}


var comparison_hoist_state = new Object();

function show_hide_panel(show_panel_id, hide_panel_class) {
    // hide the return link if this is the main panel
    if (show_panel_id == "container_div")
        $("#return_to_results_selection_link").css("visibility", "hidden");
    // otherwise, show the return link
    else $("#return_to_results_selection_link").css("visibility", "visible");
    // hide all panels
    $("div." + hide_panel_class).hide();
    // show the current panel
    $("div#" + show_panel_id).show();
}


function render_buttons_spectrum_serverside(div) {
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Spectrum Level Comparison";
    div.appendChild(header)

    populate_div_with_total_numbers = function(div, label, compare_type){
        return function(response){
            click_button = document.createElement("button");
            click_button.innerHTML = label + " - " + response.total_rows
            click_button.onclick = function(){
                url_parameters = getURLParameters()
                url_parameters["compare_type"] = compare_type
                redirect_url = "/ProteoSAFe/results_comparison_view.jsp?" + $.param(url_parameters)
                window.location.href = redirect_url
            }
            div.appendChild(click_button)
        }
    }

    task_id = get_taskid();

    //Rendering boxes for counts
    all_count_div = document.createElement("span");
    match_count_div = document.createElement("span");
    mismatch_count_div = document.createElement("span");
    group_a_count_div = document.createElement("span");
    group_b_count_div = document.createElement("span");

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "spectrum_all",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(all_count_div, "All Results", "spectrum_all")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "spectrum_match",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(match_count_div, "Intersection Match", "spectrum_match")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "spectrum_mismatch",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(mismatch_count_div, "Intersection Mismatch", "spectrum_mismatch")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "spectrum_unique_first",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_a_count_div, "Group A Unique", "spectrum_unique_first")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "spectrum_unique_second",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_b_count_div, "Group B Unique", "spectrum_unique_second")
    })

    div.appendChild(all_count_div);
    div.appendChild(match_count_div);
    div.appendChild(mismatch_count_div);
    div.appendChild(group_a_count_div);
    div.appendChild(group_b_count_div);
}


function render_buttons_peptide_serverside(div) {
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Peptide Level Comparison";
    div.appendChild(header)

    populate_div_with_total_numbers = function(div, label, compare_type){
        return function(response){
            click_button = document.createElement("button");
            click_button.innerHTML = label + " - " + response.total_rows
            click_button.onclick = function(){
                url_parameters = getURLParameters()
                url_parameters["compare_type"] = compare_type
                redirect_url = "/ProteoSAFe/results_comparison_view.jsp?" + $.param(url_parameters)
                window.location.href = redirect_url
            }
            div.appendChild(click_button)
        }
    }

    task_id = get_taskid();

    all_count_div = document.createElement("span");
    match_count_div = document.createElement("span");
    group_a_count_div = document.createElement("span");
    group_b_count_div = document.createElement("span");



    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "peptide_all",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(all_count_div, "All Results", "peptide_all")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "peptide_match",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(match_count_div, "Intersection Match", "peptide_match")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "peptide_unique_first",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_a_count_div, "Group A Unique", "peptide_unique_first")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "peptide_unique_second",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_b_count_div, "Group B Unique", "peptide_unique_second")
    })

    div.appendChild(all_count_div);
    div.appendChild(match_count_div);
    div.appendChild(group_a_count_div);
    div.appendChild(group_b_count_div);

}


function render_buttons_protein_serverside(div) {
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Protein Level Comparison";
    div.appendChild(header)



    populate_div_with_total_numbers = function(div, label, compare_type){
        return function(response){
            click_button = document.createElement("button");
            click_button.innerHTML = label + " - " + response.total_rows
            click_button.onclick = function(){
                url_parameters = getURLParameters()
                url_parameters["compare_type"] = compare_type
                redirect_url = "/ProteoSAFe/results_comparison_view.jsp?" + $.param(url_parameters)
                window.location.href = redirect_url
            }
            div.appendChild(click_button)
        }
    }

    task_id = get_taskid();

    all_count_div = document.createElement("span");
    match_count_div = document.createElement("span");
    group_a_count_div = document.createElement("span");
    group_b_count_div = document.createElement("span");



    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "protein_all",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(match_count_div, "All Results", "protein_all")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "protein_match",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(mismatch_count_div, "Intersection Match", "protein_match")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "protein_unique_first",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_a_count_div, "Group A Unique", "protein_unique_first")
    })

    $.ajax({
        type: "GET",
        url:  "/ProteoSAFe/ResultCompareSession",
        data : {
            task : task_id,
            compare_type : "protein_unique_second",
            pageSize : 30,
            offset : 0
        },
        async: true,
        dataType: "json",
        success: populate_div_with_total_numbers(group_b_count_div, "Group B Unique", "protein_unique_second")
    })

    div.appendChild(all_count_div);
    div.appendChild(match_count_div);
    div.appendChild(group_a_count_div);
    div.appendChild(group_b_count_div);

}



function display_comparison_selection(task_id, div){


    url = "http://ccms-dev1.ucsd.edu/ProteoSAFe/DownloadResultFile?task=" + task_id + "&file=params/params.xml&block=main"

    displaying_div = document.createElement("div");
    displaying_div.id = "comparison_selection_div"
    displaying_div.style.width = "100%"
    displaying_div.style.overflow = "auto"
    div.appendChild(displaying_div)

    comparison_params_handler = function(div){
        return function(response){
            parameters = response.getElementsByTagName("parameter")
            left_parameters = new Array()
            right_parameters = new Array()
            for(i = 0; i < parameters.length; i++){
                if(parameters[i].getAttribute("name") == "left_comparison"){
                    left_parameters = JSON.parse(parameters[i].childNodes[0].nodeValue)
                }
                if(parameters[i].getAttribute("name") == "right_comparison"){
                    right_parameters = JSON.parse(parameters[i].childNodes[0].nodeValue)
                }
                console.log(parameters[i])
            }

            console.log(left_parameters)
            console.log(right_parameters)

            queue_status_left = new Object()
            queue_status_left.total_finished_requests = 0
            queue_status_left.total_fired_requests = left_parameters.length

            queue_status_right = new Object()
            queue_status_right.total_finished_requests = 0
            queue_status_right.total_fired_requests = right_parameters.length

            left_table_div = document.createElement("div");
            right_table_div = document.createElement("div");
            left_table_div.style.width = "50%"
            left_table_div.style.float = "left"
            left_table_div.style.marginBottom = "20px"

            right_table_div.style.width = "50%"
            right_table_div.style.float = "right"
            right_table_div.style.marginBottom = "20px"
            div.appendChild(left_table_div)
            div.appendChild(right_table_div)

            //Display this
            for(i in left_parameters){
                task_info_url = "http://ccms-dev1.ucsd.edu/ProteoSAFe/status_json.jsp?task=" + left_parameters[i].task
                $.ajax({
                    type: "GET",
                    url: task_info_url,
                    async: true,
                    dataType: "json",
                    success: function(queue_status, parameter_object, parameter_list, div){
                        return function(response){
                            parameter_object.description = response.description
                            queue_status.total_finished_requests += 1
                            if(queue_status.total_finished_requests == queue_status.total_fired_requests){
                                console.log("Requests Finished")

                                var task = new Object();
                                task.id = "12345";
                                task.workflow = "Left Comparison";
                                task.description = "A Tab Files";
                                var generic_table = new ResultViewTableGen(get_comparison_tabs_XML(), "left_comparision", task, 0);
                                generic_table.setData(parameter_list);
                                generic_table.render(div, 0);
                            }
                        }
                    }(queue_status_left, left_parameters[i], left_parameters, left_table_div)
                })
            }
            for(i in right_parameters){
                task_info_url = "http://ccms-dev1.ucsd.edu/ProteoSAFe/status_json.jsp?task=" + right_parameters[i].task
                $.ajax({
                    type: "GET",
                    url: task_info_url,
                    async: true,
                    dataType: "json",
                    success: function(queue_status, parameter_object, parameter_list, div){
                        return function(response){
                            parameter_object.description = response.description
                            queue_status.total_finished_requests += 1
                            if(queue_status.total_finished_requests == queue_status.total_fired_requests){
                                console.log("Requests Finished")

                                var task = new Object();
                                task.id = "12347";
                                task.workflow = "Right Comparison";
                                task.description = "B Tab Files";
                                var generic_table = new ResultViewTableGen(get_comparison_tabs_XML(), "right_comparision", task, 0);
                                generic_table.setData(parameter_list);
                                generic_table.render(div, 0);
                            }
                        }
                    }(queue_status_right, right_parameters[i], right_parameters, right_table_div)
                })
            }
        }
    }(displaying_div)


    $.ajax({
        type: "GET",
        url: url,
        dataType: "xml",
        success: comparison_params_handler
    });
}

function get_comparison_tabs_XML(){
    var tableXML_str = '<block id="task_list" type="table" pagesize="10"> \
                            <row>  \
                                <column field="task" label="task" type="text" width="10"/> \
                                <column field="description" label="Description" type="text" width="10"/> \
                                <column field="workflow" label="workflow" type="text" width="10"/> \
                                <column label="Tab File" type="genericurlgenerator" width="10" field="tab_name"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum"/>\
                                    <parameter name="REQUESTPARAMETER=task" value="[task]"/>\
                                    <parameter name="REQUESTPARAMETER=file" value="[tab_name]"/>\
                                    <parameter name="LABEL" value="[tab_name]"/>\
                                </column>\
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


//####################################################
// Deprecated
//####################################################

function render_buttons_spectrum(main_div_name, data, table_name) {
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Spectrum Level Comparison";
    $("#" + main_div_name).append(header);

    // display tabs for this section
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_intersection(data, table_name)
        }
    }(data, table_name);


    intersection_different_button = document.createElement("button");
    intersection_different_button.innerHTML = "Intersection Mismatch";
    intersection_different_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_intersected_differences(data, table_name)
        }
    }(data, table_name);

    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_unique(data, table_name)
        }
    }(data, table_name);

    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_unique(data, table_name)
        }
    }(data, table_name);


    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(intersection_different_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}


function render_buttons_peptide(main_div_name, data, table_name){
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Peptide Level Comparison";
    $("#" + main_div_name).append(header);

    // display tabs for this section
    console.log("Peptide Comparison Render");
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_peptide_intersection(data, table_name)
        }
    }(data, table_name);

    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_peptide_unique(data, table_name)
        }
    }(data, table_name);

    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_peptide_unique(data, table_name)
        }
    }(data, table_name);


    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}

function render_buttons_protein(main_div_name, data, table_name){
    // display header for this section
    var header = document.createElement("h3");
    header.innerHTML = "Protein Level Comparison";
    $("#" + main_div_name).append(header);

    // display tabs for this section
    console.log("Protein Comparison Render");
    intersection_button = document.createElement("button");
    intersection_button.innerHTML = "Intersection Match";
    intersection_button.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_protein_intersection(data, table_name)
        }
    }(data, table_name);

    task1_unique = document.createElement("button");
    task1_unique.innerHTML = "Group A Unique";
    task1_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task1_protein_unique(data, table_name)
        }
    }(data, table_name);

    task2_unique = document.createElement("button");
    task2_unique.innerHTML = "Group B Unique";
    task2_unique.onclick = function(data, table_name){
        return function(){
            $("#" + table_name).empty();
            display_task2_protein_unique(data, table_name)
        }
    }(data, table_name);

    $("#" + main_div_name).append(intersection_button);
    $("#" + main_div_name).append(task1_unique);
    $("#" + main_div_name).append(task2_unique);
}



function get_tab_data_list(tab_list, task_data, view, group, overlayDivID) {



    data_retreval_object = new Object();
    data_retreval_object.retreived_count = 0;
    data_retreval_object.expected = tab_list.length;

    for(i = 0; i < tab_list.length; i++){
        task = tab_list[i].task
        tab_name = tab_list[i].tab_name
        site = tab_list[i].site
        workflow = tab_list[i].workflow

        remote_url = "http://massive.ucsd.edu"

        if (site == "MassIVE")
            remote_url = "http://massive.ucsd.edu";
        else if (site == "ProteoSAFe-BETA")
            remote_url = "http://proteomics2.ucsd.edu";
        else if (site == "ProteoSAFe-ALPHA")
            remote_url = "http://ccms-internal.ucsd.edu";

        fetch_url = remote_url + "/ProteoSAFe/result_jsonp.jsp"

        $.ajax({
            url: fetch_url,
            jsonp: "callback",
            async: false,
            dataType: "jsonp",
            data: {
                task: task,
                view: view,
                file: tab_name
            },
            success: function(
                task_data, group, taskid, site, workflow, data_retreval_object
            ) {
                return function(response) {
                    if (isNaN(comparison_hoist_state.loaded))
                        comparison_hoist_state.loaded = 0;
                    comparison_hoist_state.loaded++;
                    data_retreval_object.retreived_count++;
                    if (data_retreval_object.retreived_count ==
                        data_retreval_object.expected)
                        log_debug_message("Data Hoisted Successfully");
                    else console.log("Count = " +
                        data_retreval_object.retreived_count +
                        " , Expected = " + data_retreval_object.expected);
                    for (var i=0; i<response.blockData.length; i++)
                        response.blockData[i].taskid = taskid
                    // get the data group as specified by the function argument;
                    // this is necessary to do here since we need the whole
                    // "task_data" object to retrieve data in advance for
                    // populating the link list view
                    var data_group = null;
                    if (group.toUpperCase() == "A")
                        data_group = task_data.taskid1_data;
                    else data_group = task_data.taskid2_data;
                    response.blockData.forEach(function(v) {
                        v.site = site
                        v.workflow = workflow
                        data_group.push(v)
                    }, data_group);
                    // if both comparison groups have been loaded,
                    // collect the stats for the various comparisons
                    if (comparison_hoist_state.loaded >= 2) {
                        if (view == "group_by_spectrum") {
                            comparison_hoist_state.intersection_list =
                                build_intersection_list(task_data);
                            console.log("Intersection Match = " +
                                comparison_hoist_state.intersection_list.length);
                            comparison_hoist_state.intersected_differences_list =
                                build_intersected_differences_list(task_data);
                            console.log("Intersection Mismatch = " +
                                comparison_hoist_state.intersected_differences_list.length);
                            comparison_hoist_state.task1_unique_list =
                                build_task1_unique_list(task_data);
                            console.log("Group A Unique = " +
                                comparison_hoist_state.task1_unique_list.length);
                            comparison_hoist_state.task2_unique_list =
                                build_task2_unique_list(task_data);
                            console.log("Group B Unique = " +
                                comparison_hoist_state.task2_unique_list.length);
                        }
                        // stop progress spinner
                        setTimeout(function() {
                            enableOverlay(document.getElementById(overlayDivID),
                            false); }, 200);
                    }
                }
            }(task_data, group, task, site, workflow, data_retreval_object),
            error: function(response, text, error) {
                // stop progress spinner
                setTimeout(function() {
                    enableOverlay(document.getElementById(overlayDivID),
                    false); }, 200);
                console.log(
                    "Error hoisting result data to compare: [" + text + "].");
            }
        })
    }
}


function log_debug_message(output_message){
    $("#debug_panel").append(output_message + "<br>");
}
