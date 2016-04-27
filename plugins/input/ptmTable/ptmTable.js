function ProteoSAFeInputModule_ptmTable(div, id, properties) {
	// set argument properties
	this.div = div;
	this.id = id;
	this.specifiedTypes = new Array();
	this.defaultType = null;
	if (properties != null) {
		// parse out specified PTM types
		if (properties.types != null) {
			var types = properties.types.split(",");
			for (var i=0; i<types.length; i++) {
				var type = types[i];
				// add the specified PTM type if it is a known type
				if (this.knownPTMTypes[type] != null)
					this.specifiedTypes.push(type);
			}
		}
		// set default type, if specified
		if (properties.defaultType != null)
			this.defaultType = properties.defaultType;
		else if (this.specifiedTypes.length > 0)
			this.defaultType = this.specifiedTypes[0];
	}
}

// TODO: this information should be obtained via web service
ProteoSAFeInputModule_ptmTable.prototype.knownPTMTypes = {
	fix: "FIXED",
	opt: "OPTIONAL",
	fix_nterm: "FIXED, N-TERMINAL",
	opt_nterm: "OPTIONAL, N-TERMINAL"
}

ProteoSAFeInputModule_ptmTable.prototype.knownPTMs = {
	OXIDATION:               {mass:"+15.994915", residue:"M",   type:"opt",       label:"Oxidation"},
	LYSINE_METHYLATION:      {mass:"+14.015650", residue:"K",   type:"opt",       label:"Lysine Methylation"},
	PYROGLUTAMATE_FORMATION: {mass:"-17.026549", residue:"Q",   type:"opt_nterm", label:"Pyroglutamate Formation"},
	PHOSPHORYLATION:         {mass:"+79.966331", residue:"STY", type:"opt",       label:"Phosphorylation"},
	NTERM_CARBAMYLATION:     {mass:"+43.005814", residue:"*",   type:"opt_nterm", label:"N-terminal Carbamylation"},
	NTERM_ACETYLATION:       {mass:"+42.010565", residue:"*",   type:"opt_nterm", label:"N-terminal Acetylation"},
	DEAMIDATION:             {mass:"+0.984016",  residue:"NQ",  type:"opt",       label:"Deamidation"}
}

ProteoSAFeInputModule_ptmTable.prototype.init = function() {
	// make sure to declare that this module is loaded regardless of the outcome
	try {
		// get the PTM table
		var table = document.getElementById(this.id + "_PTMTable");
		if (table == null) {
			// TODO: report error, fail
			return;
		}
		// if no known PTM types are specified, then hide the whole widget
		if (this.specifiedTypes.length < 1) {
			table.style.display = "none";
			return;
		}
		// splice in all relevant fixed PTM (checkbox) fields
		for (var name in this.knownPTMs) {
			var ptm = this.knownPTMs[name];
			// only add this PTM if its type was specified as allowable
			if (this.isTypeAllowed(ptm.type)) {
				var tr = table.insertRow(table.rows.length);
				// add checkbox for this fixed PTM
				var td = tr.insertCell(0);
				var input = document.createElement("input");
				input.type = "checkbox";
				input.name = this.id + "." + name;
				td.appendChild(input);
				td.appendChild(document.createTextNode(" " + ptm.label));
				// add mass, residue and type
				td = tr.insertCell(1);
				td.appendChild(document.createTextNode(ptm.mass));
				td = tr.insertCell(2);
				td.appendChild(document.createTextNode(ptm.residue));
				td = tr.insertCell(3);
				td.appendChild(
					document.createTextNode(this.knownPTMTypes[ptm.type]));
			}
		}
		// set up custom PTM addition UI
		var tr = table.insertRow(table.rows.length);
		// add "plus" button for adding a custom PTM
		var td = tr.insertCell(0);
		var img = document.createElement("img");
		img.src = "images/plus.png";
		img.className = "selectable";
		var parent = this;
		img.onclick = function() {
			parent.addCustomPTM();
		};
		td.appendChild(img);
		// add mass input
		td = tr.insertCell(1);
		var input = document.createElement("input");
		input.type = "text";
		input.size = "8";
		input.name = this.id + "_mass";
		td.appendChild(input);
		// add residue input
		td = tr.insertCell(2);
		input = document.createElement("input");
		input.type = "text";
		input.size = "8";
		input.name = this.id + "_residue";
		td.appendChild(input);
		// add radio button array of PTM types
		td = tr.insertCell(3);
		for (var i=0; i<this.specifiedTypes.length; i++) {
			var type = this.specifiedTypes[i];
			input = document.createElement("input");
			input.type = "radio";
			input.name = this.id + "_type";
			input.value = type;
			if (type == this.defaultType)
				input.checked = true;
			td.appendChild(input);
			td.appendChild(document.createTextNode(this.knownPTMTypes[type]));
			td.appendChild(document.createElement("br"));
		}
		// register the PTM type form field handler
		// to this widget's radio button fields
		CCMSFormUtils.handlersByField[this.id + "_type"] =
			CCMSFormUtils.handlers.PTMTypeFormFieldHandler;
		// register the custom PTM form field handler
		// to this widget's custom parameters
		CCMSFormUtils.handlersByField[this.id + ".custom_PTM"] =
			CCMSFormUtils.handlers.customPTMFormFieldHandler;
	} finally {
		// indicate that this form element has been
		// properly loaded and initialized
		ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
	}
}

