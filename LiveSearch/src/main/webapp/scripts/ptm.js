/**
 * Form field handler for PTM type selection radio buttons.
 */
var PTMTypeFormFieldHandler = {
	getFieldValue: radioFormFieldHandler.getFieldValue,
	setFieldValue: radioFormFieldHandler.setFieldValue,
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else if (field.value == "opt")
			field.checked = true;
		else field.checked = false;
	}
}

/**
 * Form field handler for custom PTM parameters.
 */
var customPTMFormFieldHandler = {
	getFieldValue: defaultFormFieldHandler.getFieldValue,
	
	setFieldValue: function(field, value) {
		if (field == null || value == null)
			return;
		else {
			var customPTMs = getFields(field.form, field.name);
			for (var i in customPTMs)
				if (customPTMs[i].value == value)
					return;
			var values = value.split(",");
			if (values != null && values.length == 3) {
				setFieldValue(field.form, "ptm_mass", values[0]);
				setFieldValue(field.form, "ptm_residue", values[1]);
				setFieldValue(field.form, "ptm_type", values[2]);
//
var variant = null;
var tool = getFieldValue(field.form, "tool");
if (tool == "MSGFDB")
	variant = "MSGFDB";
else if (tool == "MSALIGN_PLUS")
	variant = "MSAlign";
else if (tool == "MODA")
	variant = "MODa";
else if (tool == "XTANDEM")
	variant = "XTandem";
//
				addPTM(field.form, variant);
			}
		}
	},
	
	clearFieldValue: function(field) {
		if (field == null)
			return;
		else field.parentNode.cells[0].firstChild.onclick();
	}
}

// assign PTM-related form field handlers
formFieldHandlers["ptm_type"] = PTMTypeFormFieldHandler;
formFieldHandlers["ptm.custom_PTM"] = customPTMFormFieldHandler;

var PTM_DISPLAY = {
	fix: "FIXED",
	opt: "OPTIONAL",
	fix_nterm: "FIXED, N-TERMINAL",
	opt_nterm: "OPTIONAL, N-TERMINAL"
}
var floatRegExp = /^[\+\-]?(\d+(\.\d*)?|\.?\d+)$/;
var aminoAcidRegExp = /^(\*|[ACDEFGHIKLMNPQRSTVWY]+)$/;

function addPTM(form, variant) {
	var prefix = "";
	if (variant != null)
		prefix = variant + ".";
	var mass = getFieldValue(form, prefix + "ptm_mass");
	var resd = getFieldValue(form, prefix + "ptm_residue");
	var type = getFieldValue(form, prefix + "ptm_type");
	if (!floatRegExp.test(mass)) {
		alert("Mass must be a real number.");
		form[prefix + "ptm_mass"].focus();
		return;
	}
	if (!aminoAcidRegExp.test(resd)) {
		alert("Residues must be an asterisk or " +
			"a string of amino acid abbreviations.");
		form[prefix + "ptm_residue"].focus();
		return;
	}
	var tbl = document.getElementById(prefix + "PTM_table");
	var row = tbl.insertRow(tbl.rows.length - 1);
	var ctrlCell = row.insertCell(0);
	var massCell = row.insertCell(1);
	var resdCell = row.insertCell(2);
	var typeCell = row.insertCell(3);
	var ctrlButton = document.createElement("img");
	ctrlButton.src = "images/minus.png";
	ctrlButton.className = "selectable"
	ctrlButton.onclick = function() {
		tbl.deleteRow(row.rowIndex);
		return false;
	}
	ctrlCell.appendChild(ctrlButton);
	massCell.innerHTML = mass;
	resdCell.innerHTML = resd;
	typeCell.innerHTML = PTM_DISPLAY[type];
	var param = document.createElement("input");
	param.type = "hidden";
	param.name = "ptm.custom_PTM";
	if (variant == null)
		param.name = "Default." + param.name;
	else param.name = variant + "." + param.name;
	param.value = mass + "," + resd + "," + type;
	row.appendChild(param);
	clearFieldValue(form, prefix + "ptm_mass");
	clearFieldValue(form, prefix + "ptm_residue");
	clearFieldValue(form, prefix + "ptm_type");
	form[prefix + "ptm_mass"].focus();
}
