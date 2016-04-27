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
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotationSet"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="java.util.*"
    import="org.apache.commons.lang3.StringEscapeUtils"
    import="org.json.simple.JSONObject"
%>
<%!
    public static long calculate_all_task_time(Collection<Task> all_tasks){
        long total_run_time = 0;
        //Grouping tasks by user
        for(Task task : all_tasks){
            total_run_time += task.getElapsedTime();
        }

        return total_run_time;
    }
%>

<%!
    public static Map<String, List<Task> > create_workflow_histogram(Collection<Task> all_tasks){
        Map<String, List<Task> > user_tasks = new HashMap<String, List<Task> >();
        String workflow_filter = "ALL";
        String site_filter = "GNPS";
        //Grouping tasks by user
        for(Task task : all_tasks){
            String user = task.getUser();
            String workflow = task.getFlowName();
            String site = task.getSite();

            if(site.compareTo(site_filter) != 0 && site.compareTo("MASSIVE") != 0){
                continue;
            }

            if(workflow_filter.compareTo("ALL") != 0 && workflow_filter.compareTo(workflow) != 0){
                continue;
            }
            if(!user_tasks.containsKey(user)){
                user_tasks.put(user, new ArrayList<Task>());
            }
            user_tasks.get(user).add(task);
        }

        return user_tasks;
    }
%>

<%!
    public static Map<String, List<Dataset> > create_user_dataset_mapping(){
        Map<Task, Dataset> tasks_map = DatasetManager.queryAllDatasets();
        Map<String, List<Dataset> > user_datasets = new HashMap<String, List<Dataset> > ();

        for(Task task : tasks_map.keySet()){
            String user = task.getUser();

            if(!user_datasets.containsKey(user)){
                user_datasets.put(user, new ArrayList<Dataset>());
            }

            user_datasets.get(user).add(tasks_map.get(task));
        }

        return user_datasets;
    }
%>

<%!
    public static List<SpectrumInfo> get_library_spectra(){
        List<String> library_names = new ArrayList<String>();

        library_names.add("GNPS-LIBRARY");
        library_names.add("GNPS-PRESTWICKPHYTOCHEM");
        library_names.add("GNPS-SELLECKCHEM-FDA-PART1");
        library_names.add("GNPS-SELLECKCHEM-FDA-PART2");
        library_names.add("GNPS-NIH-CLINICALCOLLECTION1");
        library_names.add("GNPS-NIH-CLINICALCOLLECTION2");
        library_names.add("GNPS-NIH-NATURALPRODUCTSLIBRARY");
        library_names.add("GNPS-NIH-SMALLMOLECULEPHARMACOLOGICALLYACTIVE");
        library_names.add("GNPS-FAULKNERLEGACY");
        library_names.add("GNPS-EMBL-MCF");
        library_names.add("DEREPLICATOR_IDENTIFIED_LIBRARY");


        List<SpectrumInfo> spectra = new ArrayList<SpectrumInfo>();

        for(String library_name : library_names){
            List<SpectrumInfo> additional_lib_spectra = AnnotationManager.Get_Library_Spectra(library_name, false);
            spectra.addAll(additional_lib_spectra);
        }

        return spectra;
    }
%>

<%!
    public static Map<SpectrumInfo, Task> get_Task_SpectrumInfo(List<SpectrumInfo> spectra){
        List<String> submitted_spectra_tasks_list = new ArrayList<String>();
        for(SpectrumInfo spec : spectra){
            submitted_spectra_tasks_list.add(spec.getTask_id());
        }
        Map<String, Task> submitted_spectra_tasks_map = TaskManager.queryTaskList(submitted_spectra_tasks_list);

        Map<SpectrumInfo, Task> spec_to_task_map = new HashMap<SpectrumInfo, Task>();

        for(SpectrumInfo spec : spectra){
            spec_to_task_map.put(spec, submitted_spectra_tasks_map.get(spec.getTask_id()) );
        }

        return spec_to_task_map;
    }
%>

