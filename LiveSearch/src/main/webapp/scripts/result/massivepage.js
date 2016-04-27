/**
 * File stream result view block implementation
 */
// constructor
function ResultViewFileMassive(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    this.massiveID = null;
    // set up the file retrieval
    this.init(blockXML);

}

// initialize block from XML specification
ResultViewFileMassive.prototype.init = function(blockXML) {
    //Determining if this is not on GNPS
    if(blockXML != null){
        if(blockXML.getAttribute("site") == "MASSIVE"){
            this.massive_page = true;
        }
        else{
            this.massive_page = false;
        }
    }


    //Get the dataset ID via blocking ajax call
    task_id = get_taskid();
    context = this;
    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/MassiveServlet",
        data: { task: task_id, function: "tasktomassiveid"},
        async: false,
        cache: false,
        success: function(response){
            context.massiveID = response
        }
    });
}

function get_host_url(exec_location){
    var host_location_map = new Object();
    //host_location_map["ProteoSAFe"] = "http://proteomics.ucsd.edu/";
    host_location_map["ProteoSAFe"] = "http://ccms-dev1.ucsd.edu/";
    host_location_map["ProteoSAFe-BETA"] = "http://proteomics2.ucsd.edu/";
    host_location_map["GNPS"] = "http://gnps.ucsd.edu/";
    host_location_map["MassIVE"] = "http://massive.ucsd.edu/";
    host_location_map["MassIVE_secure"] = "https://massive.ucsd.edu/";

    if (exec_location in host_location_map){
        return host_location_map[exec_location];
    }
    else{
        return "/";
    }
}

function get_static_vars(){
    var host_location_map = new Object();
    //host_location_map["ProteoSAFe"] = "http://proteomics.ucsd.edu/";
    host_location_map["ProteoSAFe"] = "http://ccms-dev1.ucsd.edu/";
    host_location_map["ProteoSAFe-BETA"] = "http://proteomics2.ucsd.edu/";
    host_location_map["GNPS"] = "http://gnps.ucsd.edu/";
    host_location_map["MassIVE"] = "http://massive.ucsd.edu/";

    return host_location_map;
}


function perform_subscribe(subscription_button){
      var request = createRequest();
      url = "MassiveServlet"
      task_id = get_taskid();
      parameters = "?task=" + task_id + "&function=subscription";
      url += parameters
      request.open("POST", url, true);
      request.onreadystatechange = function() {
        if (request.readyState == 4) {
          if (request.status == 200) {
            if(request.responseText == "LOGIN"){
              //alert("login");
              return;
            }
            if(request.responseText == "ERROR"){
              alert("error");
              return
            }
            sub_request_json = JSON.parse(request.responseText);
            if(sub_request_json["status"] == 0){
              subscription_button.setAttribute('name','Unsubscribe');
              subscription_button.setAttribute('value','Unsubscribe');
              subscription_button.onclick = function(){
                perform_unsubscribe(subscription_button);
              };
            }
          }
          else{
            alert("Bad News Bears");
          }
        }
      };
      request.send(null);
}

function perform_unsubscribe(subscription_button){
      var request = createRequest();
      url = "MassiveServlet"
      task_id = get_taskid();
      parameters = "?task=" + task_id + "&function=subscription";
      url += parameters
      request.open("DELETE", url, true);
      request.onreadystatechange = function() {
        if (request.readyState == 4) {
          if (request.status == 200) {
            if(request.responseText == "LOGIN"){
              //alert("login");
              return;
            }
            if(request.responseText == "ERROR"){
              alert("error");
              return
            }
            sub_request_json = JSON.parse(request.responseText);
            if(sub_request_json["status"] == 0){
              subscription_button.setAttribute('name','Subscribe');
              subscription_button.setAttribute('value','Subscribe');
              subscription_button.onclick = function(){
                perform_subscribe(subscription_button);
              };
            }
          }
          else{
            alert("Bad News Bears");
          }
        }
      };
      request.send(null);
}

function get_subscription_status(){
      var request = createRequest();
      url = "MassiveServlet"
      task_id = get_taskid();
      parameters = "?task=" + task_id + "&function=subscription";
      url += parameters
      request.open("GET", url, true);
      request.onreadystatechange = function() {
        if (request.readyState == 4) {
          if (request.status == 200) {
            if(request.responseText == "LOGIN"){
              //alert("login");
              return;
            }
            if(request.responseText == "ERROR"){
              alert("error");
              return
            }
            sub_request_json = JSON.parse(request.responseText);
          }
          else{
            alert("Bad News Bears");
          }
        }
      };
      request.send(null);
}

function create_ftp_download_link(div, ftp_url){
    a_element = document.createElement("a");
    a_element.innerHTML = ftp_url;
    a_element.href = ftp_url;
    div.appendChild(a_element);
}

