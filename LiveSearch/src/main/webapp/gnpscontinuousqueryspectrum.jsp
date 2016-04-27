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
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="java.util.*"
    import="org.apache.commons.lang3.StringEscapeUtils"
%><%
    boolean authenticated = ServletUtils.isAuthenticated(session);
    String url = request.getParameter("url");
    String identity = (String)session.getAttribute("livesearch.user");
    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");

    String datasettask = request.getParameter("datasettask");
    String scannumber = request.getParameter("scan");
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
    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/massivepage.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/absolutelink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/parameterlink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/gnpscontinuousqueryspectrum.js" language="javascript" type="text/javascript"></script>

    <!-- Help text tooltip scripts -->
    <script src="/ProteoSAFe/scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>

    <script src="/ProteoSAFe/scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="/ProteoSAFe/styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />

    <script src="/ProteoSAFe/scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="/ProteoSAFe/styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />


    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/jquery.flot.selection.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/specview.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/peptide.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/aminoacid.js"></script>
    <script type="text/javascript" src="/ProteoSAFe/scripts/result/lorikeet/js/ion.js"></script>
    <link REL="stylesheet" TYPE="text/css" HREF="/ProteoSAFe/scripts/result/lorikeet/css/lorikeet.css">



    <script language="javascript" type="text/javascript">
        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>

            div = document.createElement("div")
            $("#textWrapper").append(div);

            var task = new Object();
            task.id = "1234";
            task.workflow = "Spectrum Annotations and Comments";
            task.description = "Spectrum Annotations and Comments";
            var query_spectrum_page = new ResultViewGNPSContinuousQuerySpectrum(null, "gnpscontinuousqueryspectrum", task);

            query_spectrum_page.render(div, 0);
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

</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="/ProteoSAFe/static/gnps-splash.jsp"><div id="logo"></div></a>

    <div id="titleHeader" style="text-align: justify;margin-left:auto; margin-right:auto">
        <h1 id="header">
        </h1>
    </div>

    <div id="textWrapper">
    </div>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
