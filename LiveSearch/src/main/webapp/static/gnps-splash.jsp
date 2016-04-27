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
    <title>GNPS - The Future of Natural Products Research and Mass Spectrometry</title>
    <meta content='The Global Natural Product Social Molecular Networking (GNPS) site creates a community for natural product researchers working with mass spectrometry data. ' name='description'>
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
            The Future of Natural Products Research and Mass Spectrometry
        </h1>
        <div  style="margin-left:auto; margin-right:auto;width: 990px;height: 300px;margin: auto;position: relative;top: 17px;">
            <div style="
                        position: absolute;">
                <iframe width="495" height="280" src="//www.youtube.com/embed/vj1g0_5HeXQ?showinfo=0&rel=0&controls=1" frameborder="0" allowfullscreen rel="0" controls="0" showinfo="0"></iframe>
            </div>

            <div style="
                        left: 495px;
                        position: absolute;">
                <iframe width="495" height="280" src="//www.youtube.com/embed/TFe793BYW8s?showinfo=0&rel=0&controls=1" frameborder="0" allowfullscreen rel="0" controls="0" showinfo="0"></iframe>
            </div>
        </div>
    </div>

    <div style="height:0px;margin-top:20px">
        <a href="https://twitter.com/share" class="twitter-share-button" data-url="http://gnps.ucsd.edu" data-text="Check out GNPS! Lets share data together" data-size="large" data-count="none" data-hashtags="GNPS">Tweet</a>
        <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');</script>
        <a href="https://www.facebook.com/sharer/sharer.php?u=http%3A%2F%2Fgnps.ucsd.edu%2FProteoSAFe%2Fstatic%2Fgnps-splash.jsp">
            <img src="/ProteoSAFe/images/plugins/facebook/sharebutton.png">
            </img>
        </a>
    </div>


    <div id="textWrapper" style="text-align: justify;height:960px;top:30px;left:42px;">
        <div style="position:absolute;top:0px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Data Analysis
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS"%7D'>
                    <img src="../images/gnps_splash/molecularnetworking.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                The <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS",%22library_on_server%22:%22d.speclibs;%22%7D'><b>Data Analysis</b></a> portal will allow you to organize and visualize your mass spectrometry data. Leveraging the molecular networking techniques, there are additional tools to aid in understanding the unknowns in your sample.
                Check out the <a href="https://bix-lab.ucsd.edu/display/Public/Molecular+Networking+Documentation" >documentation</a> and live <strong> <a href='http://gnps.ucsd.edu/ProteoSAFe/result.jsp?view=network_displayer&componentindex=67&task=c95481f0c53d42e78a61bf899e9f9adb#%7B%7D'> demo</a></strong>.
                Further, a separate <a href="http://gnps.ucsd.edu/ProteoSAFe/index.jsp?params=%7B%22workflow%22:%22MOLECULAR-LIBRARYSEARCH%22,%22library_on_server%22:%22d.speclibs;%22%7D"> <b>dereplication workflow</b></a> is provided as a standalone workflow.
            </div>
        </div>

        <div style="position:absolute;top:0px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Create Public MassIVE Datasets
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='http://massive.ucsd.edu/ProteoSAFe/static/massive.jsp'>
                    <img src="../images/gnps_splash/submitdata.png" alt="some_text" width="125" align="left" style="border:1px;margin:3px;margin-right:6px">
                </a>
                <a href='http://massive.ucsd.edu/ProteoSAFe/?params=%7B%22workflow%22:%22MASSIVE%22,%22desc%22:%22GNPS%20-%20%22%7D'><b>Submit</b></a> your own data to be made public MassIVE datasets. These MassIVE datasets must be <b> prefixed with GNPS </b> to be visible to other GNPS users. Take advantage of <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a> to learn more about your dataset after publication automatically. New hits to the community curated libraries and related datasets are reported. <a href='https://bix-lab.ucsd.edu/display/Public/GNPS+MassIVE+Dataset+Creation'> <b>Documentation</b> </a>
            </div>
        </div>

        <div style="position:absolute;top:200px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Contribute to Libraries
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-BRONZE"%7D'>
                    <img src="../images/gnps_splash/annotate.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-top:5px;margin-right:6px">
                </a>
                Be a part of the collaborative community effort to <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-BRONZE"%7D'><b>create</b></a> the definitive collection of natural products MS/MS spectra.
                Additionally users can contribute varying levels of quality of spectra:  <a href='/ProteoSAFe/index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-BRONZE"%7D'><b>bronze</b></a>, <a href='/ProteoSAFe/index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-SILVER"%7D'><b>silver</b></a>, <a href='/ProteoSAFe/index.jsp?params=%7B"workflow":"ADD-SINGLE-ANNOTATED-GOLD"%7D'><b>gold</b></a>.
                GNPS gives the power to add spectra, update annotations, and facilitate dialog around these spectra, to provide a truly collaborative and open natural product MS/MS database.
                For documentation and definition of quality requirements click <a href="https://bix-lab.ucsd.edu/display/Public/Add+Spectra+Single+Documentation" >here</a>.
                To make corrections to and comments on exisiting spectra in the libraries, users should refer to <a href="https://bix-lab.ucsd.edu/display/Public/Spectral+Library+Curation"> this documentation</a>.
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
                <a href='../datasets.jsp#{"title_input":"GNPS"}'><b>Browse</b></a> publically available datasets. Here you can download these datasets as well as comment on them so others in the community can see any updates or any new analysis.
                Additionally, users can subscribe to the datasets and get updates when new identifications are made via GNPS's <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a>.
                To read further on how to take advantage of the subscriptions to MassIVE datasets and other social networking features <a href='https://bix-lab.ucsd.edu/display/Public/MassIVE+Datasets+Social+Networking'> click here </a>.
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
                <a href='../libraries.jsp'><b>Browse</b></a> the community contributed and community curated spectral libraries of natural products. These MS/MS libraries are community contributed and community curated. Users can peak at the inside of these libraries, as well as use them for data analysis. If corrections need to be made, users should refer to <a href="https://bix-lab.ucsd.edu/display/Public/Spectral+Library+Curation"> this documentation</a>.
            </div>
        </div>

        <div style="position:absolute;top:430px;left:500px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Molecular Explorer
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_molecule_dataset'>
                    <img src="../images/gnps_splash/moleculeexplorer.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                Bridge the connection between molecules and datasets. <a href='/ProteoSAFe/static/gnps-molecular-explorer.jsp'>Explore</a> exactly where certain molecules are found in all the publically available dataset at GNPS. Powered by GNPS's <a href="https://bix-lab.ucsd.edu/display/Public/GNPS+Continuous+Identification+and+Data+Exploration"><b>continuous identification</b></a>, users are able to see not only which datasets contain what compound, but how many known and unknown analogs exist in all datasets!
            </div>
        </div>


        <div style="position:absolute;top:640px;left:0px;width:400px;height:200px">
            <h2 style="margin-bottom:8px" align="center">
                Rarefaction Curve Generation
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                <a href='../index.jsp?params=%7B"workflow":"METABOLOMICS-SNETS"%7D'>
                    <img src="../images/gnps_splash/rarefactioncurve.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
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
                    <img src="../images/gnps_splash/challengespectra.png" alt="some_text" width="110" align="left" style="border:0px;margin:3px;margin-right:7px">
                </a>
                There will always be spectra that stumps the best. Scientists can share these intriguing unidentified yet interesting spectra as challenge spectra.
                To deposit challenge spectra click <a href='../index.jsp?params=%7B"workflow":"ADD-SINGLE-CHALLENGE"%7D'>here</a> and please refer to the <a href="https://bix-lab.ucsd.edu/display/Public/Challenge+Spectra" >documentation</a>. To browse user uploaded challenge spectra, <a href="http://gnps.ucsd.edu/ProteoSAFe/gnpslibrary.jsp?library=GNPS-LIBRARY#%7B%22Library_Class_input%22%3A%2210%22%7D" >click here</a>.
            </div>
        </div>

        <div style="position:absolute;top:840px;left:0px;width:400px;height:150px">
            <h2 style="margin-bottom:8px" align="center">
                GNPS Theoretical/Insilico
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                All things related to theoretical/insilico dereplication of natural products. <a href='../static/gnps-theoretical.jsp'>Browse</a> the tools and theoretical MS/MS libraries avaiable at GNPS.
            </div>
        </div>


        <div style="position:absolute;top:840px;left:500px;width:400px;height:150px">
            <h2 style="margin-bottom:8px" align="center">
                Documentation
            </h2>
            <div id=dataanalysisimg style="position:relative;top:0px;width:400px">
                Video tutorials can be found on <a href='https://www.youtube.com/channel/UCufTdDIUPjfoN604Igv_29g/videos'>YouTube</a>. Written documentation can be found <a href='https://bix-lab.ucsd.edu/display/Public/GNPS+Documentation+Page'>here</a>. To suggest further updates to GNPS or ask questions to the community, please
                post to the <a href='https://groups.google.com/forum/#!forum/molecular_networking_bug_reports'>forum</a>.
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
