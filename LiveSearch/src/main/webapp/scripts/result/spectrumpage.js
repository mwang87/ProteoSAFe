/**
 * File stream result view block implementation
 */
// constructor
function ResultViewFileSpectrum(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewFileSpectrum.prototype.init = function(blockXML) {

}


function get_taskid(){
   // return get_URL_parameter("task");
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  var task_id = "";
  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
    if(pair[0] == "task"){
      task_id = pair[1];
      return task_id;
    }
  }
  return "";
}

function get_SpectrumID(){
    //return get_URL_parameter("SpectrumID");
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  var spec_id = "";
  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
    if(pair[0] == "SpectrumID"){
      spec_id = pair[1];
      return spec_id;
    }
  }
  return "";
}

function get_URL_parameter(parameter_name){
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    var spec_id = "";
    for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        if(pair[0] == parameter_name){
        spec_id = pair[1];
        return spec_id;
        }
    }
    return "";
}

function get_spectrumpage_tableXML(){
    var tableXML_str = '<block id="spectrum_annotations" type="table"> \
                            <row>  \
                                <column type="mingplugin" target="/" label="Comment"> \
                                    <parameter name="workflow" value="ADD-ANNOTATION-COMMENT"/> \
                                    <parameter name="SPECTRUMID" value="[SpectrumID]"/> \
                                    <parameter name="ANNOTATION_TASK" value="[task_id]"/> \
                                </column> \
                                <column field="Compound_Name" label="Compound Name" type="text" width="5"/> \
                                <column field="PI" label="PI" type="text" width="5"/> \
                                <column field="Adduct" label="Adduct" type="text" width="5"/> \
                                <column field="Data_Collector" label="Data_Collector" type="text" width="5"/> \
                                <column field="Precursor_MZ" label="Precursor_MZ" type="text" width="5"/> \
                                <column field="user_id" label="user_id" type="text" width="5"/> \
                                <column field="Pubmed_ID" label="Pubmed ID" type="text" width="5"/> \
                                <column field="create_time" label="Time" type="text" width="10"/> \
                            </row>\
                            <row expander="Show Comments:Hide Comments" expandericontype="text">\
                                <column type="callbackblock" block="spectrum_annotations_comments" colspan="7">\
                                    <parameter name="annotation_task_id" value="[task_id]"/>\
                                </column>\
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

function get_comments_XML(comments, parameters){
    comments_filtered = [];


    for(i = 0; i < comments.length; i++){
        var keep = true;
        for(parameter in parameters){
            if(parameters[parameter] != comments[i][parameter]){
                keep = false;
                break;
            }
        }
        if ( keep == true ){
            comments_filtered.push(comments[i]);
        }
    }

    var tableXML_str = '<blockInstance> \
                            <block id="spectrum_annotations_comments" type="table"> \
                                <row> \
                                    <column field="comment" label="comment" type="text" width="5"/> \
                                    <column field="user_id" label="User" type="text" width="5"/> \
                                    <column type="absolutelink" label="View Attachments"> \
                                        <parameter name="link" value="[taskurl]"/> \
                                    </column> \
                                    <column field="create_time" label="Time" type="text" width="10"/> \
                                </row> \
                            </block>\n \
                        <blockData>\n';
    tableXML_str += JSON.stringify(comments_filtered);
    tableXML_str += '\n</blockData></blockInstance>\n';
    return (parseXML(tableXML_str));
}

//Blocking AJAX call will populate object with the latest annotation
ResultViewFileSpectrum.prototype.get_annotation_comments = function(div){
    spec_id = get_SpectrumID();


    //Creating the outer Spectrum Information Table
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
    massive_table_header_row_header.innerHTML = "Latest Library Spectrum Information";
    massive_table_header_row.appendChild(massive_table_header_row_header);
    massive_information_tbody.appendChild(massive_table_header_row);

    //Content Row
    massive_table_info_row = document.createElement("tr");
    massive_table_info_row_content = document.createElement("td");
    massive_table_info_row.appendChild(massive_table_info_row_content);
    massive_information_tbody.appendChild(massive_table_info_row);
    //this.create_massive_information(massive_table_info_row_content);

    //Header row
    massive_table_footer_row = document.createElement("tr");
    massive_table_footer_row_header = document.createElement("td");
    massive_table_footer_row_header.className = "bottomline";
    massive_table_footer_row.appendChild(massive_table_footer_row_header);
    massive_information_tbody.appendChild(massive_table_footer_row);

    context = this

    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/SpectrumCommentServlet",
        data: { SpectrumID: spec_id},
        cache: false,
        async: false,
        success: function(json){
            jobs_json_object = JSON.parse(json);

            annotations = jobs_json_object["annotations"];
            comments = jobs_json_object["comments"];
            specinfo = jobs_json_object["spectruminfo"];
            spectrum_tags = jobs_json_object["spectrum_tags"];
            canAdmin = jobs_json_object["canAdmin"];

            context.render_spectrumviewer(div, specinfo);


            //Displaying Table for Spectrum Information
            //Creating the Table
            spectrum_information_table = document.createElement("table");
            spectrum_information_table.border = "1";
            spectrum_information_table.cellspacing = "1";
            spectrum_information_table.cellpadding = "4";
            spectrum_information_table.width = "100%";
            spectrum_information_table.className = "sched";
            spectrum_information_table.align = "center";

            //Tbody
            spectrum_information_tbody = document.createElement("tbody");
            spectrum_information_table.appendChild(spectrum_information_tbody);

            massive_table_info_row_content.appendChild(spectrum_information_table);
            div.appendChild(massive_table)

            //Computation to Prepare for Annotations Table

            for(i = 0; i < annotations.length; i++){
                annotations[i].id = i;

                var workflow_fillin = {};
                workflow_fillin["workflow"] = "ADD-ANNOTATION-COMMENT";
                workflow_fillin["ANNOTATION_TASK"] = annotations[i]["task_id"];
                workflow_fillin["SPECTRUMID"] = annotations[i]["SpectrumID"];
                var comment_url = "index.jsp?params=" + JSON.stringify(workflow_fillin);

                annotations[i].addcommenturl = encodeURI(comment_url);
            }

            //Sorting Annotations by date
            annotations.sort(function(a,b){
                return new Date(b["create_time"]) - new Date(a["create_time"]);
            });

            for(i = 0; i < comments.length; i++){
                comments[i].id = i;

                var hostname = get_host_url(comments[i]["execution_site"]);
                hostname += "ProteoSAFe/";
                comments[i].taskurl = encodeURI(hostname + "status.jsp?task=" + comments[i]["task"]);
            }

            var column_handler = function(block, parameters){
                return get_comments_XML(comments, parameters);
            };

            var task = new Object();
            task.id = "1234";
            task.workflow = "Spectrum Annotations and Comments";
            task.description = "Spectrum Annotations and Comments";
            var generic_table = new ResultViewTableGen(get_spectrumpage_tableXML(), "table1", task, 0, column_handler);

            //Grabbing Oldest and Newest Annotation
            newest_annotation = annotations[0]
            oldest_annotation = annotations[annotations.length-1]

            //Adding relevant Fields to Table
            //Table Content
            add_spectrum_information_table(spectrum_information_tbody, "Spectrum ID", newest_annotation.SpectrumID)
            context.add_appropriate_compound_linkouts(spectrum_information_tbody, "Compound Name", newest_annotation.Compound_Name)
            add_spectrum_information_table(spectrum_information_tbody, "PI", newest_annotation.PI)
            add_spectrum_information_table(spectrum_information_tbody, "Data Collector", newest_annotation.Data_Collector)

            if(newest_annotation.Pubmed_ID != null && newest_annotation.Pubmed_ID != "N/A"){
                pubmed_url = 'http://www.ncbi.nlm.nih.gov/pubmed/?term=' + newest_annotation.Pubmed_ID
                context.add_linkout_url(spectrum_information_tbody, "Pubmed", newest_annotation.Pubmed_ID, pubmed_url)
            }

            if(newest_annotation.CAS_Number != null && newest_annotation.CAS_Number != "N/A"){
                cas_url = 'http://pubchem.ncbi.nlm.nih.gov/search/#collection=compounds&query_type=text&query=' + newest_annotation.CAS_Number
                context.add_linkout_url(spectrum_information_tbody, "CAS Number", newest_annotation.CAS_Number, cas_url)
            }
            else{
                add_spectrum_information_table(spectrum_information_tbody, "CAS Number", newest_annotation.CAS_Number)
            }
            add_linkout_to_table(spectrum_information_tbody, "Original Submitter", oldest_annotation.user_id, "/ProteoSAFe/user/summary.jsp?user=" + oldest_annotation.user_id)
            if(oldest_annotation.user_email != null){
                add_linkout_to_table(spectrum_information_tbody, "Original Submitter Email", oldest_annotation.user_email, "mailto:" + oldest_annotation.user_email + "?Subject=GNPS%20Spectrum%20" + newest_annotation.SpectrumID)
            }
            add_linkout_to_table(spectrum_information_tbody, "Most Recent Revisor", newest_annotation.user_id, "/ProteoSAFe/user/summary.jsp?user=" + newest_annotation.user_id)
            if(newest_annotation.user_email != null){
                add_linkout_to_table(spectrum_information_tbody, "Most Recent Revisor Email", oldest_annotation.user_email, "mailto:" + newest_annotation.user_email + "?Subject=GNPS%20Spectrum%20" + newest_annotation.SpectrumID)
            }

            context.displaylibraryquality(spectrum_information_tbody, newest_annotation.Library_Class)

            add_spectrum_information_table(spectrum_information_tbody, "Smiles", newest_annotation.Smiles)
            add_spectrum_information_table(spectrum_information_tbody, "InChI", newest_annotation.INCHI)
            add_structure_to_table(spectrum_information_tbody, "Structure", newest_annotation.Smiles, newest_annotation.INCHI)
            add_spectrum_information_table(spectrum_information_tbody, "Precursor M/Z", newest_annotation.Precursor_MZ)
            add_spectrum_information_table(spectrum_information_tbody, "Exact Mass", newest_annotation.ExactMass)
            add_spectrum_information_table(spectrum_information_tbody, "Charge", newest_annotation.Charge)
            add_spectrum_information_table(spectrum_information_tbody, "Adduct", newest_annotation.Adduct)
            add_spectrum_information_table(spectrum_information_tbody, "Ion Source", newest_annotation.Ion_Source)
            add_spectrum_information_table(spectrum_information_tbody, "Instrument", newest_annotation.Instrument)
            add_spectrum_information_table(spectrum_information_tbody, "Ion Mode", newest_annotation.Ion_Mode)


            provenance_url = "ftp://ccms-ftp.ucsd.edu/GNPS_Library_Provenance/" + specinfo.task + "/" + encodeURIComponent(specinfo.source_file.replace(/^.*[\\\/]/, '').replace(';', ''))
            context.add_download_to_table(spectrum_information_tbody, "Spectrum Provenance Input File", "Download Provenance File", provenance_url, specinfo.source_file.replace(/^.*[\\\/]/, '').replace(';', ''))
            context.add_download_spectrum_peaks(spectrum_information_tbody, JSON.parse(specinfo.peaks_json), newest_annotation.Precursor_MZ, newest_annotation.Charge, specinfo.spectrum_id)

            add_spectrum_information_table(spectrum_information_tbody, "Library Membership", specinfo.library_membership)
            add_spectrum_information_table(spectrum_information_tbody, "SPLASH Key", specinfo.splash)

            add_update_annotation_table(spectrum_information_tbody, "Update Annotation", newest_annotation)

            add_disable_spectrum(spectrum_information_tbody, "Disable/Enable Spectrum", specinfo, canAdmin);

            add_makepublic_spectrum(spectrum_information_tbody, "Control Spectrum Privacy", specinfo, canAdmin);

            context.add_tag_button(spectrum_information_tbody, newest_annotation.SpectrumID)

            //Adding display for tags
            child_for_tags_table_header = document.createElement("h2");
            child_for_tags_table_header.innerHTML = "Spectrum Tags";
            div.appendChild(child_for_tags_table_header);
            context.render_tags(div, spectrum_tags)

            //Adding Headers and Annotations Table
            child_for_comment_table_header = document.createElement("h2");
            child_for_comment_table_header.innerHTML = "Spectrum Annotation History";
            div.appendChild(child_for_comment_table_header);

            comments_table = document.createElement("div");
            div.appendChild(comments_table)


            generic_table.getupdateddata(annotations);
            generic_table.render(comments_table, 0);

            //Adding headers and Ratings Summary
            child_table_header = document.createElement("h2");
            child_table_header.innerHTML = "Spectrum ID Ratings";
            div.appendChild(child_table_header);

            ratings_table = document.createElement("div")
            div.appendChild(ratings_table);

            get_and_display_ratings(ratings_table, newest_annotation.SpectrumID)

            //Filling in context
            context.annotation = newest_annotation
        }
    });
}


