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
%>
<%
    String user = (String)session.getAttribute("livesearch.user");


%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
    <link href="../styles/main.css" rel="stylesheet" type="text/css" />
    <link href="../images/favicon.ico" rel="shortcut icon" type="image/icon" />
    <script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
    <script language="javascript" type="text/javascript">
    function init() {
        <%= ServletUtils.JSLogoBlock("logo", request, session) %>
    }
    </script>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <title>GNPS - Theoretical/Insilico Tools and Libraries</title>
    <meta content='The Global Natural Product Social Molecular Networking (GNPS) site creates a community for natural product researchers working with mass spectrometry data. ' name='description'>
</head>
<body onload="init()">
<div id="bodyWrapper">
    <a href="/ProteoSAFe/static/gnps-splash.jsp"><div id="logo"></div></a>
    <br/>
    <% if (user == null) { %>
                                        <div class="askuserlogin"><b><font color="#980905">Please Login to Use Workflows</font></b></div>
        <% } %>


    <div id="textWrapper" style="text-align: justify;margin-left:auto; margin-right:auto">
        <h1>
            GNPS Theoretical/Insilico Tools and Libraries
        </h1>
    </div>

    <div id="textWrapper" style="text-align: justify;height:960px;top:30px;left:42px;">
        <div style="position:absolute;top:0px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Insilico Peptidic Natural Product Dereplicator
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                Dereplication of Peptidic Natural Product workflow. This analysis will compare your experimental MS/MS spectra against chemical structure databases, e.g. PubChem, etc.
                This workflow is currently in beta development stages so any feedback is welcome to improve analysis and usability. It is available <a href='../index.jsp?params=%7B"workflow":"DEREPLICATOR"%7D'>here</a>.
                View <a href='https://bix-lab.ucsd.edu/display/Public/Insilico+Peptide+Dereplicator+Documentation'>documentation</a>.
            </div>
        </div>

        <div style="position:absolute;top:0px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Peptidogenomics for Ribosomally Synthesized Post-translationally Modified Peptides (RiPPs) - RiPPquest
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                Identification of RiPP Natural Products workflow. This analysis will compare your experimental MS/MS spectra against full genome sequences to mine them for possible RiPP natural products.
                This workflow is currently in beta development stages so any feedback is welcome to improve analysis and usability. It is available <a href='../index.jsp?params=%7B"workflow":"RIPPQUEST"%7D'>here</a>.
            </div>
        </div>
    </div>


    <div id="textWrapper" style="text-align: justify;height:50px;top:80px;left:42px;position:relative;margin-left:auto;margin-right:auto;text-align:center">
    <jsp:include page="/filtered/footer.jsp"/>
    </div>

</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
