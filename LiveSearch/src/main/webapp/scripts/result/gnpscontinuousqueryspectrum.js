/**
 * File stream result view block implementation
 */
// constructor
function ResultViewGNPSContinuousQuerySpectrum(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewGNPSContinuousQuerySpectrum.prototype.init = function(blockXML) {

}


// set data to the file streamer
ResultViewGNPSContinuousQuerySpectrum.prototype.setData = function(data) {
    this.data = data;
}

//Given task id of the massive dataset, queries for all continuous identification jobs
ResultViewGNPSContinuousQuerySpectrum.prototype.get_reported_jobs = function(task_id){
    //Doing jquery

    return_jobs = new Array();

    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/ContinuousIDServlet",
        data: { task: task_id},
        cache: false,
        async: false,
        success: function(json){
            jobs_json_object = JSON.parse(json);

            var jobs = jobs_json_object["jobs"];

            for(i = 0; i < jobs.length; i++){
                jobs[i].id = i;

                var hostname = get_host_url(jobs[i]["execution_site"]);
                hostname += "ProteoSAFe/";
                jobs[i].resulturl = encodeURI(hostname + "result_json.jsp?task=" + jobs[i]["task"] + "&view=group_by_spectrum_all_beta");
                jobs[i].task_id = jobs[i]["task"]
            }

            var filtered_jobs = new Array();
            for(i = 0; i < jobs.length;i++){
                if(jobs[i].reported == "1"){
                    filtered_jobs.push(jobs[i]);
                }
            }

            //Doing shit with it
            return_jobs = filtered_jobs;
        }
    });

    return return_jobs;
}

ResultViewGNPSContinuousQuerySpectrum.prototype.render_revision_history = function(reported_jobs, scan, div){
    //We have jobs, so lets go and ping them all
    context = this;
    context.job_returned_count = 0
    context.job_total = reported_jobs.length
    for(job in reported_jobs){
        job_result_url = reported_jobs[job].resulturl
        $.ajax({
            url: job_result_url,
            cache: false,
            success: function(context, job_object, all_jobs, scan, div){
                return function(data){
                    context.job_returned_count += 1
                    job_results = JSON.parse(data);
                    job_object.results = job_results
                    if(context.job_returned_count == context.job_total){
                        console.log(reported_jobs);
                        context.find_history_of_scan(all_jobs, scan, div);
                        //Lets now pull out the scan of interest
                    }
                }
            }(context, reported_jobs[job], reported_jobs, scan, div)
        });
    }
}

ResultViewGNPSContinuousQuerySpectrum.prototype.find_history_of_scan = function(reported_jobs, scan, div){
    job_results = new Array();

    //Lets assume jobs are sorted
    for(i in reported_jobs){
        job = reported_jobs[i];

        for(result_i in job.results["blockData"]){
            if(scan == job.results["blockData"][result_i]["#Scan#"]){
                scan_identification = new Object();
                scan_identification.Compound_Name = job.results["blockData"][result_i]["Compound_Name"]
                scan_identification.SpectrumID = job.results["blockData"][result_i]["SpectrumID"]
                scan_identification.timestamp = job["timestamp"]
                scan_identification.task_id = job["task_id"]
                job_results.push(scan_identification)
                break;
            }
        }
    }

    console.log(job_results)
    var task = new Object();
    task.id = "1234";
    task.workflow = "MassIVE Comments";
    task.description = "MassIVE Comments";
    var generic_table = new ResultViewTableGen(this.revision_history_XML(), "massive_comments", task, 0);
    generic_table.setData(job_results);
    generic_table.render(div, 0);

}

ResultViewGNPSContinuousQuerySpectrum.prototype.revision_history_XML = function(){
    var tableXML_str = '<block id="spectrum_annotations" type="table"> \
                            <row>  \
                                <column field="Compound_Name" label="Compound Name" type="text" width="5"/> \
                                <column field="SpectrumID" label="SpectrumID" type="text" width="5"/> \
                                <column field="timestamp" label="timestamp" type="text" width="5"/> \
                                <column field="task_id" label="task_id" type="text" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

// render the streamed file
ResultViewGNPSContinuousQuerySpectrum.prototype.render = function(div, index) {
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

    // add a new child div for this file
    var child = document.createElement("div");
    child.id = this.id;
    div.appendChild(child);


    dataset_id = get_URL_parameter("dataset").replace("_specs_ms.mgf", "");
    scan_number = parseInt(get_URL_parameter("scan"));

    //Updating Header
    $("#header").html("GNPS Dataset " + dataset_id + " scan " + scan_number + " Annotation History");


    task_id = ""
    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/MassiveServlet",
        data: { massiveid: dataset_id, function: "massiveidtotask"},
        async: false,
        success: function(response){
            response_obj = JSON.parse(response);
            task_id = response_obj.massive_task_id;
        }
    });




    reported_jobs = this.get_reported_jobs(task_id)
    console.log(reported_jobs);


    this.render_revision_history(reported_jobs, scan_number, child);






    //Div for molecule explorer
    var molecule_explorer_child = document.createElement("div");
    div.appendChild(molecule_explorer_child);

    var other_spectra_child = document.createElement("div");
    div.appendChild(other_spectra_child);


    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));


}

// assign this view implementation to block type "stream"
resultViewBlocks["gnpscontinuousqueryspectrum"] = ResultViewGNPSContinuousQuerySpectrum;
