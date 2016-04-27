/**
 * This file contains utility scripts used to build, populate, update and
 * submit various web forms within the CCMS software system.
 */

// static form utility script package
var CCMSFormUtils = {};

// bookkeeping hash to maintain the state of all currently active forms
var CCMSForms = {};
CCMSForms.forms = {};	// maintains DOM and parameter state of active forms
CCMSForms.modules = {};	// maintains loaded module constructors
// module key constants
CCMSForms.FLOATING = "FLOATING_MODULES";

/******************************************************************************
 * Basic form accessor functions
 ******************************************************************************/

/**
 * Finds and returns the enclosing form of the specified HTML element,
 * or null if the specified element is not the descendant of a form.
 */
CCMSFormUtils.getEnclosingForm = function(div) {
	if (div == null)
		return null;
	else if (div.nodeName.toUpperCase() == "FORM")
		return div;
	var parent = div.parentNode;
	while (parent != null) {
		if (parent.nodeName.toUpperCase() == "FORM")
			return parent;
		else parent = parent.parentNode;
	}
	return null;
}

/**
 * Retrieves the bookkeeping record for the specified form, or null
 * if no record has yet been stored for the form.
 * 
 * Form records are Javascript objects which typically contain the
 * following members:
 * 
 * form -		the associated form's DOM object
 * protocols -	hash of the associated form's protocols, or arrays
 * 				of captured parameter values
 * modules -	hash of the associated form's instantiated module
 * 				objects, keyed to the module's ID
 * asynchronousElements - array of asynchronous element state objects
 */
CCMSFormUtils.getFormRecord = function(formName) {
	if (formName == null)
		return null;
	else return CCMSForms.forms[formName];
}

/**
 * Assigns the provided form bookkeeping record to its associated form's name.
 */
CCMSFormUtils.setFormRecord = function(record, formName) {
	if (formName == null)
		return;
	else if (record == null)
		CCMSForms.forms[formName] = undefined;
	else CCMSForms.forms[formName] = record;
}

/******************************************************************************
 * Dynamic form content loading/building functions
 ******************************************************************************/

/**
 * Retrieves the specified module instance.
 */
CCMSFormUtils.getModuleInstance = function(moduleID, formName) {
	if (moduleID == null)
		return null;
	// if no form was specified, then it's a floating module
	if (formName == null)
		formName = CCMSForms.FLOATING;
	// retrieve the record for the specified form
	var record = this.getFormRecord(formName);
	if (record == null || record.modules == null)
		return null;
	// retrieve the instantiated Javascript object for the specified module ID
	return record.modules[moduleID];
}

/**
 * Assigns the provided module instance to its associated form's record
 */
CCMSFormUtils.setModuleInstance = function(module, moduleID, formName) {
	if (moduleID == null)
		return;
	// if no form was specified, then it's a floating module
	if (formName == null)
		formName = CCMSForms.FLOATING;
	// retrieve the record for the specified form
	var record = this.getFormRecord(formName);
	// instantiate the record as necessary
	if (record == null) {
		record = {};
		if (formName != CCMSForms.FLOATING)
			record.form = document.forms[formName];
	}
	if (record.modules == null)
		record.modules = {};
	// assign the provided module instance to this form record
	record.modules[moduleID] = module;
	// reassign this form record to its associated form's name
	this.setFormRecord(record, formName);
}

/**
 * Function to generically load and instantiate a
 * form content module within the specified div
 */