<%!
    public static Map<String, List<SpectrumInfo> > create_user_spectra_mapping(List<SpectrumInfo> spectra){
        Map<String, List<SpectrumInfo> > spectra_map = new HashMap<String, List<SpectrumInfo> > ();

        //Creating a list of tasks to query the owner
        List<String> submitted_spectra_tasks_list = new ArrayList<String>();
        for(SpectrumInfo spec : spectra){
            submitted_spectra_tasks_list.add(spec.getTask_id());
        }
        Map<String, Task> submitted_spectra_tasks_map = TaskManager.queryTaskList(submitted_spectra_tasks_list);

        for(SpectrumInfo spec : spectra){
            String user = submitted_spectra_tasks_map.get(spec.getTask_id()).getUser();

            if(!spectra_map.containsKey(user)){
                spectra_map.put(user, new ArrayList<SpectrumInfo>());
            }

            spectra_map.get(user).add(spec);
        }

        return spectra_map;
    }
%>



<%!
    public static String render_user_json(  Map<String, List<Task> > users_to_tasks,
                                            Map<String, List<Dataset> > users_to_datasets,
                                            Map<String, List<SpectrumInfo> > users_to_spectra){
        String output = " { \"users\": [";

        int user_count = 0;
        for(String user : users_to_tasks.keySet()){
            //Counting the number of public and private datasets
            int private_datasets = 0;
            int public_datasets = 0;

            int user_spectra = 0;

            if(users_to_datasets.get(user) != null){
                for(Dataset dataset : users_to_datasets.get(user)){
                    if(dataset.isPrivate()){
                        private_datasets += 1;
                    }
                    else{
                        public_datasets += 1;
                    }
                }
            }

            if(users_to_spectra.get(user) != null){
                user_spectra = users_to_spectra.get(user).size();
            }

            //Getting user information
            Map<String, String> user_info = AccountManager.getInstance().getProfile(user);

            user_count++;
            output += "{";
            output += "\"user\"" + ":" + "\"" + user + "\"" + ",";
            output += "\"organization\"" + ":" + "\"" + user_info.get("organization") + "\"" + ",";
            output += "\"jobscount\"" + ":" + "\"" + users_to_tasks.get(user).size() + "\"" +  ",";
            output += "\"privatedatasets\"" + ":" + "\"" + private_datasets + "\"" + ",";
            output += "\"publicdatasets\"" + ":" + "\"" + public_datasets + "\"" + ",";
            output += "\"alldatasets\"" + ":" + "\"" + (private_datasets + public_datasets) + "\"" + ",";
            output += "\"numberspectra\"" + ":" + "\"" + user_spectra + "\"";
            output += "}";
            if(user_count < users_to_tasks.size())
                output += ",";
        }

        output += "] }";

        return output;
    }

%>

<%!
    public static String render_spectra_string(List<SpectrumInfo> spectra, Map<SpectrumInfo, Task> spec_to_task){

        List<String> all_spectra_id = new ArrayList<String>();
        Map<String, SpectrumInfo> specid_to_specinfo = new HashMap<String, SpectrumInfo>();

        for(SpectrumInfo spec : spectra){
            all_spectra_id.add(spec.getSpectrum_id());
            specid_to_specinfo.put(spec.getSpectrum_id(), spec);
        }

        Map<String, SpectrumAnnotationSet> spectra_to_all_annotations = AnnotationManager.Get_All_Spectrum_Annotations_Batch_Map(all_spectra_id);

        //Building the String
        StringBuilder render_string_builder = new StringBuilder();
        render_string_builder.append("{ \"spectra\": [");
        int spectrum_count = 0;
        for(String spec_id : spectra_to_all_annotations.keySet()){
            spectrum_count++;
            render_string_builder.append("{");
            render_string_builder.append("\"spectrumid\":");
            render_string_builder.append("\"" + spec_id + "\",");
            render_string_builder.append("\"creationtime\":");
            render_string_builder.append("\"" + spec_to_task.get(specid_to_specinfo.get(spec_id)).getCreateTime() + "\",");
            render_string_builder.append("\"annotationcount\":");
            render_string_builder.append("\"" + spectra_to_all_annotations.get(spec_id).Annotation_List.size() + "\",");
            render_string_builder.append("\"annotation\":");
            render_string_builder.append("\"" + JSONObject.escape(spectra_to_all_annotations.get(spec_id).Annotation_List.get(0).getCompound_Name()) + "\",");
            render_string_builder.append("}");
            if(spectrum_count < spectra.size()){

                render_string_builder.append(",");
            }
        }
        render_string_builder.append("]}");
        String render_string = render_string_builder.toString();

        return render_string;
    }

