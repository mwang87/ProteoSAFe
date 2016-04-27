/**
 * This file contains utility scripts used to build, populate, update and
 * submit the main ProteoSAFe workflow input form.
 */

// static workflow input form script utility package
var ProteoSAFeInputUtils = {};

/******************************************************************************
 * Form builder functions
 ******************************************************************************/

/**
 * Object to keep track of all asynchronously loaded parameters
 * and modules in the ProteoSAFe workflow input form.
 */
ProteoSAFeInputUtils.asynchronousElements = function() {
	this.elements = new Array();
	this.populated = false;
}

ProteoSAFeInputUtils.asynchronousElements.prototype.addElement = function(
	element
) {
	if (element == null)
		return;
	else this.elements.push({
		name: element.name,
		element: element,
		loaded: false
	});
}

ProteoSAFeInputUtils.asynchronousElements.prototype.setElementLoaded =
function(element) {
	if (element == null)
		return;
	for (var i=0; i<this.elements.length; i++) {
		if (this.elements[i].name == element) {
			this.elements[i].loaded = true;
			return;
		}
	}
}

ProteoSAFeInputUtils.asynchronousElements.prototype.setElementsPopulated =
function() {
	this.populated = true;
}

ProteoSAFeInputUtils.asynchronousElements.prototype.isLoaded = function() {
	// the set of asynchronous parameters cannot be fully
	// loaded if it isn't even fully populated yet
	if (this.populated == false)
		return false;
	// look for an unloaded parameter
	for (var i=0; i<this.elements.length; i++) {
		if (this.elements[i].loaded == false)
			return false;
	}
	// if no unloaded parameter could be found,
	// then all parameters have been loaded
	return true;
}

ProteoSAFeInputUtils.asynchronousElements.prototype.dump = function() {
	var report = "This input form is ";
	if (this.populated == false)
		report += "NOT ";
	report += "fully populated.\nAsynchronous form elements:";
	for (var i=0; i<this.elements.length; i++) {
		var element = this.elements[i];
		report += "\n\t" + i + ". Name = \"" + element.name +
			"\", loaded = " + element.loaded;
	}
	return report;
}

/**
 * Gets the asynchronous element tracking object for the specified form
 */
ProteoSAFeInputUtils.getAsynchronousElementTracker = function(div) {
	if (div == null)
		return null;
	var form = CCMSFormUtils.getEnclosingForm(div);
	if (form == null)
		return null;
	var record = CCMSFormUtils.getFormRecord(form.name);
	if (record == null)
		return null;
	else return record.asynchronousElements;
}

/**
 * Sets an element as loaded within the given form div, and then polls the
 * form's asyonchronous element loading queue to determine if all such
 * elements are now loaded.
 */
ProteoSAFeInputUtils.setAsynchronousElementLoaded = function(div, element) {
	if (div == null || element == null)
		return;
	// get this form's asynchronous element tracker
	var tracker = this.getAsynchronousElementTracker(div);
	if (tracker == null) {
		// TODO: report error, fail
		return;
	}
	// notify the tracker that the element has loaded
	tracker.setElementLoaded(element);
	// if the tracker's queue is fully loaded, then activate the form
	this.pollAsynchronousElementQueue(div);
}

/**
 * Checks the loading status of the given form's asynchronous elements,
 * and if all are loaded, enables the form.
 */
ProteoSAFeInputUtils.pollAsynchronousElementQueue = function(div) {
	if (div == null)
		return;
	// get this form's asynchronous element tracker
	var tracker = this.getAsynchronousElementTracker(div);
	if (tracker == null) {
		// TODO: report error, fail
		return;
	}
	// if the tracker's queue is fully loaded, then activate the form
	if (tracker.isLoaded()) {
		this.populateInputForm();
		enableOverlay(document.getElementById("overlay"), false);
		document.getElementById("submit_workflow").disabled = false;
	}
}

/**
 * Retrieves a workflow's input specification, and if found, submits the
 * document for building and pre-populating the workflow input form.
 */