ResultViewFileSpectrum.prototype.render_spectrumviewer = function(div, spectrum_info){

    if (spectrum_info.peaks_json == "null"){
        console.log("Spectrum Peaks Not Present in DB, plotting from flat file");

        var specplot_picture = document.createElement("div");
        specplot_picture.id = "SpectrumPeaksDisplayDiv";
        div.appendChild(specplot_picture);

        var specplot_peaks = document.createElement("div");
        specplot_peaks.id = "SpectrumPeaksTextDiv";
        div.appendChild(specplot_peaks);

        this.backup_mgf_displaypeaks(specplot_picture, specplot_peaks);

        return;
    }
    peaks = JSON.parse(spectrum_info.peaks_json);

    peaks_display = document.createElement("div")
    peaks_display.id = "spectrum_viewer"
    //Peaks aren't empty
    div.appendChild(peaks_display);

    try{
        //Displaying the peaks
        $("#spectrum_viewer").specview( {
                            sequence: "",
                            peaks: peaks,
                            labelImmoniumIons: false,
                            width:935,
                            height:600,
                            showOptionsTable:false,
                            showIonTable:false,
                            showSequenceInfo:false
                        })
    }
    catch(err){
        console.log(err);
    }
}

//Request information from Database about ratings
function get_and_display_ratings(div, spectrumid){
    $.ajax({
        type: "GET",
        url: "/ProteoSAFe/ContinuousIDRatingSummaryServlet",
        data: { spectrum_id: spectrumid, summary_type: "per_spectrum"},
        cache: false,
        success: render_ratings_gen(div)
    });
}

