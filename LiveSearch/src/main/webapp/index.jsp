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
	import="edu.ucsd.livesearch.account.AccountManager"
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
    boolean isAdmin = AccountManager.getInstance().checkRole(user, "administrator");

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

    //Checking if users are required to login
    String requirelogin = request.getParameter("requirelogin");
    boolean redirect_login = false;
    if(requirelogin != null && user == null){
        redirect_login = true;
    }
%>
<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<% ServletUtils.printSessionInfo(out, session); %>
<head>
	<link href="styles/jquery/jquery-ui.css" rel="stylesheet" type="text/css"/>
	<link href="styles/jquery/jquery-ui-ext.css" rel="stylesheet" type="text/css"/>
	<link href="styles/main.css" rel="stylesheet" type="text/css"/>
	<link href="images/favicon.ico" rel="shortcut icon" type="image/icon"/>

	<!-- General ProteoSAFe scripts -->
	<script src="scripts/form.js?1" language="javascript" type="text/javascript"></script>
	<script src="scripts/input.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/resource.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/util.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/uuid.js" language="javascript" type="text/javascript"></script>

	<!-- Third-party utility scripts -->
	<script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/jquery/jquery-ui-1.10.4.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/jquery/selectableScroll.js" language="javascript" type="text/javascript"></script>

	<!-- Third-party widget scripts -->
	<script src="scripts/tooltips/spin.min.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
	<script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>

	<!-- Special script code pertaining exclusively to this page -->
	<script language="javascript" type="text/javascript">
		var task = "<%= taskId %>";
		var workflow = "<%= workflow %>";
		var params = "<%= params %>";
		var email = "<%= email %>";
		var tooltip = createTooltip();

		// function to trigger the initial population of the
		// input form, by clearing the workflow parameter
		function loadInputForm() {
			CCMSFormUtils.clearFieldValue(
				document.forms["mainform"], "workflow");
		}

		function init() {
            <% if(redirect_login){ %>
                login_url = "/ProteoSAFe/user/login.jsp?url=" + encodeURIComponent(document.URL);
                window.location.replace(login_url);
            <% } %>
			// load page logo block
			<%= ServletUtils.JSLogoBlock("logo", request, session) %>
			// try to evaluate request URL parameters, if any were found
			if (params != "") {
				// ensure that a JSON parser is available
				var complete = function() {
					params = JSON.parse(params);
					var protocol = new Array();
					for (var param in params) {
						if (param == "workflow")
							workflow = params[param];
						else protocol.push(
							{name:param, value:params[param]});
					}
					CCMSFormUtils.setProtocol(protocol, "protocol");
					loadInputForm();
				};
				if (JSON != null && JSON.parse != null)
					complete();
				else $.getScript("scripts/json2.js", complete);
			} else loadInputForm();

			if('${livesearch.build}' == 'GNPS'){
                <% if(!isAdmin){ %>
                    $("#workflowselector_label").css("visibility","hidden");
                    $("#workflowselector_select").css("visibility","hidden");
                <% } %>

            }
		}
	</script>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
	<meta http-equiv="PRAGMA" content="NO-CACHE"/>
	<title>CCMS ProteoSAFe Workflow Input Form</title>
</head>
<body onload="init();">

