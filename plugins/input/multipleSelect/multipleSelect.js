function ProteoSAFeInputModule_multipleSelect(div, id, properties, parameters) {
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
}

ProteoSAFeInputModule_multipleSelect.prototype.init = function() {
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
						// extract any descriptive text from the official label
						var content = map[value];
						var label = content;
						var description = null;
						var pipe = content.indexOf("|");
						if (pipe != null && pipe >= 0) {
							description = content.substring(pipe + 1);
							// note if any of the found descriptions are long
							if (description.length > 60)
								selector.shortDesc = false;
						}
						// first add option to options map for later reference
						selector.options[value] = label;
						// then add the option to the tags list
						tags.push({
							value: value,
							label: label,
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
ProteoSAFeInputModule_multipleSelect.prototype.initWidget = function(dataSource) {
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
ProteoSAFeInputModule_multipleSelect.prototype.clear = function() {
	$("input[name='" + this.id + "_input']").val("");
}

/**
 * Clears all selections for this module.
 */
ProteoSAFeInputModule_multipleSelect.prototype.clearSelections = function() {
	this.setSelections(null);
}

/**
 * Assigns the selected parameter value to the list of selected values.
 */
ProteoSAFeInputModule_multipleSelect.prototype.setSelections = function(value) {
	this.selectedValues = new Array();
	if (value != null && value != "") {
		// if it's a string, then treat it as a semicolon-delimited value list
		if (typeof value == 'string' || value instanceof String) {
			var values = value.split(";");
			for (var i=0; i<values.length; i++)
				this.selectedValues.push(values[i]);
		}
		// if it's an array, treat it as a list of "item" hashes
		// (members: "value", "label", "desc")
		else if (value instanceof Array) {
			for (var i=0; i<value.length; i++) {
				var item = value[i];
				// add this value to the list of selected values
				if (item != null && item.value != null && item.value != "") {
					this.selectedValues.push(item.value);
					// if this selection came from the CV,
					// ensure that it is present in the widget's options
					if (item.label != null && item.label != "") {
						if (this.options == null)
							this.options = {};
						if (this.options[item.value] == null) {
							// if a description is present,
							// append it to the option content
							var content = item.label;
							if (item.desc != null)
								content += "|" + item.desc;
							this.options[item.value] = content;
						}
					}
				}
			}
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
ProteoSAFeInputModule_multipleSelect.prototype.add = function(item) {
	// extract value from selected item; the passed object may be a hash
	// with a string "value" property, or it may just be a string itself
	var value = null;
	if (item != null && item.value != null)
			value = item.value;
	else value = item;
	// if no proper CV term was typed, just grab the literal text
	if (value == null || value == "")
		value = CCMSFormUtils.getFieldValue(this.form, this.id + "_input");
	// otherwise, if this selection came from the CV,
	// ensure that it is present in the widget's options
	else if (item != null && item.label != null) {
		if (this.options == null)
			this.options = {};
		if (this.options[value] == null) {
			// if a description is present, append it to the option content
			var content = item.label;
			if (item.desc != null)
				content += "|" + item.desc;
			this.options[value] = content;
		}
	}
	// if no value could be resolved, then nothing was typed, so do nothing
	if (value == null || value == "")
		return;
	// verify that this value is not already present in the selections array
	for (var i=0; i<this.selectedValues.length; i++) {
		if (this.selectedValues[i] == value) {
			this.clear();
			return;
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
ProteoSAFeInputModule_multipleSelect.prototype.remove = function(value) {
	if (value == null)
		return;
	// find this value in the selections array
	var index = -1;
	for (var i=0; i<this.selectedValues.length; i++) {
		if (this.selectedValues[i] == value) {
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
ProteoSAFeInputModule_multipleSelect.prototype.renderSelections = function() {
	// retrieve the selections list div
	var div = document.getElementById(this.id + "_selections");
	// clear any previous rendering of the selections list
	removeChildren(div);
	// set up selection removal function closure generator
	var getRemover = function(selector, value) {
		return function() { selector.remove(value); }
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
		parameterValue += currentValue + ";";
		var selection = document.createElement("tr");
		// render label first
		var label = document.createElement("td");
		label.style.width = "280px";
		label.style.paddingLeft = "10px";
		label.style.backgroundColor = "#E0EEEE";
		// try a few different sources for the label text
		var labelText = null;
		if (this.options != null)
			labelText = this.options[currentValue];
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
		// then render value
		var value = document.createElement("td");
		value.style.width = "115px";
		value.style.paddingLeft = "10px";
		value.style.backgroundColor = "#E0EEEE";
		value.innerHTML = currentValue;
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

CCMSForms.modules["multipleSelect"] = ProteoSAFeInputModule_multipleSelect;

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
