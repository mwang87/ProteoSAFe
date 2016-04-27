/**
 * This file contains scripts used by the main ProteoSAFe UI input form page.
 */
 
/**
 * Table registering parameter groups with their associated workflows.
 * 
 * When a tool is selected by the user in the ProteoSAFe UI input form, this 
 * table is consulted to determine which parameter groups should be enabled and
 * which should be disabled in the UI for the selected workflow, as well as the
 * manner in which these changes should occur.
 */
var parameterGroups = [
	// "Tool Selection" box
	{name:'Default.msc', type:'inline'},
	{name:'Default.instrument', type:'row'},
	{name:'Default.fragmentation', type:'row'},			// disabled by default
	{name:'XTandem.fragmentation', type:'row'},			// disabled by default
	{name:'Default.cysteine_protease', type:'row'},
	{name:'MSGFDB.cysteine_protease', type:'row'},		// disabled by default
	{name:'Default.c13_nnet', type:'row'},				// disabled by default
	{name:'XTandem.c13_nnet', type:'row'},				// disabled by default
	{name:'Default.tolerance', type:'row'},
	{name:'Default.tolerance_unit', type:'inline'},		// disabled by default
	{name:'XTandem.tolerance_unit', type:'inline'},		// disabled by default
	{name:'Default.pepnovo_spec_options', type:'row'},	// disabled by default
	
	// "Allowed Post-Translational Modifications" Box
	{name:'Default.ptm_top', type:'row'},
	{name:'Default.ptm'},
	{name:'MSGFDB.ptm'},								// disabled by default
	{name:'MSAlign.ptm'},								// disabled by default
	{name:'MODa.ptm'},									// disabled by default
	{name:'XTandem.ptm'},								// disabled by default
	{name:'Default.msalign'},							// disabled by default
	{name:'Default.moda'},								// disabled by default
	{name:'Default.ptm_bot', type:'row'},
	
	// "More Options" box
	{name:'Default.db'},
	{name:'Extra.db'},									// disabled by default
	{name:'Proteogenomics.db'},							// disabled by default
	{name:'SpectralLibrary.db'},						// disabled by default
	{name:'DynamicSpectralLibrary.db'},					// disabled by default
	{name:'SearchableSpectralLibrary.db'},				// disabled by default
	{name:'Default.pepnovo'},							// disabled by default
	{name:'Default.msdeconv'},							// deprecated
	{name:'Default.filter'},
	{name:'Default.FDR', type:'row'},
	{name:'Default.PepFDR', type:'row'},
	{name:'Default.FPR', type:'row'},
	{name:'Default.ModFDR', type:'row'},				// disabled by default
	{name:'Default.ITRAQ'},								// disabled by default
	{name:'Default.spec_archive'}						// disabled by default
];

/**
 * General input form content loading function.  This function is used as the
 * process callback function for the above protocol load queue.
 */
function loadContent(process) {
	if (process == null)
		return;
	else if (process.type == "workflow")
		loadWorkflowContent(process.form, process.content);
	else if (process.type == "cached")
		loadCachedContent(process.form, process.content);
	else loadProtocolContent(process.form, process.type, process.content);
}

/**
 * Protocol load queue.
 * 
 * Because search protocols (either saved parameter sets, or parameter sets
 * from existing tasks) are loaded asynchronously, they must be maintained in
 * a queue to enforce that they are loaded in the order in which they were
 * fetched.
 */
var protocolQueue = new SynchronizedQueue(loadContent);

/**
 * Updates the ProteoSAFe UI input form appropriately
 * when a new workflow is selected by the user.
 */
function loadWorkflowContent(form, workflow) {
	if (form == null || workflow == null || workflow == "")
		return;
	var request = createRequest();
    var url = "DownloadWorkflowInterface?workflow=" +
    	encodeURIComponent(workflow) + "&type=input";
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
			if (request.status == 200) {
				renderExtendedInputs(request.responseXML);
				enableGroups(request.responseXML);
				populateDefaults(form, request.responseXML);
			} else alert("There was a problem retrieving the " +
				"interface specification for workflow \"" + tool + "\".");
			protocolQueue.advance();
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
    request.send(null);
}