function format_ratings_into_groupby(ratings_input){
    //Grouping by Dataset Scan Number
    mygroups = new Array();
    for(var i in ratings_input){
        dataset_id = ratings_input[i].dataset_id
        scan_number = parseInt(ratings_input[i].scan)
        unique_key = dataset_id + "_" + scan_number + "_" + ratings_input[i].spectrum_id
        ratings_input[i]["unique_key"] = unique_key
        ratings_input[i]["id"] = i
        if(!(unique_key in mygroups)){
            mygroups[unique_key] = []
            mygroups[unique_key]["allratings"] = []
            mygroups[unique_key]["sumratings"] = 0
            mygroups[unique_key]["countratings"] = 0
        }
        mygroups[unique_key]["allratings"].push(ratings_input[i])
        mygroups[unique_key]["sumratings"] += parseInt(ratings_input[i].rating)
        mygroups[unique_key]["countratings"] += 1
    }

    //Finding average Ratings
    allgroups_render = []
    for(var i in mygroups){
        rating_item = new Array()
        rating_item["dataset_id"] = mygroups[i]["allratings"][0].dataset_id
        rating_item["scan"] = mygroups[i]["allratings"][0].scan
        rating_item["averagerating"] = (mygroups[i]["sumratings"] / mygroups[i]["countratings"]).toString()
        rating_item["unique_key"] = i

        var str = "" + parseInt(mygroups[i]["allratings"][0].spectrum_id)
        var pad = "00000000000"
        var ans = pad.substring(0, pad.length - str.length) + str

        //rating_item["spectrum_id"] = mygroups[i]["allratings"][0].spectrum_id
        rating_item["spectrum_id"] = "CCMSLIB" + ans
        rating_item["id"] = i
        allgroups_render.push(rating_item)
    }
    return {
        allratings: ratings_input,
        groupratings: allgroups_render
    };
}

//Callback for rendering the new table
function render_ratings_gen(div){
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
        var generic_table = new ResultViewTableGen(get_spectrumpage_ratings_tableXML(), "spectrum_ratings", task, 0, column_handler_second);

        generic_table.setData(output_render_groupratings);
        generic_table.render(div, 0);
    }
}

//Rendering the molecule explorer view
ResultViewFileSpectrum.prototype.compound_occurences_table = function(div){
    molecule_explorer_url = "/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_molecule_dataset"
    $.ajax({
        url: molecule_explorer_url,
        cache: false,
        success: function(context, div){
            return function(html){
                results_data = get_block_data_from_page(html);

                compound_name = context.annotation.Compound_Name

                //Searching for compound name in list
                for(var i in results_data){
                    result = results_data[i]
                    if(result.Compound == compound_name){
                        //We have found it, lets render, otherwise lets not
                        child_header = document.createElement("h2");
                        child_header.innerHTML = "Compound Dataset Occurences";
                        div.appendChild(child_header);

                        compound_dataset_information = JSON.parse(result.Datasets)

                        //Setting the compound name
                        for(var dataset_index in compound_dataset_information){
                            compound_dataset_information[dataset_index].compound_name = compound_name

                            analog_filter_string = JSON.parse(compound_dataset_information[dataset_index].unique_unidentified_precursor_neighbors_in_dataset_candidate_scans).join("||")
                            analog_filter_string += "||EXACT"

                            compound_dataset_information[dataset_index].neighbors_scan_filter = analog_filter_string
                        }


                        child_table = document.createElement("div");
                        div.appendChild(child_table);

                        var task = new Object();
                        task.id = "1234";
                        task.workflow = "Datasets ID";
                        task.description = "Datasets ID";
                        var generic_table = new ResultViewTableGen(context.compound_dataset_tableXML(), "dataset_compounds", task, 0);
                        generic_table.setData(compound_dataset_information);
                        generic_table.render(child_table, 0);

                        break
                    }
                }
            }
        }(this, div)
    });
}

ResultViewFileSpectrum.prototype.add_tag_button = function(tbody_object, spectrumID){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = "Add Spectrum Tag";
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    var add_tag_button = document.createElement('button');
    add_tag_button.innerHTML = "Add Spectrum Tag"
    add_tag_button.setAttribute('name','Add Spectrum Tag');
    add_tag_button.setAttribute('value','Add Spectrum Tag');

    add_tag_button.setAttribute("data-toggle", "modal")
    add_tag_button.setAttribute("data-target", "#myModal")

    //Setting callback for adding tag in modal
    $("#finalize_add_tag")[0].onclick = function(){
        tag_type = $("#tag_type").val();
        tag_desc = $("#tag_desc").val();
        tag_database = $("#tag_database").val();
        tag_database_url = $("#tag_database_url").val();

        parameters = new Object()
        parameters["tag_type"] = tag_type
        parameters["tag_desc"] = tag_desc
        parameters["tag_database"] = tag_database
        parameters["tag_database_url"] = tag_database_url
        parameters["SpectrumID"] = spectrumID

        parameters["action"] = "addspectrumtag"

        $.ajax({
            url: "/ProteoSAFe/LibraryServlet",
            method: "POST",
            data: parameters,
            cache: false,
            success: function(){
                location.reload();
            },
            failure: function(){
                alert("Failed to Add");
                location.reload();
            }
        });
    }


    inner_table_row_content.appendChild(add_tag_button)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);

    update_tag_description_autocomplete();
    set_autocomplete_drug_database();
}