ProteoSAFeInputUtils.selectWorkflow = function(workflow) {
	if (workflow == null)
		return;
	// retrieve the selected workflow's input specification
	var request = createRequest();
    var url = "DownloadWorkflowInterface?workflow=" +
    	encodeURIComponent(workflow) + "&type=input";
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
			if (request.status == 200) {
				// capture current form contents
				var previous =
					CCMSFormUtils.getProtocol("previous", "mainform");
				var current =
					CCMSFormUtils.captureForm(document.forms["mainform"]);
				if (previous == null)
					previous = current;
				else CCMSFormUtils.mergeProtocols(previous, current);
				// remove all protocol elements whose values match this
				// workflow's defaults from the "previous" protocol, to ensure
				// that only user-altered parameter values are preserved
				var defaults =
					CCMSFormUtils.getProtocol("defaults", "mainform");
				if (defaults != null) {
					// traverse the protocol array in reverse order, to ensure
					// that the array is pruned without skipping any elements
					for (var i=previous.length-1; i>=0; i--) {
						var defaultElement = CCMSFormUtils.getProtocolElement(
							defaults, previous[i].name);
						if (defaultElement != null &&
							defaultElement.value == previous[i].value)
							previous.splice(i, 1);
					}
				}
				CCMSFormUtils.setProtocol(previous, "previous", "mainform");
				// build the input form
				ProteoSAFeInputUtils.buildInputForm(request.responseXML);
			} else if (request.status == 404) {
				ProteoSAFeInputUtils.displayWorkflowHelp();
        	} else alert("There was a problem retrieving the " +
				"interface specification for workflow \"" + workflow + "\".");
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
    request.send(null);
}

/**
 * Parses a workflow's input specification, and then builds the workflow
 * input form based on the information contained therein.
 */
