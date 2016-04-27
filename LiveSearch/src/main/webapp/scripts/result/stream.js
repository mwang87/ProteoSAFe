/**
 * File stream result view block implementation
 */
// constructor
function ResultViewFileStream(blockXML, id, task) {
	// properties
	this.id = id;
	this.div = null;
	this.index = null;
	this.task = task;
	this.description = task.description;
	this.data = null;
	this.fileType = null;
	this.contentType = null;
    this.panelHeight = "200px";
	// set up the file retrieval
	this.init(blockXML);
}

// initialize block from XML specification
ResultViewFileStream.prototype.init = function(blockXML) {
	if (blockXML == null)
		return;
	// get file type from source element
	var source = blockXML.getElementsByTagName("source");
	if (source != null && source.length > 0)
		source = source[0];
	if (source != null)
		this.fileType = getAttribute(source, "type");
	// get "stream" parser element
	var parser = null;
	var parsers = blockXML.getElementsByTagName("parser");
	if (parsers != null && parsers.length > 0) {
		for (var i=0; i<parsers.length; i++) {
			if (getAttribute(parsers[i], "type") == "stream") {
				parser = parsers[i];
				break;
			}
		}
	}
	if (parser != null) {
		// get content type from "stream" parser element
		this.contentType = getAttribute(parser, "contentType");
        
        if(getAttribute(blockXML, "panelHeight") != null){
            this.panelHeight = getAttribute(blockXML, "panelHeight");
        }
        
		// since the file to be streamed will be retrieved by the "invoke" tool
		// during page load, this block should have its file type set to "file"
		this.fileType = "file";
	}
}

// render the streamed file
ResultViewFileStream.prototype.render = function(div, index) {
	if (div != null)
		this.div = div;
	if (this.div == null) {
		alert("No div was provided under which to render this result block.");
		return;
	}
	if (index != null)
		this.index = index;
	if (this.index == null)
		this.index = 0;
	// if this file is already present, remove it
	var child = getChildById(this.div, this.id);
	if (child != null)
		this.div.removeChild(child);
	// add a new child div for this file
	child = document.createElement("div");
	child.id = this.id;
	spliceChild(this.div, child, this.index);
	// retrieve the file from the server and display it
	var task = this.task.id;
	var fileType = this.fileType;
	var filename = this.data;
	var contentType = this.contentType;
    var panelHeight = this.panelHeight;
	displayFileContents(
		child, task, fileType, filename, null, contentType, panelHeight, this.id);
}

// set data to the file streamer
ResultViewFileStream.prototype.setData = function(data) {
	this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["stream"] = ResultViewFileStream;

/**
 * Result view table column handler for streaming result files
 */
function renderStream(tableId, rowId, columnId, attributes) {
	return function(td, record, index) {
		td.id = getColumnElementId(tableId, record.id, rowId, columnId);
		if (attributes.colspan != null)
			td.colSpan = attributes.colspan;
		// get loader parameters
		var task = attributes.task.id;
		var tool = attributes.invoke;
	    var invokeParameters = null;
	    var parameters = attributes.parameters;
	    if (parameters != null) {
	    	invokeParameters = {};
	    	for (var parameter in parameters)
	    		invokeParameters[parameter] =
	    			resolveFields(parameters[parameter], record);
	    }
	    var contentType = attributes.contentType;
        var panelHeight = this.panelHeight;
	    var block = attributes.blockId;
		// set up on-demand loader function
		var columnLoader = function() {
			removeChildren(td);
			displayFileContents(
				td, task, "invoke", tool,
				invokeParameters, contentType, panelHeight, index, block);
		};
		// if this column is already loaded, just invoke the loader function
		if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
			columnLoader();
		// otherwise, assign the loader function to this record,
		// so that it can be invoked when the column is loaded
		else tableManager.setColumnLoader(
			tableId, record.id, rowId, columnId, columnLoader);
	}
}

var streamColumnHandler = {
	render: renderStream
};

// assign this column handler implementation to column type "stream"
columnHandlers["stream"] = streamColumnHandler;

/**
 * Helper function to stream and render workflow result files
 */
function displayFileContents(
	div, task, type, source, parameters, contentType, panelHeight, block
) {
	if (div == null || task == null || type == null || source == null)
		return;
	// set up URL to download result file
    var url = "DownloadResultFile?task=" + task + "&" + type + "=" + source;
    if (block != null)
    	url += "&block=" + block;
    if (parameters != null){
        for (var parameter in parameters){
            if(parameter == "compoundname"){
                compound_name = parameters[parameter];
                if(compound_name.indexOf("Peptide: ") == 0){
                    peptide_sequence = compound_name.slice(9)
                    console.log("Peptide: " + peptide_sequence);
                    if(peptide_sequence.length < 3){
                        peptide_sequence = "*..*";
                    }
                    url += "&" + "peptide" + "=" + peptide_sequence;
                }
                else{
                    if(parameters["peptide"] == null){
                        url += "&" + "peptide" + "=" + "*..*";
                    }
                }
            }
            else{
                url += "&" + parameter + "=" +
                    encodeURIComponent(parameters[parameter]);
            }
        }
    }
    
    // show "in-progress" download spinner
	removeChildren(div);
    var child = document.createElement("img");
    child.src = "images/inProgress.gif";
	div.appendChild(child);
    
    // create and submit AJAX request for the file data
	var request = createRequest();
	request.open("GET", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			// remove "in-progress" download spinner
			removeChildren(div);
			if (request.status == 200) {
				// display text file content appropriately based on type
			    if (contentType != null && contentType.indexOf("image") >= 0) {
			    	child = document.createElement("img");
			    	child.style.border = "0px";
			    	child.style.margin = "0px";
			    	child.style.padding = "0px";
			    	// servlet should write image content
			    	// as a properly formatted data URL
			    	child.src = "data:" + contentType + ";base64," +
			    		request.responseText;
			    } else if (contentType == "text/plain") {
                    child = document.createElement("div")
                    spectrum_peaks = document.createElement("pre");
                    if (panelHeight == null) {
                        spectrum_peaks.style.height = "600px";
                    } else {
                        spectrum_peaks.style.height = panelHeight;
                    }
                    spectrum_peaks.style.overflow = "auto";
                    spectrum_peaks.style.overflowX = "hidden"
                    spectrum_peaks.textContent = request.responseText;
                    child.appendChild(spectrum_peaks);
			    } else if (contentType == "text/html") {
			    	child = document.createElement("div");
					child.innerHTML = request.responseText;
					injectScripts(request.responseText);
			    } else {
			    	child = document.createElement("div");
					child.innerHTML = request.responseText;
			    }
				div.appendChild(child);
			} else if (request.status == 410) {
				alert("The input file associated with this request could not " +
					"be found. This file was probably deleted by its owner " +
					"after this workflow task completed.");
			} else if (request.status == 0) {
                consoleLog("Navigating away from page");
            } else {
				consoleLog("Could not download result artifact of type \"" +
					type + "\" and value [" + source + "], belonging to " +
					"task [" + task + "]: error = " + request.status + ".");
		    	child = document.createElement("img");
		    	child.src = "images/icons/not_found.png";
		    	child.className = "help";
		    	attachTooltip(child,
		    		"There was an error retrieving or generating this file.");
				div.appendChild(child);
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
	request.send(null);
}