function create_browse_results_link(div, task_id){
    a_element = document.createElement("a");
    a_element.innerHTML = "Browse Submitted Result Files";
    a_element.href =
    	"/ProteoSAFe/result.jsp?task=" + task_id + "&view=view_result_list";
    div.appendChild(a_element);
}


function create_otheractions_link(div){
    task_id = get_taskid();

    var update_metadata_link = document.createElement('a');
    update_metadata_link.innerHTML = "Add/Update Metadata";
    update_metadata_link.href = get_host_url("MassIVE") + "ProteoSAFe/updateDataset.jsp?task=" + task_id;

    var add_publications_link = document.createElement('a');
    add_publications_link.innerHTML = "Add Publication";
    add_publications_link.href = get_host_url("MassIVE") +
    	"ProteoSAFe/addPublication.jsp?task=" + task_id;

    div.appendChild(update_metadata_link);
    var spacer = document.createElement("span");
    spacer.style.paddingRight = "10px";
    div.appendChild(spacer);
    div.appendChild(add_publications_link);
}

function create_subscribe_buttons(div){
      var request = createRequest();
      url = "MassiveServlet"
      task_id = get_taskid();
      parameters = "?task=" + task_id + "&function=subscription";
      url += parameters
      request.open("GET", url, true);
      request.onreadystatechange = function() {
        if (request.readyState == 4) {
          if (request.status == 200) {
            if(request.responseText == "LOGIN"){
              //alert("login");
              return;
            }
            if(request.responseText == "ERROR"){
              alert("error");
              return
            }
            sub_request_json = JSON.parse(request.responseText);

            var sub_c_id= document.createElement('input');
            sub_c_id.setAttribute('type','button');

            if(sub_request_json["status"] == 0){
              //No sub so lets display sub
              sub_c_id.setAttribute('name','Subscribe');
              sub_c_id.setAttribute('value','Subscribe');
              sub_c_id.onclick = function(){
                perform_subscribe(sub_c_id);
              };
            }
            if(sub_request_json["status"] == 1){
              sub_c_id.setAttribute('name','Unsubscribe');
              sub_c_id.setAttribute('value','Unsubscribe');
              sub_c_id.onclick = function(){
                perform_unsubscribe(sub_c_id);
              };
            }


            div.appendChild(sub_c_id);

          }
          else{
            alert("Bad News Bears");
          }
        }
      };
      request.send(null);
}

function get_massivejobs_tableXML(){
    var tableXML_str = "";
    var window_hash = location.hash;
    if(window_hash == "#DEBUG"){
        tableXML_str = '<block id="massive_jobs" type="table"> \
                                <row>  \
                                    <column type="absolutelink" label="View"> \
                                            <parameter name="link" value="[taskurl]"/> \
                                    </column> \
                                    <column field="workflowname" label="Workflow" type="text" width="5"/> \
                                    <column field="reported" label="Reported" type="text" width="5"/> \
                                    <column field="jobstatus" label="Status" type="text" width="5"/> \
                                    <column field="timestamp" label="Time" type="text" width="5"/> \
                                </row> \
                            </block>' ;
    }
    else{
        tableXML_str = '<block id="massive_jobs" type="table"> \
                                <row>  \
                                    <column type="absolutelink" label="View"> \
                                            <parameter name="link" value="[taskurl]"/> \
                                    </column> \
                                    <column field="timestamp" label="Time" type="text" width="5"/> \
                                    <column type="displaycontinuoustasksummary" label="Total IDs"> \
                                            <parameter name="task_id" value="[task_id]"/> \
                                            <parameter name="view_name" value="group_by_spectrum_all"/> \
                                    </column> \
                                    <column type="displaycontinuoustasksummary" label="New IDs"> \
                                            <parameter name="task_id" value="[task_id]"/> \
                                            <parameter name="view_name" value="group_by_spectrum_new"/> \
                                    </column> \
                                    <column type="displaycontinuoustasksummary" label="Diff IDs"> \
                                            <parameter name="task_id" value="[task_id]"/> \
                                            <parameter name="view_name" value="group_by_spectrum_different"/> \
                                    </column> \
                                    <column type="displaycontinuoustasksummary" label="Deleted IDs"> \
                                            <parameter name="task_id" value="[task_id]"/> \
                                            <parameter name="view_name" value="group_by_spectrum_deleted"/> \
                                    </column> \
                                </row> \
                            </block>' ;
    }
    return (parseXML(tableXML_str));
}


function get_massivecomment_tableXML(){
    var tableXML_str = '<block id="massive_comments" type="table"> \
                            <row>  \
                                <column type="absolutelink" label="View"> \
                                         <parameter name="link" value="[taskurl]"/> \
                                </column> \
                                <column field="comment" label="Comment" type="text" width="5"/> \
                                <column field="user_id" label="User" type="text" width="5"/> \
                                <column field="create_time" label="Time" type="text" width="5"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_massivereanalyses_tableXML(){
    var tableXML_str = '<block id="massive_reanalyses" type="table"> \
                            <row>  \
                                <column type="absolutelink" label="View"> \
                                         <parameter name="link" value="[taskurl]"/> \
                                </column> \
    							<column field="comment" label="Description" type="text" width="5"/> \
                                <column field="user_id" label="User" type="text" width="5"/> \
                                <column field="create_time" label="Time" type="text" width="5"/> \
                            </row> \
                        </block>' ;
    return (parseXML(tableXML_str));
}

