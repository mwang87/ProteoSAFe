function ProteoSAFeInputModule_fileSelector(div, id, properties) {
	// set argument properties
	this.div = div;
	this.id = id;
	if (properties != null)
		this.label = properties.label;
	if (this.label == null)
		this.label = "Input Files";
	// initialize other properties
	this.fileSelector = true;
	this.files = new Array();
}

ProteoSAFeInputModule_fileSelector.prototype.init = function() {
	// register the data form field handler to this file selector's field
	CCMSFormUtils.handlersByField[this.id] =
		CCMSFormUtils.handlers.dataFormFieldHandler;
	// indicate that this form element has been properly loaded and initialized
	ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
}

/**
 * Gets the list of currently selected files associated with this file selector.
 */
ProteoSAFeInputModule_fileSelector.prototype.getSelectedFiles = function() {
	if (this.files.length < 1)
		return null;
	var selected = new Array();
	for (var i=0; i<this.files.length; i++)
		selected.push(this.files[i]);
	return selected;
}

/**
 * Adds the provided list of selected files to this file selector widget.
 * 
 * @param selection	a string representing a semicolon-delimited list of
 * 					CCMS-formatted file paths
 */
ProteoSAFeInputModule_fileSelector.prototype.addSelectedFiles = function(
	selection
) {
	if (selection == null)
		return;
	// split the list of files, traverse and add them to the current selection
	var files = selection.split(";");
	for (var i=0; i<files.length; i++) {
		var file = files[i];
		if (file == "")
			continue;
		// if this file is not already selected, add it to the current selection
		else if (this.files.indexOf(file) == -1)
			this.files.push(file);
	}
	// update the form parameter and UI for this file selector
	this.update();
}

/**
 * Populates this file selector widget with the provided list of selected files.
 * 
 * @param selection	a string representing a semicolon-delimited list of
 * 					CCMS-formatted file paths
 */
ProteoSAFeInputModule_fileSelector.prototype.setSelectedFiles = function(
	selection
) {
	// first clear the list of currently selected files
	this.files = new Array();
	// then add the newly selected files to the cleared list
	this.addSelectedFiles(selection);
}

/**
 * Removes the provided list of selected files from this file selector widget.
 * 
 * @param selection	a string representing a semicolon-delimited list of
 * 					CCMS-formatted file paths
 */
ProteoSAFeInputModule_fileSelector.prototype.removeSelectedFiles = function(
	selection
) {
	if (selection == null)
		return;
	// split the list of files, traverse and add them to the current selection
	var files = selection.split(";");
	for (var i=0; i<files.length; i++) {
		var file = files[i];
		if (file == "")
			continue;
		else {
			// if this file is already selected,
			// remove it from the current selection
			var index = this.files.indexOf(file);
			if (index != -1)
				this.files.splice(index, 1);
		}
	}
	// update the form parameter and UI for this file selector
	this.update();
}

/**
 * Clears this file selector widget's current list of selected files.
 */
ProteoSAFeInputModule_fileSelector.prototype.clearSelectedFiles = function() {
	// reset the list of selected files to an empty array
	this.files = new Array();
	// update the form parameter and UI for this file selector
	this.update();
}

/**
 * Updates both the user interface and backing form field for this file
 * selector widget with the current list of selected files.
 */
ProteoSAFeInputModule_fileSelector.prototype.update = function() {
	// build form and UI content for this file selector
	var value = "";
	var folderCount = 0;
	var fileCount = 0;
	var folders = document.createElement("div");
	var files = document.createElement("div");
	var div = null;
	for (var i=0; i<this.files.length; i++) {
		var file = this.files[i];
		// add this file to the parameter value
		value += file + ";";
		// increment count and add to tooltip lists as appropriate
		if (file.charAt(0) == "d") {
			folderCount++;
			div = folders;
		} else {
			fileCount++;
			div = files;
		}
		// update tooltip content
		div.appendChild(document.createTextNode(file.substr(2)));
		div.appendChild(document.createElement("br"));
	}
	// update this file selector's hidden form parameter
	this.setFieldValue(value);
	// populate this file selector's display div
	var name = this.id;
	var display = document.getElementById(name + "_display");
	if (display != null) {
		removeChildren(display);
		var text = fileCount + " file";
		if (fileCount != 1)
			text += "s";
		text += " and " + folderCount + " folder";
		if (folderCount != 1)
			text += "s";
		text += " are selected";
		display.appendChild(document.createTextNode(text));
		// populate this file selector's tooltip div
		var list = document.getElementById(name + "_list");
		if (list != null) {
			removeChildren(list);
			// if there are any selected folders, add them
			if (folderCount > 0) {
				// set up folder div layout
				folders.style.fontSize = "90%";
				// create folder div header
				var header = document.createElement("div");
				header.style.fontWeight = "bold";
				header.style.fontStyle = "italic";
				header.appendChild(document.createTextNode("Folders:"));
				header.appendChild(document.createElement("br"));
				// add folder div to tooltip div
				var folderDiv = document.createElement("div");
				folderDiv.appendChild(header);
				folderDiv.appendChild(folders);
				list.appendChild(folderDiv);
			}
			// if there are any selected files, add them
			if (fileCount > 0) {
				// if there's a folder div above this one, separate them
				if (folderCount > 0)
					list.appendChild(document.createElement("br"));
				// set up file div layout
				files.style.fontSize = "90%";
				// create file div header
				var header = document.createElement("div");
				header.style.fontWeight = "bold";
				header.style.fontStyle = "italic";
				header.appendChild(document.createTextNode("Files:"));
				header.appendChild(document.createElement("br"));
				// add file div to tooltip div
				var fileDiv = document.createElement("div");
				fileDiv.appendChild(header);
				fileDiv.appendChild(files);
				list.appendChild(fileDiv);
			}
			// update display div's tooltip registration
			if (fileCount < 1 && folderCount < 1) {
				display.className = "folderControl";
				display.onmouseover = undefined;
			} else {
				display.className = "folderControl help";
				display.onmouseover = function(event) {
					showTooltip(display, event, "load:" + name + "_list");
				};
			}
		}
	}
}

/**
 * Sets the value of this file selector widget's backing form field.
 * This must be done manually, since calling the form field handler
 * registered to this parameter would result in an infinite loop.
 */
ProteoSAFeInputModule_fileSelector.prototype.setFieldValue = function(value) {
	// get this file selector's enclosing form
	var form = CCMSFormUtils.getEnclosingForm(this.div);
	if (form == null) {
		// TODO: report error, fail
		return;
	}
	// get this file selector's form field
	var field = null;
	var fields = CCMSFormUtils.getFields(form, this.id);
	if (fields == null || fields.length < 1) {
		// TODO: report error, fail
		return;
	} else field = fields[0];
	// manually update the form field
	var handler = CCMSFormUtils.handlers.defaultFormFieldHandler;
	if (value == null)
		handler.clearFieldValue(field);
	else handler.setFieldValue(field, value);
}

// register this module constructor to indicate that it has been loaded
CCMSForms.modules["fileSelector"] = ProteoSAFeInputModule_fileSelector;

/******************************************************************************
 * File selector module-related form field handler implementations
 ******************************************************************************/

/**
 * Form field handler for server-side data form fields.
 */
CCMSFormUtils.handlers.dataFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else return field.value;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else {
			var fileSelector =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (fileSelector != null)
				fileSelector.setSelectedFiles(value);
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else {
			var fileSelector =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (fileSelector != null)
				fileSelector.clearSelectedFiles();
		}
	}
}
