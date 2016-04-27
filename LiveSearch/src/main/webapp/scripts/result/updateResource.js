/**
 * System resource update result view block implementation
 */
// constructor
function ResultViewResourceUpdater(blockXML, id, task) {
	// properties
	this.id = id;
	this.div = null;
	this.index = null;
	this.task = task;
	this.description = task.description;
	this.data = null;
	this.fileType = null;
	this.resourceType = null;
	this.resourceParameter = null;
	// set up the file retrieval
	this.init(blockXML);
}

// initialize block from XML specification
ResultViewResourceUpdater.prototype.init = function(blockXML) {
	if (blockXML == null)
		return;
	// get file type from source element
	var source = blockXML.getElementsByTagName("source");
	if (source != null && source.length > 0)
		source = source[0];
	if (source != null)
		this.fileType = getAttribute(source, "type");
	// get resource type and parameter from resource element
	var resource = blockXML.getElementsByTagName("resource");
	if (resource != null && resource.length > 0)
		resource = resource[0];
	if (resource != null) {
		this.resourceType = getAttribute(resource, "type");
		this.resourceParameter = getAttribute(resource, "parameter");
	}
}

// render the resource updater widgets
ResultViewResourceUpdater.prototype.render = function(div, index) {
	if (div != null)
		this.div = div;
	if (this.div == null) {
		alert("No div was provided under which to render this result block.");
		return;
	}
	if (index != null)
		this.index = index;
	if (this.index == null)
		this.index = 0;
	// if this button is already present, remove it
	var child = getChildById(this.div, this.id);
	if (child != null)
		this.div.removeChild(child);
	// render the button
	var button = document.createElement("div");
	button.id = this.id;
	button.style.textAlign = "center";
	var img = document.createElement("img");
	img.style.height = "60px";
	img.style.width = "180px";
	img.style.marginTop = "20px";
	img.style.marginBottom = "20px";
	img.style.cursor = "pointer";
	img.src = "images/updateResource/update_button.jpg";
	img.title = "Click here to replace the system's resource " +
		"with the result of this workflow task.";
	img.onclick = getUpdateResourceClosure(
		button, this.task.id, this.fileType, this.data,
		this.resourceType, this.resourceParameter);
	button.appendChild(img);
	this.div.appendChild(button);
}

// set data to the resource updater
ResultViewResourceUpdater.prototype.setData = function(data) {
	this.data = data;
}

// assign this view implementation to block type "updateResource"
resultViewBlocks["updateResource"] = ResultViewResourceUpdater;

/**
 * Helper function to update system resources (when the button is pressed)
 */
function updateResource(div, task, type, source, resource, parameter) {
	if (div == null || task == null || type == null || source == null ||
		resource == null || parameter == null)
		return;
	// set up URL to update resource
    var url = "UpdateResource?task=" + task + "&" + type + "=" + source +
    	"&resource=" + resource + "&parameter=" + parameter;
    
    // gray out commit button
    var img = div.getElementsByTagName("img")[0];
    img.src = "images/updateResource/waiting_update_button.jpg";
	img.title = "The system's resource is currently being replaced " +
		"with the result of this workflow task.";
	img.style.cursor = "auto";
    img.onclick = undefined;
    // show "in-progress" spinner
    
    // create and submit AJAX request for the resource to be updated
	var request = createRequest();
	request.open("PUT", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			// remove "in-progress" spinner
			if (request.status == 200) {
				img.src = "images/updateResource/updated_button.jpg";
				img.title = "The system's resource has been successfully " +
					"replaced with the result of this workflow task.";
			} else {
				img.title = "There was an error replacing the system's " +
					"resource with the result of this workflow task.";
				if (request.status == 403) {
					alert("You are not authenticated to " +
						"update the specified resource.");
				} else {
					alert("Could not update system resource type \"" +
						resource + "\" with updated file \"" + source +
						"\", belonging to task " + task + ".");
				}
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
	request.send(null);
}

function getUpdateResourceClosure(
	div, task, fileType, data, resourceType, resourceParameter
) {
	return function() {
		var sure = confirm("You are about to modify a system resource. " +
			"Are you sure you want to overwrite the existing resource with " +
			"the result of this workflow task?");
		if (sure == true) {
			updateResource(
				div, task, fileType, data, resourceType, resourceParameter);
		}
	};
}