ResultViewFileMassive.prototype.get_jobs = function(div){
    var request = createRequest();
    url = "ContinuousIDServlet"
    task_id = get_taskid();
    parameters = "?task=" + task_id;
    url += parameters

    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            if (request.status == 200) {
                if(request.responseText == "LOGIN"){
                    //alert("login");
                    return;
                }
                if(request.responseText == "ERROR"){
                    alert("error");
                    return
                }
                jobs_json_object = JSON.parse(request.responseText);

                var jobs = jobs_json_object["jobs"];

                for(i = 0; i < jobs.length; i++){
                    jobs[i].id = i;

                    var hostname = get_host_url(jobs[i]["execution_site"]);
                    hostname += "ProteoSAFe/";
                    jobs[i].taskurl = encodeURI(hostname + "status.jsp?task=" + jobs[i]["task"]);
                    jobs[i].task_id = jobs[i]["task"]
                }

                var task = new Object();
                task.id = "1234";
                task.workflow = "Massive Continuous ID";
                task.description = "Massive Continuous ID";
                var generic_table = new ResultViewTableGen(get_massivejobs_tableXML(), "massive_continuous_id", task, 0);

                var filtered_jobs = new Array();
                var window_hash = location.hash;
                if(window_hash == "#DEBUG"){
                    filtered_jobs = jobs;
                }
                else{
                    for(i = 0; i < jobs.length;i++){
                        if(jobs[i].reported == "1"){
                            filtered_jobs.push(jobs[i]);
                        }
                    }

                }


                generic_table.setData(filtered_jobs);
                generic_table.render(div, 0);

                return;

            }
            else{
            alert("Bad News Bears");
            }
        }
    };
    request.send(null);
}

ResultViewFileMassive.prototype.get_comments = function(div){
    var request = createRequest();
    url = "MassiveServlet"
    task_id = get_taskid();
    parameters = "?task=" + task_id + "&function=comment";
    url += parameters

    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            if (request.status == 200) {
                if(request.responseText.slice(0,5) == "ERROR"){
                    alert("error");
                    return
                }
                comments_json = JSON.parse(request.responseText);


                var comments = comments_json["massivecomments"];

                for(i = 0; i < comments.length; i++){
                    comments[i].id = i;

                    var hostname = get_host_url(comments[i]["execution_site"]);
                    hostname += "ProteoSAFe/";
                    comments[i].taskurl = encodeURI(hostname + "status.jsp?task=" + comments[i]["task"]);
                }

                var task = new Object();
                task.id = "1234";
                task.workflow = "MassIVE Comments";
                task.description = "MassIVE Comments";
                var generic_table = new ResultViewTableGen(get_massivecomment_tableXML(), "massive_comments", task, 0);


                generic_table.setData(comments);
                generic_table.render(div, 0);

                return;

            }
            else{
            alert("Bad News Bears");
            }
        }
    };
    request.send(null);
}

ResultViewFileMassive.prototype.get_reanalyses = function(div){
    var request = createRequest();
    var url = "MassiveServlet";
    var task_id = get_taskid();
    var parameters = "?task=" + task_id + "&function=reanalysis";
    url += parameters;

    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            if (request.status == 200) {
                if(request.responseText.slice(0,5) == "ERROR") {
                    alert("error");
                    return;
                }
                var reanalyses_json = JSON.parse(request.responseText);
                var reanalyses = reanalyses_json["massivereanalyses"];
                for(var i=0; i<reanalyses.length; i++) {
                	reanalyses[i].id = i;
                    var hostname =
                    	get_host_url(reanalyses[i]["execution_site"]);
                    hostname += "ProteoSAFe/";
                    reanalyses[i].taskurl = encodeURI(
                    	hostname + "status.jsp?task=" + reanalyses[i]["task"]);
                }
                var task = new Object();
                task.id = "1234";
                task.workflow = "MassIVE Reanalyses";
                task.description = "MassIVE Reanalyses";
                var generic_table = new ResultViewTableGen(
                	get_massivereanalyses_tableXML(), "massive_reanalyses",
                	task, 0);
                generic_table.setData(reanalyses);
                generic_table.render(div, 0);
                return;
            } else {
            	alert("Bad News Bears");
            }
        }
    };
    request.send(null);
}


