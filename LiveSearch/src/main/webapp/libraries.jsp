<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface"
    import="edu.ucsd.livesearch.servlet.ManageParameters"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.storage.FileManager"
    import="edu.ucsd.livesearch.storage.SequenceRepository"
    import="edu.ucsd.livesearch.storage.SequenceFile"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.AminoAcidUtils.PTM"
    import="edu.ucsd.livesearch.util.AminoAcidUtils.PTMType"
    import="edu.ucsd.livesearch.util.Commons"
    import="java.io.File"
    import="java.net.URLDecoder"
    import="java.util.List"
    import="java.util.Map"
    import="org.apache.commons.lang3.StringEscapeUtils"
%><%
    // prevent caching
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Cache-Control", "post-check=0, pre-check=0");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);
    // get authenticated user, if any
    String user = (String)session.getAttribute("livesearch.user");
    // if this search is based on a previous task, pre-populate accordingly
    String taskId = request.getParameter("task");
    if (taskId == null)
        taskId = "";
    // pre-populate workflow parameter as necessary
    String workflow = request.getParameter("workflow");
    if (workflow == null)
        workflow = "";
    // get any pre-specified parameter values encoded into the request
    String params = request.getParameter("params");
    if (params == null)
        params = "";
    else params = StringEscapeUtils.escapeJava(params);
    // pre-populate email parameter as necessary
    String email = (String)session.getAttribute("livesearch.email");
    if (email == null)
        email = "";
    // get installed workflows
    Map<String, String> workflows =
        DownloadWorkflowInterface.getInstalledWorkflows(user);
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<% ServletUtils.printSessionInfo(out, session); %>
<head>
    <link href="styles/main.css" rel="stylesheet" type="text/css"/>
    <link href="images/favicon.ico" rel="shortcut icon" type="image/icon"/>

    <!-- General ProteoSAFe scripts -->
    <script src="/ProteoSAFe/scripts/form.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/render.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/util.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/massivepage.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/parameterlink.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>

        <!-- Help text tooltip scripts -->
    <script src="/ProteoSAFe/scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>

    <!-- Widget scripts -->
    <script src="scripts/tooltips/spin.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>

    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="/ProteoSAFe/scripts/result/lorikeet/css/lorikeet.css">

    <script language="javascript" type="text/javascript">
        function init() {
            // load page logo block
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>

            //Loading libraries
            populate_recent_library_spectra()
        }

        function populate_recent_library_spectra(){
            all_library_spectrum_url = "/ProteoSAFe/LibraryServlet?library=GNPS-LIBRARY"

            $.ajax({
                url: all_library_spectrum_url,
                cache: false,
                success: function(json){
                    results_data = JSON.parse(json).spectra
                    render_data = new Array()

                    for(var i = 0; i < results_data.length; i++){
                        if (results_data[i].Library_Class < 4){
                            render_data.push(results_data[i])
                        }
                    }

                    render_data.sort(function(a,b){
                        return a.create_time < b.create_time ? 1 : -1;
                    });

                    //creating an id
                    for(var i = 0; i < render_data.length; i++){
                        render_data[i].id = "spectra_" + i;
                    }


                    child_table = document.createElement("div");
                    $("#recentLibraryAdditions").append(child_table);

                    var task = new Object();
                    task.id = "1234";
                    task.workflow = "Recent Library Spectra";
                    task.description = "Recent Library Spectra";
                    var generic_table = new ResultViewTableGen(recent_library_spectra_xml(), "other_library_spectra", task, 0);
                    generic_table.setData(render_data);
                    generic_table.render(child_table, 0);

                }
            });
        }

        function recent_library_spectra_xml(){
            var tableXML_str = '<block id="gnps_library_compounds" type="table" pagesize="10"> \
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

        // TODO: fix or move all below functions
        function decorateTable(table) {
            table.cellSpacing = "1";
            table.cellPadding = "4";
            table.className = "result";
            table.border = "0";
            table.width = "100%";
        }

    </script>

    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <meta http-equiv="PRAGMA" content="NO-CACHE"/>
    <title>GNPS Spectral Libraries</title>
    <meta content='The Global Natural Product Social Molecular Networking (GNPS) site creates a community for natural product researchers working with mass spectrometry data. ' name='description'>
</head>
<body onload="init();">

<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>

    <div id="textWrapper">
    <!-- Modal overlay -->
    <div id="overlay" class="overlay"><div id="overlay_spinner"></div></div>

        <div id="Library and Revisions">
            <h1>
                GNPS Public Spectral Libraries
            </h1>
            <br>


            <table class="result">
                <tr>
                    <th>Library Name</th>
                    <th>View</th>
                    <th>Description</th>
                    <th>Releases</th>
                </tr>
                <tr>
                    <td>All GNPS Library Spectra</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=all">View</a></td>
                    <td>This contains all available spectra available publically for search at GNPS excluding third party libraries. </td>
                    <td></td>
                </tr>
                <tr>
                    <td>GNPS Library</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-LIBRARY#%7B%22Library_Class_input%22%3A%221%7C%7C2%7C%7C3%7C%7CEXACT%22%7D">View</a></td>
                    <td>The GNPS library contains natural product compounds from user contributions.</td>
                    <td  style="width:80px">
                        <a href="/ProteoSAFe/result.jsp?task=c1f66c034410434387915189b65691cc&view=view_all_annotations_DB" title="May 30 2014">Release 8</a>
                        <br>
                        <a href="/ProteoSAFe/result.jsp?task=376b10809dfa4e938e096ee1d4735dc0&view=view_all_annotations_DB" title="May 19 2014">Release 7</a>
                        <br>
                        <a href="/ProteoSAFe/result.jsp?task=15a0db6a36484a8da2a3ab0fee176ba6&view=view_all_annotations_DB" title="April 21 2014">Release 6</a>
                        <br>
                        <a href="/ProteoSAFe/result.jsp?task=b2c8df530ded46b4be976263e2f4209e&view=view_all_annotations_DB" title="April 7 2014">Release 5</a>
                        <br>
                        <a href="/ProteoSAFe/result.jsp?task=f7eff913895e4d29b83ff1b250602f85&view=view_all_annotations_DB" title="March 24 2014">Release 3</a>
                        <br>
                        <a href="/ProteoSAFe/status.jsp?task=6bc47aa177a24513949827f3accdadd1" title="March 3 2014">Release 2</a>
                        <br>
                        <a href="/ProteoSAFe/status.jsp?task=698fc5a09db74c7492983b3673ff5bf6" title="February 2 2014">Release 1</a>
                        <br>
                    </td>
                </tr>
                <tr>
                    <td>FDA Library Pt 1</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-SELLECKCHEM-FDA-PART1">View</a></td>
                    <td>Approved drug library from Selleckchem Part 1 run by Sirenas MD.</td>
                    <td><a href="/ProteoSAFe/result.jsp?task=172e5a0ba5f243ff866d416cc6ec6e5e&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>FDA Library Pt 2</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-SELLECKCHEM-FDA-PART2">View</a></td>
                    <td>This set of reference compounds generated by the Dorrestein Lab contains 535 FDA natural product compounds complements part 1. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=2d8f711dd1d54e40972fce07330781ad&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>PhytoChemical Library</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-PRESTWICKPHYTOCHEM">View</a></td>
                    <td>140 compounds from the Prestwick Phytochemical Library generated by the Dorrestein Lab.</td>
                    <td><a href="/ProteoSAFe/result.jsp?task=e68680173b5d45b1882cf474db34fa3e&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>NIH Clinical Collection 1</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-NIH-CLINICALCOLLECTION1">View</a></td>
                    <td>327 compounds from the NIH Clinical Collection 1 generated by the Dorrestein Lab. Further information about the collection can be found <a href="http://www.nihclinicalcollection.com/index.php"> here </a>. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=2d9f494226e043189f4afadf20d82e55&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>NIH Clinical Collection 2</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-NIH-CLINICALCOLLECTION2">View</a></td>
                    <td>164 compounds from the NIH Clinical Collection 2 generated by the Dorrestein Lab. Further information about the collection can be found <a href="http://www.nihclinicalcollection.com/index.php"> here </a>. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=37891aa9e4ee42d49c8e41e0d411b091&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>NIH Natural Products Library</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-NIH-NATURALPRODUCTSLIBRARY">View</a></td>
                    <td>1256 compounds from the NIH Natural Products Library generated by the Dorrestein Lab. Further information about the collection can be found <a href="http://www.ncats.nih.gov/research/tools/preclinical/npc/pharmaceutical-collection.html"> here </a>. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=f108451b86d74573af181603673aecde&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>Pharmacologically Active Compounds in the NIH Small Molecule Repository</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-NIH-SMALLMOLECULEPHARMACOLOGICALLYACTIVE">View</a></td>
                    <td>1460 compounds from the Pharmacologically Active Compounds in the NIH Small Molecule Repository generated by the Dorrestein Lab. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=291afda2c99a446db9c03cba77c06e59&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>Faulkner Legacy Library provided by Sirenas MD</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-FAULKNERLEGACY">View</a></td>
                    <td>127 compounds from the Faulkner natural product legacy library. </td>
                    <td><a href="/ProteoSAFe/result.jsp?task=b21d25a6b613434d814382a4d5b48300&view=view_all_annotations_DB">Release 1</a></td>
                </tr>
                <tr>
                    <td>EMBL Metabolomics Core Facility (EMBL MCF)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=GNPS-EMBL-MCF">View</a></td>
                    <td>Standards run by EMBL Metabolomics Core Facility (EMBL MCF)</td>
                    <td></td>
                </tr>
                <tr>
                    <td>Dereplicator Identified MS/MS Spectra</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=DEREPLICATOR_IDENTIFIED_LIBRARY">View</a></td>
                    <td>MS/MS spectra identified in GNPS Public data automatically by dereplicator tool. Searching various compound databases, including marinlit, etc. and best matching MS/MS spectra with significant p-values.</td>
                    <td></td>
                </tr>
                <tr>
                    <td>Massbank Spectral Library (3rd Party)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=MASSBANK">View</a></td>
                    <td>ESI Positive MS/MS spectra from <a href="http://www.massbank.jp/?lang=en">Massbank</a>.</td>
                    <td></td>
                </tr>
                <tr>
                    <td>Massbank EU Spectral Library (3rd Party)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=MASSBANKEU">View</a></td>
                    <td>MS/MS spectra from <a href="http://massbank.ufz.de/MassBank/">Massbank EU </a> that are not included in Massbank JP.</td>
                    <td></td>
                </tr>
                <tr>
                    <td>Massbank NA Spectral Library (3rd Party)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=MONA">View</a></td>
                    <td>MS/MS spectra from <a href="http://mona.fiehnlab.ucdavis.edu/#/">Massbank NA </a> that were not present in Massbank JP.</td>
                    <td></td>
                </tr>
                <tr>
                    <td>ReSpect Spectral Library (3rd Party)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=RESPECT">View</a></td>
                    <td>ESI Positive MS/MS spectra from <a href="http://spectra.psc.riken.jp/">ReSpect for Phytochemicals</a>.</td>
                    <td></td>
                </tr>
                <tr>
                    <td>HMDB Spectral Library (3rd Party)</td>
                    <td><a href="/ProteoSAFe/gnpslibrary.jsp?library=HMDB">View</a></td>
                    <td>MS/MS spectra from <a href="http://www.hmdb.ca//">HMDB</a>.</td>
                    <td></td>
                </tr>
            </table>
        </div>

        <div>
            <h3 style="text-align:center">
                Recent Library Additions
            </h3>
            <div id="recentLibraryAdditions">
            </div>
        </div>

    </div>
    <br/>

    <jsp:include page="/filtered/footer.jsp"/>
    <br/>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
