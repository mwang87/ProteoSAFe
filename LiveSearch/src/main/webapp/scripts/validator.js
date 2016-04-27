function validateForm(form) {
    var integerRegExp = /^[\+\-]?\d+$/;
	var floatRegExp = /^[\+\-]?(\d+(\.\d*)?|\.?\d+)$/;
	var expRegExp = /^[\+\-]?(\d+(\.\d*)?|\.?\d+)([eE][\-\+]?\d+)$/;
    var emailRegExp = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;
    
    // description parameter
    var desc = form["desc"];
    if (desc != null)
    	desc = desc.value;
    else desc = "";
    if (desc.length > 100) {
    	alert("'Description' cannot contain more than 100 characters.");
        return false;
    }
	
	// spectrum parameter
    var spec = form["spectrum"];
    if (spec != null)
    	spec = spec.value;
    else spec = "";
    var spec_on_server = form["spec_on_server"];
    if (spec_on_server != null)
    	spec_on_server = spec_on_server.value;
    else spec_on_server = "";
    if (spec == "" && spec_on_server == "") {
        alert('Please provide spectrum files.');
        return false;
    }
    
    // email parameter
	var email = form["email"].value;
	if (!emailRegExp.test(email)) {
		alert('Empty or invalid email address');
		form["email"].focus();
		return false;
	}
    
    // MS-Align parameters
    var tool = form["tool"].value;
    if (tool == "MSALIGN" || tool == "MSALIGN-CONVEY") {
	    var minmms = form["Default.msalign.minmodmass"].value;
	    var maxmms = form["Default.msalign.maxmodmass"].value;
    	if (floatRegExp.test(minmms) && floatRegExp.test(maxmms)) {
			var min = parseFloat(minmms);
			var max = parseFloat(maxmms);
			if (minmms< -200 || maxmms > 200 || minmms > maxmms) {
				alert('Mass Modification should be between -200 and 200.');
				form["Default.msalign.minmodmass"].focus();
				return false;
			}
    	} else {
			alert('Mass Modification should be real numbers.');
			form["Default.msalign.minmodmass"].focus();
			return false;
		}
	}
	
	// PepNovo parameters
	if (tool == "PEPNOVO") {
    	var instrument = form["Default.instrument.instrument"].value;
    	if (instrument != 'ESI-ION-TRAP') {
    		alert('Instrument must be ESI-ION-TRAP in PepNovo search');
    		return false;
    	}
	    var pepnrsol = form["Default.pepnovo.pepnrsol"].value;
	    var msg = null;
	    if (integerRegExp.test(pepnrsol)) {
	    	var v = parseInt(pepnrsol);
	    	if (v < 0 || v > 2000) {
	    		alert('Number of desired solutions should be between 1 and 2000.');
	        	form["Default.pepnovo.pepnrsol"].focus();
	        	return false;
	    	}
		} else {
			alert('Number of desired solutions should be integral.');
			form["Default.pepnovo.pepnrsol"].focus();
			return false;
		}
	}
	
	// tolerances
    if (tool == "INSPECT" || tool == "MSALIGN" || tool == "MSALIGN-CONVEY" ||
    	tool == "PEPNOVO" || tool == "PROTEOGENOMICS") {
		// parent mass tolerance parameter
    	var pmtolStr = form["Default.tolerance.PM_tolerance"].value;
    	if (floatRegExp.test(pmtolStr)) {
			var pmtol = parseFloat(pmtolStr);
			if (pmtol < 0 || pmtol > 2.5) {
				alert('Parent Mass Tolerance should be between 0 and 2.5.');
				form["Default.tolerance.PM_tolerance"].focus();
				return false;
			}
    	} else {
			alert('Parent Mass Tolerance should be a real number.');
			form["Default.tolerance.PM_tolerance"].focus();
			return false;
		}
		
		// ion tolerance parameter
    	var itolStr = form["Default.tolerance.Ion_tolerance"].value;
    	if (floatRegExp.test(itolStr)) {
			var itol = parseFloat(itolStr);
			if (itol <= 0 || itol > 1) {
				alert('Ion Tolerance should be positive and less than 1');
				form["Default.tolerance.Ion_tolerance"].focus();
				return false;
			}
    	} else {
        	alert('Ion Tolerance should be a real number.');
        	form["Default.tolerance.Ion_tolerance"].focus();
        	return false;
		}
	}
	
	// maximum number of ptms parameter
	if (tool == "INSPECT" || tool == "PEPNOVO" || tool == "PROTEOGENOMICS") {
    	var modsStr = form["Default.ptm.mods"].value;
    	if (integerRegExp.test(modsStr)) {
			var m = parseInt(modsStr);
			if (m < 0 || m > 3) {
				alert('Mods should be between 0 and 3.');
				form["Default.ptm.mods"].focus();
				return false;
			}
    	} else {
        	alert('Mods should be integral.');
        	form["Default.ptm.mods"].focus();
        	return false;
		}
	}
	
	// database parameter
    var db = form["Default.db.DB"].value;
    if ((tool == "INSPECT" || tool == "MSALIGN" || tool == "MSALIGN-CONVEY") &&
    	db == "None") {
    	var otherFiles = form["Default.db.sequence_file"];
        if (otherFiles != null)
        	otherFiles = otherFiles.value;
        else otherFiles = "";
    	var seq_on_server = form["seq_on_server"];
        if (seq_on_server != null)
        	seq_on_server = seq_on_server.value;
        else seq_on_server = "";
    	if (otherFiles == "" && seq_on_server == "") {
    		alert('Please provide sequence files or specify a sequence database.');
    		form["Default.db.DB"].focus();
    		return false;
    	}
    }
    
    // Proteogenomics parameters
    if (tool == "PROTEOGENOMICS") {
    	var organism = form["Proteogenomics.db.DB"].value;
    	if (organism == null || organism == "None") {
			alert('Please specify an organism.');
			form["Proteogenomics.db.DB"].focus();
			return false;
    	}
    }
	
	// filter parameters
	var filter = form["Default.filter.filter"];
	var filterValue = null;
	var filterLength = filter.length;
	if (filterLength == null)
		filterValue = filter.value;
	else for (var i=0; i<filterLength; i++) {
		if (filter[i].checked) {
			filterValue = filter[i].value;
			break;
		}
	}
    if (tool == "INSPECT" || tool == "MSALIGN" || tool == "MSALIGN-CONVEY") {
    	if (filterValue == "FDR") {
    		var fdr = form["Default.FDR.FDR"].value;
    		if (floatRegExp.test(fdr) || expRegExp.test(fdr)) {
				var fFdr = parseFloat(fdr);
				if (fFdr < 0 || fFdr > 1) {
					alert("Spectrum-Level FDR should be between 0 and 1.");
					form["Default.FDR.FDR"].focus();
					return false;
				}
   			} else {
        		alert("FDR should be a real number.");
        		form["Default.FDR.FDR"].focus();
        		return false;
			}
    	} else if (filterValue == "PepFDR") {
    		var pepFdr = form["Default.PepFDR.PepFDR"].value;
    		if (floatRegExp.test(pepFdr) || expRegExp.test(pepFdr)) {
				var fPepFdr = parseFloat(pepFdr);
				if (fPepFdr < 0 || fPepFdr > 1) {
					alert("Peptide-Level FDR should be between 0 and 1.");
					form["Default.PepFDR.PepFDR"].focus();
					return false;
				}
   			} else {
        		alert("Peptide-Level FDR should be a real number.");
        		form["Default.PepFDR.PepFDR"].focus();
        		return false;
			}
    	} else if (filterValue == "FPR") {
    		var fpr = form["Default.FPR.FPR"].value;
    		if (floatRegExp.test(fpr) || expRegExp.test(fpr)) {
				var fFpr = parseFloat(fpr);
				if (fFpr < 0 || fFpr > 1) {
					alert("FPR should be between 0 and 1.");
					form["Default.FPR.FPR"].focus();
					return false;
				}
   			} else {
        		alert("FPR should be a real number.");
        		form["Default.FPR.FPR"].focus();
        		return false;
			}
    	} else if (filterValue == "ModFDR") {
    		var modFdr = form["Default.ModFDR.ModFDR"].value;
    		if (floatRegExp.test(modFdr) || expRegExp.test(modFdr)) {
				var fModFdr = parseFloat(modFdr);
				if (fModFdr < 0 || fModFdr > 1) {
					alert("Mod-FDR should be between 0 and 1");
					form["Default.ModFDR.ModFDR"].focus();
					return false;
				}
   			} else {
        		alert("Mod-FDR should be a real number.");
        		form["Default.ModFDR.ModFDR"].focus();
        		return false;
			}
    	} else {
    		alert("Please select either " +
    			"Spectrum-Level FDR, Peptide-Level FDR, or FPR.");
    		return false;
    	}
    }
	
	return true;
}