ProteoSAFeInputModule_ptmTable.prototype.addCustomPTM = function() {
	// set up validation regular expressions
	var floatRegExp = /^[\+\-]?(\d+(\.\d*)?|\.?\d+)$/;
	var aminoAcidRegExp = /^(\*|[ACDEFGHIKLMNPQRSTVWY]+)$/;
	// get this widget's form
	var form = CCMSFormUtils.getEnclosingForm(this.div);
	if (form == null) {
		// TODO: report error, fail
		return;
	}
	// collect and validate user-specified mass value
	var mass = CCMSFormUtils.getFieldValue(form, this.id + "_mass");
	if (!floatRegExp.test(mass)) {
		alert("Mass must be a real number.");
		form[this.id + "_mass"].focus();
		return;
	}
	// collect and validate user-specified residue value
	var residue = CCMSFormUtils.getFieldValue(form, this.id + "_residue");
	if (!aminoAcidRegExp.test(residue)) {
		alert("Residues must be an asterisk or " +
			"a string of amino acid abbreviations.");
		form[this.id + "_residue"].focus();
		return;
	}
	// collect and validate user-specified PTM type value
	var type = CCMSFormUtils.getFieldValue(form, this.id + "_type");
	if (this.isTypeAllowed(type) == false) {
		alert("Please select a valid Type.");
		return;
	}
	var value = mass + "," + residue + "," + type;
	// once the form inputs are validated, if there is still a problem, then
	// be sure to clear the PTM form inputs regardless of the outcome
	try {
		// only add this custom PTM if it hasn't been added yet
		var customPTMs = CCMSFormUtils.getFields(form, this.id + ".custom_PTM");
		if (customPTMs != null) {
			for (var i=0; i<customPTMs.length; i++)
				if (customPTMs[i].value == value)
					return;
		}
		// get the PTM table
		var table = document.getElementById(this.id + "_PTMTable");
		if (table == null) {
			// TODO: report error, fail
			return;
		}
		// insert the row for this custom PTM into
		// the next-to-last position in the table
		var tr = table.insertRow(table.rows.length - 1);
		// add "minus" button for adding a custom PTM
		var td = tr.insertCell(0);
		var img = document.createElement("img");
		img.src = "images/minus.png";
		img.className = "selectable";
		var callback = this.addCustomPTM;
		img.onclick = function() {
			table.deleteRow(tr.rowIndex);
		};
		td.appendChild(img);
		// add mass, residue and type display cells
		td = tr.insertCell(1);
		td.appendChild(document.createTextNode(mass));
		td = tr.insertCell(2);
		td.appendChild(document.createTextNode(residue));
		td = tr.insertCell(3);
		td.appendChild(document.createTextNode(this.knownPTMTypes[type]));
		// aggregate values into a single hidden input
		var input = document.createElement("input");
		input.type = "hidden";
		input.name = this.id + ".custom_PTM";
		input.value = value;
		tr.appendChild(input);
	} finally {
		// clear the custom inputs 
		CCMSFormUtils.clearFieldValue(form, this.id + "_mass");
		CCMSFormUtils.clearFieldValue(form, this.id + "_residue");
		CCMSFormUtils.clearFieldValue(form, this.id + "_type");
		form[this.id + "_mass"].focus();
	}
}