ProteoSAFeInputUtils.buildInputForm = function(inputXML) {
	if (inputXML == null)
		return;
	// fetch dynamic input form div
	var content = document.getElementById("searchinterface");
	if (content == null)
		return;
	// fetch parent form object
	var form = CCMSFormUtils.getEnclosingForm(content);
	if (form == null)
		return;
	// if a task is being cloned, fetch its protocol
	// and let it trigger a form rebuild
	if (task != null && task != "") {
		this.loadProtocol(form, "task", task);
		return;
	}
	// or, if a specific workflow was requested, then load it
	else if (workflow != null && workflow != "") {
		var newWorkflow = workflow;
		workflow = null;	// necessary to prevent infinite loop
		CCMSFormUtils.setFieldValue(form, "workflow", newWorkflow);
		return;
	}
	// otherwise, get this form's record
	var record = CCMSFormUtils.getFormRecord(form.name);
	if (record == null) {
		record = {form: form};
		CCMSFormUtils.setFormRecord(record, form.name);
	}
	// reset this form's record
	record.modules = {};
	record.asynchronousElements =
		new ProteoSAFeInputUtils.asynchronousElements();
	var tracker = record.asynchronousElements;
	// clear out input form
	removeChildren(content);
	// disable form until all inputs have been built and initialized
	document.getElementById("submit_workflow").disabled = true;
	enableOverlay(document.getElementById("overlay"), true, true);
	// collect parameter data
	var xmlParams = inputXML.getElementsByTagName("parameter");
	var parameters = {};
	for (var i=0; i<xmlParams.length; i++) {
		var xmlParam = xmlParams[i];
		var parameter = {};
		// get parameter attributes
		parameter.name = getAttribute(xmlParam, "name");
		parameter.label = getAttribute(xmlParam, "label");
		// read parameter's options, if any
		var options = xmlParam.getElementsByTagName("options");
		if (options != null && options.length > 0) {
			options = options[0];
			// if the options are tied to a system resource,
			// queue a fetch of the resource for this parameter
			var resource = getAttribute(options, "resource");
			if (resource != null)
				parameter.options = {"_RESOURCE": resource};
			// if the options are tied to a web service query,
			// queue a call to that service for this parameter
			var service = getAttribute(options, "service");
			if (service != null)
				parameter.options = {"_SERVICE": service};
			// otherwise, simply read the options specified in input.xml
			else {
				options = options.getElementsByTagName("option");
				if (options != null && options.length > 0) {
					parameter.options = {};
					for (var j=0; j<options.length; j++) {
						var option = options[j];
						var value = getAttribute(option, "value");
						var label = getAttribute(option, "label");
						if (label == null)
							label = value;
						parameter.options[value] = label;
					}
				}
			}
		}
		// read parameter's default value, if any
		var defaultValue = xmlParam.getElementsByTagName("default");
		if (defaultValue != null && defaultValue.length > 0) {
			defaultValue = defaultValue[0];
			parameter.defaultValue = getAttribute(defaultValue, "value");
		}
		// collect parameter
		parameters[parameter.name] = parameter;
	}
	// store all specified parameter defaults as a form protocol
	var defaults = new Array();
	for (var name in parameters) {
		var parameter = parameters[name];
		if (parameter.defaultValue != null)
			defaults.push({name: name, value: parameter.defaultValue});
	}
	// set email address as a special default
	if (email != null && email != "")
		CCMSFormUtils.addProtocolElement(
			defaults, {name: "email", value: email}, true);
	// save parameter defaults protocol
	if (defaults.length > 0)
		CCMSFormUtils.setProtocol(defaults, "defaults", form.name);
	else CCMSFormUtils.setProtocol(null, "defaults", form.name);
	// read input form layout specification, build form according to layout;
	// render labels immediately, but just queue up the rendering 
	// of all inputs/modules, in case they are asynchronous
	var blocks = inputXML.getElementsByTagName("block");
	for (var i=0; i<blocks.length; i++) {
		var block = blocks[i];
		var label = getAttribute(block, "label");
		if (label == null) {
			// TODO: report error, fail
		}
		// separate this block from the one above with a line break
		content.appendChild(document.createElement("br"));
		// create block table
		var table = document.createElement("table");
		table.className = "mainform";
		// add header row
		var tr = document.createElement("tr");
		var td = document.createElement("th");
		td.colSpan = "10";
		td.appendChild(document.createTextNode(label));
		tr.appendChild(td);
		table.appendChild(tr);
        
        //Adding show hide button
        if(label.indexOf("Advanced") != -1){
            var show_hide = document.createElement("button");
            show_hide.id = label.replace(/ /g, '') + "_showhide";
            show_hide.style.float = "right"
            show_hide.innerHTML = "Show Fields";
            show_hide.type = "button"
            
            show_hide.onclick = generate_show_input_callback("." + label.replace(/ /g, '') + "ROW", show_hide);
            td.appendChild(show_hide);
        }
        
        
		// add footer row
		tr = document.createElement("tr");
		td = document.createElement("td");
		td.className = "bottomline";
		td.colSpan = "10";
		td.innerHTML = "&nbsp;";
		tr.appendChild(td);
        tr.className = label.replace(/ /g, '') + "ROW";
		table.appendChild(tr);
		// add this block to the form
		content.appendChild(table);
		// add parameter inputs
		var rows = block.getElementsByTagName("row");
		for (var j=0; j<rows.length; j++) {
			var row = rows[j];
			tr = document.createElement("tr");
            tr.className = label.replace(/ /g, '') + "ROW";
			spliceChild(table, tr, (j+1));
			var cells = row.getElementsByTagName("cell");
			for (var k=0; k<cells.length; k++) {
				var cell = cells[k];
				td = document.createElement("td");
				tr.appendChild(td);
				// get generic styling for this cell
				var style = getAttribute(cell, "style");
				if (style != null)
					td.style.cssText = style;
				// if not already specified, set default vertical-align to "top"
				if (td.style.verticalAlign == null ||
					td.style.verticalAlign == "")
					td.style.verticalAlign = "top";
				// set this cell's "colspan"
				var colspan = getAttribute(cell, "colspan");
				if (colspan != null)
					td.colSpan = colspan;
				// process "align" attribute for backwards compatibility
				var align = getAttribute(cell, "align");
				if (align != null)
					td.style.textAlign = align;
				// add inputs and labels for this cell
				var items = cell.childNodes;
				var count = 0;
				for (var n=0; n<items.length; n++) {
					// prepend a space if this is not the first item
					if (count > 0)
						td.appendChild(document.createTextNode(" "));
					// add the item
					var item = items[n];
					if (item.nodeName == "label") {
						ProteoSAFeInputUtils.populateLabel(
							td, item, parameters);
						// if the first content item in a cell is a label,
						// then by convention it should be aligned right
						if (count == 0) {
							// but don't override manually specified alignment
							if (td.style.textAlign == null ||
								td.style.textAlign == "")
								td.style.textAlign = "right";
							// if this is a standard parameter prefix
							// label, append the requisite colon
							var prefix = getAttribute(item, "prefix");
							if (prefix != "false" && prefix != "no")
								td.appendChild(document.createTextNode(":"));
						}
						count++;
					} else if (item.nodeName == "input") {
						ProteoSAFeInputUtils.queueInput(td, item, parameters);
						count++;
					} else if (item.nodeName == "module") {
						ProteoSAFeInputUtils.queueModule(td, item, parameters);
						count++;
					}
				}
			}
		}
		
		//Hiding rows
        if(label.indexOf("Advanced") != -1){
            $("." + label.replace(/ /g, '') + "ROW").hide();
        }
	}
	// indicate that all asynchronous form elements have been populated
	tracker.setElementsPopulated();
	// now that all parameters have been queued, load them
	var loadElement = function(element) {
		if (element == null) {
			// TODO: report error, fail
		}
		else if (element.node.nodeName == "input")
			ProteoSAFeInputUtils.populateInput(
				element.div, element.node, element.parameters);
		else if (element.node.nodeName == "module")
			ProteoSAFeInputUtils.populateModule(
				element.div, element.node, element.parameters);
		else {
			// TODO: report error, fail
		}
	};
	for (var i=0; i<tracker.elements.length; i++)
		loadElement(tracker.elements[i].element);
	// if the tracker's queue is fully loaded, then activate the form
	this.pollAsynchronousElementQueue(form);
	// set a timeout to wait for elements to load
	setTimeout(function() {
		// if the tracker is not yet loaded, report
		// a timeout error and clear the form
		if (tracker.isLoaded() == false) {
			alert("There was a problem loading the elements of this form:\n" +
				tracker.dump());
			removeChildren(document.getElementById("searchinterface"));
			enableOverlay(document.getElementById("overlay"), false);
		}
	}, 20000);
}