function update_tag_description_autocomplete(){
    drug_class_tags = ["Antibiotic", "Anticancer", "Statin"]
    chemical_family = ["Alcohol","Anthrocyanin","Bile acid","Canonical Amino acid","Carotenoid","Ceramide","Diglyceride","Diketopiperazine","Fatty acid","Glycerolipid","Glycerophospholipid","Glycopeptide","Glycopeptidolipid","Glyco-polyketide","Hybrid polyketide-nonribosomal peptide","Lanthibiotic","Lipid","Lipopolysaccharide","Monoglyceride","Non-canonical amino acid","Nonribosomal peptide","Nucleoside","Nucleotide","Organic acid","Peptide","Pharmaceutical","Phospholipid","Phytosterol","Pigment","Polyketide","Polymeric substance","Polysaccharide","Prenol lipid","Primary Metabolite","Prostiglandin","RiPP","Saccharide","Saccharolipid","Secondary Metabolite","Specialized Metabolite","Sphingolipid","Sterol lipid","Surfactant","Terpene","Triglyceride","Vitamin"]
    molecular_family = ["Acidobactin","Actinorhodin","Alteramide","Alterochromide","amphisin","Arenicolide","Arylomycin","Bacitracin","Calcium Dependent Antibiotic (CDA)","Citrinadin","Citrinin","Coelichelin","Columbamide","Cyanosporaside","Cyclomarin","Daptomycin","Desferrioxamine","Entolysin","Eponemycin","Epoxomicin","Estatin","Iturin","Kulolide","Kurstakin","Lomaiviticin","Massetolide","Napsamycin","Phenazine","Phenol soluble modulin","Plipistatin","Prodigiosin","Prodignine","Promysalin","Pyochelin","Pyoverdine","Quinolone","Retimycin","Rhamnolipid","Rifamycin","Saliniketal","SapB","Serratamolide","Serrawettin","SKF","Staurosporine","Stenothricin","Subtilosin","Surfactin","Syringomycin","Taromycin","Thalassospiramide","Thanamycin","Tolaasin","Variobactic","Viscosin","WLIP"]
    environment = ["Algae","Anemone","Arthropod","Bay","Bird","Blood","Cave","CNS","Coral","Delta","Desert","Farm/Plantation","Feces","Fish","Flower","Fresh-water","Gut","Lake","Leaf","Lung","Mammal","Marine","Mountain","Plant","Pond","Prairie","Reptile","Rhizosphere","River","Root","Serum","Skin","Sponge","Sputum","Stem","Terrestrial","Tundra","Urine"]
    experimental_artifact = ["Polymer","Polyethylene glycol (PEG)","Triton Detergents","Detergent","Common MS contaminant","Polyproylene glycol (PGG)","Polysiloxane","Nylon"]
    bioactivity = ["Gram-negative","Gram-positive","Cancer","Cancer Cell Line","Parasite"]
    disease_state = ["Healthy","Obese","Diabetes","COPD","Asthma","Cystic Fibrosis","Psoriasis","Dermatitis","Acne","Gingivitis","Cancer","HIV/AIDS"]

    $(".tag_desc").autocomplete({messages: {noResults: '', results: function(){}}});

    if($("#tag_type").val() == "Chemical Family"){
        $("#tag_desc")[0].placeholder = "Lipid, Glycopeptide, Peptide, Nucleotide, etc."
        $(".tag_desc").autocomplete({source: chemical_family})
    }

    if($("#tag_type").val() == "Molecular Family"){
        $("#tag_desc")[0].placeholder = "e.g. Acidobactin, Columbamide, etc."
        $(".tag_desc").autocomplete({source: molecular_family})
    }

    if($("#tag_type").val() == "Source Environment"){
        $("#tag_desc")[0].placeholder = "e.g. Gut, Blood, Bird, Mammal, etc."
        $(".tag_desc").autocomplete({source: environment})
    }

    if($("#tag_type").val() == "Experimental Artifact"){
        $("#tag_desc")[0].placeholder = "e.g. Polymer, Detergent, Nylon, etc."
        $(".tag_desc").autocomplete({source: experimental_artifact})
    }

    if($("#tag_type").val() == "Drug Class"){
        $("#tag_desc")[0].placeholder = "e.g. Antibiotic, Anticancer, Statin"
        $(".tag_desc").autocomplete({source: drug_class_tags})
    }

    if($("#tag_type").val() == "Indication"){
        $("#tag_desc")[0].placeholder = "Drug Indication"
        $(".tag_desc").autocomplete({source: new Array()})
    }

    if($("#tag_type").val() == "Pathway"){
        $("#tag_desc")[0].placeholder = "Drug Target Pathway, KEGG identifier"
        $(".tag_desc").autocomplete({source: new Array()})
    }

    if($("#tag_type").val() == "Interactions"){
        $("#tag_desc")[0].placeholder = "Drugs interacting with compound"
        $(".tag_desc").autocomplete({source: new Array()})
    }

    if($("#tag_type").val() == "Metabolizing Enzyme"){
        $("#tag_desc")[0].placeholder = "Uniprot Accession"
        $(".tag_desc").autocomplete({source: new Array()})
    }

    if($("#tag_type").val() == "Target Protein"){
        $("#tag_desc")[0].placeholder = "Uniprot Accession"
        $(".tag_desc").autocomplete({source: new Array()})
    }

    if($("#tag_type").val() == "Bioactivity (Active against)"){
        $("#tag_desc")[0].placeholder = "Gram-negative, Cancer, Parasite, etc."
        $(".tag_desc").autocomplete({source: bioactivity})
    }

    if($("#tag_type").val() == "Disease State"){
        $("#tag_desc")[0].placeholder = "Obese, Diabetes, COPD, etc."
        $(".tag_desc").autocomplete({source: disease_state})
    }


    $( ".tag_desc" ).autocomplete( "option", "appendTo", "#desc_div" );
}


function set_autocomplete_drug_database(){
    drug_database_tags = ["Drugbank", "Metlin", "Pubchem", "NIST", "Massbank", "Chemspider", "ChEBI"]
    $(".tag_database").autocomplete({messages: {noResults: '', results: function(){}}});
    $(".tag_database").autocomplete({source: drug_database_tags})
    $( ".tag_database" ).autocomplete( "option", "appendTo", "#database_input_div" );
}

ResultViewFileSpectrum.prototype.render_tags = function(div, tag_list){
    var tableXML_str = '<block id="spectrum_tags" type="table"> \
                            <row>  \
                                <column field="tag_type" label="Type" type="text" width="10"/> \
                                <column field="tag_desc" label="Description" type="text" width="20"/> \
                                <column field="tag_database" label="Database" type="text" width="10"/> \
                                <column field="tag_database_url" label="URL" type="text" width="5"/> \
                                <column label="URL" type="genericurlgenerator" width="5"> \
                                    <parameter name="URLBASE" value="[tag_database_url]"/>\
                                    <parameter name="LABEL" value="linkout"/>\
                                </column>\
                                <column label="Remove Tag" type="removespectrumtag" width="5"> \
                                    <parameter name="task_id" value="[tag_task_id]"/>\
                                </column>\
                            </row>\
                        </block>' ;


    child_table = document.createElement("div");
    div.appendChild(child_table);

    var task = new Object();
    task.id = "1234";
    task.workflow = "Datasets ID";
    task.description = "Datasets ID";
    var generic_table = new ResultViewTableGen(parseXML(tableXML_str), "spectrum_tags", task, 0);
    generic_table.setData(tag_list);
    generic_table.render(child_table, 0);
}





