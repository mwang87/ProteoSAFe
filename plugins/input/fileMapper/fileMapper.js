function ProteoSAFeInputModule_fileMapper(div, id, properties) {
	// set argument properties
	this.div = div;
	this.id = id;
	if (properties == null) {
		// TODO: report error
	} else {
		this.spectrumParameter = properties.spectrum_parameter;
		this.resultParameter = properties.result_parameter;
	}
	// initialize other properties
	this.unassignedSpectrumFiles = {};
	this.unassignedResultFiles = {};
	this.assignedFiles = {};
	this.value = "";
}

ProteoSAFeInputModule_fileMapper.prototype.init = function() {
	// set up jQuery selector functionality
	$(function() {
		$(".multipleSelectable").selectableScroll();
		// add custom select handler to only allow
		// one item to be selected at a time
		$(".singleSelectable").selectableScroll({
			selected: function(event, ui) {
				$(ui.selected).addClass("ui-selected").siblings()
				.removeClass("ui-selected").each(
					function(key,value) {
						$(value).find('*').removeClass("ui-selected");
					}
				);
			}
		});
	});
	// set up file population callback
	var mapper = this;
	var getPopulationCallback = function() {
		return function() { mapper.populateFilenames(); };
	};
	document.getElementById(this.id + "_populator").onclick =
		getPopulationCallback();
	// set up file assignment callback
	var getAssignmentCallback = function() {
		return function() {
			// get file selections
			var spectrum = null;
			var results = new Array();
			$(".ui-selected").each(function() {
				var list = $(this).parent().attr("id");
				if (list == mapper.id + "_spectra")
					spectrum = $(this).text();
				else if (list == mapper.id + "_results")
					results.push($(this).text());
			});
			// if a spectrum file has been selected,
			// assign the selected result files to it
			if (spectrum != null) {
				mapper.addAssignments(spectrum, results);
				// render the display of file assignments
				mapper.render();
			}
		};
	};
	document.getElementById(this.id + "_assigner").onclick =
		getAssignmentCallback();
	// register the mapping form field handler to this file selector's field
	CCMSFormUtils.handlersByField[this.id] =
		CCMSFormUtils.handlers.mappingFormFieldHandler;
	// indicate that this form element has been properly loaded and initialized
	ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
}

ProteoSAFeInputModule_fileMapper.prototype.populateFilenames = function(type) {
	// clear all previous mappings
	this.assignedFiles = {};
	// get form parameter values for user-selected spectrum and result files
	var form = CCMSFormUtils.getEnclosingForm(this.div);
	if (form == null ||
		this.spectrumParameter == null || this.resultParameter == null) {
		// TODO: report error
		return;
	}
	var spectra = CCMSFormUtils.getFieldValue(form, this.spectrumParameter);
	if (spectra == null || spectra == "")
		return;
	var results = CCMSFormUtils.getFieldValue(form, this.resultParameter);
	if (results == null || results == "")
		return;
	// query service for pre-mapping selected filenames
	var url = "MapFilenames?source=" + encodeURIComponent(spectra) +
 		"&target=" + encodeURIComponent(results);
	// populate the widget with the returned values
	var mapper = this;
	var complete = function(data) {
		try {
			mapper.unassignedSpectrumFiles = data.spectrum;
			mapper.unassignedResultFiles = data.result;
			mapper.assignedFiles = data.mappings;
		} catch (error) {
			alert(error);
		}
		enableOverlay(document.getElementById(mapper.id + "_overlay"), false);
	};
	// handle any errors that may be returned by the service
	var error = function(request, status, error) {
		var message = "There was a problem reading your \"Peak List\" and " +
			"\"Result\" files into the file assignment interface";
		var serverMessage = extractResponseMessage(request.responseText);
		if (serverMessage == null)
			message += ".";
		else message += ":\n\n" + decodeHTMLEntities(serverMessage);
		alert(message);
		enableOverlay(document.getElementById(mapper.id + "_overlay"), false);
		// clear selected peak and result files
		CCMSFormUtils.clearFieldValue(form, mapper.spectrumParameter);
		CCMSFormUtils.clearFieldValue(form, mapper.resultParameter);
	};
	// start up loading spinner
	document.getElementById(this.id + "_checkmark").style.visibility = "hidden";
	enableOverlay(
		document.getElementById(this.id + "_overlay"), true, true, "100%");
    /* jQuery AJAX request */
	$.ajax({
		type: "GET",
		url: url,
		async: false,
		success: complete,
		error: error
    });
	// render the populated widget
	this.update();
	this.render();
}

