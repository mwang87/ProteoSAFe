/**
 * This file contains scripts used by the ProteoSAFe UI administrator page.
 */

function cleanupLabel() {
	// remove previous results, if any
	var resultDiv = document.getElementById("cleanLabelResult");
	resultDiv.innerHTML = "";
	// generate and submit cleanup request
	var request = createRequest();
	request.open("DELETE", "CleanupLabel", true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			// stop in-progress indicator
			enableDiv("cleanLabelProgress", false);
			// if request was good, display results
			if (request.status == 200) {
				resultDiv.innerHTML = request.responseText;
			} else {
				alert(request.responseText);
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(null);
	// start in-progress indicator
	enableDiv("cleanLabelProgress", true);
}

function generateMasses() {
	// remove previous results, if any
	var resultDiv = document.getElementById("generateMassesResult");
	resultDiv.innerHTML = "";
	// generate and submit file generation request
	var request = createRequest();
	request.open("POST", "GenerateMassesFiles", true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			// stop in-progress indicator
			enableDiv("generateMassesProgress", false);
			// if request was good, display results
			if (request.status == 200) {
				resultDiv.innerHTML = request.responseText;
			} else {
				alert(request.responseText);
			}
		}
	}
	// set the proper header information for a POST request
	request.setRequestHeader("Content-type",
		"application/x-www-form-urlencoded");
	request.setRequestHeader("Content-length", 0);
	request.setRequestHeader("Connection", "close");
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(null);
	// start in-progress indicator
	enableDiv("generateMassesProgress", true);
}