ResultViewFileSpectrum.prototype.alternate_spectra_table = function(div){
    all_library_spectrum_url = "/ProteoSAFe/LibraryServlet?library=GNPS-LIBRARY"

    $.ajax({
        url: all_library_spectrum_url,
        cache: false,
        success: function(context, div){
            return function(json){
                results_data = JSON.parse(json).spectra

                compound_name = context.annotation.Compound_Name

                all_spectra = []

                for(var i in results_data){
                    result = results_data[i]
                    if(result.Compound_Name == compound_name){
                        all_spectra.push(result)
                    }
                }

                child_header = document.createElement("h2");
                child_header.innerHTML = "Other Compound Spectra in Library";
                div.appendChild(child_header);

                child_table = document.createElement("div");
                div.appendChild(child_table);

                var task = new Object();
                task.id = "1234";
                task.workflow = "Other Compound Spectra in Library";
                task.description = "Other Compound Spectra in Library";
                var generic_table = new ResultViewTableGen(context.other_library_spectra(), "other_library_spectra", task, 0);
                generic_table.setData(all_spectra);
                generic_table.render(child_table, 0);

            }
        }(this, div)
    });
}

ResultViewFileSpectrum.prototype.compound_dataset_tableXML = function(){
    var tableXML_str = '<block id="dataset_compounds" type="table"> \
                            <row>  \
                                <column field="dataset_name" label="Dataset" type="text" width="5"/> \
                                <column label="Description" type="genericurlgenerator" width="5"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/dataset_id_redirect.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=massiveid" value="[dataset_name]"/>\
                                    <parameter name="LABEL" value="[dataset_display_name]"/>\
                                </column>\
                                <column label="Matches" type="genericurlgenerator" width="5"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=show" value="true"/>\
                                    <parameter name="REQUESTPARAMETER=task" value="[search_task_id]"/>\
                                    <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum_all_beta"/>\
                                    <parameter name="HASHPARAMTER=Compound_Name_input" value="[compound_name]"/>\
                                    <parameter name="LABEL" value="View Matches"/>\
                                </column>\
                                <column label="Analogs" type="genericurlgenerator" width="5"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=show" value="true"/>\
                                    <parameter name="REQUESTPARAMETER=task" value="[search_task_id]"/>\
                                    <parameter name="REQUESTPARAMETER=view" value="view_all_clusters_withID"/>\
                                    <parameter name="HASHPARAMTER=cluster index_input" value="[neighbors_scan_filter]"/>\
                                    <parameter name="LABEL" value="View Analogs"/>\
                                </column>\
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


ResultViewFileSpectrum.prototype.other_library_spectra = function(){
    var tableXML_str = '<block id="dataset_compounds" type="table"> \
                            <row>  \
                                <column label="SpectrumID" type="genericurlgenerator" width="5"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=SpectrumID" value="[SpectrumID]"/>\
                                    <parameter name="LABEL" value="[SpectrumID]"/>\
                                </column>\
                                <column field="Adduct" label="Adduct" type="text" width="5"/> \
                                <column field="Instrument" label="Instrument" type="text" width="5"/> \
                                <column field="PI" label="PI" type="text" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


function get_spectrumpage_ratings_tableXML(){
    var tableXML_str = '<block id="spectrum_ratings" type="table"> \
                            <row>  \
                                <column field="dataset_id" label="Dataset" type="text" width="5"/> \
                                <column field="scan" label="scan" type="text" width="5"/> \
                                <column label="Avg Rating" type="ratydisplayrating"> \
                                    <parameter name="rating" value="[averagerating]"/>\
                                    <parameter name="maxrating" value="4"/>\
                                </column>\
                            </row>\
                            <row expander="Show Rating Comments:Hide Rating Comments" expandericontype="text">\
                                <column type="callbackblock" block="constituent_ratings" colspan="7">\
                                    <parameter name="unique_key" value="[unique_key]"/>\
                                </column>\
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


function get_constituent_ratings_XML(input_data, parameters){
    input_data_filtered = [];


    for(i = 0; i < input_data.length; i++){
        var keep = true;
        for(parameter in parameters){
            if(parameters[parameter] != input_data[i][parameter]){
                keep = false;
                break;
            }
        }
        if ( keep == true ){
            input_data_filtered.push(input_data[i]);
        }
    }

    var tableXML_str = '<blockInstance> \
                            <block id="constituent_ratings" type="table"> \
                                <row> \
                                    <column field="user_id" label="User" type="text" width="5"/> \
                                    <column label="Rating" type="ratydisplayrating"> \
                                        <parameter name="rating" value="[rating]"/>\
                                        <parameter name="maxrating" value="4"/>\
                                    </column>\
                                    <column field="rating_comment" label="Comment" type="text" width="5"/> \
                                    <column label="Link" type="genericurlgenerator"> \
                                        <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                        <parameter name="REQUESTPARAMETER=task" value="[task_id]"/>\
                                        <parameter name="REQUESTPARAMETER=view" value="group_by_spectrum_all_beta"/>\
                                        <parameter name="HASHPARAMTER=#Scan#_lowerinput" value="[scan]"/>\
                                        <parameter name="HASHPARAMTER=#Scan#_upperinput" value="[scan]"/>\
                                        <parameter name="LABEL" value="Link"/>\
                                    </column>\
                                </row> \
                            </block>\n \
                        <blockData>\n';
    tableXML_str += JSON.stringify(input_data_filtered);
    tableXML_str += '\n</blockData></blockInstance>\n';
    return (parseXML(tableXML_str));
}


//Adding new line to the specturm information table
function add_spectrum_information_table(tbody_object, label, value){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");
    inner_table_row_content.textContent = value;
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

function add_linkout_to_table(tbody_object, label, value, url, filename){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    link_element = document.createElement("a");

    link_element.innerHTML = value
    link_element.href = url

    inner_table_row_content.appendChild(link_element)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

//Adding Structure to spectrum to div
function add_structure_to_table(tbody_object, label, smiles_structure, inchi_structure){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    displaying_smiles = false;
    if(smiles_structure != null){
        if(smiles_structure != "N/A" && smiles_structure.length > 3){
            image_element = document.createElement("img")
            width = 600
            height = 350
            cache_flag = "2"
            structure_url_prefix = "http://ccms-support.ucsd.edu:5000/smilesstructure?smiles=" + encodeURIComponent(smiles_structure) + "&width=" + width + "&height=" + height + "&cache=" + cache_flag

            image_element.src = structure_url_prefix
            inner_table_row_content.appendChild(image_element)
            inner_table_row.appendChild(inner_table_row_content);
            tbody_object.appendChild(inner_table_row);
            displaying_smiles = true;
        }
    }
    if(inchi_structure != null && displaying_smiles == false){
        if(inchi_structure != "N/A" && inchi_structure.length > 3){
            image_element = document.createElement("img")
            width = 600
            height = 350
            cache_flag = "2"
            structure_url_prefix = "http://ccms-support.ucsd.edu:5000/smilesstructure?inchi=" + encodeURIComponent(inchi_structure) + "&width=" + width + "&height=" + height + "&cache=" + cache_flag

            image_element.src = structure_url_prefix
            inner_table_row_content.appendChild(image_element)
            inner_table_row.appendChild(inner_table_row_content);
            tbody_object.appendChild(inner_table_row);
        }

    }

}

ResultViewFileSpectrum.prototype.add_download_to_table = function(tbody_object, label, value, url, filename){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    link_element = document.createElement("a");

    link_element.innerHTML = value
    link_element.href = url
    link_element.target = "_blank"
    link_element.download = filename
    inner_table_row_content.style.width = "750px"

    inner_table_row_content.appendChild(link_element)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

ResultViewFileSpectrum.prototype.add_download_spectrum_peaks = function(tbody_object, peaks_to_render, precursor_mz, charge, spectrum_id){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = "Download Library Spectrum Peaks";
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    output_string = "BEGIN IONS\n"
    output_string += "PEPMASS=" + precursor_mz + "\n"
    output_string += "CHARGE=" + charge + "\n"
    for(i = 0; i < peaks_to_render.length; i++){
        output_string += peaks_to_render[i][0] + "\t" + peaks_to_render[i][1] + "\n"
    }
    output_string += "END IONS\n"
    var download = document.createElement('a');

    download.innerHTML = "Download Spectrum Peaks"
    download.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(output_string)
    download.download = spectrum_id + ".mgf";
    //download.style.display = 'none';

    inner_table_row_content.style.width = "750px"

    inner_table_row_content.appendChild(download)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

ResultViewFileSpectrum.prototype.add_linkout_url = function(tbody_object, label, value, url){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    link_element = document.createElement("a");

    link_element.innerHTML = value
    link_element.href = url
    link_element.target = "_blank"
    inner_table_row_content.style.width = "750px"

    inner_table_row_content.appendChild(link_element)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

ResultViewFileSpectrum.prototype.add_appropriate_compound_linkouts = function(tbody_object, label, compound_name){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    if(compound_name.indexOf("Massbank:") != -1 || compound_name.indexOf("MassbankEU:") != -1){
        link_element = document.createElement("a");

        massbank_url = 'http://www.massbank.eu/MassBank/jsp/FwdRecord.jsp?id='

        massbank_fullstring = compound_name.split(" ")[0]
        massbank_id = massbank_fullstring.split(":")[1]

        massbank_url += massbank_id

        link_element.innerHTML = compound_name
        link_element.href = massbank_url
        link_element.target = "_blank"
        inner_table_row_content.style.width = "750px"

        inner_table_row_content.appendChild(link_element)
        inner_table_row.appendChild(inner_table_row_content);
        tbody_object.appendChild(inner_table_row);

        return;
    }

    if(compound_name.indexOf("ReSpect:") != -1){
        link_element = document.createElement("a");

        url = 'http://spectra.psc.riken.jp/menta.cgi/respect/datail/datail?accession='

        id_fullstring = compound_name.split(" ")[0]
        accession = id_fullstring.split(":")[1]

        url += accession

        link_element.innerHTML = compound_name
        link_element.href = url
        link_element.target = "_blank"
        inner_table_row_content.style.width = "750px"

        inner_table_row_content.appendChild(link_element)
        inner_table_row.appendChild(inner_table_row_content);
        tbody_object.appendChild(inner_table_row);

        return;
    }


    if(compound_name.indexOf("MoNA:") != -1){
        link_element = document.createElement("a");

        url = 'http://mona.fiehnlab.ucdavis.edu/#/spectra/display/'

        id_fullstring = compound_name.split(" ")[0]
        accession = id_fullstring.split(":")[1]

        url += accession

        link_element.innerHTML = compound_name
        link_element.href = url
        link_element.target = "_blank"
        inner_table_row_content.style.width = "750px"

        inner_table_row_content.appendChild(link_element)
        inner_table_row.appendChild(inner_table_row_content);
        tbody_object.appendChild(inner_table_row);

        return;
    }

    inner_table_row_content.textContent = compound_name;
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);

    return;


}


function add_update_annotation_table(tbody_object, label, latest_annotation_object){
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = label;
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    updatelink_element = document.createElement("input")
    updatelink_element.setAttribute('type','button');
    updatelink_element.setAttribute('name','Update Annotation');
    updatelink_element.setAttribute('value','Update Annotation');


    //Parameters grabbed from the annotation object
    parameters = {}

    annotation_object_mapping = {}
    annotation_object_mapping["Adduct"] = "ADDSPECTRA_ADDUCT"
    annotation_object_mapping["CAS_Number"] = "ADDSPECTRA_CASNUMBER"
    annotation_object_mapping["Charge"] = "ADDSPECTRA_CHARGE"
    annotation_object_mapping["Compound_Name"] = "ADDSPECTRA_COMPOUND_NAME"
    annotation_object_mapping["Data_Collector"] = "ADDSPECTRA_DATACOLLECTOR"
    annotation_object_mapping["INCHI"] = "ADDSPECTRA_INCHI"
    annotation_object_mapping["INCHI_AUX"] = "ADDSPECTRA_INCHIAUX"
    annotation_object_mapping["Instrument"] = "ADDSPECTRA_INSTRUMENT"
    annotation_object_mapping["Ion_Mode"] = "ADDSPECTRA_IONMODE"
    annotation_object_mapping["Ion_Source"] = "ADDSPECTRA_IONSOURCE"
    annotation_object_mapping["Compound_Source"] = "ADDSPECTRA_ACQUISITION"
    annotation_object_mapping["PI"] = "ADDSPECTRA_PI"
    annotation_object_mapping["Precursor_MZ"] = "ADDSPECTRA_MOLECULEMASS"
    annotation_object_mapping["ExactMass"] = "ADDSPECTRA_EXACTMASS"
    annotation_object_mapping["Pubmed_ID"] = "ADDSPECTRA_PUB"
    annotation_object_mapping["Smiles"] = "ADDSPECTRA_SMILES"
    annotation_object_mapping["SpectrumID"] = "SPECTRUMID"
    annotation_object_mapping["Library_Class"] = "ADDSPECTRA_LIBQUALITY"

    for(var key in annotation_object_mapping){
        real_value = latest_annotation_object[key]
        parameters[annotation_object_mapping[key]] = real_value
    }


    //Adding extra parameter for workflow based on existing library quality
    switch(parameters["ADDSPECTRA_LIBQUALITY"]) {
        case "1":
            parameters["workflow"] = "UPDATE-SINGLE-ANNOTATED-GOLD"
            break;
        case "2":
            parameters["workflow"] = "UPDATE-SINGLE-ANNOTATED-SILVER"
            break;
        case "3":
            parameters["workflow"] = "UPDATE-SINGLE-ANNOTATED-BRONZE"
            break;
        case "10":
            parameters["workflow"] = "UPDATE-SINGLE-ANNOTATED-BRONZE"
            updatelink_element.setAttribute('name','Promote Annotation to Bronze');
            updatelink_element.setAttribute('value','Promote Annotation to Bronze');
            break;
        default:
            return;
    }

    params_string = ""
    for (var parameter in parameters){
        field_value = parameters[parameter]
        //field_value = field_value.replace(/\"/g,'\\"')
        field_value = field_value.escapeSpecialChars()
        //field_value = escape(field_value)
        params_string += "\"" + parameter + "\":\"" + field_value + "\",";
    }

    params_string = params_string.slice(0, -1);
    params_string = "{" + params_string + "}"
    //params_string = encodeURIComponent(params_string)
    params_string = encodeURIComponent(JSON.stringify(parameters))

    updatelink_element.onclick = function(target_param){
        return function(){
            full_url = "/ProteoSAFe" + "?params=" + target_param
            window.location.href = full_url
            //alert(target_param);
        }
    }(params_string);

    inner_table_row_content.appendChild(updatelink_element)
    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);

}

function add_makepublic_spectrum(tbody_object, label, spec_info, canAdmin){
    if(canAdmin == 1 && (spec_info.library_membership == "GNPS-LIBRARY" ||  spec_info.library_membership == "PRIVATE-USER")){
        //list to disable
        inner_table_row = document.createElement("tr");
        inner_table_row_header = document.createElement("th");
        inner_table_row_header.innerHTML = label;
        inner_table_row.appendChild(inner_table_row_header);
        inner_table_row_content = document.createElement("td");

        var disable_button= document.createElement('input');
        disable_button.setAttribute('type','button');

        if(spec_info.library_membership == "GNPS-LIBRARY"){
            disable_button.setAttribute('name','Make Private');
            disable_button.setAttribute('value','Make Private');
            disable_button.onclick = button_makeprivate_callback_gen(disable_button, spec_info.spectrum_id)
        }
        if(spec_info.library_membership == "PRIVATE-USER"){
            disable_button.setAttribute('name','Make Public');
            disable_button.setAttribute('value','Make Public');
            disable_button.onclick = button_makepublic_callback_gen(disable_button, spec_info.spectrum_id)

        }

        inner_table_row_content.appendChild(disable_button)
        inner_table_row.appendChild(inner_table_row_content);
        tbody_object.appendChild(inner_table_row);
    }
}

function button_makepublic_callback_gen(button, spectrum_id){
    return function(){
        button.setAttribute('name','Make Private');
        button.setAttribute('value','Make Private');
        button.onclick = button_makeprivate_callback_gen(button, spectrum_id);

        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/LibraryServlet",
            data: { SpectrumID: spectrum_id, action: "promotepublic"},
            cache: false,
            async: false,
            success: function(json){
            }
        });
    }
}

function button_makeprivate_callback_gen(button, spectrum_id){
    return function(){
        button.setAttribute('name','Make Public');
        button.setAttribute('value','Make Public');
        button.onclick = button_makepublic_callback_gen(button, spectrum_id);

        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/LibraryServlet",
            data: { SpectrumID: spectrum_id, action: "demoteprivate"},
            cache: false,
            async: false,
            success: function(json){
            }
        });
    }
}

//Displaying Library Quality
ResultViewFileSpectrum.prototype.displaylibraryquality = function(tbody_object, library_quality) {
    inner_table_row = document.createElement("tr");
    inner_table_row_header = document.createElement("th");
    inner_table_row_header.innerHTML = "Library Quality";
    inner_table_row.appendChild(inner_table_row_header);
    inner_table_row_content = document.createElement("td");

    switch(library_quality){
        case "1":
            inner_table_row_content.innerHTML = "Gold Spectrum"
            break;
        case "2":
            inner_table_row_content.innerHTML = "Silver Spectrum"
            break;
        case "3":
            inner_table_row_content.innerHTML = "Bronze Spectrum"
            break;
        case "10":
            inner_table_row_content.innerHTML = "Challenge Spectrum"
            break;
        default:
            return;
    }

    inner_table_row_content.style.width = "750px"

    inner_table_row.appendChild(inner_table_row_content);
    tbody_object.appendChild(inner_table_row);
}

function add_disable_spectrum(tbody_object, label, spec_info, canAdmin){
    if(canAdmin == 1){
        //list to disable
        inner_table_row = document.createElement("tr");
        inner_table_row_header = document.createElement("th");
        inner_table_row_header.innerHTML = label;
        inner_table_row.appendChild(inner_table_row_header);
        inner_table_row_content = document.createElement("td");

        var disable_button= document.createElement('input');
        disable_button.setAttribute('type','button');

        if(spec_info.spectrum_status == "1"){
            disable_button.setAttribute('name','Disable');
            disable_button.setAttribute('value','Disable');
            disable_button.onclick = button_disable_callback_gen(disable_button, spec_info.spectrum_id)


        }
        if(spec_info.spectrum_status == "2"){
            disable_button.setAttribute('name','Enable');
            disable_button.setAttribute('value','Enable');
            disable_button.onclick = button_enable_callback_gen(disable_button, spec_info.spectrum_id)

        }

        inner_table_row_content.appendChild(disable_button)
        inner_table_row.appendChild(inner_table_row_content);
        tbody_object.appendChild(inner_table_row);
    }
}

function button_disable_callback_gen(button, spectrum_id){
    return function(){
        button.setAttribute('name','Enable');
        button.setAttribute('value','Enable');
        button.onclick = button_enable_callback_gen(button, spectrum_id);

        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/LibraryServlet",
            data: { SpectrumID: spectrum_id, newstatus: "2", action: "enabledisable"},
            cache: false,
            async: false,
            success: function(json){
            }
        });
    }
}

