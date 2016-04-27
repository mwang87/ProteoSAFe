function ProteoSAFeInputModule_ptmTableCVParam(div, id, properties) {
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
ProteoSAFeInputModule_ptmTableCVParam.prototype.knownPTMTypes = {
	fix: "FIXED",
	opt: "VARIABLE"
}

ProteoSAFeInputModule_ptmTableCVParam.prototype.knownPTMs = {
	OXIDATION:               {mass:"+15.994915", residue:"M",   type:"opt", label:"Oxidation"},
	LYSINE_METHYLATION:      {mass:"+14.015650", residue:"K",   type:"opt", label:"Lysine Methylation"},
	PYROGLUTAMATE_FORMATION: {mass:"-17.026549", residue:"Q",   type:"opt", label:"Pyroglutamate Formation"},
	PHOSPHORYLATION:         {mass:"+79.966331", residue:"STY", type:"opt", label:"Phosphorylation"},
	NTERM_CARBAMYLATION:     {mass:"+43.005814", residue:"*",   type:"opt", label:"N-terminal Carbamylation"},
	NTERM_ACETYLATION:       {mass:"+42.010565", residue:"*",   type:"opt", label:"N-terminal Acetylation"},
	DEAMIDATION:             {mass:"+0.984016",  residue:"NQ",  type:"opt", label:"Deamidation"}
}

ProteoSAFeInputModule_ptmTableCVParam.prototype.init = function() {
	// make sure to declare that this module is loaded regardless of the outcome
	try {
		// get the PTM table
		var table = document.getElementById(this.id + "_PTMTableCVParam");
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
	} finally {
		// indicate that this form element has been
		// properly loaded and initialized
		ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
	}
}

ProteoSAFeInputModule_ptmTableCVParam.prototype.isTypeAllowed = function(type) {
	if (type == null)
		return false;
	for (var i=0; i<this.specifiedTypes.length; i++) {
		if (type == this.specifiedTypes[i])
			return true;
	}
	return false;
}

// register this module constructor to indicate that it has been loaded
CCMSForms.modules["ptmTableCVParam"] = ProteoSAFeInputModule_ptmTableCVParam;