ResultViewFileMassive.prototype.create_comment_link = function(div){

    var request = createRequest();
    url = "MassiveServlet"
    task_id = get_taskid();
    parameters = "?task=" + task_id + "&function=tasktomassiveid";
    url += parameters

    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            if (request.status == 200) {
                if(request.responseText.slice(0,5) == "ERROR"){
                    alert("error");
                    return
                }

                var comment_link = document.createElement('a');
                comment_link.textContent = ("Comment on Dataset");
                comment_link.id = "commentmassivebutton";
                var workflow_fillin = {};
                workflow_fillin["workflow"] = "ADD-MASSIVE-COMMENT";
                workflow_fillin["MASSIVEID"] = request.responseText;
                var comment_url = get_host_url("MassIVE") + "ProteoSAFe/index.jsp?params=" + JSON.stringify(workflow_fillin);
                comment_link.href = encodeURI(comment_url);
                div.appendChild(comment_link);



            } else if(request.status == 0){
                console.log("Navigating away from page");
            } else{
                alert("Bad News Bears");
            }
        }
    };
    request.send(null);
}

ResultViewFileMassive.prototype.create_publications_display = function(div, publications){
    div.style.width = "85%";
    for(i in publications){
        var pub_title = document.createElement('span');
        pub_title.innerHTML = publications[i]["authors"];
        pub_title.style.fontWeight = "bold";

        var pub_authors = document.createElement('span');
        pub_authors.innerHTML = publications[i]["title"];
        pub_authors.style.fontStyle = "italic";

        var pub_citation = document.createElement('span');
        pub_citation.innerHTML = publications[i]["citation"];

        div.appendChild(pub_title);
        div.appendChild(document.createElement("br"));
        div.appendChild(pub_authors);
        div.appendChild(document.createElement("br"));
        div.appendChild(pub_citation);
        div.appendChild(document.createElement("br"));
        // only add an extra line break if this isn't the last publication
        if (i < publications.length - 1)
        	div.appendChild(document.createElement("br"));
    }

}

ResultViewFileMassive.prototype.create_makepublic_link = function(div, px) {
	var is_massive_site = this.is_massive_site();
	// build dataset publish URL
	var url = null;
	// if this is not the MassIVE web server, then we need to redirect there,
	// since only there can a dataset be made public
	if (is_massive_site)
	    url = "/ProteoSAFe/PublishDataset";
	else url = "http://massive.ucsd.edu/ProteoSAFe/result.jsp";
    var task_id = get_taskid();
    var parameters = "?task=" + task_id;
	if (is_massive_site == false)
	    parameters += "&view=advanced_view";
    url += parameters;
	var confirm_message = null;
	if (is_massive_site) {
		confirm_message = "Are you sure? If you make this dataset public, " +
			"then it will become viewable and downloadable by all users";
		if (px && px != "null")
			confirm_message += ", and will be publicly announced " +
				"via the ProteomeXchange consortium";
		confirm_message += ".";
	} else confirm_message = "Datasets can only be made public on the " +
		"official MassIVE web server. Would you like to go there now?";

    var makepublic_link = document.createElement("a");
    makepublic_link.innerHTML = "Make Public";
    makepublic_link.href= url;

    makepublic_link.onclick = function() {
    	return confirm(confirm_message);

    };

    div.appendChild(makepublic_link);
}

function create_import_information(div, dataset_name, computable, is_massive_site){
	var import_dataset_button = document.createElement('input');
    import_dataset_button.setAttribute('type','button');
    import_dataset_button.setAttribute('name','Analyze Submitted Spectra');
    import_dataset_button.setAttribute('value','Analyze Submitted Spectra');

    import_dataset_button.onclick = function(){
    	$.ajax({
            url: "/ProteoSAFe/ManageSharing",
            data: { importUser: dataset_name },
            complete: function(data, status){
                window.location = 'http://proteomics2.ucsd.edu/ProteoSAFe/index.jsp?params=%7B"workflow":"MSGFDB", "spec_on_server":"d.' + dataset_name +'/peak;"%7D';
            },
            type: "POST"
        });
	};

    div.appendChild(import_dataset_button);
    var spacer = document.createElement("span");
    spacer.style.paddingRight = "10px";
    div.appendChild(spacer);


    if(computable == "true"){

        var analyze_dataset_button = document.createElement('input');
        analyze_dataset_button.setAttribute('type','button');
        analyze_dataset_button.setAttribute('name','Import and Analyze Dataset with Networking Now!');
        analyze_dataset_button.setAttribute('value','Import and Analyze Dataset with Networking Now!');


        analyze_dataset_button.onclick = function(){
            $.ajax({
                url: "/ProteoSAFe/ManageSharing",
                data: { importUser: dataset_name },
                complete: function(data, status){
                    spec_on_server_string = '"d.' + dataset_name +'/ccms_peak;d.' + dataset_name +'/peak;"%7D'
                    window.location = 'http://gnps.ucsd.edu/ProteoSAFe/index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS", "spec_on_server":' + spec_on_server_string;
                },
                type: "POST"
            });
        };

        var analyze_proteomics_dataset_button = document.createElement('input');
        analyze_proteomics_dataset_button.setAttribute('type','button');
        analyze_proteomics_dataset_button.setAttribute('name','Analyze CCMS Converted Spectra');
        analyze_proteomics_dataset_button.setAttribute('value','Analyze CCMS Converted Spectra');


        analyze_proteomics_dataset_button.onclick = function(){
            $.ajax({
                url: "/ProteoSAFe/ManageSharing",
                data: { importUser: dataset_name },
                complete: function(data, status){
                    window.location = 'http://proteomics2.ucsd.edu/ProteoSAFe/index.jsp?params=%7B"workflow":"MSGFDB", "spec_on_server":"d.' + dataset_name +'/ccms_peak;"%7D';
                },
                type: "POST"
            });
        };

        div.appendChild(analyze_proteomics_dataset_button);
        if (!is_massive_site) {
            div.appendChild(document.createElement("br"));
            div.appendChild(analyze_dataset_button);
        }
    } else {
    	var request_convert_link = document.createElement("span");
    	request_convert_link.innerHTML = '<a href="mailto:ccms-web@cs.ucsd.edu?Subject=Dataset ' +  dataset_name + ' Conversion Request" target="_top">Email CCMS</a> to request spectrum conversion to standard format';
        div.appendChild(request_convert_link);
    }
}