function button_enable_callback_gen(button, spectrum_id){
    return function(){
        button.setAttribute('name','Disable');
        button.setAttribute('value','Disable');
        button.onclick = button_disable_callback_gen(button, spectrum_id);

        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/LibraryServlet",
            data: { SpectrumID: spectrum_id, newstatus: "1", action: "enabledisable"},
            cache: false,
            async: false,
            success: function(json){
            }
        });
    }
}


function display_specplot_peaks(
        plot_div, div, task, type, source, parameters, contentType, block
) {
        if (div == null || task == null || type == null || source == null)
                return;
        // set up URL to download result file
    var url = "DownloadResultFile?task=" + task + "&" + type + "=" + source;
    if (block != null)
        url += "&block=" + block;
    if (parameters != null)
        for (var parameter in parameters)
                url += "&" + parameter + "=" +
                        encodeURIComponent(parameters[parameter]);

    // show "in-progress" download spinner
        removeChildren(div);
    var child = document.createElement("img");
    child.src = "images/inProgress.gif";
        div.appendChild(child);

    // create and submit AJAX request for the file data
        var request = createRequest();
        request.open("GET", url, true);
        request.onreadystatechange = function() {
                if (request.readyState == 4) {
                        // remove "in-progress" download spinner
                        removeChildren(div);
                        if (request.status == 200) {
                            child = document.createElement("pre");
                            child.innerHTML = request.responseText;

                            //div.appendChild(child);

                            peaklist = parseSpecplotPeaksToArray(request.responseText)
                            $("#" + plot_div.id).specview( {
                                sequence: "",
                                peaks: peaklist,
                                labelImmoniumIons: false,
                                width:935,
                                height:600,
                                showOptionsTable:false,
                                showIonTable:false,
                                showSequenceInfo:false
                            })


                        } else if (request.status == 410) {
                                alert("The input file associated with this request could not " +
                                        "be found. This file was probably deleted by its owner " +
                                        "after this workflow task completed.");
                        } else {
                                //alert("Could not download result artifact of type \"" + type +
                                //        "\" and value \"" + source + "\", belonging to task " +
                                //        task + ".");
                        }
                }
        }
        request.setRequestHeader("If-Modified-Since",
                "Sat, 1 Jan 2000 00:00:00 GMT");
        request.send(null);
}