<div id="bodyWrapper">
	<a href="${livesearch.logo.link}"><div id="logo"></div></a>
	<br/>

	<div id="textWrapper">
	<!-- Modal overlay -->
	<div id="overlay" class="overlay"><div id="overlay_spinner"></div></div>

	<!-- Temporary server status text -->
	<!--
	<h4 style="color:red">
		Note: this server is scheduled to be brought down for required
		maintenance on [DATE] at [TIME]. Please contact
		<a href="mailto:ccms@proteomics.ucsd.edu">ccms@proteomics.ucsd.edu</a>
		if you have any questions.
	</h4>
	-->

	<!-- Main workflow input form -->
	<form name="mainform" method="post" action="InvokeTools">
		<!-- Static form content -->
		<table class="mainform">
			<tr><th colspan="5">Workflow Selection</th></tr>
			<tr>
				<!-- workflow parameter -->
				<td style="text-align: right" id="workflowselector_label">
					<div class="help" onmouseover="showTooltip(this, event, 'load:hWorkflow');">Workflow:</div>
				</td>
				<td>
					<select name="workflow" onchange="ProteoSAFeInputUtils.selectWorkflow(this.options[this.selectedIndex].value);" id="workflowselector_select">
					<%
					if (workflows != null)
						for (String name : workflows.keySet()) {
							String label = workflows.get(name);
					%>
				 		<option value="<%= name %>"><%= label %></option>
					<% } %>
					</select>
				</td>

				<!-- protocol parameter -->
				<td style="text-align: right">
				<% if (user != null) { %>
					<div class="help" onmouseover="showTooltip(this, event, 'load:hProtocol');">Search Protocol:</div>
				<% } %>
				</td>
				<td>
				<% if (user != null) { %>
					<select name="protocol" onchange="ProteoSAFeInputUtils.selectProtocol(this.form, this.value);">
						<option value="None">None</option>
						<%
						List<String> protocols = ManageParameters.getProtocols(user);
						if (protocols != null)
							for (String protocol : ManageParameters.getProtocols(user)) { %>
							<option value="<%= protocol %>"><%= protocol %></option>
						<% } %>
					</select>
				<% } %>
				</td>
				<td>
					<input value="Reset Form" type="button" onclick="ProteoSAFeInputUtils.resetInputForm();"/>
					<% if (user != null) { %>
					<input value="Save as Protocol" type="button" onclick="ProteoSAFeInputUtils.saveProtocol(this.form);"/>
					<% } %>
				</td>
			</tr>

			<!-- description parameter -->
			<tr>
				<td style="text-align: right">Title:</td>
				<td colspan="4">
					<input name="desc" type="text" style="width: 80%"/>
				</td>
			</tr>

			<tr><td colspan="5" class="bottomline">&nbsp;</td></tr>
		</table>

		<!-- Dynamically generated form content -->
		<div id="searchinterface"></div>
		<br/>

		<!-- Form submission -->
		<table class="mainform">
			<tr><th colspan="5">Workflow Submission</th></tr>

			<!-- email parameter -->
			<tr>
				<td style="text-align: center">
					<div class="help" onmouseover="showTooltip(this, event, 'load:hEmail');">
						Email me at
					</div>
					&nbsp;
					<input name="email" type="text" style="width: 50%"/>
				</td>
			</tr>

			<!-- submit button -->
			<tr>
				<td class="bottomline" style="text-align: right">
					<span id="submit_spinner">&nbsp;</span>
					<input id="submit_workflow" type="button" value="Submit"
						onclick="ProteoSAFeInputUtils.submitInputForm();"
						disabled/>
				</td>
			</tr>
		</table>
	</form>

	</div>
	<br/>

	<jsp:include page="/filtered/footer.jsp"/>
	<br/>
</div>

<!-- Static help text -->
<div class="helpbox" id="hWorkflow" style="left:-5000px;">
	You can perform a restrictive search with <i><b>InsPecT</b></i>,
	where a collection of pre-specified modifications are permitted,
	or an unrestrictive (or "blind") search, using the
	<i><b>MS-Alignment</b></i> algorithm, to discover unanticipated
	modifications and point mutations.<br/><br/>
	<i><b>PepNovo</b></i> serves as a high throughput de novo peptide
	sequencing tool for tandem mass spectrometry data.
</div>
<div class="helpbox" id="hProtocol" style="left:-5000px;">
	Search protocols are saved workflow parameter sets. You can save a new
	protocol by filling out this form, and then selecting "Save as Protocol"
	below, which will record the contents of the form and save it to your
	list of protocols. Then, selecting a saved protocol from the list to the
	right will automatically populate this form with all of the values stored
	in that protocol. All parameters except input files and task description
	are included when a protocol is saved.
</div>
<div class="helpbox" id="hEmail" style="left:-5000px;">
	When your task is done, you will get a notification via the email
	address you specify here.
</div>