/**
 * Returns true if the list of unassigned result files is empty.
 */
ProteoSAFeInputModule_fileMapper.prototype.isMappingComplete = function() {
	// if the result file hash is empty, then assignment is trivially done
	if (isHashEmpty(this.unassignedResultFiles))
		return true;
	// otherwise, check all registered result files
	else for (var file in this.unassignedResultFiles) {
		// if even one file is unassigned, then we're not done
		if (this.unassignedResultFiles[file])
			return false;
	}
	// however, if all of the result files are assigned then we're done
	return true;
}

/**
 * Updates the backing form field for this file mapper widget
 * with the current list of file assignments.
 */
ProteoSAFeInputModule_fileMapper.prototype.update = function() {
	// build parameter value string with all current file assignments
	this.value = "";
	for (var spectrumFile in this.assignedFiles) {
		var assignedResults = this.assignedFiles[spectrumFile];
		if (assignedResults != null && assignedResults.length > 0) {
			this.value += spectrumFile + "|";
			for (var i=0; i<assignedResults.length; i++)
				this.value += assignedResults[i] + ",";
		}
		// chomp trailing comma, replace with semicolon
		if (this.value.length > 1)
			this.value = this.value.substring(0, this.value.length - 1) + ";";
	}
	// update this file mapper's hidden form parameter
	if (this.isMappingComplete())
		this.setFieldValue(this.value);
	else this.setFieldValue("");
}

/**
 * Updates the user interface for this file mapper widget
 * with the current list of file assignments.
 */
ProteoSAFeInputModule_fileMapper.prototype.render = function() {
	// re-populate the spectrum file selector list
	var spectra = document.getElementById(this.id + "_spectra");
	removeChildren(spectra);
	for (var file in this.unassignedSpectrumFiles) {
		if (this.unassignedSpectrumFiles[file]) {
			var li = document.createElement("li");
			li.innerHTML = file;
			spectra.appendChild(li);
		}
	}
	// re-populate the result file selector list
	var results = document.getElementById(this.id + "_results");
	removeChildren(results);
	for (var file in this.unassignedResultFiles) {
		if (this.unassignedResultFiles[file]) {
			var li = document.createElement("li");
			li.innerHTML = file;
			results.appendChild(li);
		}
	}
	// if the result file selector list is empty,
	// show a checkmark to indicate that mapping is done
	var checkmark = document.getElementById(this.id + "_checkmark");
	if (checkmark != null) {
		if (this.isMappingComplete())
			checkmark.style.visibility = "visible";
		else checkmark.style.visibility = "hidden";
	}
	// clear assignment divs
	var div = document.getElementById(this.id + "_assignments");
	removeChildren(div);
	// set up assignment removal function closure generator
	var mapper = this;
	var getClearLinkCallback = function(spectrumFile, resultFile) {
		return function() {
			mapper.clearAssignment(spectrumFile, resultFile);
			mapper.render();
		};
	};
	// re-render assignment divs
	var table = document.createElement("table");
	table.style.border = "2px inset";
	for (var spectrumFile in this.assignedFiles) {
		var resultFiles = this.assignedFiles[spectrumFile];
		for (var i=0; i<resultFiles.length; i++) {
			var resultFile = resultFiles[i];
			var row = document.createElement("tr");
			// render spectrum file
			var spectrum = document.createElement("td");
			spectrum.style.width = "420px";
			spectrum.style.paddingLeft = "10px";
			spectrum.style.backgroundColor = "#E0EEEE";
			spectrum.innerHTML = spectrumFile;
			row.appendChild(spectrum);
			// then render result File
			var result = document.createElement("td");
			result.style.width = "420px";
			result.style.paddingLeft = "10px";
			result.style.backgroundColor = "#E0EEEE";
			result.innerHTML = resultFile;
			row.appendChild(result);
			// finally, add button to remove this row
			var control = document.createElement("td");
			control.style.width = "64px";
			control.style.textAlign = "center";
			control.style.backgroundColor = "#E0EEEE";
			var remover = document.createElement("img");
			remover.src = "images/hide.png";
			remover.style.width = "20px";
			remover.style.height = "auto";
			remover.style.cursor = "pointer";
			remover.onclick = getClearLinkCallback(spectrumFile, resultFile);
			control.appendChild(remover);
			row.appendChild(control);
			// add this assignment row to the table
			table.appendChild(row);
		}
	}
	div.appendChild(table);
}