ResultViewFileMassive.prototype.create_massive_table = function(div){
    massive_table = document.createElement("table");
    massive_table.border = "0";
    massive_table.cellspacing = "1";
    massive_table.cellpadding = "4";
    massive_table.width = "957px";
    massive_table.className = "mainform";
    massive_table.align = "center";

    //Tbody
    massive_information_tbody = document.createElement("tbody");
    massive_table.appendChild(massive_information_tbody);

    //Header row
    massive_table_header_row = document.createElement("tr");
    massive_table_header_row_header = document.createElement("th");
    massive_table_header_row_header.innerHTML = "MassIVE Dataset Information";
    massive_table_header_row.appendChild(massive_table_header_row_header);
    massive_information_tbody.appendChild(massive_table_header_row);

    //Content Row
    massive_table_info_row = document.createElement("tr");
    massive_table_info_row_content = document.createElement("td");
    massive_table_info_row.appendChild(massive_table_info_row_content);
    massive_information_tbody.appendChild(massive_table_info_row);

    //Header row
    massive_table_footer_row = document.createElement("tr");
    massive_table_footer_row_header = document.createElement("td");
    massive_table_footer_row_header.className = "bottomline";
    massive_table_footer_row.appendChild(massive_table_footer_row_header);
    massive_information_tbody.appendChild(massive_table_footer_row);

    this.create_massive_information(massive_table_info_row_content);
    div.appendChild(massive_table);
}