function enableGroups(specificationXML) {
	if (specificationXML == null)
		return;
	// first note all registered parameter groups, and set them to be disabled
	var enabledState = {};
	for (var i in parameterGroups) {
		var parameterGroup = parameterGroups[i];
		enabledState[parameterGroup.name] = {
			type: parameterGroup.type,
			enabled: false
		};
	}
	// then note the groups associated with this workflow,
	// and set them to be enabled
	var enabledGroups = specificationXML.getElementsByTagName("parameterGroup");
	for (var i=0; i<enabledGroups.length; i++) {
		var id = enabledGroups[i].attributes.getNamedItem("name").nodeValue;
		var groupState = enabledState[id];
		if (groupState != null)
			groupState.enabled = true;
	}
	// enable/disable all groups properly
	for (var id in enabledState) {
		var groupState = enabledState[id];
		switch (groupState.type) {
			case "inline":
				enableInline(id, groupState.enabled);
				break;
			case "row":
				enableRow(id, groupState.enabled);
				break;
			default:
				enableDiv(id, groupState.enabled);
		}
	}
}

function populateDefaults(form, specificationXML) {
	if (specificationXML == null)
		return;
	var parameters = specificationXML.getElementsByTagName("parameter");
	for (var i=0; i<parameters.length; i++) {
		var parameter = parameters[i];
		var defaultValue =
			parameter.attributes.getNamedItem("default");
		if (defaultValue == null)
			continue;
		else defaultValue = defaultValue.nodeValue;
		var name = parameter.attributes.getNamedItem("name").nodeValue;
		var parent = parameter.parentNode;
		if (parent.nodeName == "parameterGroup")
			name = parent.attributes.getNamedItem("name").nodeValue + "." + name;
		setFieldValue(form, name, defaultValue);
	}
}

function renderExtendedInputs(specificationXML) {
	// first remove previous extended inputs
	var extended = document.getElementById("Default.extended");
	removeChildren(extended);
	// then read specification document,
	// populate with new extended inputs declared there
	if (specificationXML == null)
		return;
	var groups = specificationXML.getElementsByTagName("parameterGroup");
	if (groups == null || groups.length < 1)
		return;
	for (var i=0; i<groups.length; i++) {
		var group = groups[i];
		var id = getAttribute(group, "name");
		if (id != "Default.extended")
			continue;
		var layouts = group.getElementsByTagName("layout");
		if (layouts == null || layouts.length < 1)
			continue;
		// render the layout groups
		for (var j=0; j<layouts.length; j++) {
			var layout = layouts[j];
			// collect the inputs in this layout group
			var inputs = extractExtendedInputs(layout);
			if (inputs == null || inputs.length < 1)
				continue;
			// collect this layout group's attributes
			var type = getAttribute(layout, "type");
			var columns = parseInt(getAttribute(layout, "columns"));
			if (columns == null || columns == Number.NaN)
				columns = 1;
			var label = getAttribute(layout, "label");
			// render the table that will enclose this layout group's inputs
			var table = document.createElement("table");
			table.border = "0";
			table.width = "100%";
			var tr = null;
			var td = null;
			// if label is present, render it in the first cell;
			// otherwise, render a blank cell
			tr = document.createElement("tr");
			tr.valign = "top";
			td = document.createElement("td");
			td.align = "right";
			if (label != null)
				td.appendChild(document.createTextNode("\u00a0" + label));
			tr.appendChild(td);
			// render the inputs
			var columnIndex = 1;
			for (var k=0; k<inputs.length; k++) {
				var input = inputs[k];
				// if this is the start of a new row, render the row
				if (columnIndex == 0) {
					tr = document.createElement("tr");
					tr.valign = "top";
					// render a blank cell on the left, to fill the label row
					td = document.createElement("td");
					td.align = "right";
					tr.appendChild(td);
					columnIndex++;
				}
				// render this input's table cell
				td = document.createElement("td");
				td.align = "left";
				// build the actual input control
				var control = null;
				switch (input.type) {
					case "checkbox":
						control = createCheckbox(input.name);
						break;
					case "radio":
						control = createRadio(input.name, input.value);
						break;
				}
				// render the built control
				if (control != null) {
					td.appendChild(control);
					if (input.label != null)
						td.appendChild(
							document.createTextNode("\u00a0" + input.label));
					tr.appendChild(td);
				}
				// advance the column counter
				if (columnIndex < columns)
					columnIndex++;
				else {
					columnIndex = 0;
					table.appendChild(tr);
				}
			}
			extended.appendChild(table);
		}
	}
}

