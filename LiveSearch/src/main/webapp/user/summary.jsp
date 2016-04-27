<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.*"
    import="edu.ucsd.livesearch.servlet.ManageSharing"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.storage.FileManager"
    import="edu.ucsd.livesearch.account.AccountManager"
    import="edu.ucsd.livesearch.dataset.Dataset"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.libraries.AnnotationManager"
    import="edu.ucsd.livesearch.libraries.SpectrumInfo"
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotation"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="java.util.*"
    import="org.apache.commons.lang3.StringEscapeUtils"
%>

<%!
    public static String createSpectraString(Map<SpectrumInfo, SpectrumAnnotation> spectra_map, String key_name){
        StringBuilder render_string_builder = new StringBuilder();
        render_string_builder.append("{ \"" + key_name + "\": [");
        for(SpectrumInfo spec : spectra_map.keySet()){
            render_string_builder.append(spec.toJSON_withAnnotation(spectra_map.get(spec)) + ",");
        }
        render_string_builder.append("]}");
        String library_spectrum_string = render_string_builder.toString();

        return library_spectrum_string;
    }
%>


<%
    boolean authenticated = ServletUtils.isAuthenticated(session);
    String url = request.getParameter("url");
    String identity = (String)session.getAttribute("livesearch.user");
    boolean isAdmin = AccountManager.isAdministrator(identity);
    boolean isMassIVE = ServletUtils.isMassIVESite();
    boolean show_private = true;
    boolean show_subscriptions = true;

    String proxy_identity = request.getParameter("user");
    if(proxy_identity != null){
        identity = proxy_identity;
        show_private = false;
        show_subscriptions = false;
    }

    if(isAdmin){
        show_private = true;
        show_subscriptions = true;
    }



    Map<Task, Dataset> datasets = DatasetManager.queryOwnedDatasets(identity);
    List<Integer> dataset_subscriptions = SubscriptionManager.get_all_user_subscriptions(identity);
    Map<Task, Dataset> subscribed_datasets = new HashMap<Task, Dataset>();
    if(show_subscriptions == true){
        subscribed_datasets = DatasetManager.queryDatasetList(dataset_subscriptions);
    }

    //Filtering out private datasets
    if(show_private == false){
        if(datasets != null){
            Map<Task, Dataset> datasets_filtered = new HashMap<Task, Dataset>();
            for(Task task : datasets.keySet()){
                if(datasets.get(task).isPrivate() == false){
                    datasets_filtered.put(task, datasets.get(task));
                }

            }
            datasets = datasets_filtered;
        }
        if(subscribed_datasets != null){
            Map<Task, Dataset> datasets_filtered = new HashMap<Task, Dataset>();
            for(Task task : subscribed_datasets.keySet()){
                if(subscribed_datasets.get(task).isPrivate() == false){
                    datasets_filtered.put(task, subscribed_datasets.get(task));
                }
            }
            subscribed_datasets = datasets_filtered;
        }
    }


    //Creating Dataset subscription string
    String dataset_subscription_string = "[";
    for(Integer dataset : dataset_subscriptions){
        dataset_subscription_string += "\"" + dataset + "\",";
    }
    dataset_subscription_string += "]";

    //Querying and formatting Library Spectra
    List<SpectrumInfo> spectra = new ArrayList<SpectrumInfo>();
    List<String> all_names = new ArrayList<String>();
    all_names.add("GNPS-LIBRARY");

    for(String library_name : all_names){
        List<SpectrumInfo> additional_lib_spectra = AnnotationManager.Get_Library_Spectra(library_name, false);
        spectra.addAll(additional_lib_spectra);
    }

    //Getting annotations for everything
    List<String> all_spectra_id = new ArrayList<String>();
    for(SpectrumInfo spec : spectra){
        all_spectra_id.add(spec.getSpectrum_id());
    }
    Map<String, SpectrumAnnotation> library_annotations = AnnotationManager.Get_Most_Recent_Spectrum_Annotations_Batch(all_spectra_id);


    //Getting private spectra
    Map<SpectrumInfo, SpectrumAnnotation> private_user_annotations = new HashMap<SpectrumInfo, SpectrumAnnotation>();
    if(show_private == true){
        private_user_annotations = AnnotationManager.Get_Private_Spectrum_Recent_Annotation(identity);
    }
    //Building Private User String
    String private_library_string = createSpectraString(private_user_annotations, "spectra");

    //Creating a list of tasks to query the owner
    List<String> submitted_spectra_tasks_list = new ArrayList<String>();
    for(SpectrumInfo spec : spectra){
        submitted_spectra_tasks_list.add(spec.getTask_id());
    }
    Map<String, Task> submitted_spectra_tasks_map = TaskManager.queryTaskList(submitted_spectra_tasks_list);

    //Building the String
    StringBuilder render_string_builder = new StringBuilder();
    render_string_builder.append("{ \"spectra\": [");
    for(SpectrumInfo spec : spectra){
        spec.setSubmit_user(submitted_spectra_tasks_map.get(spec.getTask_id()).getUser());
        if(submitted_spectra_tasks_map.get(spec.getTask_id()).getUser().equals(identity)){
            render_string_builder.append(spec.toJSON_withAnnotation(library_annotations.get(spec.getSpectrum_id())) + ",");
        }
    }
    render_string_builder.append("]}");
    String library_spectrum_string = render_string_builder.toString();