/**
 * Populates the provided HTML element with the content of a single label node
 */
ProteoSAFeInputUtils.populateLabel = function(div, labelNode, parameters) {
	if (div == null || labelNode == null)
		return;
	// add label content
	var content = labelNode.getElementsByTagName("content");
	if (content == null || content.length < 1) {
		// TODO: report error, fail
		return;
	}
	content = content[0];
	var label = document.createElement("span");
	// if this content element is tied to a parameter, use its label
	var parameterName = getAttribute(content, "parameter");
	if (parameterName != null) {
		if (parameters == null) {
			// TODO: report error, fail
			return;
		}
		var parameter = parameters[parameterName];
		if (parameter == null || parameter.label == null) {
			// TODO: report error, fail
			return;
		} else label.appendChild(document.createTextNode(parameter.label));
	}
	// otherwise extract the text and CDATA content of this content element
	else {
		var text = "";
		var count = 0;
		for (var i=0; i<content.childNodes.length; i++) {
			var child = content.childNodes[i];
			// only add content from text and CDATA sections
			if (child.nodeType == 3 || child.nodeType == 4) {
				// prepend a space if this is not the first item
				if (count > 0)
					text += " ";
				text += child.nodeValue;
				count++;
			}
		}
		label.innerHTML = text;
	}
	// add label tooltip, if any
	var tooltip = labelNode.getElementsByTagName("tooltip");
	if (tooltip != null && tooltip.length > 0) {
		tooltip = tooltip[0];
		var help = getAttribute(tooltip, "id");
		if (help != null) {
			// check to see if a helpbox with this ID is already present
			var helpbox = document.getElementById(help);
			// if not, then the help div needs to be created dynamically
			// from the text and/or CDATA content of this label node
			if (helpbox == null) {
				helpbox = document.createElement("div");
				helpbox.id = help;
				helpbox.className = "helpbox";
				helpbox.style.left = "-5000px";
				for (var i=0; i<tooltip.childNodes.length; i++) {
					var child = tooltip.childNodes[i];
					// add content from text and CDATA sections
					if (child.nodeType == 3 || child.nodeType == 4)
						helpbox.innerHTML += child.nodeValue + " ";
				}
				document.body.appendChild(helpbox);
			}
			// attach the helpbox content to this label div
			label.className = "help";
			label.onmouseover = function(event) {
				showTooltip(this, event, "load:" + help);
			};
		}
	}
	div.appendChild(label);
}

/**
 * Queues the population/rendering of the specified HTML form element
 */
ProteoSAFeInputUtils.queueInput = function(div, inputNode, parameters) {
	if (div == null || inputNode == null || parameters == null)
		return;
	// get required attributes
	var parameter = getAttribute(inputNode, "parameter");
	var type = getAttribute(inputNode, "type");
	if (parameter == null || type == null) {
		// TODO: report error, fail
		return;
	}
	// if this is a synchronous element, don't bother queuing; just render now
	if (type != "select") {
		ProteoSAFeInputUtils.populateInput(div, inputNode, parameters);
		return;
	} else {
		var details = parameters[parameter];
		if (details == null || details.options == null ||
			details.options["_RESOURCE"] == null) {
			ProteoSAFeInputUtils.populateInput(div, inputNode, parameters);
			return;
		}
	}
	// get asynchronous element tracker, add element
	var tracker =
		ProteoSAFeInputUtils.getAsynchronousElementTracker(div);
	if (tracker == null) {
		// TODO: report error, fail
		return;
	} else tracker.addElement({
		name: parameter,
		div: div,
		node: inputNode,
		parameters: parameters
	});
}