CCMSFormUtils.loadModule = function(
	name, type, div, id, properties, parameters, callback
) {
	if (name == null || type == null || div == null || id == null)
		return;
	// retrieve this module's enclosing form
	var form = this.getEnclosingForm(div);
	// if there is no enclosing form, then this is a floating module
	if (form == null || form.name == null)
		form = {name: CCMSForms.FLOATING};
	// initialize module instance record to ensure proper loading order
	this.setModuleInstance(null, id, form.name);
	// render the module div under the parent div
	var child = document.createElement("div");
	child.id = id;
	div.appendChild(child);
	// add any static HTML content that may be included in this module
	var url = "DownloadModuleInterface?module=" + encodeURIComponent(name) +
		"&type=" + encodeURIComponent(type) + "&id=" + encodeURIComponent(id) +
		"&extension=inc";
	if (properties != null) {
		for (var property in properties) {
			if (property == null || property == "" || property == "id") {
				// TODO: report error, fail
				continue;
			}
			var value = properties[property];
			if (value == null || value == "") {
				// TODO: report error, fail
				continue;
			}
			url += "&" + encodeURIComponent("property:" + property) +
				"=" + encodeURIComponent(value);
		}
	}
	// retrieve module content
	var complete = function(data) {
		// populate div with this module's HTML, if present
		if (data != null)
			child.innerHTML = data;
		// get this module's script and initialize it
		CCMSFormUtils.initModule(
			name, type, child, id, properties, parameters, callback);
	}
    /* jQuery AJAX request */
	$.ajax({
		type: "GET",
		url: url,
		async: false,
		success: complete,
		error: function(request, status, error) {
			// if the return code is 404, then the file was not found,
			// which is not necessarily an error since this module may
			// only include script content; however, any other non-200
			// return code does indicate a genuine error
			if (request.status == 404)
				complete();
			else {
				alert("Error downloading HTML include file for module \"" +
					name + "." + id + "\": " + request.status);
				return;
			}
		}
    });
	/* Manual AJAX request */
//	var request = createRequest();
//    request.open("GET", url, true);
//    request.onreadystatechange = function() {
//        if (request.readyState == 4) {
//			if (request.status == 200)
//				complete(request.responseText);
//			else if (request.status == 404)
//				complete();
//        	else alert("Error downloading HTML include file for module \"" +
//				name + "." + id + "\": " + request.status);
//		}
//	}
//	request.setRequestHeader("If-Modified-Since",
//		"Sat, 1 Jan 2000 00:00:00 GMT");
//    request.send(null);
}

/**
 * Function to generically initialize the specified module
 * (assumed to be already loaded) within the specified div
 */
CCMSFormUtils.initModule = function(
	name, type, div, id, properties, parameters, callback
) {
	if (name == null || type == null || div == null || id == null)
		return;
	// retrieve this module's enclosing form
	var form = this.getEnclosingForm(div);
	// if there is no enclosing form, then this is a floating module
	if (form == null || form.name == null)
		form = {name: CCMSForms.FLOATING};
	// build URL to retrieve this module's initialization script
	var url = "DownloadModuleInterface?module=" + encodeURIComponent(name) +
	 	"&type=" + encodeURIComponent(type) + "&id=" + encodeURIComponent(id) +
		"&extension=js";
	if (properties != null) {
		for (var property in properties) {
			if (property == null || property == "" || property == "id") {
				// TODO: report error, fail
				continue;
			}
			var value = properties[property];
			if (value == null || value == "") {
				// TODO: report error, fail
				continue;
			}
			url += "&" + encodeURIComponent("property:" + property) +
				"=" + encodeURIComponent(value);
		}
	}
	// retrieve and execute this module's initialization script, if one exists
	var complete = function() {
		if (CCMSForms.modules[name] != null) {
			var instance = new CCMSForms.modules[name](
				div, id, properties, parameters);
			CCMSFormUtils.setModuleInstance(instance, id, form.name);
			instance.init();
		} else alert("There was an error injecting the script " +
			"for module \"" + name + "." + id + "\".");
		if (callback != null)
			callback();
	}
	/* jQuery conditional script loader */
	if (CCMSForms.modules[name] != null)
		complete();
	else $.ajax({
		url: url,
		async: false,
		dataType: "script",
		success: complete
    });
}

/******************************************************************************
 * Form state functions
 ******************************************************************************/

/**
 * Retrieves the specified form protocol
 * (i.e. array of captured parameter values).
 */
