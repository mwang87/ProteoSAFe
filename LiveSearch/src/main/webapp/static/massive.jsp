<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
%><%
	// prevent caching
	response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	response.addHeader("Cache-Control", "post-check=0, pre-check=0");
	response.setHeader("Pragma", "no-cache");
	response.setDateHeader("Expires", 0);
	// get authenticated user, if any
	String user = (String)session.getAttribute("livesearch.user");
	// check to see if this page was loaded due to an authentication redirect
	boolean authRedirect = false;
	String redirect = request.getParameter("redirect");
	if (redirect != null && redirect.equals("auth"))
		authRedirect = true;
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
	<link href="../styles/main.css" rel="stylesheet" type="text/css" />
	<link href="../images/favicon.ico" rel="shortcut icon" type="image/icon" />
	<script src="../scripts/util.js" language="javascript" type="text/javascript"></script>
	<script language="javascript" type="text/javascript">
	function filterDatasets(inputId) {
		if (inputId == null)
			return;
		var input = document.getElementById(inputId);
		if (input == null)
			return;
		var value = input.value;
		var url = "../datasets.jsp#{";
		var hash = "";
		switch (inputId) {
			case "dataset.title":
				hash += "\"title_input\":\"" + value + "\"";
				break;
			case "dataset.id":
				hash += "\"dataset_input\":\"" + value + "\"";
				break;
			default: return;
		}
		window.location = url + encodeURIComponent(hash) + "}";
	}
	
	function init() {
		<%= ServletUtils.JSLogoBlock("logo", request, session) %>
	}
	</script>
	<style type="text/css">
		.massive {
			font-weight: bold;
			font-style: italic;
			text-decoration: underline
		}
		.massiveStep {
			width: 140px;
			font-size: 140%;
			text-align: center;
			text-decoration: none;
			border: 2px inset blue;
			background-color: #D1EEEE
		}
	</style>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
	<title>Welcome to MassIVE</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
	<a href="../index.jsp"><div id="logo"></div></a>
	<!-- Temporary server status text -->
	<!--
	<h4 style="color:red">
		Note: this server is scheduled to be brought down for required
		maintenance on [DATE] at [TIME]. Please contact
		<a href="mailto:ccms@proteomics.ucsd.edu">ccms@proteomics.ucsd.edu</a>
		if you have any questions.
	</h4>
	-->
	<br/>
	<div id="textWrapper" style="text-align: justify;">
		<% if (authRedirect && user == null) { %>
			<div style="color:red; font-size:90%; text-align:center; border:1px dashed red;">
				You must be logged in to submit a MassIVE dataset.
			</div>
		<% } %>
		<h2 style="text-align: center">Welcome to MassIVE</h2>
		<p>
			The <span class="massive">Mass</span> spectrometry
			<span class="massive">I</span>nteractive
			<span class="massive">V</span>irtual
			<span class="massive">E</span>nvironment (MassIVE)
			is a community resource developed by the NIH-funded Center for
			Computational Mass Spectrometry to promote the global, free
			exchange of mass spectrometry data.
		</p>
		<table style="margin: auto; border-spacing: 100px 0">
			<tr>
				<th class="massiveStep">
					<a style="text-decoration:none; color:blue"
						href="../datasets.jsp">
						Browse Data
					</a>
				</th>
				<th class="massiveStep">
					<a style="text-decoration:none; color:blue"
						href="../index.jsp">
						Submit Data
					</a>
				</th>
			</tr>
			<tr>
				<th style="text-align:center;padding-top:5px;">
					<a style="text-decoration:none;color:blue;" target="_blank"
						href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Browsing">
						Help with Browsing
					</a>
				</th>
				<th style="text-align:center;padding-top:5px;">
					<a style="text-decoration:none;color:blue;" target="_blank"
						href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Submission">
						Help with Submission
					</a>
				</th>
			</tr>
		</table>
		<p style="text-align: center">
			<a href="http://www.proteomexchange.org/">
				<img src="PX_logo.png" style="width: 20%; height: auto"/>
			</a><br/>
			MassIVE is now a full member of the ProteomeXchange consortium.
			As such, MassIVE<br/> datasets can be assigned ProteomeXchange IDs
			to satisfy publication requirements.
		</p>
		<hr style="margin-top: 15px"/>
		<h3 id="Browse" style="text-align: center">
			Browsing and Downloading Datasets
		</h3>
		<table id="dataset_search_form" style="margin: auto">
			<tr>
				<td style="font-weight: bold; text-align: right">
					Search by Title:
				</td>
				<td style="padding-left: 10px">
					<input type="text" id="dataset.title" size="30"/>
					<button onclick="filterDatasets('dataset.title');">
						Search
					</button>
				</td>
			</tr>
			<tr>
				<td style="font-weight: bold; text-align: right">
					Search by ID:
				</td>
				<td style="padding-left: 10px">
					<input type="text" id="dataset.id" size="30"/>
					<button onclick="filterDatasets('dataset.id');">
						Search
					</button>
				</td>
			</tr>
		</table>
		<br/>
		<table>
			<tr>
				<th class="massiveStep">
					<a style="text-decoration:none; color:blue" target="_blank"
						href="../datasets.jsp">
						Browse Data
					</a>
				</th>
				<td style="padding-left: 10px">
					All public MassIVE datasets can be browsed on our
					<a href="../datasets.jsp">list page</a>.  To sort this
					list, just click one of the sort arrows at the top of a
					column; to filter, simply type your filter string into the
					boxes at the top of each column and then click "Filter" in
					the upper left of the table.
				</td>
			</tr>
		</table>
		<br/>
		<img src="https://bix-lab.ucsd.edu/download/attachments/17924872/MassIVE_dataset_list.png"
			style="width: 100%"/>
		<p>
			When you wish to view a particular dataset in more detail, simply
			click on the green link in the table to see that dataset's detail
			page.  From here, you can see all the relevant information
			pertaining to the dataset, as well as a link to browse and download
			the dataset's files from our FTP server.
		</p>
		<p>
			For a more detailed explanation of this process, see
			<a href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Browsing"
			target="_blank">here</a>.
		</p>
		<hr/>
		<h3 id="Submit" style="text-align: center">Submitting Datasets</h3>
		<p>
			To submit data to the MassIVE repository, please log in to
			MassIVE using the login box in the upper right corner of this
			page.  MassIVE dataset submission is only available to registered
			users.  If you haven't yet registered an account, just click on
			the "Register" link below the login box - account registration is
			easy and free.
		</p>
		<p>
			Once you're logged in, MassIVE dataset submission consists of two
			simple steps:
		</p>
		<table>
			<tr>
				<th class="massiveStep">
					<a style="text-decoration:none; color:blue" target="_blank"
						href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Submission#MassIVEDatasetSubmission-1UploadDatatoMassIVE">
						Upload Data
					</a>
				</th>
				<td style="padding-left: 10px">
					Connect to the MassIVE FTP server (using your MassIVE
					username and password) to upload and organize your dataset
					files.  Click
					<a href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Submission#MassIVEDatasetSubmission-1UploadDatatoMassIVE"
					target="_blank">here</a> for detailed instructions on how to
					do this.
				</td>
			</tr>
			<tr><td colspan="2" style="padding: 5px"></td></tr>
			<tr>
				<th class="massiveStep">
					<a style="text-decoration: none; color: blue"
						href="../index.jsp">
						Submit Data
					</a>
				</th>
				<td style="padding-left: 10px">
					Once your files are uploaded and organized into the proper
					submission categories, you can then officially submit your
					dataset by running the <a href="../index.jsp">MassIVE
					dataset submission workflow</a>.  Click
					<a href="https://bix-lab.ucsd.edu/display/PS/MassIVE+Dataset+Submission#MassIVEDatasetSubmission-2SubmitDatatoMassIVE"
					target="_blank">here</a> for detailed instructions on how to
					do this.
				</td>
			</tr>
		</table>
		<br/>
		<div style="text-align: center">
			<jsp:include page="/filtered/footer.jsp"/>
		</div>
		<br/>
	</div>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
