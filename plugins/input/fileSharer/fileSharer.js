/**
 * Resource manager module for sharing files from the ProteoSAFe user space
 * with other ProteoSAFe users.
 */
function ProteoSAFeInputModule_fileSharer(div, id) {
	// set argument properties
	this.div = div;
	this.id = id;
	// initialize other properties
	this.form = null;
	this.importForm = null;
}

/**
 * Initializes this fileSharer module
 */
ProteoSAFeInputModule_fileSharer.prototype.init = function() {
	// get sharing form
	this.form = document.forms[this.id + "_sharingForm"];
	// set file sharing button callback
	document.getElementById(this.id + "_sharingButton").onclick =
		this.getShareUserHandler();
	// display initial shared data state
	this.displaySharedUsers();
	// get data import form
	this.importForm = document.forms[this.id + "_importForm"];
	// set data import button callback
	document.getElementById(this.id + "_importButton").onclick =
		this.getImportDataHandler();
	// display initial imported data state
	this.displayImportedData();
}

/**
 * Gets the user-sharing function closure to assign as a callback
 */
ProteoSAFeInputModule_fileSharer.prototype.getShareUserHandler = function() {
	var sharer = this;
	return function() {
		var user = sharer.form.user.value;
		if (user == null || user == "") {
			// TODO: report error
			return;
		}
		var url = "ManageSharing";
		var params = "sharedUser=" + user;
		var request = createRequest();
		request.open("POST", url, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					sharer.displaySharedUsers();
					// clear previous entry
					sharer.form.user.value = "";
				} else alert(request.responseText);
			}
		}
		// set the proper header information for a POST request
		request.setRequestHeader("Content-type",
			"application/x-www-form-urlencoded");
		request.setRequestHeader("Content-length", params.length);
		request.setRequestHeader("Connection", "close");
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(params);
	}
}

/**
 * Gets the user-unsharing function closure to assign as a callback.  This
 * closure will have the username hard-coded to it, so each user row will have
 * its own unique callback closure.
 */
ProteoSAFeInputModule_fileSharer.prototype.getUnshareUserHandler =
function(user) {
	var sharer = this;
	return function() {
		if (user == null || user == "") {
			// TODO: report error
			return;
		}
		var url = "ManageSharing";
		var params = "sharedUser=" + user;
		var request = createRequest();
		request.open("DELETE", url + "?" + params, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					sharer.displaySharedUsers();
				} else alert(request.responseText);
			}
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

/**
 * Refreshes the display of shared users
 */
ProteoSAFeInputModule_fileSharer.prototype.displaySharedUsers = function() {
	var sharer = this;
	var sharedUsers = document.getElementById(this.id + "_sharedUsers");
	var request = createRequest();
    var url = "ManageSharing";
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
			if (request.status == 200) {
				removeChildren(sharedUsers);
				var sharedUserSet =
					request.responseXML.getElementsByTagName("shared");
				for (var i=0; i<sharedUserSet.length; i++) {
					var sharedUser = sharedUserSet[i].firstChild.nodeValue;
					// add unshare button
					var unshare = document.createElement("img");
					unshare.className = "selectable";
					unshare.src = "images/hide.png";
					unshare.onclick = sharer.getUnshareUserHandler(sharedUser);
					unshare.height = "16";
					unshare.width = "16";
					unshare.style.verticalAlign = "middle";
					unshare.style.marginRight = "3px";
					// add user row to display
					var div = document.createElement("div");
					div.appendChild(unshare);
					div.appendChild(document.createTextNode(sharedUser)); 
					sharedUsers.appendChild(div);
				}
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
    request.send(null);
}

/**
 * Gets the data import function closure to assign as a callback
 */
ProteoSAFeInputModule_fileSharer.prototype.getImportDataHandler =
function() {
	var sharer = this;
	return function() {
		var user = sharer.importForm.user.value;
		if (user == null || user == "") {
			// TODO: report error
			return;
		}
		// parse dataset ID
		var url = "ManageSharing";
		var params = "importUser=" + user;
		var request = createRequest();
		request.open("POST", url, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					sharer.displayImportedData();
					// clear previous entry
					sharer.importForm.user.value = "";
				} else if (request.status == 403) {
					alert("Sorry, \"" + user +
						"\" is not an available data share to import.");
					sharer.importForm.user.value = "";
				} else alert(request.responseText);
			}
		}
		// set the proper header information for a POST request
		request.setRequestHeader("Content-type",
			"application/x-www-form-urlencoded");
		request.setRequestHeader("Content-length", params.length);
		request.setRequestHeader("Connection", "close");
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(params);
	}
}

/**
 * Gets the data-unimporting function closure to assign as a callback.  This
 * closure will have the username hard-coded to it, so each user row will have
 * its own unique callback closure.
 */
ProteoSAFeInputModule_fileSharer.prototype.getUnimportDataHandler =
function(user) {
	var sharer = this;
	return function() {
		if (user == null || user == "") {
			// TODO: report error
			return;
		}
		var url = "ManageSharing";
		var params = "importUser=" + user;
		var request = createRequest();
		request.open("DELETE", url + "?" + params, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
				if (request.status == 200) {
					sharer.displayImportedData();
				} else alert(request.responseText);
			}
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

/**
 * Refreshes the display of imported users
 */
ProteoSAFeInputModule_fileSharer.prototype.displayImportedData = function() {
	var sharer = this;
	var importedUsers = document.getElementById(this.id + "_importedUsers");
	var request = createRequest();
    var url = "ManageSharing";
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
			if (request.status == 200) {
				removeChildren(importedUsers);
				var importedUserSet =
					request.responseXML.getElementsByTagName("accessible");
				for (var i=0; i<importedUserSet.length; i++) {
					var importedUser = importedUserSet[i].getAttribute("id");
					var importedUserLabel =
						importedUserSet[i].firstChild.nodeValue;
					// add unimport button
					var unimport = document.createElement("img");
					unimport.className = "selectable";
					unimport.src = "images/hide.png";
					unimport.onclick =
						sharer.getUnimportDataHandler(importedUser);
					unimport.height = "16";
					unimport.width = "16";
					unimport.style.verticalAlign = "middle";
					unimport.style.marginRight = "3px";
					// add user row to display
					var div = document.createElement("div");
					div.appendChild(unimport);
					div.appendChild(
						document.createTextNode(importedUserLabel)); 
					importedUsers.appendChild(div);
				}
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
    request.send(null);
}

CCMSForms.modules["fileSharer"] = ProteoSAFeInputModule_fileSharer;