CCMSFormUtils.getProtocol = function(protocolName, formName) {
	if (protocolName == null)
		return null;
	// TODO: this is a hack
	if (formName == null)
		formName = "mainform";
	// retrieve the record for the specified form
	var record = this.getFormRecord(formName);
	if (record == null || record.protocols == null)
		return null;
	// retrieve the protocol array with the specified name
	return record.protocols[protocolName];
}

/**
 * Assigns the provided protocol array to its associated form's record
 */
CCMSFormUtils.setProtocol = function(protocol, protocolName, formName) {
	if (protocolName == null)
		return;
	// TODO: this is a hack
	if (formName == null)
		formName = "mainform";
	// retrieve the record for the specified form
	var record = this.getFormRecord(formName);
	// instantiate the record as necessary
	if (record == null) {
		record = {};
		record.form = document.forms[formName];
	}
	if (record.protocols == null)
		record.protocols = {};
	// assign the provided protocol array to this form record
	if (protocol == null)
		record.protocols[protocolName] = undefined;
	else record.protocols[protocolName] = protocol;
	// reassign this form record to its associated form's name
	this.setFormRecord(record, formName);
}

/**
 * Gets an element from a protocol.
 */
CCMSFormUtils.getProtocolElement = function(protocol, name) {
	if (protocol == null || name == null)
		return null;
	for (var i=0; i<protocol.length; i++) {
		if (protocol[i].name == name)
			return protocol[i];
	}
	return null;
}

/**
 * Removes all elements from the provided protocol whose names match the
 * provided element name.  Normally there is at most one such element, but
 * in the case of a multi-value parameter, this function ensures that all
 * elements corresponding to this parameter are removed from the protocol array.
 */
CCMSFormUtils.removeProtocolElements = function(protocol, elementName) {
	if (protocol == null || elementName == null)
		return protocol;
	// traverse the protocol array in reverse order, to ensure
	// that the array is pruned without skipping any elements
	for (var i=protocol.length-1; i>=0; i--)
		if (protocol[i].name == elementName)
			protocol.splice(i, 1);
	// return the updated protocol
	return protocol;
}

/**
 * Adds an element to a protocol.
 * 
 * If overwrite is specified, then any existing elements with the same name
 * as the new element will be removed prior to adding the new element.
 */
CCMSFormUtils.addProtocolElement = function(protocol, element, overwrite) {
	if (protocol == null || element == null)
		return protocol;
	// if overwrite is specified, then first remove all matching elements
	if (overwrite)
		protocol = this.removeProtocolElements(protocol, element.name);
	// add the new element to the end of the protocol array
	protocol.push(element);
	// return the updated protocol
	return protocol;
}

CCMSFormUtils.mergeProtocols = function(protocol1, protocol2) {
	if (protocol1 == null && protocol2 == null)
		return null;
	else if (protocol1 == null)
		return protocol2;
	else if (protocol2 == null)
		return protocol1;
	// traverse the second protocol, adding its elements to the first
	var added = {};
	for (var i=0; i<protocol2.length; i++) {
		var element = protocol2[i];
		// if a protocol element has already been added as part of
		// this merge operation, then it should be explicitly appended
		// rather than overwriting the previously added value
		if (added[element.name])
			this.addProtocolElement(protocol1, element);
		else {
			added[element.name] = true;
			this.addProtocolElement(protocol1, element, true);
		}
	}
}

/**
 * Clears all parameters in the specified form.
 */
CCMSFormUtils.clearForm = function(form, exceptions) {
	if (form == null)
		return;
	// set up hash to keep track of fields that should not be cleared further
	var cleared = {};
	for (var field in exceptions) 
		cleared[exceptions[field]] = true;
	for (var i=0; i<form.elements.length; i++) {
		var fieldName = form.elements[i].name;
		var skip = false;
		// if a field with this name has already been cleared, leave it alone
		for (var field in cleared) {
			if (fieldName == field) {
				skip = true;
				break;
			}
		}
		if (skip)
			continue;
		// otherwise, clear it and mark it as cleared
		else {
			this.clearFieldValue(form, fieldName);
			cleared[fieldName] = true;
		}
	}
}