function extractExtendedInputs(layout) {
	// "layout" should be a DOM element representing a <layout> tag
	// from this workflow's interface specification XML document
	if (layout == null)
		return null;
	// every <layout> element should have a non-empty "type" attribute
	var type = getAttribute(layout, "type");
	if (type == null || type == "")
		return null;
	// every <layout> element should have one or more <parameter> children
	var parameters = layout.getElementsByTagName("parameter");
	if (parameters == null || parameters.length < 1)
		return null;
	var inputs = new Array();
	for (var i=0; i<parameters.length; i++) {
		var parameter = parameters[i];
		var name = getAttribute(parameter, "name");
		// if this is a radio button or select input, find its options,
		// which should be embedded in a child <validator> of type "set"
		var inputSet = false;
		var validators = parameter.getElementsByTagName("validator");
		if (validators != null && validators.length > 0) {
			for (var j=0; j<validators.length; j++) {
				var validator = validators[j];
				if (getAttribute(validator, "type") != "set")
					continue;
				else {
					// every <validator> element of type "set" should have
					// one or more more <option> children
					var options = validator.getElementsByTagName("option");
					for (var k=0; k<options.length; k++) {
						var option = options[k];
						inputs.push({
							"type": type,
							"name": name,
							"value": getAttribute(option, "value"),
							"label": getAttribute(option, "label")
						});
						inputSet = true;
					}
					break;
				}
			}
		}
		// if this parameter had options, then its inputs have been collected;
		// otherwise, collect the <parameter> element itself as the input
		if (inputSet == false)
			inputs.push({
				"type": type,
				"name": name,
				"label": getAttribute(parameter, "label")
			});
	}
	// return inputs
	if (inputs.length < 1)
		return null;
	else return inputs;
}

/**
 * Retrieves the saved parameters from an existing ProteoSAFe search protocol,
 * and loads them into the ProteoSAFe UI input form.
 */
function loadProtocolContent(form, type, content) {
	if (form == null || type == null || content == null)
		return;
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
					loadParameters(form, request.responseXML);
				}
				protocolQueue.advance();
			}
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

/**
 * Loads the saved parameters from an existing ProteoSAFe parameter XML document.
 */
function loadParameters(form, xml) {
	if (form == null || xml == null)
		return;
	// the workflow type must be noted separately,
	// since it has to be loaded first
	var workflow = null;
	var protocol = new Array();
	var parameters = xml.getElementsByTagName("parameter");
	for (var i=0; i<parameters.length; i++) {
		var fieldName = parameters[i].attributes.getNamedItem("name").nodeValue;
		var fieldValue = parameters[i].firstChild.nodeValue;
		if (fieldName == "tool")
			workflow = fieldValue;
		else protocol.push({
			name: fieldName,
			value: fieldValue
		});
	}
	// load workflow first
	if (workflow != null)
		setFieldValue(form, "tool", workflow);
	// then load the rest of the parameters
	protocolQueue.queue({
		form: form,
		type: "cached",
		content: protocol
	});
}