%>

<%!
    public static String render_subscription_stats(){



        Map<Integer, List<String> > all_subscriptions = SubscriptionManager.get_all_subscriptions();

        int total_subscriptions = 0;
        int total_subscribers = 0;
        List<String> subscriber_list = new ArrayList<String>();
        Map<String, Integer> unique_subscribers = new HashMap<String, Integer>();

        for(Integer dataset_id : all_subscriptions.keySet()){
            total_subscriptions += all_subscriptions.get(dataset_id).size();
            for(String user : all_subscriptions.get(dataset_id)){
                unique_subscribers.put(user, 0);
            }
        }

        //Building the String
        StringBuilder render_string_builder = new StringBuilder();
        render_string_builder.append("{");
        render_string_builder.append("\"subscribers\" : ");
        render_string_builder.append(unique_subscribers.keySet().size());
        render_string_builder.append(",");
        render_string_builder.append("\"subscriptions\" : ");
        render_string_builder.append(total_subscriptions);
        render_string_builder.append("}");
        String render_string = render_string_builder.toString();

        return render_string;
    }

%>

<%
    boolean authenticated = ServletUtils.isAuthenticated(session);
    String url = request.getParameter("url");
    String identity = (String)session.getAttribute("livesearch.user");
    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");

    if(isAdmin == false){
        String redirectURL = "/";
        //response.sendRedirect(redirectURL);
        //return;
    }

    //Querying for All Users
    Collection<Task> all_tasks = TaskManager.queryTasksBySite(TaskStatus.DONE, "GNPS");
    Map<String, List<Task> > user_tasks = create_workflow_histogram(all_tasks);
    Map<String, List<Dataset> > user_datasets = create_user_dataset_mapping();
    List<SpectrumInfo> spectra = get_library_spectra();
    Map<String, List<SpectrumInfo> > user_spectra = create_user_spectra_mapping(spectra);
    Map<SpectrumInfo, Task> spec_to_task = get_Task_SpectrumInfo(spectra);


    String users_data = render_user_json(user_tasks, user_datasets, user_spectra);

    String spectra_data = render_spectra_string(spectra, spec_to_task);

    String subscription_data = render_subscription_stats();

    long total_run_time = calculate_all_task_time(all_tasks);
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>UCSD/CCMS - GNPS Admin Page</title>
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
    <script src="/ProteoSAFe/scripts/admin/gnps_admin_scripts.js" language="javascript" type="text/javascript"></script>

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

    <%-- <script type="text/javascript" src="/ProteoSAFe/scripts/result/highcharts/highcharts.js"></script> --%>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/highcharts/highstock.js"></script>

    <script language="javascript" type="text/javascript">
        var users_data = <%= users_data %>;
        var spectra_data = <%= spectra_data %>;
        var subscription_data = <%= subscription_data %>;
        var total_run_time = <%= total_run_time %>;

        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>



            content_div = document.createElement("div")
            $("#textWrapper").append(content_div)

            header = document.createElement("h2");
            header.innerHTML = "All Users";
            content_div.appendChild(header);

            //Creating unique IDs
            for(var i = 0; i < users_data.users.length; i++){
                users_data.users[i].id = "user_" + i;
            }

            create_users_table(content_div, users_data)



            header = document.createElement("h2");
            header.innerHTML = "All Spectra";
            content_div.appendChild(header);
            //create_spectra_table(document.createElement("br"))
            //create_spectra_table(document.createElement("br"))

            create_unique_compound_name_count(content_div, spectra_data)
            create_GNPS_Annotation_revision_count(content_div, spectra_data)
            create_spectra_table(content_div, spectra_data)

            display_subscription_stats(content_div, subscription_data)

            library_size_throughout_time(content_div)

            plot_gnps_datasets_size_over_time(content_div)

        }





    </script>

</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>

    <div id="titleHeader" style="text-align: justify;margin-left:auto; margin-right:auto">
        <h1>
            GNPS Admin Page
        </h1>
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
