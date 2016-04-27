function createDummyFile(file_name, folder_group) {
	if (file_name == null || file_name == "" || folder_group == null)
		return;
	else {
		var url = "file_manager.jsp?action=dummy&name=" + file_name;
	    var folder = null;
	    try {
			folder = folder_group.getTargetFolder();
	    } catch (error) {}
		if (folder != null)
			url += "&folder=" + folder;
		sendRequest(url, folder_group);
	}
}

function createNewFolder(folder_name, folder_group) {
	if (folder_name == null || folder_name == "" || folder_group == null)
		return;
	else {
		var url = "file_manager.jsp?action=create&name=" + folder_name;
	    var folder = null;
	    try {
			folder = folder_group.getTargetFolder();
	    } catch (error) {}
		if (folder != null)
			url += "&folder=" + folder;
		var req = createRequest();
	    req.open("GET", url, true);
	    req.onreadystatechange = function() {
	        if (req.readyState == 4) {
				var xml = req.responseXML;
				var warnings =
					xml.getElementById('warnings').getElementsByTagName("p");
				if (warnings != null) {
					var warning = "";
					for (var i in warnings) {
						warning += warnings[i].innerHTML + "\n";
					}
					alert(warning);
					new_folder();
				}
	        	// refresh folder group
	        	else folder_group.refresh();
	        }
	    }
		req.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");    
	    req.send(null);
	}
}

/**
 * Renames an existing file.  Both argument filenames are assumed to be
 * fully qualified relative to the current user's root folder.
 * 
 * @param old_file_name
 * @param new_file_name
 * @param folder_group
 * @return
 */
function renameFile(old_file_name, new_file_name, folder_group) {
	if (old_file_name == null || old_file_name == "" ||
		new_file_name == null || new_file_name == "" ||
		folder_group == null)
		return;
	else {
		var url = "file_manager.jsp?action=rename&name=" + old_file_name +
			"&new=" + new_file_name;
		sendRequest(url, folder_group);
	}
}

function deleteFile(file_name, folder_group) {
	if (file_name == null || file_name == "" || folder_group == null)
		return;
	else {
		var url = "file_manager.jsp?action=delete&name=" + file_name;
		sendRequest(url, folder_group);
	}
}

function sendRequest(url, folder_group) {
	if (url == null || url == "" || folder_group == null)
		return;
	else {
		var req = createRequest();
	    req.open("GET", url, true);
	    req.onreadystatechange = function() {
	        if (req.readyState == 4) {
	        	// refresh folder group
	        	folder_group.refresh();
	        }
	    }
		req.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");    
	    req.send(null);
	}
}