/**
 * Populates the widget with an encoded parameter value string representing a
 * complete set of file mappings.
 */
ProteoSAFeInputModule_fileMapper.prototype.setAssignments = function(mapping) {
	// first clear all current file assignments
	this.assignedFiles = {};
	// then add the specified mappings, if any
	if (mapping == null) {
		this.render();
		return;
	}
	// split the list of assignments, traverse and add each to the current map
	var assignments = mapping.split(";");
	for (var i=0; i<assignments.length; i++) {
		var assignment = assignments[i].split("|");
		if (assignment == null ||
			(assignment.length == 1 && assignment[0] == ""))	// trailing ";"
			continue;
		else if (assignment.length != 2) {
			// TODO: report error
			continue;
		} else {
			this.unassignedSpectrumFiles[assignment[0]] = true;
			var resultFiles = assignment[1].split(",");
			for (var i=0; i<resultFiles.length; i++)
				this.addAssignment(assignment[0], resultFiles[i]);
		}
	}
	// render the display
	this.render();
}

/**
 * Clears this file mapper widget's current list of file assignments.
 */
ProteoSAFeInputModule_fileMapper.prototype.addAssignment =
function(spectrumFile, resultFile) {
	if (spectrumFile == null || resultFile == null)
		return;
	// get array of result files assigned to this spectrum file
	var resultFiles = this.assignedFiles[spectrumFile];
	if (resultFiles == null)
		resultFiles = new Array();
	// add specified result file to this assignment mapping
	resultFiles.push(resultFile);
	this.assignedFiles[spectrumFile] = resultFiles;
	// remove the newly assigned result file from the selector box
	this.unassignedResultFiles[resultFile] = false;
	// update the backing form parameter
	this.update();
}

ProteoSAFeInputModule_fileMapper.prototype.clearAssignment =
function(spectrumFile, resultFile) {
	if (spectrumFile == null || resultFile == null)
		return;
	// get array of result files assigned to this spectrum file
	var resultFiles = this.assignedFiles[spectrumFile];
	// remove specified result file from this assignment mapping
	if (resultFiles != null && resultFiles.length > 0)
		for (var i=0; i<resultFiles.length; i++)
			if (resultFile == resultFiles[i]) {
				resultFiles.splice(i, 1);
				break;
			}
	// add the newly removed result file back to the selector box
	this.unassignedResultFiles[resultFile] = true;
	// update the backing form parameter
	this.update();
}

ProteoSAFeInputModule_fileMapper.prototype.addAssignments =
function(spectrumFile, resultFiles) {
	if (spectrumFile == null)
		return;
	// add assigned result files
	if (resultFiles != null && resultFiles.length > 0)
		for (var i=0; i<resultFiles.length; i++)
			this.addAssignment(spectrumFile, resultFiles[i]);
	// if nothing was assigned, clear the assignment
	else {
		delete this.assignedFiles[spectrumFile];
		this.update();
	}
}

ProteoSAFeInputModule_fileMapper.prototype.clearAssignments =
function(spectrumFile) {
	this.setAssignments(spectrumFile, null);
}

ProteoSAFeInputModule_fileMapper.prototype.clearAllAssignments = function() {
	for (var spectrumFile in this.assignedFiles)
		this.clearAssignments(spectrumFile);
}

/**
 * Sets the value of this file selector widget's backing form field.
 * This must be done manually, since calling the form field handler
 * registered to this parameter would result in an infinite loop.
 */
ProteoSAFeInputModule_fileMapper.prototype.setFieldValue = function(value) {
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

CCMSForms.modules["fileMapper"] = ProteoSAFeInputModule_fileMapper;

/******************************************************************************
 * File mapper module-related form field handler implementations
 ******************************************************************************/

/**
 * Form field handler for server-side mapping form fields.
 */
CCMSFormUtils.handlers.mappingFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else return field.value;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else {
			var fileMapper =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (fileMapper != null)
				fileMapper.setAssignments(value);
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else {
			var fileMapper =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (fileMapper != null)
				fileMapper.clearAssignments();
		}
	}
}