//INput raw text from specplot text, returns a array of mass peak pairs
function parseSpecplotPeaksToArray(specplot_raw_text){
    lines = specplot_raw_text.split("\n")
    spectrum_peaks = []
    for(var i in lines){
        if(i > 7){
            splits = lines[i].split(" ")
            mass = parseFloat(splits[0])
            intensity = Number.NaN;
            //intensity = parseFloat(splits[splits.length-1])

            for(k = 1; k < splits.length; k++){
                if(splits[k].length > 0){
                    intensity = parseFloat(splits[k])
                    break;
                }
            }

            if(isNaN(mass) || isNaN(intensity)){
                continue
            }

            if(intensity < 0.1){
                continue
            }

            mass_int_pair = [mass, intensity]
            spectrum_peaks.push(mass_int_pair)
        }
    }
    return spectrum_peaks
}

// render the streamed file
ResultViewFileSpectrum.prototype.render = function(div, index) {
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

    // retrieve the file from the server and display it
    var task = this.task.id;
    var contentType = this.contentType;

    //Div for molecule explorer
    var molecule_explorer_child = document.createElement("div");
    div.appendChild(molecule_explorer_child);

    var other_spectra_child = document.createElement("div");
    div.appendChild(other_spectra_child);

    this.get_annotation_comments(child);
    this.compound_occurences_table(molecule_explorer_child);
    this.alternate_spectra_table(other_spectra_child);

    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));
    div.appendChild(document.createElement("br"));


}