%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>UCSD/CCMS - User Summary</title>
    <link href="/ProteoSAFe/styles/main.css" rel="stylesheet" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>

    <!-- General ProteoSAFe scripts -->
    <script src="/ProteoSAFe/scripts/form.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/render.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/util.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>

    <!-- Help text tooltip scripts -->
    <script src="/ProteoSAFe/scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="https://platform.twitter.com/widgets.js" language="javascript" type="text/javascript"></script>


    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>

    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="/ProteoSAFe/scripts/result/lorikeet/css/lorikeet.css">



    <script language="javascript" type="text/javascript">
        var user = "<%= identity %>";
    	var is_massive_site = <%= isMassIVE %>;
        var datasets = <%= DatasetManager.getDatasetTaskListJSON(datasets) %>;
        var subscribed_datasets = <%= DatasetManager.getDatasetTaskListJSON(subscribed_datasets) %>;
        var subscription_list = <%= dataset_subscription_string %>;
        var library_spectra = <%= library_spectrum_string %>;
        var private_library_spectra = <%= private_library_string %>;

        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>



            content_div = document.createElement("div")
            $("#textWrapper").append(content_div)

            header = document.createElement("h2");
            header.innerHTML = "My Datasets";
            content_div.appendChild(header);

            create_account_datasets(content_div);

            if(subscribed_datasets.length > 0){
                header = document.createElement("h2");
                header.innerHTML = "My Subscribed Datasets";
                content_div.appendChild(header);

                create_subscribed_datasets(content_div);
            }

            // don't show library spectra sections on MassIVE
            if (is_massive_site == false) {
                header = document.createElement("h2");
                header.innerHTML = "My Library Spectra";
                content_div.appendChild(header);
                create_spectra_table(content_div);

                header = document.createElement("h2");
                header.innerHTML = "My Challenge Spectra";
                content_div.appendChild(header);
                create_challenge_table(content_div);

                if (private_library_spectra.spectra.length > 0) {
                    header = document.createElement("h2");
                    header.innerHTML = "My Private Spectra";
                    content_div.appendChild(header);
                    create_private_spectra_table(content_div);
                }

                create_related_users_table(content_div, user);
            }
        }


        function create_account_datasets(div){
            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "My Datasets";
            task.description = "My Datasets";
            var generic_table = new ResultViewTableGen(my_dataset_tableXML(), "mydatasets", task, 0);
            generic_table.setData(datasets);
            generic_table.render(child_table, 0);
        }


        function create_subscribed_datasets(div){
            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "Subscribed Datasets";
            task.description = "Subscribed Datasets";
            var generic_table = new ResultViewTableGen(my_dataset_tableXML(), "subdatasets", task, 0);
            generic_table.setData(subscribed_datasets);
            generic_table.render(child_table, 0);
        }


        function my_dataset_tableXML(){
            var tableXML_str = '<block id="dataset_compounds" type="table" pagesize="15"> \
                                    <row>  \
                                        <column label="Dataset" type="genericurlgenerator" width="5"> \
                                            <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                            <parameter name="REQUESTPARAMETER=task" value="[task]"/>\
                                            <parameter name="REQUESTPARAMETER=view" value="advanced_view"/>\
                                            <parameter name="LABEL" value="[dataset]"/>\
                                        </column>\
                                        <column field="title" label="Name" type="text" width="5"/> \
                                        <column field="private" label="Private" type="text" width="5"/> \
                                        <column label="Share" type="twitterdataset" width="5"> \
                                            <parameter name="URLBASE" value="http://gnps.ucsd.edu/ProteoSAFe/result.jsp"/>\
                                            <parameter name="TASK" value="[task]"/>\
                                            <parameter name="DATASET" value="[dataset]"/>\
                                            <parameter name="TWEETTEXT" value="Check out this Dataset"/>\
                                        </column>\
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }

        //Creating a related users table
        function create_related_users_table(div, username){
            data_url = "http://gnps.ucsd.edu/ProteoSAFe/result_json.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_user_network"
            child_table = document.createElement("div");
            div.appendChild(child_table);
            $.ajax({
                url: "/ProteoSAFe/result_json.jsp",
                data: { task: this.taskid,
                    view: "view_user_network",
                    show: 'true',
                    task: '698fc5a09db74c7492983b3673ff5bf6'},
                cache: false,
                success: function(div, username){
                    return function(json_data){
                        data = JSON.parse(json_data)["blockData"]
                        neighbor_users = []
                        for(var i = 0; i < data.length; i++){
                            if(data[i].overlapscore < 0.1){
                                continue
                            }

                            if(data[i].user1 == username){
                                neighbor_users.push({neighbor:data[i].user2, overlapcount:data[i].overlapcount, overlapscore:data[i].overlapscore})
                            }
                            if(data[i].user2 == username){
                                neighbor_users.push({neighbor:data[i].user1, overlapcount:data[i].overlapcount, overlapscore:data[i].overlapscore})
                            }
                        }

                        header = document.createElement("h2");
                        header.innerHTML = "My Suggested Users";
                        div.appendChild(header);

                        child_table = document.createElement("div");
                        div.appendChild(child_table);

                        var task = new Object();
                        task.id = "1234";
                        task.workflow = "My Neighbors";
                        task.description = "My Neighbors";
                        var generic_table = new ResultViewTableGen(my_neighbors_tableXML(), "neighborusers", task, 0);
                        generic_table.setData(neighbor_users);
                        generic_table.render(child_table, 0);


                    }
                }(child_table, username)
            });
        }

        function my_neighbors_tableXML(){
            var tableXML_str = '<block id="dataset_compounds" type="table" pagesize="10"> \
                                    <row>  \
                                        <column field="neighbor" label="Neighbor" type="text" width="5"/> \
                                        <column field="overlapscore" label="overlapscore" type="float" precision="2"/> \
                                        <column field="overlapcount" label="overlapcount" type="integer" width="5"/> \
                                    </row>\
                                </block>' ;
            return (parseXML(tableXML_str));
        }


        function create_spectra_table(div){
            //creating an id
            for(var i = 0; i < library_spectra.spectra.length; i++){
                library_spectra.spectra[i].id = "spectra_" + i;
            }

            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "My Library Spectra";
            task.description = "My Library Spectra";
            var generic_table = new ResultViewTableGen(gnps_spectra_tableXML(), "spectra", task, 0);
            generic_table.setData(library_spectra.spectra);
            generic_table.render(child_table, 0);

            //Prefiltering
            var element = document.getElementById("Library_Class_input");
            element.value = "1||2||3||EXACT"
            generic_table.filter();
        }

        function create_private_spectra_table(div){
            //creating an id
            for(var i = 0; i < private_library_spectra.spectra.length; i++){
                private_library_spectra.spectra[i].id = "spectra_" + i;
            }

            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "My Private Spectra";
            task.description = "My Private Spectra";
            var generic_table = new ResultViewTableGen(gnps_spectra_tableXML(), "privatespectra", task, 0);
            generic_table.setData(private_library_spectra.spectra);
            generic_table.render(child_table, 0);

        }

        function create_challenge_table(div){
            //Renaming quality
            for(var i = 0; i < library_spectra.spectra.length; i++){
                library_spectra.spectra[i].Library_Class_Challenge = library_spectra.spectra[i].Library_Class;
            }

            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "My Challenge Spectra";
            task.description = "My Challenge Spectra";
            var generic_table = new ResultViewTableGen(gnps_challenge_tableXML(), "challenge", task, 0);
            generic_table.setData(library_spectra.spectra);
            generic_table.render(child_table, 0);

            //Prefiltering
            var element = document.getElementById("Library_Class_Challenge_input");
            element.value = "10"
            generic_table.filter();

            //tableManager.hideColumn("challenge", "Library_Class_Challenge");
            //tableManager.getBlock("challenge").rebuildTable();
        }

        function gnps_spectra_tableXML(){
            var tableXML_str = '<block id="gnps_library_compounds" type="table" pagesize="15"> \
                                    <row>  \
                                        <column label="SpectrumID" type="genericurlgenerator" width="16" field="spectrum_id"> \
                                            <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                            <parameter name="REQUESTPARAMETER=SpectrumID" value="[spectrum_id]"/>\
                                            <parameter name="LABEL" value="[spectrum_id]"/>\
                                        </column>\
                                        <column field="Compound_Name" label="Name" type="text" width="15"/> \
                                        <column field="Adduct" label="Adduct" type="text" width="5"/> \
                                        <column field="Precursor_MZ" label="MZ" type="float" precision="2"/> \
                                        <column field="Instrument" label="Inst" type="text" width="5"/> \
                                        <column field="PI" label="PI" type="text" width="10"/> \
                                        <column field="Library_Class" label="Qual" type="text" width="5"/> \
                                        <column field="spectrum_status" label="Stat" type="text" width="5"/> \
                                    </row>\
                                    <row expander="image"> \
                                        <column label="Image" type="jscolumnspectrumviewer_fromlibrary" colspan="5"> \
                                            <parameter name="spectrumid"    value="[spectrum_id]"/> \
                                        </column> \
                                    </row> \
                                </block>' ;
            return (parseXML(tableXML_str));
        }

        function gnps_challenge_tableXML(){
            var tableXML_str = '<block id="gnps_library_compounds" type="table" pagesize="15"> \
                                    <row>  \
                                        <column label="View" type="genericurlgenerator" width="16" field="spectrum_id"> \
                                            <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                            <parameter name="REQUESTPARAMETER=SpectrumID" value="[spectrum_id]"/>\
                                            <parameter name="LABEL" value="[spectrum_id]"/>\
                                        </column>\
                                        <column field="Compound_Name" label="Interest" type="text" width="15"/> \
                                        <column field="Precursor_MZ" label="MZ" type="float" precision="2"/> \
                                        <column field="Instrument" label="Inst" type="text" width="10"/> \
                                        <column field="PI" label="PI" type="text" width="10"/> \
                                        <column field="Library_Class_Challenge" label="Quality" type="text" width="10"/> \
                                    </row>\
                                    <row expander="image"> \
                                        <column label="Image" type="jscolumnspectrumviewer_fromlibrary" colspan="5"> \
                                            <parameter name="spectrumid"    value="[spectrum_id]"/> \
                                        </column> \
                                    </row> \
                                </block>' ;
            return (parseXML(tableXML_str));
        }

        function decorateTable(table) {
            table.cellSpacing = "1";
            table.cellPadding = "4";
            table.className = "result";
            table.border = "0";
            table.width = "100%";
        }
    </script>

</head>
<body onload="init()">
<div id="bodyWrapper">
<%--      <a href="${livesearch.logo.link}"><div id="logo"></div></a>  --%>
    <a href="../index.jsp"><div id="logo"></div></a>

    <div id="titleHeader" style="text-align: center;margin-left:auto; margin-right:auto">
        <h1>User Summary Page</h1>
        <h2><%= identity %></h2>
    </div>

    <div id="textWrapper">

    </div>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