/**
 * Queues the population/rendering of the specified input form module
 */
ProteoSAFeInputUtils.queueModule = function(div, moduleNode, parameters) {
	if (div == null || moduleNode == null)
		return;
	// get required attributes
	var id = getAttribute(moduleNode, "id");
	if (id == null) {
		// TODO: report error, fail
		return;
	}
	// get asynchronous element tracker, add element
	var tracker =
		ProteoSAFeInputUtils.getAsynchronousElementTracker(div);
	if (tracker == null) {
		// TODO: report error, fail
		return;
	} else tracker.addElement({
		name: id,
		div: div,
		node: moduleNode,
		parameters: parameters
	});
}

/**
 * Populates the provided HTML input with the content of a single input node
 */
ProteoSAFeInputUtils.populateInput = function(
	div, inputNode, parameters, callback
) {
	if (div == null || inputNode == null || parameters == null)
		return;
	// get required attributes
	var parameter = getAttribute(inputNode, "parameter");
	var type = getAttribute(inputNode, "type");
	if (parameter == null || type == null) {
		// TODO: report error, fail
		return;
	}
	var details = parameters[parameter];
	var asynchronous = false;
	var input;
	if (type == "select") {
		input = document.createElement("select");
		// populate the select input's options
		if (details.options == null) {
			// TODO: report error, fail
		}
		// if the options are dynamic, fetch them
		else if (details.options["_RESOURCE"] != null) {
			// for resource-driven drop-down lists,
			// there should always be a "None" option
			var defaultOption = document.createElement("option");
			defaultOption.value = "None";
			defaultOption.appendChild(document.createTextNode("None"));
			input.appendChild(defaultOption);
			// fetch the dynamic parameters
			asynchronous = true;
			ProteoSAFeInputUtils.downloadResourceMap(
				details.options["_RESOURCE"],
				function(map) {
					for (var value in map) {
						var option = document.createElement("option");
						option.value = value;
						option.innerHTML = map[value];
						input.appendChild(option);
					}
					ProteoSAFeInputUtils.setAsynchronousElementLoaded(
						div, parameter);
					if (callback != null)
						callback();
				}
			);
		}
		// otherwise write them immediately
		else for (var value in details.options) {
			var option = document.createElement("option");
			option.value = value;
			option.innerHTML = details.options[value];
			input.appendChild(option);
		}
	} else if (type == "textarea") {
		input = document.createElement("textarea");
	} else {
		input = document.createElement("input");
		input.type = type;
	}
	// assign basic attributes
	input.name = parameter;
	var value = getAttribute(inputNode, "value");
	if (value != null)
		input.value = value;
	// assign optional attributes
	var attributes = inputNode.getElementsByTagName("attribute");
	if (attributes != null && attributes.length > 0) {
		for (var i=0; i<attributes.length; i++) {
			var attribute = attributes[i];
			var name = getAttribute(attribute, "name");
			var value = getAttribute(attribute, "value");
			if (name == null || value == null) {
				// TODO: report error, fail
			}
			input[name] = value;
		}
	}
	div.appendChild(input);
	// if this input is synchronous, then call the callback now
	if (asynchronous == false) {
		ProteoSAFeInputUtils.setAsynchronousElementLoaded(
			div, parameter);
		if (callback != null)
			callback();
	}
}

/**
 * Populates the provided HTML element with the content of a single module node
 */
ProteoSAFeInputUtils.populateModule = function(
	div, moduleNode, parameters, callback
) {
	if (div == null || moduleNode == null)
		return;
	// get required attributes
	var id = getAttribute(moduleNode, "id");
	var type = getAttribute(moduleNode, "type");
	if (id == null || type == null) {
		// TODO: report error, fail
		return;
	}
	// get properties
	var properties = {};
	var propertyNodes = moduleNode.getElementsByTagName("property");
	if (propertyNodes != null && propertyNodes.length > 0) {
		for (var i=0; i<propertyNodes.length; i++) {
			var property = propertyNodes[i];
			var name = getAttribute(property, "name");
			var value = property.textContent;
			if (name == null || value == null) {
				// TODO: report error, fail
			}
			properties[name] = value;
		}
	} else properties = null;
	// fetch module data, populate div with it
	CCMSFormUtils.loadModule(
		type, "input", div, id, properties, parameters, callback);
}