/**
 * Loads cached parameters from a previously processed ProteoSAFe parameter XML
 * document.  This functionality is necessary to ensure that content loads can
 * be queued in the correct order.
 */
function loadCachedContent(form, protocol) {
	if (form == null || protocol == null)
		return;
	for (var i=0; i<protocol.length; i++)
		setFieldValue(form, protocol[i].name, protocol[i].value);
	protocolQueue.advance();
}

/**
 * Updates the ProteoSAFe UI input form with the defaults for
 * the selected workflow.
 */
function selectWorkflow(form, workflow) {
	if (form == null || workflow == null)
		return;
	if (form.protocol != null)
		form.protocol.value = "None";	// clear any protocol selection
	protocolQueue.queue({
		form: form,
		type: "workflow",
		content: workflow
	});
}

/**
 * Updates the ProteoSAFe UI input form with the saved parameters from an
 * existing ProteoSAFe task.
 */
function selectTask(form, taskID) {
	if (form == null || taskID == null)
		return;
	clearForm(form);
	protocolQueue.queue({
		form: form,
		type: "task",
		content: taskID
	});
}

/**
 * Updates the value of a specific parameter in the ProteoSAFe UI input form.
 */
function updateParameter(form, parameter, value) {
	if (form == null || parameter == null || value == null)
		return;
	var content = new Array();
	content.push({
		name: parameter,
		value: value
	});
	protocolQueue.force({
		form: form,
		type: "cached",
		content: content
	});
}

/**
 * Updates the ProteoSAFe UI input form with the saved parameters from an
 * existing ProteoSAFe search protocol.
 */
function selectProtocol(form, protocol) {
	if (form == null || protocol == null)
		return;
	clearForm(form, ["seq_on_server", "desc"]);
	if (protocol == "None")
		return;
	else protocolQueue.queue({
		form: form,
		type: "protocol",
		content: protocol
	});
}

/**
 * Consults the protocol load queue to determine if any protocols are
 * available to be loaded, and if any are, loads the saved parameters
 * from that protocol into the ProteoSAFe UI input form.
 */
function loadProtocol(form, protocol) {
	if (form == null || protocol == null)
		return;
	for (var i=0; i<protocol.length; i++) {
		var field = protocol[i];
	}
}

/**
 * Saves the parameters from the ProteoSAFe UI input form into a persisted
 * ProteoSAFe search protocol.
 */
function saveProtocol(form) {
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
			var handler = getFieldHandler(field);
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
				} else alert("There was a problem saving this search protocol.");
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

function addSelectedFiles(items, field, window) {
	if (window.selection == null)
		window.selection = {};
	if (window.selection[field] == null)
		window.selection[field] = new Array();
	// add new items to the selection array
	for (var i in items) {
		var id = items[i].path;
		var found = false;
		for (var j in window.selection[field]) {
			if (window.selection[field][j].path == id) {
				found = true;
				break;
			}
		}
		if (found == false)
			window.selection[field].push(items[i]);
	}
	if (window.selection[field].length < 1)
		window.selection[field] = undefined;
	return writeSelection(field, window);
}
	
function removeSelectedFile(item, field, window) {
	if (item != null) {
		if (item.id == "ROOT")
			window.selection[field] = undefined;
		else if (window.selection[field] != null &&
			window.selection[field].length > 0) {
			for (var i in window.selection[field]) {
				if (window.selection[field][i].path == item.id) {
					window.selection[field].splice(i, 1);
					return writeSelection(field);
				}
			}
		}
	}
	return writeSelection(field);
}