/**
 * Populates the specified form with the specified protocol.
 */
CCMSFormUtils.populateForm = function(form, protocol) {
	if (form == null || protocol == null)
		return;
	for (var i=0; i<protocol.length; i++)
		CCMSFormUtils.setFieldValue(form, protocol[i].name, protocol[i].value);
}

/**
 * Function to read a single form element and capture its basic
 * properties into a key/value pair with the following fields:
 * name  - form parameter name
 * value - form parameter value
 */
CCMSFormUtils.captureFormElement = function(element) {
	if (element == null)
		return null;
	var type = element.type.toUpperCase();
	var captured = {};
	captured.name = element.name;
	captured.value = null;
	if (type == "TEXT" || type == "TEXTAREA" || type == "PASSWORD" ||
		type == "BUTTON" || type == "RESET" || type == "SUBMIT" ||
		type == "FILE" || type == "IMAGE" || type == "HIDDEN")
		captured.value = element.value;
	else if (type == "CHECKBOX" && element.checked)
		captured.value = element.value ? element.value : "On";
	else if (type == "RADIO" && element.checked)
		captured.value = element.value;
	// TODO: handle multiple select drop-downs
	else if (type.indexOf("SELECT") != -1) {
    	for (var i=0; i<element.options.length; i++) {
    		var option = element.options[i];
    		if (option.selected) {
    			captured.value = option.value ? option.value : option.text;
    			break;
    		}
    	}
	}
	if (captured.name == null || captured.value == null)
		return null;
	else return captured;
}

/**
 * Function to capture a snapshot of the current state of a form's contents
 * into a "protocol", or an array of element captures, as defined as the
 * output of function "captureFormElement" above.
 */
CCMSFormUtils.captureForm = function(form) {
	if (form == null)
		return null;
	// read form elements into an array of element captures
	var protocol = new Array();
    for (var i=0; i<form.elements.length; i++) {
    	var captured = this.captureFormElement(form.elements[i]);
    	// only keep the element capture if its name and value are non-empty
    	if (captured != null && captured.name != null && captured.name != "" &&
    		captured.value != null && captured.value != "")
    		protocol.push(captured);
    }
    return protocol;
}

/**
 * Function to serialize a snapshot of the current state of a form's
 * contents into an HTTP request parameter query string.
 */
CCMSFormUtils.serializeForm = function(form) {
	var elements = this.captureForm(form);
	if (elements == null)
		return null;
	// concatenate parameters into query string
	var query = "";
	for (var i=0; i<elements.length; i++) {
		var element = elements[i];
		query += (query.length > 0 ? "&" : "") +
			encodeRequestParameter(element.name, element.value);
	}
	return query;
}

/******************************************************************************
 * Form field handler functions
 ******************************************************************************/

/**
 * Retrieves all fields in the specified form that match the specified field
 * name.
 */
