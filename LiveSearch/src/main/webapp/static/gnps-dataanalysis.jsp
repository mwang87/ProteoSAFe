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
    <title>Welcome to ProteoSAFe</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
    <a href="${livesearch.logo.link}"><div id="logo"></div></a>
    <br/>
    <% if (user == null) { %>
                                        <div class="askuserlogin"><b><font color="#980905">Please Login to Use Workflows</font></b></div>
        <% } %>


    <div id="textWrapper" style="text-align: justify;margin-left:auto; margin-right:auto">
        <h1>
            GNPS - Data Analysis
        </h1>
    </div>
    
    
    <div id="textWrapper" style="text-align: justify;height:800px;top:30px">
        <div style="position:absolute;top:0px;left:0px;width:950px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Molecular Networking
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:950px">
                <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS"%7D'>
                    <img src="../images/gnps_splash/molecularnetworking.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                The <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS",%22library_on_server%22:%22d.speclibs;d.staticlibs/Massbank;d.staticlibs/ReSpect;%22%7D'><b>Data Analysis</b></a> portal will allow you to organize and visualize your mass spectrometry data. Leveraging the molecular networking techniques, there are additional tools to aid in understanding the unknowns in your sample. For a documentation click <a href="https://bix-lab.ucsd.edu/display/Public/Molecular+Networking+Documentation" >here</a>. Further, a separate <a href="http://gnps.ucsd.edu/ProteoSAFe/index.jsp?params=%7B%22workflow%22:%22MOLECULAR-LIBRARYSEARCH%22,%22library_on_server%22:%22d.speclibs;d.staticlibs/Massbank;d.staticlibs/ReSpect;%22%7D"> <b>dereplication workflow</b></a> is provided as a standalone workflow.
            </div>
        </div>
        
        
        <div style="position:absolute;top:200px;left:0px;width:950px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Spectral Library Search
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:950px">
                <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-BRONZE"%7D'>
                    <img src="../images/gnps_splash/annotate.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-top:5px;margin-right:6px">
                </a>
                Be a part of the collaborative community effort to <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-BRONZE"%7D'><b>create</b></a> the definitive collection of natural products MS/MS spectra. GNPS gives the power to add spectra, update annotations, and facilitate dialog around these spectra, to provide a truly collaborative and open natural product MS/MS database. For documentation and definition of quality requirements click <a href="https://bix-lab.ucsd.edu/display/Public/Add+Spectra+Single+Documentation" >here</a>. To make corrections to and comments on exisiting spectra in the libraries, users should refer to <a href="https://bix-lab.ucsd.edu/display/Public/Knowledge+Base+Curation+-+Social+Networking"> this documentation</a>. 
            </div>
        </div>
        
        
        <div style="position:absolute;top:430px;left:0px;width:950px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                MS/MS Rarefaction Curves
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:950px">
                <a href='../libraries.jsp'>
                    <img src="../images/gnps_splash/curatedlibs.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                <a href='../libraries.jsp'><b>Browse</b></a> the community contributed and community curated spectral libraries of natural products. These MS/MS libraries are community contributed and community curated. Users can peak at the inside of these libraries, as well as use them for data analysis. If corrections need to be made, users should refer to <a href="https://bix-lab.ucsd.edu/display/Public/Knowledge+Base+Curation+-+Social+Networking"> this documentation</a>. 
            </div>
        </div>
        
        <div style="position:absolute;top:640px;left:0px;width:950px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Other
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:950px">
                <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS"%7D'>
                    <img src="../images/gnps_splash/molecularnetworking.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                The <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS",%22library_on_server%22:%22d.speclibs;d.staticlibs/Massbank;d.staticlibs/ReSpect;%22%7D'><b>Data Analysis</b></a> portal will additionally provide a method to create rarefaction curves. These curves will allow you to assess the diversity of MS/MS spectra and ultimately of compounds within your data. For documentation click <a href="https://bix-lab.ucsd.edu/display/Public/Rarefaction+Curve+Documentation" >here</a>.
            </div>
        </div>
        
    </div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