ResultViewFileMassive.prototype.create_massive_information = function(div){
    main_context = this;

    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/MassiveServlet",
        data: { task: task_id, function: "massiveinformation"},
        cache: false,
        async: false,
            success: function(context, div){

                return function(json){
                if(json.slice(0,5) == "ERROR"){
                    //alert("error");
                    console.log("error");
                    return
                }

                dataset_information = JSON.parse(json);

                // create table
                massive_information_table = document.createElement("table");
                massive_information_table.border = "1";
                massive_information_table.cellspacing = "1";
                massive_information_table.cellpadding = "4";
                massive_information_table.width = "100%";
                massive_information_table.className = "sched";
                massive_information_table.align = "center";

                // create table body
                massive_information_tbody = document.createElement("tbody");
                massive_information_table.appendChild(
                	massive_information_tbody);

                // title
                dataset_title_row = document.createElement("tr");
                dataset_title_row_header = document.createElement("th");
                dataset_title_row_header.innerHTML = "Title";
                dataset_title_row.appendChild(dataset_title_row_header);
                dataset_title_row_content = document.createElement("td");
                dataset_title_row_content.innerHTML =
                	dataset_information['title'];
                dataset_title_row.appendChild(dataset_title_row_content);
                massive_information_tbody.appendChild(dataset_title_row);


                // description
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Description";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                if (dataset_information['description'] != "null") {
                    dataset_row_content.innerHTML =
                    	dataset_information['description'];
                }
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // MassIVE ID
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "MassIVE Accession";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['dataset_id'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // PX ID
                if (dataset_information['pxaccession'] != "null") {
                    dataset_row = document.createElement("tr");
                    dataset_row_header = document.createElement("th");
                    dataset_row_header.innerHTML = "ProteomeXchange Accession";
                    dataset_row.appendChild(dataset_row_header);
                    dataset_row_content = document.createElement("td");
                    var dataset_px_link = document.createElement('a');
                    dataset_px_link.textContent =
                    	dataset_information['pxaccession'];
                    dataset_px_link.href =
                    	"http://proteomecentral.proteomexchange.org" +
                    	"/cgi/GetDataset?ID=" +
                    	dataset_information['pxaccession'];
                    dataset_px_link.target = "_blank";
                    dataset_row_content.appendChild(dataset_px_link);
                    dataset_row.appendChild(dataset_row_content);
                    massive_information_tbody.appendChild(dataset_row);
                }

                // make public
                if (dataset_information['private'] == "true" &&
                	dataset_information['has_access'] == "true") {
                    dataset_row = document.createElement("tr");
                    dataset_row_header = document.createElement("th");
                    dataset_row_header.innerHTML = "Make Public";
                    dataset_row.appendChild(dataset_row_header);
                    dataset_row_content = document.createElement("td");
                    dataset_row.appendChild(dataset_row_content);
                    massive_information_tbody.appendChild(dataset_row);
                    main_context.create_makepublic_link(dataset_row_content,
                    	dataset_information['pxaccession']);
                }

                // principal investigator
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Principal Investigators";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                if (dataset_information['pi'] != "null") {
                	dataset_row_content.innerHTML = dataset_information['pi'];
                }
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // user
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Username";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                var dataset_user_link = document.createElement('a');
                dataset_user_link.textContent = (dataset_information['user']);
                dataset_user_link.href = '/ProteoSAFe/user/summary.jsp?user=' +
                	dataset_information['user'];
                dataset_row_content.appendChild(dataset_user_link);
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                //user email
                if(dataset_information['email'] != null){
                    dataset_row = document.createElement("tr");
                    dataset_row_header = document.createElement("th");
                    dataset_row_header.innerHTML = "Contact Email";
                    dataset_row.appendChild(dataset_row_header);
                    dataset_row_content = document.createElement("td");
                    var dataset_user_link = document.createElement('a');
                    dataset_user_link.textContent = (dataset_information['email']);
                    dataset_user_link.href = 'mailto:' + dataset_information['email'] +
                        '?Subject=Let\'s Collaborate on Dataset ' +
                        dataset_information['dataset_id'];
                    dataset_row_content.appendChild(dataset_user_link);
                    dataset_row.appendChild(dataset_row_content);
                    massive_information_tbody.appendChild(dataset_row);
                }

                // species
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Species";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML = dataset_information['species'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // instruments
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Instrument";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['instrument'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // PTMs
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML =
                	"Post-Translational Modifications";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['modifications'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // PTMs
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML =
                	"Keywords";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['keywords'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // file count
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Number of Files";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['filecount'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // total file size
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Total Size";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML = dataset_information['filesize'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // number of subscribers
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Subscribers";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.innerHTML =
                	dataset_information['subscriptions'];
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // subscription status
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Subscription Status";
                dataset_row.appendChild(dataset_row_header);
                dataset_subscribe_row_content = document.createElement("td");
                dataset_subscribe_row_content.id = "subscribe_button";
                dataset_row.appendChild(dataset_subscribe_row_content);
                massive_information_tbody.appendChild(dataset_row);
                create_subscribe_buttons(dataset_subscribe_row_content);

                // import and analyze
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Analyze Data";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);

                // JSONP to accurate get information
                context.update_computable_information(
                	dataset_row_content, context.is_massive_site());

                // FTP link
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "FTP Download";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);
                ftp_url = dataset_information['ftp'];
                //ftp_url = ftp_url.replace("ccms-ftp", "massive")
                create_ftp_download_link(
                	dataset_row_content, ftp_url);

                // browse mzTab results (if complete submission)
                if (dataset_information['complete'] == "true") {
                	dataset_row = document.createElement("tr");
                	dataset_row_header = document.createElement("th");
                	dataset_row_header.innerHTML = "Browse Results";
                	dataset_row.appendChild(dataset_row_header);
                	dataset_row_content = document.createElement("td");
                	dataset_row.appendChild(dataset_row_content);
                	massive_information_tbody.appendChild(dataset_row);
                	create_browse_results_link(
                			dataset_row_content, dataset_information['task']);
                }

                // other actions - add/update metadata, publications, etc.
                if (dataset_information['has_access'] == "true") {
                	dataset_row = document.createElement("tr");
                	dataset_row_header = document.createElement("th");
                	dataset_row_header.innerHTML = "Other Dataset Actions";
                	dataset_row.appendChild(dataset_row_header);
                	dataset_row_content = document.createElement("td");
                	dataset_row.appendChild(dataset_row_content);
                	massive_information_tbody.appendChild(dataset_row);
                	create_otheractions_link(dataset_row_content);
                }

                // add comment
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Add Comment";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);
                main_context.create_comment_link(dataset_row_content);

                // publications
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Publications";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);
                main_context.create_publications_display(
                	dataset_row_content, dataset_information['publications']);

                /*
                //FDR
                dataset_row = document.createElement("tr");
                dataset_row_header = document.createElement("th");
                dataset_row_header.innerHTML = "Continuous ID Experimental FDR";
                dataset_row.appendChild(dataset_row_header);
                dataset_row_content = document.createElement("td");
                dataset_row_content.id = "ContinuousIDFDRDisplay";
                dataset_row.appendChild(dataset_row_content);
                massive_information_tbody.appendChild(dataset_row);
                */

                div.appendChild(massive_information_table);

                // final div style
                div.style.textAlign = "center";
            }
        }(this, div)
    });

}