ResultViewFileSpectrum.prototype.displayspecplot = function(div) {
    var task = get_taskid();
    var type = "invoke";
    //var source  = "annotatedSpectrumImageThumbnail";
    var source  = "annotatedSpectrumImage";
    var contentType = "image/png";
    var block = 0;
    var invokeParameters = {};

    var file_path = get_URL_parameter("file");

    if (file_path[0] == '/' ){
        invokeParameters["file"] = "FILE->"  + "../../../../../" + file_path;
    }
    else{
        var just_filename = file_path.replace(/^.*[\\\/]/, '');
        invokeParameters["file"] = "FILE->"  +  get_URL_parameter("file_prefix") + "/" + just_filename;
    }


    //invokeParameters["scan"] = scan_to_plot;
    invokeParameters["spectrumid"] = get_URL_parameter("SpectrumID");
    invokeParameters["peptide"] = "*..*";
    invokeParameters["force"] = "true";

    displayFileContents(div, task, type, source, invokeParameters, contentType, block);
}

ResultViewFileSpectrum.prototype.backup_mgf_displaypeaks = function(plotdiv, text_div) {
    var task = get_taskid();
    var type = "invoke";
    //var source  = "annotatedSpectrumImageThumbnail";
    var source  = "annotatedSpectrumImageText";
    var contentType = "image/text";
    var block = 0;
    var invokeParameters = {};

    var file_path = get_URL_parameter("file");

    if (file_path[0] == '/' ){
        invokeParameters["file"] = "FILE->"  + "../../../../../" + file_path;
    }
    else{
        var just_filename = file_path.replace(/^.*[\\\/]/, '');
        invokeParameters["file"] = "FILE->"  +  get_URL_parameter("file_prefix") + "/" + just_filename;
    }


    //invokeParameters["scan"] = scan_to_plot;
    invokeParameters["spectrumid"] = get_URL_parameter("SpectrumID");
    invokeParameters["peptide"] = "*..*";
    invokeParameters["force"] = "true";

    display_specplot_peaks(plotdiv, text_div, task, type, source, invokeParameters, contentType, block);
}

// set data to the file streamer
ResultViewFileSpectrum.prototype.createTable = function(div) {
    var tableXML_str = '<block id="clusters_list_specnets" type="table"> \
                            <row> \
                                <column field="AllGroups" label="AllGroups" type="text" width="5"/> \
                                <column field="DefaultGroups" label="DefaultGroups" type="text" width="5"/> \
                            </row> \
                        </block>' ;

    var tableXML = (new window.DOMParser() ).parseFromString(tableXML_str, "text/xml");

    //var new_table = document.createElement("div");
    //new_table.id = "nice_table";

    var task = new Object();
    task.id = "1234";
    task.workflow = "workflow-1234";
    task.description = "desc-1234";
    var generic_table = new ResultViewTableGen(tableXML, "nice_table", task, 0);
    var test_data = [{"AllGroups" : "1"}];
    generic_table.getupdateddata(test_data);
    generic_table.render(div, 0);
}

// set data to the file streamer
ResultViewFileSpectrum.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["spectrumpage"] = ResultViewFileSpectrum;
