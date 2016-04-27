function ProteoSAFeInputModule_multipleSelectCVParam(
	div, id, properties, parameters
) {
	// set argument properties
	this.div = div;
	this.id = id;
	// set this module's form
	this.form = CCMSFormUtils.getEnclosingForm(this.div);
	// set this select input's options
	this.options = null;
	if (parameters != null) {
		var parameter = parameters[this.id];
		if (parameter != null)
			this.options = parameter.options;
	}
	// set any text properties for this select input
	this.properties = {};
	if (properties != null) {
		for (var property in properties)
			this.properties[property] = properties[property];
	}
	// initialize selections property
	this.selectedValues = new Array();
	// initialize display parameters
	this.shortDesc = true;
	// initialize CV param and resource label format regular expressions
	this.paramFormat =
		/^\[([^,]*),\s*([^,]*),\s*\"?([^\"]*)\"?,\s*\"?([^\"]*)\"?\]$/;
	this.resourceLabelFormat = /\[[^\[\]]*\]\s*\[[^\[\]]*\]\s*([^\|]*)\|?.*/;
	// initialize the set of known "generic" items,
	// i.e. items that can be added more than once
	this.genericItems = ["MS:1001460"];
}

ProteoSAFeInputModule_multipleSelectCVParam.prototype.init = function() {
	var selector = this;
	// retrieve the input box
	var input = document.getElementById(this.id + "_input");
	// no loading needs to occur if there are no select options
	if (this.options == null) {
		input.type = "text";
		ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
	}
	// only build the select input if options are specified
	else {
		// if the options are dynamic, fetch them
		if (this.options["_RESOURCE"] != null) {
			ProteoSAFeInputUtils.downloadResourceMap(
				this.options["_RESOURCE"],
				function(map) {
					var tags = new Array();
					for (var value in map) {
						var content = map[value];
						var label = content;
						// extract any descriptive text from the official label
						var description = null;
						var pipe = content.indexOf("|");
						if (pipe != null && pipe >= 0) {
							label = content.substring(0, pipe);
							description = content.substring(pipe + 1);
							// note if any of the found descriptions are long
							if (description.length > 60)
								selector.shortDesc = false;
						}
						// extract the CV term's name, if present
						var name = "";
						var matches =
							selector.resourceLabelFormat.exec(label);
						if (matches != null && matches.length != null &&
							matches.length > 1)
							name = matches[1];
						else name = label;
						// first add option to options map for later reference
						selector.options[value] = content;
						// then add the option to the tags list
						tags.push({
							value: value,
							name: name,
							label: content,
							desc: description
						});
					}
					// load tags into the input box
					selector.initWidget(tags);
					// indicate that this form element has been
					// properly loaded and initialized
					ProteoSAFeInputUtils.setAsynchronousElementLoaded(
						input, selector.id);
				}
			);
		}
		// if the options come from a web service, simply pass the name
		// of that service to the underlying widget constructor
		else if (this.options["_SERVICE"] != null) {
			this.initWidget(this.options["_SERVICE"]);
			ProteoSAFeInputUtils.setAsynchronousElementLoaded(input, this.id);
		}
		// otherwise write them immediately
		else {
			var tags = new Array();
			for (var value in this.options)
				tags.push({value: value, label: this.options[value]});
			// load tags into the input box
			this.initWidget(tags);
			// indicate that this form element has been
			// properly loaded and initialized
			ProteoSAFeInputUtils.setAsynchronousElementLoaded(input, this.id);
		}
	}
	// set the proper callback for the "Add" button
	var button = document.getElementById(this.id + "_add");
	button.onclick = function() {
		selector.add();
	}
	// register the multiple select form field handler
	// to this multiple selector's field
	CCMSFormUtils.handlersByField[this.id] =
		CCMSFormUtils.handlers.multipleSelectFormFieldHandler;
}

/**
 * Initializes the jQuery autocomplete widget for this module
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.initWidget =
function(dataSource) {
	// escape dots in ID, so jQuery won't think they're CSS class selectors
	var dot = new RegExp("\\.", "g");
	var inputId = (this.id + "_input").replace(dot, "\\.");
	// load data into the input box
	var selector = this;
	$("#" + inputId).autocomplete({
		source: dataSource,
		minLength: 3,
		select: function(event, ui) {
			selector.add(ui.item);
	        return false;
		}
	});
	// extend drop-down item renderer to show description
	$("#" + inputId).data("ui-autocomplete")._renderItem =
	function(ul, item) {
		// extract real label, stripping off description, if any
		var label = item.label;
		var pipe = label.indexOf("|");
		if (pipe != null && pipe >= 0)
			label = label.substring(0, pipe);
		var content =
			"<a><span style=\"font-weight:bold\">" + label + "</span>";
		// display the description conditionally,
		// based on its length
		if (item.desc != null) {
			if (selector.shortDesc)
				content += " (" + item.desc + ")";
			else {
				content +=
					"<br/><span style=\"font-size:75%\">&nbsp;&nbsp;&nbsp;";
				var tokens = item.desc.split("|");
				for (var i=0; i<tokens.length; i++) {
					// put the first token on its own line
					if (i == 0)
						content += "<span style=\"font-style:italic\">" +
							tokens[i] + "</span>";
					// put all following tokens on their own line
					else if (i == 1)
						content += "<br/>&nbsp;&nbsp;&nbsp;" + tokens[i];
					else content += " ; " + tokens[i];
				}
				content += "</span>";
			}
		}
		content += "</a>";
		return $("<li>").append(content).appendTo(ul);
	};
}

/**
 * Clears the auto-complete input field for this module.
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.clear = function() {
	$("input[name='" + this.id + "_input']").val("");
}

/**
 * Clears all selections for this module.
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.clearSelections =
function() {
	this.setSelections(null);
}

/**
 * Assigns the selected parameter value to the list of selected values.
 * For CV terms, the value should be a pipe-delimited ("|") list of CV param
 * tuples in the form [cvLabel, accession, name, value]
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.setSelections =
function(value) {
	this.selectedValues = new Array();
	if (value != null && value != "") {
		var values = value.split("|");
		for (var i=0; i<values.length; i++) {
			var record = this.parseCVParam(values[i]);
			if (record != null)
				this.selectedValues.push(record);
		}
	}
	// render the selections list
	this.renderSelections();
	// clear current selection input
	this.clear();
}

/**
 * Adds the current value of the text field to the list of selected values.
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.add = function(item) {
	// extract value from selected item
	var value = {value:""};
	// if the item is a hash with a "value" property, then it came from the CV,
	// and should be parseable into label, accession and name
	if (item != null && item.value != null) {
		var tokens = item.value.split(":");
		if (tokens != null && tokens.length > 1) {
			value.label = tokens[0];
			value.accession = item.value;
			value.name = item.name;
			if (value.name == null)
				value.name = "";
		}
	}
	// otherwise, it's just custom free text from the text field
	if (value == null || value.accession == null) {
		value.label = "";
		value.accession = "";
		value.name =
			CCMSFormUtils.getFieldValue(this.form, this.id + "_input");
	}
	// if this selection came from the CV, ensure
	// that it is present in the widget's options
	else if (item != null && item.label != null) {
		if (this.options == null)
			this.options = {};
		if (this.options[value.accession] == null) {
			var content = item.label;
			// if a description is present, append it to the option content
			if (item.desc != null)
				content += "|" + item.desc;
			this.options[value.accession] = content;
		}
	}
	// if there is neither an accession nor a name specified,
	// then nothing was typed, so do nothing
	if (value == null || ((value.accession == null || value.accession == "") &&
		(value.name == null || value.name == "")))
		return;
	// if this item is a "generic" item, then it can be added even if another
	// item with the same ID is already present in the selections array
	var generic = false;
	if (value.accession != null && value.accession != "") {
		for (var i=0; i<this.genericItems.length; i++) {
			if (this.genericItems[i] == value.accession) {
				generic = true;
				break;
			}
		}
	}
	// if the item is not generic, then refuse it if it is already present
	if (generic == false) {
		for (var i=0; i<this.selectedValues.length; i++) {
			if (this.selectedValues[i].name == value.name) {
				this.clear();
				return;
			}
		}
	}
	// if this value is not yet present, add it
	this.selectedValues.push(value);
	// render the selections list
	this.renderSelections();
	// clear current selection input
	this.clear();
}

/**
 * Removes the indicated value from the list of selected values
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.remove =
function(record) {
	if (record == null)
		return;
	// find this value in the selections array
	var index = -1;
	for (var i=0; i<this.selectedValues.length; i++) {
		if (this.selectedValues[i] == record) {
			index = i;
			break;
		}
	}
	// splice out the value from the selections array
	this.selectedValues.splice(index, 1);
	// render the selections list
	this.renderSelections();
}

/**
 * Renders the current selections list.
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.renderSelections =
function() {
	// retrieve the selections list div
	var div = document.getElementById(this.id + "_selections");
	// clear any previous rendering of the selections list
	removeChildren(div);
	// set up selection removal function closure generator
	var getRemover = function(selector, record) {
		return function() { selector.remove(record); }
	};
	// set up value assignment function closure generator
	var getValueAssigner = function(selector, record) {
		return function() {
			var text = this.value;
			if (text == null)
				text = "";
			record.value = text;
			selector.renderSelections();
		}
	};
	var selector = this;
	// render all current selections
	var list = document.createElement("table");
	list.style.border = "2px inset";
	// build running text value for the actual server parameter
	var parameterValue = "";
	for (var i=0; i<this.selectedValues.length; i++) {
		var currentValue = this.selectedValues[i];
		// add current value to the hidden parameter
		parameterValue += this.printCVParam(currentValue) + "|";
		var selection = document.createElement("tr");
		// render label first
		var label = document.createElement("td");
		label.style.width = "280px";
		label.style.paddingLeft = "10px";
		label.style.backgroundColor = "#E0EEEE";
		// try a few different sources for the label text
		var labelText = null;
		if (this.options != null)
			labelText = this.options[currentValue.accession];
		if (labelText == null && this.properties != null)
			labelText = this.properties["custom_label"];
		if (labelText == null)
			labelText = "Custom user input";
		// extract real label, stripping off description, if any
		var content = labelText;
		var pipe = content.indexOf("|");
		if (pipe != null && pipe >= 0) {
			labelText = content.substring(0, pipe);
			if (this.shortDesc)
				labelText += " (" + content.substring(pipe + 1) + ")";
		}
		label.innerHTML = labelText;
		selection.appendChild(label);
		// then render name
		var name = document.createElement("td");
		name.style.width = "115px";
		name.style.paddingLeft = "10px";
		name.style.backgroundColor = "#E0EEEE";
		// rendered label should be the accession,
		// if present, but if not then the value
		content = currentValue.accession;
		if (content == null || content == "")
			content = currentValue.name;
		name.innerHTML = content;
		selection.appendChild(name);
		// then render value assignment text field
		var value = document.createElement("td");
		value.style.width = "64px";
		value.style.paddingLeft = "10px";
		value.style.backgroundColor = "#E0EEEE";
		var assigner = document.createElement("input");
		assigner.type = "text";
		assigner.style.width = "50px";
		assigner.value = currentValue.value;
		assigner.onchange =  getValueAssigner(selector, currentValue);
		value.appendChild(assigner);
		selection.appendChild(value);
		// finally, add button to remove this selection
		var control = document.createElement("td");
		control.style.width = "64px";
		control.style.textAlign = "center";
		control.style.backgroundColor = "#E0EEEE";
		var remover = document.createElement("img");
		remover.src = "images/hide.png";
		remover.style.width = "20px";
		remover.style.height = "auto";
		remover.style.cursor = "pointer";
		remover.onclick = getRemover(selector, currentValue);
		control.appendChild(remover);
		selection.appendChild(control);
		// add this selection row to the list table
		list.appendChild(selection);
	}
	// write final parameter value to the hidden parameter
	if (parameterValue.length > 0)
		parameterValue = parameterValue.substring(0, parameterValue.length-1);
	$("input[name='" + this.id + "']").val(parameterValue);
	// render visible table
	div.appendChild(list);
}

/**
 * Parses the given CV param string into a populated object.  Returns null
 * if the argument string does not conform to the expected CV param format of
 * [cvLabel, accession, name, value]
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.parseCVParam =
function(cvParam) {
	if (cvParam == null)
		return null;
	var matches = this.paramFormat.exec(cvParam);
	if (matches == null || matches.length == null || matches.length < 1)
		return null;
	var record = {};
	for (var i=1; i<matches.length; i++) {
		switch (i) {
			case 1: record.label = matches[i]; break;
			case 2: record.accession = matches[i]; break;
			case 3: record.name = matches[i]; break;
			case 4: record.value = matches[i]; break;
		}
	}
	return record;
}

/**
 * Prints the given populated object into a CV param string.
 */
ProteoSAFeInputModule_multipleSelectCVParam.prototype.printCVParam =
function(record) {
	if (record == null)
		return null;
	var cvParam = "[";
	var label = record.label;
	if (label == null)
		label = "";
	var accession = record.accession;
	if (accession == null)
		accession = "";
	var name = record.name;
	if (name == null)
		name = "";
	var value = record.value;
	if (value == null)
		value = "";
	cvParam += label + "," + accession + ","
	// enclose "name" in quotation marks, if it contains a comma
	if (name.indexOf != null && name.indexOf(",") >= 0)
		cvParam += "\"" + name + "\",";
	else cvParam += name + ",";
	// enclose "value" in quotation marks, if it contains a comma
	if (value.indexOf != null && value.indexOf(",") >= 0)
		cvParam += "\"" + value + "\"]";
	else cvParam += value + "]";
	return cvParam;
}

CCMSForms.modules["multipleSelectCVParam"] =
	ProteoSAFeInputModule_multipleSelectCVParam;

/******************************************************************************
 * Multiple select module-related form field handler implementations
 ******************************************************************************/

/**
 * Form field handler for multiple select form fields.
 */
CCMSFormUtils.handlers.multipleSelectFormFieldHandler = {
	getFieldValue: function(field) {
		if (field == null)
			return;
		else return field.value;
	},
	
	setFieldValue: function(field, value) {
		if (field == null)
			return;
		else {
			var multipleSelector =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (multipleSelector != null)
				multipleSelector.setSelections(value);
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else {
			var multipleSelector =
				CCMSFormUtils.getModuleInstance(field.name, field.form.name);
			if (multipleSelector != null)
				multipleSelector.clearSelections();
		}
	}
}
