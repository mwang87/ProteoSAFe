<%@ page language="java"
	contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
%><% {
	/*========================================================================
	 * This page assumes:
	 * 1. The "id" of the <div> to contain the file upload progress elements,
	 *    which is also the workflow task ID, is passed to this page as a
	 *    request parameter with name "task".
	 * 
	 * 2. The purpose of the file uploads whose progress is to be tracked,
	 *    if specified, is passed to this page as a request parameter
	 *    with name "purpose".
	 * 
	 * 3. The header text of the <div> to contain the file upload progress
	 *    elements, if specified, is passed to this page as a request
	 *    parameter with name "legend".
	 *========================================================================*/
	String pendingUploadTask = (String)request.getParameter("task");
	String pendingUploadPurpose = (String)request.getParameter("purpose");
	String pendingUploadLegend = (String)request.getParameter("legend");
	if (pendingUploadTask != null) {
		String pendingUploadDiv = pendingUploadTask;
		if (pendingUploadPurpose != null)
			pendingUploadDiv += pendingUploadPurpose;
		%>
		<div id="<%= pendingUploadDiv %>" class="fieldset flash"
			style="display: none;">
			<% if (pendingUploadLegend != null) { %>
				<span class="legend"><%= pendingUploadLegend %></span>
			<% } %>
		</div>
		<script language="javascript" type="text/javascript">
			var requestTimer<%= pendingUploadDiv %> = null;
			function updateUploads<%= pendingUploadDiv %>() {
				var url =
					"/ProteoSAFe/QueueUploads?task=<%= pendingUploadTask %>";
				<% if (pendingUploadPurpose != null) { %>
					url += "&purpose=<%= pendingUploadPurpose %>";
				<% } %>
				var request = createRequest();
				request.open("GET", url, true);
				request.onreadystatechange = function() {
					if (request.readyState == 4) {
						if (request.status == 200) {
							var response = request.responseText;
							if (response == null || response == "") {
								enableDiv("<%= pendingUploadDiv %>", false);
								return;
							} else enableDiv("<%= pendingUploadDiv %>", true,
								"table-cell");
							// populate pending uploads
							var pendingUploads = eval("(" + response + ")");
							if (pendingUploads.length < 1)
								enableDiv("<%= pendingUploadDiv %>", false);
							else for (var i in pendingUploads) {
								var upload = pendingUploads[i];
								var progress = new FileProgress(upload,
									"<%= pendingUploadDiv %>");
								progress.toggleCancel(false);
								if (upload.percent == null)
									progress.setComplete();
								else if (upload.percent <= 0.0) {
									progress.setStatus("Upload Queued...");
								} else if (upload.percent >= 100.0) {
									progress.setComplete();
									progress.setStatus("Upload Complete.");
								} else {
									progress.setProgress(upload.percent);
									var status = "Uploading (" +
										upload.percent + "% Done)...";
									// only display the elapsed time since the
									// last update if it is large enough to be
									// expressed in units greater than ms
									var elapsed = upload.elapsed;
									if (elapsed != null &&
										!elapsed.match(new RegExp(".*ms"))) {
										progress.setError();
										status += " last updated " +
											upload.elapsed + " ago.";
									}
									progress.setStatus(status);
								}
		 					}
		 					// queue another update in 1 second
		 					requestTimer<%= pendingUploadDiv %> = setTimeout(
		 						"updateUploads<%= pendingUploadDiv %>()", 1000);
						} else {
							// TODO: report error in upload display
							if (requestTimer<%= pendingUploadDiv %> != null)
								clearTimeout(
									requestTimer<%= pendingUploadDiv %>);
						}
					}
				}
				request.setRequestHeader("If-Modified-Since",
					"Sat, 1 Jan 2000 00:00:00 GMT");    
				request.send(null);
			}
			updateUploads<%= pendingUploadDiv %>();
		</script>
	 <% }
} %>