/**
 * Pre-populates the ProteoSAFe workflow input form
 * with any stored parameter protocols.
 */
ProteoSAFeInputUtils.populateInputForm = function() {
	// get the main workflow input form
	var form = document.forms["mainform"];
	if (form == null) {
		// TODO: report error, fail
		return;
	}
	// first copy the workflow's parameter defaults into the values array
	var values = new Array();
	var protocol = CCMSFormUtils.getProtocol("defaults", form.name);
	if (protocol != null) {
		for (var i=0; i<protocol.length; i++)
			values.push(protocol[i]);
	}
	// then, copy any task or protocol state into the values array
	protocol = CCMSFormUtils.getProtocol("protocol", form.name);
	if (protocol != null)
		CCMSFormUtils.mergeProtocols(values, protocol);
	// finally, copy any previous user-specified values into the values array
	protocol = CCMSFormUtils.getProtocol("previous", form.name);
	if (protocol != null)
		CCMSFormUtils.mergeProtocols(values, protocol);
	// be sure to remove the "workflow" parameter, since it has already been set
	var index = -1;
	for (var i=0; i<values.length; i++) {
		if (values[i].name.toUpperCase() == "WORKFLOW") {
			index = i;
			break;
		}
	}
	if (index >= 0)
		values.splice(index, 1);
	// when the values array has been fully populated,
	// use it to populate the form
	CCMSFormUtils.populateForm(form, values);
}

/**
 * Resets the ProteoSAFe workflow input form, i.e. clears it
 * and then populates it with any stored parameter protocols.
 */
ProteoSAFeInputUtils.resetInputForm = function() {
	// get the main workflow input form
	var form = document.forms["mainform"];
	if (form == null) {
		// TODO: report error, fail
		return;
	}
	// remove the "previous" protocol, since the form is being reverted to
	// its default state (i.e. only defaults and loaded protocols matter)
	CCMSFormUtils.setProtocol(null, "previous", form.name);
	// clear the input form, except for the workflow and protocol parameters
	CCMSFormUtils.clearForm(form, ["workflow", "protocol"]);
	// re-populate the form
	this.populateInputForm();
}

/**
 * Serializes and submits the ProteoSAFe workflow input form.
 */
ProteoSAFeInputUtils.submitInputForm = function() {
	// get the main workflow input form
	var form = document.forms["mainform"];
	if (form == null) {
		// TODO: report error, fail
		return;
	}
	// prevent multiple submissions
	if (form.submitted)
		return;
	else this.enableInputForm(false);
	// generate request parameters
	var parameters = CCMSFormUtils.serializeForm(form);
	parameters += "&uuid=" + new UUID();
	// generate and submit request
	var url = "/ProteoSAFe/InvokeTools";
	var request = createRequest();
	request.open("POST", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				var taskIdRegExp = /^[0-9a-f]{32}$/;
				var message = request.responseText;
				// if the task creation was successful, then the servlet
				// should write the task ID as its text output
				if (taskIdRegExp.test(message))
					window.location = "/ProteoSAFe/status.jsp?task=" + message;
				// otherwise, it should write the
				// validation failures to its output
				else {
					alert(message);
					ProteoSAFeInputUtils.enableInputForm(true);
				}
			} else {
				alert("Task creation failed due to an unknown error.");
				ProteoSAFeInputUtils.enableInputForm(true);
			}
		}
	}
	// set the proper header information for a POST request
	request.setRequestHeader("Content-type",
		"application/x-www-form-urlencoded");
	request.setRequestHeader("Content-length", parameters.length);
	request.setRequestHeader("Connection", "close");
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(parameters);
}

/**
 * Sets the availability of the ProteoSAFe workflow input form.  When the
 * form is disabled, it should be flagged as such to prevent further
 * submissions, the "Submit" button should be grayed out and unclickable, and
 * a spinner should appear to indicate that the server is processing.
 */