function writeSelection(field, window) {
	if (window.selection[field] != null &&
		window.selection[field].length > 0) {
		var files = 0;
		var folders = 0;
		var fieldValue = "";
		for (var i in window.selection[field]) {
			if (window.selection[field][i].directory)
				folders++;
			else files++;
			var file = "f." + window.selection[field][i].path;
			fieldValue = fieldValue + file + ';';
		}
		window.document.mainform[field].value = fieldValue;
		return files + " files and " + folders + " folders are selected";
	} else {
		window.document.mainform[field].value = "";
		return "";
	}
}

function shareUser(form) {
	if (form == null)
		return;
	// generate request parameters
	var sharedUser = getFieldValue(form, "sharedUser");
	var parameters = "sharedUser=" + sharedUser;
	// generate and submit request
	var url = "/ProteoSAFe/ManageSharing";
	var request = createRequest();
	request.open("POST", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				var div = document.getElementById("sharedUsers");
				if (div != null)
					div.innerHTML = request.responseText;
				alert("You have successfully shared your files with user \"" +
					sharedUser + "\".");
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
 * Validates and submits the ProteoSAFe UI input form.
 */
function submitForm(form) {
	if (form == null)
		return;
	// generate request parameters
	var parameters = serializeForm(form);
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
				else alert(message);
			} else alert("Task creation failed due to an unknown error.");
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
	//if (validateForm(form)) {
	//	form.action = "InvokeTools?uuid=" + new UUID();
	//	form.submit();
		//toggleDiv('searchinterface');
		//toggleDiv('uploadmeter');
		//refreshPercentage();
	//}
}

/**
 * Clears all parameters in the ProteoSAFe UI input form.
 */
function clearForm(form, exceptions) {
	if (form == null)
		return;
	// initialize the list of cleared fields to include the tool parameter,
	// so that it won't be cleared here (which would trigger a reload of its
	// default parameter values)
	var cleared = {tool: true};
	for (var i in exceptions) 
		cleared[exceptions[i]] = true;
	for (var i=0; i<form.elements.length; i++) {
		var fieldName = form.elements[i].name;
		var skip = false;
		for (var j in cleared)
			if (fieldName == j) {
				skip = true;
				break;
			}
		if (skip)
			continue;
		else {
			clearFieldValue(form, fieldName);
			cleared[fieldName] = true;
		}
	}
}

/**
 * Resets all parameters in the ProteoSAFe UI input form to their default state.
 */
function resetForm(form) {
	if (form == null)
		return;
	clearForm(form);
	// clear tool last, to populate its parameter defaults
	// after everything else has first been cleared
	clearFieldValue(form, "tool");
}

/**
 * In the ProteoSAFe UI, functionality for processing parameter input from the
 * user is meant to be both reusable and extensible.  This is achieved by
 * encapsulating the logic for both setting and retrieving a parameter's value
 * into what is referred to as a "form field handler" object.  Each parameter in
 * the ProteoSAFe UI input form is associated with a handler class implemented
 * to handle the unique processing requirements of parameters of that type. 
 * 
 * Any time a new parameter type is introduced, with new UI features and
 * requirements, a new form field handler class should be implemented to process
 * the new parameter type properly.
 * 
 * Each form field handler class should contain three handler functions,
 * with the following function signatures:
 * 
 * getFieldValue(field)
 * setFieldValue(field, value)
 * clearFieldValue(field)
 */

/**
 * Default form field handler for "normal" form fields, i.e. those whose
 * value is accessed directly through the "value" object property, such as
 * plain text boxes.
 */
var defaultFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else return field.value;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else field.value = value;
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else field.value = "";
	}
}

/**
 * A subclass of defaultFormFieldHandler, for "normal" fields that are
 * meant to be hidden, and should not be cleared when the form is reset.
 */
var hiddenFormFieldHandler = {
	getFieldValue: defaultFormFieldHandler.getFieldValue,
	setFieldValue: defaultFormFieldHandler.setFieldValue,
	clearFieldValue: function(field) {}
}

/**
 * Form field handler for standard checkbox form fields.
 */
var checkboxFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else if (field.checked)
			return "on";
		else return null;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else if (value == "on")
			field.checked = true;
		else field.checked = false;
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else field.checked = false;
	}
}

/**
 * Form field handler for standard radio button form fields.
 */
var radioFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else if (field.checked)
			return field.value;
		else return null;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else if (field.value == value)
			field.checked = true;
		else field.checked = false;
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else field.checked = false;
	}
}

/**
 * Form field handler for standard drop-down list form fields.
 */
var selectFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else {
			var index = field.selectedIndex;
			if (index == null || index < 0)
				return null;
			else return field.options[index].value;
		}
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else for (var i=0; i<field.options.length; i++) {
			var option = field.options[i];
			if (option.value == value) {
				option.selected = true;
				field.selectedIndex = i;
			} else option.selected = false;
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else for (var i=0; i<field.options.length; i++) {
			var option = field.options[i];
			if (i == 0) {
				option.selected = true;
				field.selectedIndex = i;
			} else option.selected = false;
		}
	}
}

/**
 * Form field handler for tool selection drop-down list.
 */
var toolFormFieldHandler = {
	getFieldValue: selectFormFieldHandler.getFieldValue,
	
	setFieldValue: function(field, value) {
		selectFormFieldHandler.setFieldValue(field, value);
		selectWorkflow(field.form, value);
	},
	
	clearFieldValue: function(field) {
		selectFormFieldHandler.clearFieldValue(field);
		selectWorkflow(field.form, "INSPECT");
	}
}

/**
 * Form field handler for server-side data form fields.
 */
var dataFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else return field.value;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else {
			var display = null;
			if (field.name == "spec_on_server")
				display = "spec_on_server_display";
			else display = "seq_on_server_display";
			field.value = value;
			if (value != null) {
				var sequences = value.split(";");
				var items = new Array();
				for (var i=0; i<sequences.length; i++) {
					if (sequences[i] == "")
						continue;
					var item = {path: sequences[i].substr(2)};
					items.push(item);
				}
				document.getElementById(display).innerHTML =
					addSelectedFiles(items, field.name, window);
			} else document.getElementById(display).innerHTML = "";
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else {
			field.value = null;
			document.getElementById("seq_on_server_display").innerHTML = "";
		}
	}
}

/**
 * Table registering form fields with their appropriate handler objects.
 * 
 * The purpose of this table is to assign any new, complex form field handler
 * functionality to any parameters needing it.  Any new type of form field
 * requiring special treatment will need to be implemented and registered to
 * all form parameters of its type, in this manner.
 */
var formFieldHandlers = {
	tool: toolFormFieldHandler,
	spec_on_server: dataFormFieldHandler,
	seq_on_server: dataFormFieldHandler
};

/**
 * Retrieves the appropriate field handler object for the specified field.
 */
function getFieldHandler(field) {
	if (field == null)
		return;
	// first try full literal field name
	var fieldName = field.name;
	var handler = formFieldHandlers[fieldName];
	if (handler == null) {
		// extract the "variant" prefix and try again
		var dot = fieldName.indexOf(".");
		if (dot >= 0 && fieldName.length > (dot + 1)) {
			fieldName = fieldName.substring(dot + 1);
			handler = formFieldHandlers[fieldName];
		}
	}
	// if no handler was registered for this field,
	// use one that's appropriate for the field type
	if (handler == null) switch (field.type) {
		case "checkbox":
			handler = checkboxFormFieldHandler;
			break;
		case "radio":
			handler = radioFormFieldHandler;
			break;
		case "select":
		case "select-one":
		case "select-multiple":
			handler = selectFormFieldHandler;
			break;
		default:
			// hopefully this is appropriate for this field type
			handler = defaultFormFieldHandler;
	}
	return handler;
}