ResultViewFileMassive.prototype.update_computable_information = function(analyze_data_div, is_massive_site){
    remote_url = "http://gnps.ucsd.edu"
    fetch_url = remote_url + "/ProteoSAFe/MassiveServlet"

    $.ajax({
        url: fetch_url,
        jsonp: "callback",
        dataType: "jsonp",
        data: {
            "function": "massiveinformation",
            "task": this.task.id
        },
        success: function(analyze_data_div, is_massive_site){
            return function(response){
                create_import_information(analyze_data_div, response['dataset_id'], response['convertedandcomputable'], is_massive_site);
            }
        }(analyze_data_div)
    })
}

ResultViewFileMassive.prototype.create_ratings_table = function(div, massiveID){
    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/ContinuousIDRatingSummaryServlet",
        data: { dataset_id: massiveIDToString(this.massiveID), summary_type: "per_dataset"},
        cache: false,
        success: function(div, context){
            return function(json){
                obj = JSON.parse(json);

                map_output = format_ratings_into_groupby(obj.ratings)
                output_render_allratings = map_output.allratings
                output_render_groupratings = map_output.groupratings


                var task = new Object();
                task.id = "12345";
                task.workflow = "Spectrum Ratings";
                task.description = "Spectrum Ratings";
                var column_handler_second = function(block, parameters){
                    return get_constituent_ratings_XML(output_render_allratings, parameters);
                };
                var generic_table = new ResultViewTableGen(context.massive_dataset_ratings_tableXML(), "spectrum_ratings", task, 0, column_handler_second);

                generic_table.setData(output_render_groupratings);
                generic_table.render(div, 0);
            }
        }(div, this)
    });
}


ResultViewFileMassive.prototype.create_related_datasets_table = function(div){
    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_dataset_network",
        cache: false,
        success: function(context, div){
            return function(html){
                results_data = get_block_data_from_page(html);

                related_datasets = new Array()

                for(var pair_index in results_data){
                    if(results_data[pair_index].Dataset1 == massiveIDToString(context.massiveID)){
                        relation = new Object();
                        relation.dataset_name = results_data[pair_index].Dataset2
                        relation.mydatasetname = massiveIDToString(context.massiveID)
                        relation.score = parseInt(results_data[pair_index].SharedCompounds)
                        relation.scorestring = results_data[pair_index].SharedCompounds
                        relation.id = pair_index
                        if(relation.score > 0){
                            related_datasets.push(relation)
                        }
                    }
                    if(results_data[pair_index].Dataset2 == massiveIDToString(context.massiveID)){
                        relation = new Object();
                        relation.dataset_name = results_data[pair_index].Dataset1
                        relation.mydatasetname = massiveIDToString(context.massiveID)
                        relation.score = parseInt(results_data[pair_index].SharedCompounds)
                        relation.scorestring = results_data[pair_index].SharedCompounds
                        relation.id = pair_index
                        if(relation.score > 0){
                            related_datasets.push(relation)
                        }
                    }
                }

                //sort by descending score
                related_datasets.sort(function(a,b){return b.score - a.score})

                //We have found it, lets render, otherwise lets not
                child_header = document.createElement("h2");
                child_header.innerHTML = "Related Datasets";
                div.appendChild(child_header);

                child_table = document.createElement("div");
                div.appendChild(child_table);

                var task = new Object();
                task.id = "1234";
                task.workflow = "Related Datasets";
                task.description = "Related Datasets";
                var generic_table = new ResultViewTableGen(context.related_dataset_tableXML(), "related_datasets", task, 0);
                generic_table.setData(related_datasets);
                generic_table.render(child_table, 0);
            }
        }(this, div)
    });
}