ProteoSAFeInputUtils.enableInputForm = function(enable) {
	// get the main workflow input form, submit button and spinner elements
	var form = document.forms["mainform"];
	var submitButton = document.getElementById("submit_workflow");
	var spinner = document.getElementById("submit_spinner");
	// set the availability of the form properly
	if (enable) {
		form.submitted = false;
		submitButton.disabled = false;
		removeChildren(spinner);
	} else {
		form.submitted = true;
		submitButton.disabled = true;
		var img = document.createElement("img");
		img.src = "images/inProgress.gif";
		img.height = img.width = "16";
		spinner.appendChild(img);
	}
}

/**
 * Downloads the name/label mapping of a specified ProteoSAFe system resource,
 * and parses the downloaded XML document into a Javascript hash, which is
 * then submitted to the provided callback function.
 */
ProteoSAFeInputUtils.downloadResourceMap = function(resource, callback) {
	if (resource == null || callback == null)
		return null;
	// retrieve the specified resource mapping
	var request = createRequest();
    var url = "DownloadResourceMapping?resource=" +
    	encodeURIComponent(resource);
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
			if (request.status == 200) {
				// parse out resources into a Javascript hash
				var map = {};
				var items = request.responseXML.getElementsByTagName("item");
				for (var i=0; i<items.length; i++)
					map[getAttribute(items[i], "name")] =
						items[i].firstChild.nodeValue;
				// invoke callback
				callback(map);
			} else alert("There was a problem retrieving the " +
				"mapping for resource \"" + resource + "\".");
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
    request.send(null);
}

/******************************************************************************
 * Form state functions
 ******************************************************************************/

/**
 * Captures the parameter content of an existing workflow or search protocol
 */
ProteoSAFeInputUtils.captureProtocol = function(paramsXML) {
	if (paramsXML == null)
		return;
	var protocol = new Array();
	var parameters = paramsXML.getElementsByTagName("parameter");
	for (var i=0; i<parameters.length; i++) {
		protocol.push({
			name: parameters[i].attributes.getNamedItem("name").nodeValue,
			value: parameters[i].firstChild.nodeValue
		});
	}
	return protocol;
}

/**
 * Retrieves the saved parameters from an existing ProteoSAFe search protocol,
 * and loads them into the ProteoSAFe UI input form
 */