<div class="helpbox" id="hSpectrumFile" style="left:-5000px;">
	Accepted formats: mzXML (preferred), dta, pkl, and mgf.
	Archived files (zip, gz, bz2, tar.gz, tar.bz2) are supported too; you can put multiple
	spectrum files of an experiment in a single archive.
</div>
<div class="helpbox" id="hInstrument" style="left:-5000px;">
	The type of mass spectrometer used to generate the experimental spectra. <br/>
	<i>ESI-ION-TRAP</i> (default) - InsPecT attempts to correct the parent mass.<br/>
	<i>QTOF</i> - InsPecT uses a fragmentation model trained on QTOF data. (QTOF
	data typically features a stronger y ladder and weaker b ladder than
	other spectra). <br/>
	<i>High accuracy LTQ</i> - an FT-LTQ or an orbitrap.
</div>
<div class="helpbox" id="hFragmentation" style="left:-5000px;">
	The fragmentation method used.
</div>
<div class="helpbox" id="hCysteine" style="left:-5000px;">
	<div class="helpbox-main">The chemical modification used to treat the cysteine residues
		in the peptide (typically a +57 Carbamidomethylation is used).</div>
	<div class="helpbox-bottom">&nbsp;</div>
</div>
<div class="helpbox" id="hProtease" style="left:-5000px;">
	Specifies the name of a protease. <i>"Trypsin"</i>,
	<i>"None"</i>, and <i>"Chymotrypsin"</i> are the available values. If tryptic digest
	is specified, then matches with non-tryptic termini are penalized.
</div>
<div class="helpbox" id="hC13" style="left:-5000px;">
	Number of allowed C13.
</div>
<div class="helpbox" id="hNnet" style="left:-5000px;">
	Number of allowed non-enzymatic termini.
</div>
<div class="helpbox" id="hParentMass" style="left:-5000px;">
	Specify the parent mass tolerance, in Daltons. Default value
	is 2 <i>Da</i>. Note that secondary ions are often selected for fragmentation, so parent mass
	errors near 1.0 <i>Da</i> or -1.0 <i>Da</i> are not uncommon in typical datasets, even on FT machines;
	therefore, the program examines &plusmn; <i>Da</i> shifts even if a low precursor tolerance is selected.
	If you are sure there are no Dalton shifts, you should check the box "use spectrum
	precursor m/z".
</div>
<div class="helpbox" id="hIonTolerance" style="left:-5000px;">
	Specify how far <i>b</i> and <i>y</i> peaks can be shifted from their expected masses.
	Default is 0.5.
</div>
<div class="helpbox" id="hMods" style="left:-5000px;">
	Suggested values here are 1 and 2.
</div>
<div class="helpbox" id="hDatabase" style="left:-5000px;">
	Select a database to search. As databases are updated
	regularly, the timestamp of the last modification is specified in paranthesis.
	For faster searches InsPecT uses internally the (.trie) file format.
</div>
<div class="helpbox" id="hContaminant" style="left:-5000px;">
	Searches a small database of common protein contaminants of proteomics searches:
	trypsin (TRYP_PIG, TRYP_BOVIN) and keratin (K22E_HUMAN, K22O_HUMAN, K2C1_HUMAN,
	K2C3_HUMAN, K2C7_HUMAN, K1C1_HUMAN).
</div>
<div class="helpbox" id="hAdditionalSeq" style="left:-5000px;">
	Specify the name of a FASTA-format protein database to search.
</div>
<div class="helpbox" id="hFDR" style="left:-5000px;">
	Spectrum-Level False Discovery Rate
</div>
<div class="helpbox" id="hFPR" style="left:-5000px;">
	False Positive Rate<br/>
	See the
	<a style="color: blue;"
		href="http://proteomics.ucsd.edu/Software/MSGeneratingFunction.html">
		MS-GF documentation
	</a>
	for more information.
</div>

<div class="helpbox" id="spectrum_gnps" style="left:-5000px;">
    Accepted formats: 32 bit uncompressed mzXML (preferred) and mgf.
</div>