ResultViewFileMassive.prototype.related_dataset_tableXML = function(){
    var tableXML_str = '<block id="dataset_compounds" type="table"> \
                            <row>  \
                                <column field="dataset_name" label="Dataset" type="text" width="5"/> \
                                <column label="Score" type="genericurlgenerator" width="16"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/dataset_comparison.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=dataset1" value="[dataset_name]"/>\
                                    <parameter name="REQUESTPARAMETER=dataset2" value="[mydatasetname]"/>\
                                    <parameter name="LABEL" value="[scorestring]"/>\
                                </column>\
                                <column type="datasetFieldWithMassiveID" label="Description"> \
                                    <parameter name="DATASET" value="[dataset_name]"/> \
                                    <parameter name="DISPLAYFIELD" value="title"/> \
                                </column> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

ResultViewFileMassive.prototype.massive_dataset_ratings_tableXML = function(){
    var tableXML_str = '<block id="spectrum_ratings" type="table"> \
                            <row>  \
                                <column field="scan" label="scan" type="text" width="5"/> \
                                <column label="Avg Rating" type="ratydisplayrating"> \
                                    <parameter name="rating" value="[averagerating]"/>\
                                    <parameter name="maxrating" value="4"/>\
                                </column>\
                                <column field="spectrum_id" label="SpecID" type="text" width="15"/> \
                                <column label="View Library" type="genericurlgenerator" width="15"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=SpectrumID" value="[spectrum_id]"/>\
                                    <parameter name="LABEL" value="View Library"/>\
                                </column>\
                            </row>\
                            <row expander="Show Raters:Hide Raters" expandericontype="text">\
                                <column type="callbackblock" block="constituent_ratings" colspan="7">\
                                    <parameter name="unique_key" value="[unique_key]"/>\
                                </column>\
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

// render the streamed file
ResultViewFileMassive.prototype.render = function(div, index) {
    if (div != null)
        this.div = div;
    if (this.div == null) {
        alert("No div was provided under which to render this result block.");
        return;
    }
    if (index != null)
        this.index = index;
    if (this.index == null)
        this.index = 0;
    // if this file is already present, remove it
    var child = getChildById(this.div, this.id);
    if (child != null)
        this.div.removeChild(child);

    visualization_container = document.createElement("div");
    visualization_container.id = "visualization_container"
    visualization_container.style.width = "990px"
    visualization_container.style.marginLeft = "auto"
    visualization_container.style.marginRight = "auto"

    div.appendChild(visualization_container)

    visualization_container.appendChild(document.createElement("hr"));

    //Adding Massive Information On Top
    massive_info_div = document.createElement("div");
    massive_info_div.id = this.id;
    visualization_container.appendChild(massive_info_div);

    // add a new child div for this file
    subscribe_box = document.createElement("div");
    subscribe_box.id = this.id;
    visualization_container.appendChild(subscribe_box);




    // retrieve the file from the server and display it
    var task = this.task.id;
    var contentType = this.contentType;

    this.create_massive_table(massive_info_div);

    // continuous ID
    if (this.is_massive_site() == false) {
        child_for_CID_table_header = document.createElement("h2");
        child_for_CID_table_header.innerHTML =
        	"Dataset Continuous Identifications";
        visualization_container.appendChild(child_for_CID_table_header);
        child_for_CID_table = document.createElement("div");
        child_for_CID_table.id = this.id + "cid_table";
        visualization_container.appendChild(child_for_CID_table);
    }

    // comments
    child_for_comment_table_header = document.createElement("h2");
    child_for_comment_table_header.innerHTML = "Dataset Comments";
    visualization_container.appendChild(child_for_comment_table_header);
    child_for_comment_table = document.createElement("div");
    child_for_comment_table.id = this.id + "comment_table";
    visualization_container.appendChild(child_for_comment_table);

    // reanalyses
    child_for_reanalyses_table_header = document.createElement("h2");
    child_for_reanalyses_table_header.innerHTML = "Dataset Reanalyses";
    visualization_container.appendChild(child_for_reanalyses_table_header);
    child_for_reanalyses_table = document.createElement("div");
    child_for_reanalyses_table.id = this.id + "reanalyses_table";
    visualization_container.appendChild(child_for_reanalyses_table);

    // ID ratings
    if (this.is_massive_site() == false) {
        table_header = document.createElement("h2");
        table_header.innerHTML = "Dataset ID Ratings";
        visualization_container.appendChild(table_header);
        ratings_table = document.createElement("div");
        ratings_table.id = this.id + "ratings table";
        visualization_container.appendChild(ratings_table);
    }

    // related datasets
    related_datasets_table = document.createElement("div");
    related_datasets_table.id = this.id + "related_datasets_table";
    visualization_container.appendChild(related_datasets_table);

    // render subsections
    if (this.is_massive_site() == false)
    	this.get_jobs(child_for_CID_table);
    this.get_comments(child_for_comment_table);
    this.get_reanalyses(child_for_reanalyses_table);
    if (this.is_massive_site() == false)
    	this.create_ratings_table(ratings_table);
    this.create_related_datasets_table(related_datasets_table);

    // add a spacer row at the bottom of the page
    visualization_container.appendChild(document.createElement("br"));
}

ResultViewFileMassive.prototype.is_massive_site = function(){
    if(this.massive_page == true){
        return true;
    }

    var window_hash = location.hash;
    if(window_hash == "#MASSIVEDEBUG"){
        return true;
    }
    return false;
}

// set data to the file streamer
ResultViewFileMassive.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["massivepage"] = ResultViewFileMassive;