ProteoSAFeInputUtils.loadProtocol = function(form, type, content) {
	if (form == null || type == null || content == null)
		return;
	// queue up protocol fetch
	var url = "ManageParameters";
	var parameters = null;
	if (type == "task")
		parameters = "?task=" + content;
	else if (type == "protocol")
		parameters = "?protocol=" + content;
	if (parameters != null) {
		var request = createRequest();
		request.open("GET", url + parameters, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					// assign protocol
					var protocol = ProteoSAFeInputUtils.captureProtocol(
						request.responseXML);
					// after retrieving the protocol, clear
					// the task to avoid an infinite loop
					task = null;
					// if the protocol could not be retrieved,
					// display the appropriate error message
					if (protocol == null ||
						protocol.length == null || protocol.length < 1) {
						var message = "Could not retrieve parameters for ";
						switch (type) {
							case "task":
								message += "cloned task with ID \"";
								break;
							case "protocol":
								message += "workflow protocol \"";
								break;
							default:
								message += "unknown content \"";
						}
						message += content + "\".\nThe associated " +
							"parameters file may be invalid or missing.";
						alert(message);
						// reload the default form
						task = null;
						CCMSFormUtils.clearFieldValue(form, "workflow");
						return;
					}
					// otherwise rebuild input form with the new protocol
					CCMSFormUtils.setProtocol(protocol, "protocol", form.name);
					var workflow =
						CCMSFormUtils.getProtocolElement(protocol, "workflow");
					// added for backwards compatibility
					if (workflow == null)
						workflow =
							CCMSFormUtils.getProtocolElement(protocol, "tool");
					CCMSFormUtils.setFieldValue(
						form, "workflow", workflow.value);
				}
			}
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

/**
 * Updates the ProteoSAFe UI input form with the saved parameters from an
 * existing ProteoSAFe search protocol.
 */
ProteoSAFeInputUtils.selectProtocol = function(form, protocol) {
	if (form == null || protocol == null)
		return;
	CCMSFormUtils.clearForm(form, ["seq_on_server", "desc"]);
	if (protocol == "None")
		return;
	else this.loadProtocol(form, "protocol", protocol);
}

/**
 * Saves the parameters from the ProteoSAFe UI input form into a persisted
 * ProteoSAFe search protocol.
 */
ProteoSAFeInputUtils.saveProtocol = function(form) {
	if (form == null)
		return;
	// retrieve this protocol's name
	var protocol = form.protocol.value;
	if (protocol == "None")
		protocol = form.desc.value;
	protocol = prompt("Save this search as protocol with name:", protocol);
	if (protocol == null)
		return;
	else if (protocol == "" || protocol.match(/^(\s)+$/)) {
		alert("You must provide a name to save the current search protocol.");
		return;
	} else if (protocol.match(/[^\w\s]+/)) {
		alert("Sorry, your search protocol's name may only contain " +
			"letters, numbers, underscores and spaces.");
		return;
	} else if (protocol == "None") {
		alert("Sorry, you may not name your search protocol \"None\".");
		return;
	}
	// determine if this is a new protocol, or an existing one
	var overwrite = false;
	for (var i=0; i<form.protocol.length; i++) {
		if (protocol == form.protocol.options[i].value) {
			overwrite = true;
			break;
		}
	}
	// if this is an existing protocol, be sure the user wants to overwrite it
	var sure = true;
	if (overwrite)
		sure = confirm("Overwrite search protocol \"" + protocol + "\"?");
	if (sure) {
		var url = "ManageParameters";
		// build form request parameters
		var parameters = "protocol=" + protocol + "&contents=" +
			"<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n<parameters>\n";
		for (var i=0; i<form.elements.length; i++) {
			var field = form.elements[i];
			var fieldName = field.name;
			// do not add description or spectrum files to protocol
			if (fieldName == null || fieldName == "" ||
				fieldName == "ptm_type" || fieldName == "desc" ||
				fieldName == "spec_on_server")
				continue;
			var handler = CCMSFormUtils.getFieldHandler(field);
			if (handler == null)
				continue;
			var fieldValue = handler.getFieldValue(field);
			if (fieldValue == null || fieldValue == "")
				continue;
			parameters += "\t<parameter name=\"" + fieldName + "\">";
			if (fieldName == "protocol")
				parameters += protocol;
			else parameters += fieldValue;
			parameters += "</parameter>\n";
		}
		parameters += "</parameters>\n";
		// generate and submit request
		var request = createRequest();
		request.open("POST", url, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					// update form's protocol drop-down to include this value
					if (overwrite == false) {
						var option = document.createElement("option");
						option.value = protocol;
						option.innerHTML = protocol;
						option.selected = "selected";
						form.protocol.appendChild(option);
					}
					// select this protocol
					form.protocol.value = protocol;
					alert("Search protocol \"" + protocol +
						"\" was successfully saved.");
				} else alert(
					"There was a problem saving this search protocol.");
			}
		}
		// set the proper header information for a POST request
		request.setRequestHeader("Content-type",
			"application/x-www-form-urlencoded");
		request.setRequestHeader("Content-length", parameters.length);
		request.setRequestHeader("Connection", "close");
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(parameters);
	}
}

/**
 * Displays ProteoSAFe's static "no workflow available" help text
 */
ProteoSAFeInputUtils.displayWorkflowHelp = function() {
	// retrieve the static help page
	var request = createRequest();
    var url = "${livesearch.workflow.help}";
    
    request.open("GET", url, true);
    request.onreadystatechange = function() {
		if (request.readyState == 4) {
			var div = document.getElementById("searchinterface");
			if (request.status == 200) {
				// if help page exists, redirect to it
				window.location.replace(url + "?redirect=auth");
			} else {
				// display default workflow help text
				var text = document.createElement("h3");
				text.style.textAlign = "center";
				text.innerHTML = "Please log in to access " +
					"the workflows installed on this server.";
				div.appendChild(text);
			}
		}
    }
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(null);
}

/******************************************************************************
 * Form field handler implementations
 ******************************************************************************/

/**
 * Form field handler for workflow selection drop-down list.
 */
CCMSFormUtils.handlers.workflowFormFieldHandler = {
	getFieldValue: CCMSFormUtils.handlers.selectFormFieldHandler.getFieldValue,
	
	setFieldValue: function(field, value) {
		CCMSFormUtils.handlers.selectFormFieldHandler.setFieldValue(
			field, value);
		ProteoSAFeInputUtils.selectWorkflow(value);
	},
	
	clearFieldValue: function(field) {
        this.setFieldValue(field, "${livesearch.default.workflow}");
	}
}

// assign input form-specific field handlers to their relevant fields
CCMSFormUtils.handlersByField["workflow"] =
	CCMSFormUtils.handlers.workflowFormFieldHandler;