<div class="helpbox" id="library_gnps" style="left:-5000px;">
    Pick a single spectral library from the speclibs folder. If you do not have access email miw023@cs.ucsd.edu for access.
</div>

<div class="helpbox" id="min_pair_cosine_gnps" style="left:-5000px;">
    Minimum cosine score for consideration in a network.
</div>

<div class="helpbox" id="min_matched_peaks_gnps" style="left:-5000px;">
    Minimum peaks matching between two spectra to be in consideration for network.
</div>

<div class="helpbox" id="network_topk_gnps" style="left:-5000px;">
    Number of neighbors to retain in the network.
</div>

<div class="helpbox" id="min_cluster_size_gnps" style="left:-5000px;">
    Minimum of spectra to be in a cluster for cluster be considered in network. Requires clustering to be turned on.
</div>

<div class="helpbox" id="max_component_size_gnps" style="left:-5000px;">
    Maximum size of connected component to allow in network. Larger networks will be broken by increasing cosine threshold on that particular component and not globally.
</div>

<div class="helpbox" id="group_mapping_gnps" style="left:-5000px;">
    Input text file organizing input files into groups.
</div>

<div class="helpbox" id="attribute_mapping_gnps" style="left:-5000px;">
    Input text file organizing groups into attributes. These attributes are columns in the output.
</div>

<div class="helpbox" id="min_cosine_library_gnps" style="left:-5000px;">
    Minimum cosine score for consideration in library search.
</div>

<div class="helpbox" id="min_matched_peaks_library_gnps" style="left:-5000px;">
    Minimum peaks matching between two spectra to be in consideration for library search.
</div>

<div class="helpbox" id="filterstd_gnps" style="left:-5000px;">
    For each spectrum the 25% least intense peaks are collected and the std-dev is calculated as well as the mean. A minimum peak intensity is calculated as mean + <i>k</i> * std-dev where <i>k</i> is user selectable. All peaks below this threshold are deleted.
</div>

<div class="helpbox" id="filterint_gnps" style="left:-5000px;">
    All peaks below this raw intensity value are deleted.
</div>

<div class="helpbox" id="filterprecursor_gnps" style="left:-5000px;">
    All peaks +/- 17Da around precursor mass are deleted.
</div>

<div class="helpbox" id="filterlibrary_gnps" style="left:-5000px;">
    Apply all of these filters to library spectra as well before searching.
</div>

<div class="helpbox" id="filterpeakwindow_gnps" style="left:-5000px;">
    For each peak in spectrum to be kept, it must be at least 6th most intense peak in a window +/- 50Th around its m/z.
</div>

<div class="helpbox" id="library_quality_gnps" style="left:-5000px;">
    The quality of the library spectrum source in addition to the trust of the annotation.
</div>

<div class="helpbox" id="ion_source_gnps" style="left:-5000px;">
    Source of the ions.
</div>

<div class="helpbox" id="instrument_gnps" style="left:-5000px;">
    Type of instrument to fragment and acquire the spectra.
</div>

<div class="helpbox" id="ionmode_gnps" style="left:-5000px;">
    Ionization mode, positive or negative.
</div>

<div class="helpbox" id="acquisition_gnps" style="left:-5000px;">
    Original source of the compound.
</div>

<div class="helpbox" id="pi_gnps" style="left:-5000px;">
    Principle Investigator of the new spectrum.
</div>

<div class="helpbox" id="datacollector_gnps" style="left:-5000px;">
    Data Collector of spectrum.
</div>

<!-- Dereplicator -->
<div class="helpbox" id="hPrecursorIonTolerance" style="left:-5000px;">
	Specify the precursor ion tolerance, in Daltons. The value will be used only if
	Running Mode is set to 'custom'. Otherwise, default value for 'low' mode is 0.5 Da,
	for 'high' mode is 0.02 Da.
</div>
<div class="helpbox" id="hProductIonTolerance" style="left:-5000px;">
	Specify the product ion tolerance, in Daltons. The value will be used only if
	Running Mode is set to 'custom'. Otherwise, default value for 'low' mode is 0.5 Da,
	for 'high' mode is 0.02 Da.
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
