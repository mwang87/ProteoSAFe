<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.*"
    import="edu.ucsd.livesearch.servlet.ManageSharing"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.storage.FileManager"
    import="edu.ucsd.livesearch.account.AccountManager"
    import="edu.ucsd.livesearch.dataset.Dataset"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.libraries.AnnotationManager"
    import="edu.ucsd.livesearch.libraries.SpectrumInfo"
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotation"
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="edu.ucsd.livesearch.servlet.LibraryServlet"
    import="java.util.*"
    import="org.apache.commons.lang3.StringEscapeUtils"
%><%
    boolean authenticated = ServletUtils.isAuthenticated(session);
    String url = request.getParameter("url");
    String identity = (String)session.getAttribute("livesearch.user");
    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");

    String library_target = request.getParameter("library");

    if(library_target == null){
        library_target = "all";
    }

    String library_name = library_target;
    List<String> all_names = new ArrayList<String>();
    if(library_name.compareTo("all") == 0){
        all_names.add("GNPS-LIBRARY");
        all_names.add("GNPS-PRESTWICKPHYTOCHEM");
        all_names.add("GNPS-SELLECKCHEM-FDA-PART1");
        all_names.add("GNPS-SELLECKCHEM-FDA-PART2");
        all_names.add("GNPS-NIH-CLINICALCOLLECTION1");
        all_names.add("GNPS-NIH-CLINICALCOLLECTION2");
        all_names.add("GNPS-NIH-NATURALPRODUCTSLIBRARY");
        all_names.add("GNPS-NIH-SMALLMOLECULEPHARMACOLOGICALLYACTIVE");
        all_names.add("GNPS-FAULKNERLEGACY");
        all_names.add("GNPS-EMBL-MCF");
        all_names.add("DEREPLICATOR_IDENTIFIED_LIBRARY");
    }
    else if(library_name.compareTo("PRIVATE-USER") != 0){
        all_names.add(library_name);
    }
    else if(library_name.compareTo("PRIVATE-USER") == 0 && isAdmin){
        all_names.add(library_name);
    }

    String render_string = LibraryServlet.queryLibrarySpectra(all_names, false);
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>GNPS Spectral Libraries</title>
    <meta content='The Global Natural Product Social Molecular Networking (GNPS) site creates a community for natural product researchers working with mass spectrometry data. ' name='description'>
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
    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>

    <!-- Help text tooltip scripts -->
    <script src="/ProteoSAFe/scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>


    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="/ProteoSAFe/scripts/result/lorikeet/css/lorikeet.css">


    <script language="javascript" type="text/javascript">
        var spectra = <%= render_string %>;

        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>

            content_div = document.createElement("div")
            $("#textWrapper").append(content_div)

            create_spectra_table(content_div)
        }

        function create_spectra_table(div){
            //creating an id
            for(var i = 0; i < spectra.spectra.length; i++){
                spectra.spectra[i].id = "spectra_" + i;
            }

            child_table = document.createElement("div");
            div.appendChild(child_table);

            var task = new Object();
            task.id = "1234";
            task.workflow = "Spectra";
            task.description = "Spectra";
            var generic_table = new ResultViewTable(gnps_spectra_tableXML(), "main", task);
            generic_table.setData(spectra.spectra);
            generic_table.render(child_table, 0);
        }

        function gnps_spectra_tableXML(){
            var tableXML_str = '<block id="gnps_library_compounds" type="table"> \
                                    <row>  \
                                        <column label="View" type="genericurlgenerator" width="16" field="spectrum_id"> \
                                            <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                            <parameter name="REQUESTPARAMETER=SpectrumID" value="[spectrum_id]"/>\
                                            <parameter name="LABEL" value="[spectrum_id]"/>\
                                        </column>\
                                        <column field="library_membership" label="Lib" type="text" width="10"/> \
                                        <column field="Compound_Name" label="Name" type="text" width="15"/> \
                                        <column field="Adduct" label="Adduct" type="text" width="15"/> \
                                        <column field="Precursor_MZ" label="MZ" type="float" precision="2"/> \
                                        <column field="Instrument" label="Inst" type="text" width="10"/> \
                                        <column field="PI" label="PI" type="text" width="10"/> \
                                        <column field="Library_Class" label="Quality" type="text" width="10"/> \
                                        <column field="submit_user" label="User" type="text" width="10"/> \
                                        <column field="create_time" label="Time" type="text" width="10"/> \
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
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>

    <div id="titleHeader" style="text-align: justify;margin-left:auto; margin-right:auto; height: 50px">
        <h1>
            GNPS Public Spectral Library
        </h1>
    <% if( library_target.equals("all") == false && library_target.equals("PRIVATE-USER") == false ){%>
        <a href="ftp://ccms-ftp.ucsd.edu/Spectral_Libraries/<%= library_target %>.mgf" target="_blank" download="<%= library_target %>.mgf" style="position : relative; top:-50px; left: 680px">
            <img src="/ProteoSAFe/images/plugins/download_icon.png" height="42" width="42" >
        </a>
    <%}%>
    <% if( library_target.equals("all") == true ){%>
        <a href="ftp://ccms-ftp.ucsd.edu/Spectral_Libraries/ALL_GNPS.mgf" target="_blank" download="ALL_GNPS.mgf" style="position : relative; top:-50px; left: 680px">
            <img src="/ProteoSAFe/images/plugins/download_icon.png" height="42" width="42" >
        </a>
    <%}%>
    </div>

    <div id="textWrapper">
    </div>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
