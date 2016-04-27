<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="styles/main.css" rel="stylesheet" type="text/css" />
	<link rel="shortcut icon" href="images/favicon.ico" type="image/icon" />
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/invoke.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
	function init(){
		<%= ServletUtils.JSLogoBlock("logo", request, session) %>
	}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>Proteomics Search</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br />
	<div id="textWrapper" style="text-align: justify;">

<h2>General Information</h2>
ProteoSAFe is a web server for analyzing your MS/MS proteomics data online.

The
<a href="http://proteomics.ucsd.edu">Center for Computational Mass Spectrometry</a>
has many tools, and ProteoSAFe allows you to use all of them within the
framework of a single user interface. The server utilizes a distributed
computational cluster, so your analysis should finish fairly quickly.
Please <a href="contact.jsp">contact us</a> with any questions you may have.

<h2>Using the Server</h2>
The interface should be fairly self-explanatory.  Just choose the workflow
(see below) and fill in the associated options. Then upload your data files,
assign them to the proper file categories of your search, and click "Submit".
After the analysis is completed, you can browse or download the results.

<p>
<!--
There is a <a>video demo</a> to walk you through the basic use of the website.
-->
In addition, much of the interface comes with tool-tips.  If you hover your
mouse over a piece of text and see a question mark next to the mouse pointer,
then context-sensitive help exists.  A floating help bubble should appear,
which you can "pin" in place by simply clicking on the original page text.
Then click on the small "X" in the upper right corner of the help bubble to
remove it.
</p>

<h2>Current Workflows</h2>
<!--
Currently we have three tool integrated into the website, with more to come.  The first tool,
<b>Inspect</b>, is a general purpose database search program.  It is specifically
built to handle accurate identifications of post-translational modifications.
Moreover, the tag-based filters make Inspect orders of magnitude faster than
SEQUEST or other database search engines. The second tool is <b>MS-Alignment</b>, which
is a variant of Inspect that allows
the user to search for any and all post-translational modifications, a "blind"
search. The third tool is, <b>Pepnovo</b>, is a software tool for  de novo sequencing
of peptides from mass spectra. PepNovo uses a probabilistic network to model
the peptide fragmentation events in a mass spectrometer.
-->

<p>For a more in-depth discussion of the various tools offered as ProteoSAFe
workflows, please visit our software development page
<a href="http://proteomics.ucsd.edu/Software/">here</a>.
</p>

<h2>FAQ</h2>
<!--
<p><b>How do I upload multiple files as a single dataset?</b><br/>
Multiple files intended to be searched together (e.g. different fractions of a MudPIT)
should be zipped together, and the zip should be uploaded.
</p>

<p><b>What's the difference between the "search type" options?</b><br/>
The different search types (e.g. Inspect, MS-Alignment, PepNovo) correspond
to different search algorithms.  To read more about the tools, please visit
the lab's <a href="http://proteomics.bioprojects.org/Software.html"> Software</a> page.
</p>
-->

<p><b>How do I know what to choose for parameter values?</b><br/>
We have set up the interface with default parameters that should work well.  For
questions about a specific parameter, read the tool-tip.
</p>

<p><b>How do I view the results of my search?</b><br/>
If you sign up for an account, then all searches are kept for you.  You can view
the results at any time by clicking on Jobs in the main navigation bar.  You will
see a list of all your jobs.  If you don't have an account, then then you can
bookmark the submittion page for your job, which is a static link (deleted after 1 month).
</p>

<p><b>How do I know when my job is finished?</b><br/>
If you sign up for an account, then you will receive an email when you search completes.
</p>

</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
