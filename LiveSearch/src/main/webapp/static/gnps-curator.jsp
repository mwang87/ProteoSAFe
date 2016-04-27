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
            GNPS - Library Curation
        </h1>
    </div>
    
    
    <div id="textWrapper" style="text-align: justify;height:800px;top:30px">
        <div style="position:absolute;top:0px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                GNPS Library Types
            </h2>
            <br>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <h3>Community Annotated Spectral Libraries</h3>
                <br>
                The GNPS library is a community contributed and contain spectra that have been positively identified by contributors to be from a given compound. 
                <br>
                <br>
                <h3>Challenge Spectra</h3>
                At some point interesting spectra are unidentifiable in the data. So instead of writing these spectra off, make them available to the community to help you decrypt in the GNPS Challenge Spectra. 
                
            </div>
        </div>
        
        <div style="position:absolute;top:0px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Updating 
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='http://massive.ucsd.edu/ProteoSAFe/static/massive.jsp'>
                    <img src="../images/gnps_splash/submitdata.png" alt="some_text" width="125" align="left" style="border:1px;margin:3px;margin-right:6px">
                </a>
                <a href='http://massive.ucsd.edu/ProteoSAFe/static/massive.jsp'><b>Submit</b></a> your own data to be made public MassIVE datasets. These MassIVE datasets must be <b> prefixed with GNPS </b> to be visible to other GNPS users. Take advantage of <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a> to learn more about your dataset after publication automatically. New hits to the community curated libraries and related datasets are reported. <a href='https://bix-lab.ucsd.edu/display/Public/GNPS+MassIVE+Dataset+Creation'> <b>Documentation</b> </a>
            </div>
        </div>
        
        
        <div style="position:absolute;top:200px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                MassIVE Public GNPS Datasets
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../datasets.jsp#{"title_input":"GNPS"}'>
                    <img src="../images/gnps_splash/browsedatapng.png" alt="some_text" width="120" align="left" style="border:0px;margin:3px;margin-right:12px">
                </a>
                <a href='../datasets.jsp#{"title_input":"GNPS"}'><b>Browse</b></a> publically available datasets. Here you can download these datasets as well as comment on them so others in the community can see any updates or any new analysis. Additionally, users can subscribe to the datasets and get updates when new identifications are made via GNPS's <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a>. To read further on how to take advantage of the subscriptions to MassIVE datasets and other social networking features <a href='https://bix-lab.ucsd.edu/display/Public/Knowledge+Base+Curation+-+Social+Networking'> click here </a>.
            </div>
        </div>
        
        <div style="position:absolute;top:430px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Browse Community Spectral Library
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../libraries.jsp'>
                    <img src="../images/gnps_splash/curatedlibs.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                <a href='../libraries.jsp'><b>Browse</b></a> the community contributed and community curated spectral libraries of natural products. These MS/MS libraries are community contributed and community curated. Users can peak at the inside of these libraries, as well as use them for data analysis. If corrections need to be made, users should refer to <a href="https://bix-lab.ucsd.edu/display/Public/Knowledge+Base+Curation+-+Social+Networking"> this documentation</a>. 
            </div>
        </div>
        
        <div style="position:absolute;top:430px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Molecule Explorer
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_molecule_dataset'>
                    <img src="../images/gnps_splash/curatedlibs.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                Bridge the connection between molecules and datasets. <a href='/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_molecule_dataset#{"table_sort_history":"UnidentifiedPrecursorNeighbor_dsc"}'>Explore</a> exactly where certain molecules are found in all the publically available dataset at GNPS. Powered by GNPS's <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a>, users are able to see not only which datasets contain what compound, but how many known and unknown analogs exist in all datasets!
            </div>
        </div>
        
        
        <div style="position:absolute;top:640px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Rarefaction Curve Generation
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS"%7D'>
                    <img src="../images/gnps_splash/molecularnetworking.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                The <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS",%22library_on_server%22:%22d.speclibs;d.staticlibs/Massbank;d.staticlibs/ReSpect;%22%7D'><b>Data Analysis</b></a> portal will additionally provide a method to create rarefaction curves. These curves will allow you to assess the diversity of MS/MS spectra and ultimately of compounds within your data. For documentation click <a href="https://bix-lab.ucsd.edu/display/Public/Rarefaction+Curve+Documentation" >here</a>.
            </div>
        </div>
        
        <div style="position:absolute;top:640px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Challenge Spectra
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-CHALLENGE"%7D'>
                    <img src="../images/gnps_splash/annotate.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                There will always be spectra that stumps the best. Scientists can share these intriguing unidentified yet interesting spectra as challenge spectra. To deposit challenge spectra click <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-CHALLENGE"%7D'>here</a> and please refer to the <a href="https://bix-lab.ucsd.edu/display/Public/Add+Spectra+Single+Documentation" >documentation</a>. To browse user uploaded challenge spectra, <a href="http://gnps.ucsd.edu/ProteoSAFe/result.jsp?task=a6d038580d6a4423a16f82ed59ff7a8d&view=view_all_annotations_DB_current" >click here</a>. 
            </div>
        </div>
    </div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
