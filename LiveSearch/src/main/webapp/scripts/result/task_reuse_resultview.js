function ResultViewTaskReuse(blockXML, id, task) {
    // properties
    this.id = id;
    this.div = null;

    this.init(blockXML);
}

// initialize block from XML specification
ResultViewTaskReuse.prototype.init = function(blockXML) {
}



// render the streamed file
ResultViewTaskReuse.prototype.render = function(div, index) {
    reanalyze_msgfdb_button = document.createElement("button");
    reanalyze_msgfdb_button.innerHTML = "Reanalyze MSGFDB";

    reanalyze_msgfdb_button.onclick = function(){
        task_id = get_taskid();
        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/ManageSharing",
            data: { importUser: task_id},
            cache: false,
            success: function(task_id){
                return function(returned){
                    //returned_object = JSON.parse(returned)
                    preset_path = "f." + task_id + "/spectra/specs_ms.mgf"
                    reanalysis_description = "Molecular Networking Cluster Re-analysis with MSGFDB from task " + task_id;

                    parameters_map = new Object();
                    parameters_map["workflow"] = "MSGFDB"
                    parameters_map["desc"] = reanalysis_description
                    parameters_map["spec_on_server"] = preset_path

                    parameters_string = encodeURIComponent(JSON.stringify(parameters_map))

                    analyze_url = "http://proteomics2.ucsd.edu/ProteoSAFe/?requirelogin=true&params=" + parameters_string
                    window.location.href = analyze_url;
                }
            }(task_id),
            failure: function(returned){
                
            }
        });
    }

    div.appendChild(reanalyze_msgfdb_button);


    //PEPNOVO
    reanalyze_msgfdb_button = document.createElement("button");
    reanalyze_msgfdb_button.innerHTML = "Reanalyze PEPNOVO";

    reanalyze_msgfdb_button.onclick = function(){
        task_id = get_taskid();
        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/TaskReuseServlet",
            data: { task: task_id},
            cache: false,
            success: function(returned){
                returned_object = JSON.parse(returned)

                if(returned_object.status == "success"){
                    preset_path = "f." + returned_object.path;
                    reanalysis_description = "Molecular Networking Cluster Re-analysis with PepNovo from task " + task_id;
                    analyze_url = "http://proteomics2.ucsd.edu/ProteoSAFe/?requirelogin=true&params=" + "%7B%22workflow%22%3A%22PEPNOVO%22%2C%22desc%22%3A%22" +  reanalysis_description + "%22%2C%22spec_on_server%22%3A%22" + preset_path.escapeSpecialChars() + "%3B%22%7D"
                    window.location.href = analyze_url;
                }
                else{
                    alert("Please Login");
                }
            }
        });
    }

    div.appendChild(reanalyze_msgfdb_button);
}

// set data to the file streamer
ResultViewTaskReuse.prototype.setData = function(data) {
    this.data = data;
}


// assign this view implementation to block type "network_displayer"
resultViewBlocks["task_data_reuse"] = ResultViewTaskReuse;