CCMSFormUtils.getFields = function(form, fieldName) {
	if (form == null || fieldName == null)
		return;
	var fields = new Array();
	for (var i=0; i<form.elements.length; i++) {
		var field = form.elements[i];
		if (field.name == fieldName)
			fields.push(field);
	}
	if (fields.length < 1)
		return null;
	else return fields;
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
 * Retrieves the appropriate field handler object for the specified field.
 */
CCMSFormUtils.getFieldHandler = function(field) {
	if (field == null)
		return;
	// first see if any special handler was registered to this field name
	var handler = this.handlersByField[field.name];
	// if no handler was registered for this field,
	// use one that's appropriate for the field type
	if (handler == null) switch (field.type) {
		case "checkbox":
			handler = this.handlers.checkboxFormFieldHandler;
			break;
		case "radio":
			handler = this.handlers.radioFormFieldHandler;
			break;
		case "select":
		case "select-one":
		case "select-multiple":
			handler = this.handlers.selectFormFieldHandler;
			break;
		case "button":
			handler = this.handlers.buttonFormFieldHandler;
			break;
		default:
			// hopefully this is appropriate for this field type
			handler = this.handlers.defaultFormFieldHandler;
	}
	return handler;
}

/**
 * Gets the array of values for the specified form field.
 */
CCMSFormUtils.getFieldValues = function(form, fieldName) {
	if (form == null || fieldName == null)
		return;
	else {
		var fields = this.getFields(form, fieldName);
		var values = new Array();
		for (var i in fields) {
			var field = fields[i];
			var handler = this.getFieldHandler(field);
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
CCMSFormUtils.getFieldValue = function(form, fieldName) {
	var values = this.getFieldValues(form, fieldName);
	if (values == null || values.length < 1)
		return null;
	else return values[0];
}

/**
 * Sets the value of the specified form field.
 */
CCMSFormUtils.setFieldValue = function(form, fieldName, value) {
	if (value == null)
		this.clearFieldValue(form, fieldName);
	else if (form == null || fieldName == null)
		return;
	else {
		var fields = this.getFields(form, fieldName);
		// the "ptm.custom_PTM" rule - for parameters that may not already
		// be present in the input form, but need to be added
		if (fields == null || fields.length < 1)
			fields = [{
				name: fieldName,
				form: form
			}];
		for (var i in fields) {
			var field = fields[i];
			var handler = this.getFieldHandler(field);
			if (handler == null)
				return;
			else handler.setFieldValue(field, value);
		}
	}
}

/**
 * Clears the value of the specified form field (i.e. sets it to "null").
 */
CCMSFormUtils.clearFieldValue = function(form, fieldName) {
	if (form == null || fieldName == null || fieldName == "")
		return;
	else {
		var fields = this.getFields(form, fieldName);
		for (var i in fields) {
			var field = fields[i];
			var handler = this.getFieldHandler(field);
			if (handler == null)
				return;
			else handler.clearFieldValue(field);
		}
	}
}

/******************************************************************************
 * Form field handler implementations
 ******************************************************************************/

CCMSFormUtils.handlers = {};

/**
 * Default form field handler for "normal" form fields, i.e. those whose
 * value is accessed directly through the "value" object property, such as
 * plain text boxes.
 */
CCMSFormUtils.handlers.defaultFormFieldHandler = {
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
CCMSFormUtils.handlers.hiddenFormFieldHandler = {
	getFieldValue: CCMSFormUtils.handlers.defaultFormFieldHandler.getFieldValue,
	setFieldValue: CCMSFormUtils.handlers.defaultFormFieldHandler.setFieldValue,
	clearFieldValue: function(field) {}
}

/**
 * Form field handler for standard checkbox form fields.
 */
CCMSFormUtils.handlers.checkboxFormFieldHandler = {
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
		else if (value != null && value.toUpperCase() == "ON")
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
CCMSFormUtils.handlers.radioFormFieldHandler = {
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
CCMSFormUtils.handlers.selectFormFieldHandler = {
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
 * Form field handler for form buttons, which should be static - i.e., they
 * may have a retrievable value, but this value should be immutable.
 */
CCMSFormUtils.handlers.buttonFormFieldHandler = {
	getFieldValue: CCMSFormUtils.handlers.defaultFormFieldHandler.getFieldValue,
	setFieldValue: function(field, value) {},
	clearFieldValue: function(field) {}
}

/**
 * Table registering form fields with their appropriate handler objects.
 * 
 * The purpose of this table is to assign any new, complex form field handler
 * functionality to any parameters needing it.  Any new type of form field
 * requiring special treatment will need to be implemented and registered to
 * all form parameters of its type, in this manner.
 */
CCMSFormUtils.handlersByField = {};