ProteoSAFeInputModule_ptmTable.prototype.isTypeAllowed = function(type) {
	if (type == null)
		return false;
	for (var i=0; i<this.specifiedTypes.length; i++) {
		if (type == this.specifiedTypes[i])
			return true;
	}
	return false;
}

//register this module constructor to indicate that it has been loaded
CCMSForms.modules["ptmTable"] = ProteoSAFeInputModule_ptmTable;

/******************************************************************************
 * PTM table module-related form field handler implementations
 ******************************************************************************/

/**
 * Form field handler for PTM type selection radio buttons.
 */
CCMSFormUtils.handlers.PTMTypeFormFieldHandler = {
	getFieldValue: CCMSFormUtils.handlers.radioFormFieldHandler.getFieldValue,
	setFieldValue: CCMSFormUtils.handlers.radioFormFieldHandler.setFieldValue,
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		// retrieve the associated module instance
		var moduleName = null;
		var suffixIndex = field.name.indexOf("_type");
		if (suffixIndex < 1) {
			CCMSFormUtils.handlers.radioFormFieldHandler.clearFieldValue(field);
			return;
		} else moduleName = field.name.substring(0, suffixIndex);
		var ptmTable =
			CCMSFormUtils.getModuleInstance(moduleName, field.form.name);
		// if the module instance could not be found, then
		// fall back to standard radio button clearing
		if (ptmTable == null) {
			// TODO: report error
			alert("Could not find the Javascript object corresponding " +
				"to module \"" + moduleName + "\" within form \"" +
				field.form.name + "\".");
			CCMSFormUtils.handlers.radioFormFieldHandler.clearFieldValue(field);
		}
		// otherwise use the module to determine if this is the default value
		else if (field.value == ptmTable.defaultType)
			field.checked = true;
		else field.checked = false;
	}
}

/**
 * Form field handler for custom PTM parameters.
 */
CCMSFormUtils.handlers.customPTMFormFieldHandler = {
	getFieldValue: CCMSFormUtils.handlers.defaultFormFieldHandler.getFieldValue,
	
	setFieldValue: function(field, value) {
		if (field == null || value == null)
			return;
		// retrieve the associated module instance
		var moduleName = null;
		var suffixIndex = field.name.indexOf(".custom_PTM");
		if (suffixIndex < 1) {
			// TODO: report error, fail
			return;
		} else moduleName = field.name.substring(0, suffixIndex);
		var ptmTable =
			CCMSFormUtils.getModuleInstance(moduleName, field.form.name);
		// parse out the PTM values and insert them into the form
		var values = value.split(",");
		// only add this PTM if its type is valid for the current workflow
		if (values != null && values.length == 3 &&
			ptmTable.isTypeAllowed(values[2])) {
			CCMSFormUtils.setFieldValue(
				field.form, moduleName + "_mass", values[0]);
			CCMSFormUtils.setFieldValue(
				field.form, moduleName + "_residue", values[1]);
			CCMSFormUtils.setFieldValue(
				field.form, moduleName + "_type", values[2]);
			// user the module to add this PTM to the form
			ptmTable.addCustomPTM();
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else field.parentNode.cells[0].firstChild.onclick();
	}
}