/**
 * Retrieves all fields in the specified form that match the specified unique
 * field name.
 * 
 * In the ProteoSAFe web application, fields with the same unique name (defined
 * as the field's "group.name" suffix) are considered identical for the purposes
 * of form processing.  Any preceding "variant" is only used for look and feel
 * variations, and is ignored for the purposes of this comparison.
 */
function getFields(form, fieldName) {
	if (form == null || fieldName == null)
		return;
	var fields = new Array();
	var pattern = new RegExp("(\\w+\\.)*" + fieldName);
	for (var i=0; i<form.elements.length; i++) {
		var field = form.elements[i];
		if (pattern.test(field.name)) {
			fields.push(field);
		}
	}
	if (fields.length < 1)
		return null;
	else return fields;
}

/**
 * Gets the array of values for the specified form field.
 */
function getFieldValues(form, fieldName) {
	if (form == null || fieldName == null)
		return;
	else {
		var fields = getFields(form, fieldName);
		var values = new Array();
		for (var i in fields) {
			var field = fields[i];
			var handler = getFieldHandler(field);
			if (handler == null)
				continue;
			else {
				var value = handler.getFieldValue(field);
				if (value != null)
					values.push(value);
			}
		}
		if (values.length < 1)
			return null;
		else return values;
	}
}

/**
 * Gets the first value of the specified form field.
 */
function getFieldValue(form, fieldName) {
	var values = getFieldValues(form, fieldName);
	if (values == null || values.length < 1)
		return null;
	else return values[0];
}

/**
 * Sets the value of the specified form field.
 */
function setFieldValue(form, fieldName, value) {
	if (value == null)
		clearFieldValue(form, fieldName);
	else if (form == null || fieldName == null)
		return;
	else {
		var fields = getFields(form, fieldName);
		// the "ptm.custom_PTM" rule - for parameters that may not already
		// be present in the input form, but need to be added
		if (fields == null || fields.length < 1)
			fields = [{
				name: fieldName,
				form: form
			}];
		for (var i in fields) {
			var field = fields[i];
			var handler = getFieldHandler(field);
			if (handler == null)
				return;
			else handler.setFieldValue(field, value);
		}
	}
}

/**
 * Clears the value of the specified form field (i.e. sets it to "null").
 */
function clearFieldValue(form, fieldName) {
	if (form == null || fieldName == null || fieldName == "")
		return;
	else {
		var fields = getFields(form, fieldName);
		for (var i in fields) {
			var field = fields[i];
			var handler = getFieldHandler(field);
			if (handler == null)
				return;
			else handler.clearFieldValue(field);
		}
	}
}

function refreshPercentage(){
    var req = createRequest();
    var url = "upload_progress.jsp?uuid=" + uuid;
    req.open("GET", url, true);
    req.onreadystatechange = function(){
		try{
	        if(req.readyState == 4){
			    var xml = req.responseXML;
			    var percentage = xml.getElementsByTagName("percentage")[0]
			    	.firstChild.nodeValue;
			    document.getElementById("uploadprogress").style.width =
			    	(percentage * 3) + "px";
			    document.getElementById("uploadprogress").firstChild.nodeValue =
			    	percentage + "%";
			    document.getElementById("rate").firstChild.nodeValue = 
				    xml.getElementsByTagName("rate")[0].firstChild.nodeValue +
				    "KB/s";
			    document.getElementById("remaining").firstChild.nodeValue = 
				    xml.getElementsByTagName("remaining")[0]
				    .firstChild.nodeValue;
			    document.getElementById("elapsed").firstChild.nodeValue = 
				    xml.getElementsByTagName("elapsed")[0].firstChild.nodeValue;
				if(percentage == "100")
					;
//					location.reload(true);
			    else if(percentage != "-1")
				    setTimeout("refreshPercentage()", 500);
	        }
	    }
	    catch(err){
			setTimeout("refreshPercentage()", 500);	    
	    }
    }
	req.setRequestHeader( "If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT" );    
    req.send(null);
}
