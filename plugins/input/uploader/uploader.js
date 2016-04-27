/**
 * Resource manager module for uploading files to the ProteoSAFe user space.
 * 
 * Because this module makes extensive use of the "SWFUpload" Javascript
 * module, it is dependent on ProteoSAFe script files "swfupload*" and
 * "fileprogress.js".
 */
function ProteoSAFeInputModule_uploader(div, id) {
	// set argument properties
	this.div = div;
	this.id = id;
	// initialize other properties
	this.folderTree = null;
	this.swfu = null;
	this.queuedTokens = {};
	this.queuedUploads = new Array();
	this.lastProgress = 0;
}

/**
 * Initializes this uploader module by setting up its HTML content and
 * Flash uploader module
 */
ProteoSAFeInputModule_uploader.prototype.init = function() {
	// create and build folder tree
	this.folderTree =
		new ProteoSAFeTreeClasses.folderTree(this.id + "_folderTree");
	this.buildFolderTree();
	// set file management button callbacks
	document.getElementById(this.id + "_createButton").onclick =
		ProteoSAFeTreeUtils.getCreateNewFolderHandler(this.folderTree);
	document.getElementById(this.id + "_renameButton").onclick =
		ProteoSAFeTreeUtils.getRenameFileHandler(this.folderTree);
	document.getElementById(this.id + "_deleteButton").onclick =
		ProteoSAFeTreeUtils.getDeleteSelectedHandler(this.folderTree);
	// initialize Flash uploader object properties
	var id = this.id;
	var folderTree = this.folderTree.tree;
	var folderModel = folderTree.model;
	var queuedTokens = this.queuedTokens;
	var queueUploads = this.getQueueUploadHandler();
	var toggleUploadWarning = this.getToggleUploadWarningHandler();
	var progressUpload = this.getProgressUploadHandler();
	var cancelUpload = this.getCancelUploadHandler();
	// initialize Flash uploader object
	var uploader = this;
	this.swfu = new SWFUpload({
		flash_url: "scripts/swfupload.swf",
		upload_url: "ManageFiles",
		file_size_limit: "2 GB",
		file_types: "*.*",
		file_types_description: "All Files",
		file_upload_limit: 100,
		file_queue_limit: 0,
		custom_settings: {
			progressTarget: id + "_fsUploadProgress",
			cancelButtonId: id + "_btnCancel"
		},
		debug: false,
		// Button settings
		button_image_url: "images/upload.png",
		button_width: "61",
		button_height: "22",
		button_placeholder_id: id + "_spanButtonPlaceHolder",
		// Event handler functions defined in upload_handlers.js
		file_queued_handler: getFileQueuedHandler(uploader),
		file_queue_error_handler: getFileQueueErrorHandler(uploader),
		file_dialog_complete_handler: getFileDialogCompleteHandler(
			folderTree, folderModel, uploader,
			queueUploads, toggleUploadWarning),
		upload_start_handler: getUploadStartHandler(),
		upload_progress_handler: getUploadProgressHandler(
			progressUpload, uploader),
		upload_success_handler: getUploadSuccessHandler(),
		upload_error_handler: getUploadErrorHandler(cancelUpload),
		upload_complete_handler: getUploadCompleteHandler(
			queuedTokens, toggleUploadWarning, uploader),
		queue_complete_handler: getQueueCompleteHandler(id)
	});
}

/**
 * Initializes this treeSelector module by rebuilding the folder tree
 */
ProteoSAFeInputModule_uploader.prototype.refresh = function() {
	this.buildFolderTree();
}

/**
 * Builds the folder tree
 */
ProteoSAFeInputModule_uploader.prototype.buildFolderTree = function() {
	this.folderTree.build(true, false);
}

ProteoSAFeInputModule_uploader.prototype.getQueueUploadHandler = function() {
	var moduleInstance = this;
	return function(files, folder, uploader) {
		if (files == null || folder == null)
			return;
		var url = "QueueUploads";
		var params = "files=" + files + "&folder=" + folder;
		var request = createRequest();
		request.open("POST", url, true);
		request.onreadystatechange = function() {
			if (request.readyState == 4) {
//
trackedConsoleLog(
	"Module uploader.js - \"queue upload\" handler AJAX returned, " +
	"with status " + request.status + ".\n\tFiles = \"" + files +
	"\"\n\tFolder = \"" + folder + "\"", "UPLOAD");
//
				if (request.status == 200) {
					// if a handshake token was returned,
					// add it to post params
					var token = request.responseText;
					if (token != null && token != "" && uploader != null)
						uploader.addPostParam("token", token);
					// associate all of these queued uploads
					// with this token
					var uploads = files.split(";");
					for (var i=0; i<uploads.length; i++) {
						var details = uploads[i].split(":");
						moduleInstance.queuedTokens[details[0]] = token;
					}
				}
				// start upload whether queuing was successful or not
				if (uploader != null)
					uploader.startUpload();
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

ProteoSAFeInputModule_uploader.prototype.getProgressUploadHandler =
function() {
	var moduleInstance = this;
	return function(id, bytes) {
		if (id == null || bytes == null)
			return;
		var token = moduleInstance.queuedTokens[id];
		if (token == null)
			return;
		var url = "QueueUploads";
		var params = "id=" + id + "&token=" + token + "&bytes=" + bytes;
		var request = createRequest();
		request.open("PUT", url + "?" + params, true);
		request.onreadystatechange = function() {
//
//if (request.readyState == 4)
//	trackedConsoleLog(
//		"Module uploader.js - \"progress upload\" handler AJAX returned, " +
//		"with status " + request.status + ".\n\tID = \"" + id +
//		"\"\n\tToken = \"" + token + "\"\n\tbytes = " + bytes, "UPLOAD");
//
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

ProteoSAFeInputModule_uploader.prototype.getCancelUploadHandler =
function() {
	var moduleInstance = this;
	return function(id) {
		if (id == null)
			return;
		var token = moduleInstance.queuedTokens[id];
		if (token == null)
			return;
		var url = "QueueUploads";
		var params = "id=" + id + "&token=" + token;
		var request = createRequest();
		request.open("DELETE", url + "?" + params, true);
		request.onreadystatechange = function() {
//
if (request.readyState == 4)
	trackedConsoleLog(
		"Module uploader.js - \"cancel upload\" handler AJAX returned, " +
		"with status " + request.status + ".\n\tID = \"" + id +
		"\"\n\tToken = \"" + token + "\"", "UPLOAD");
//
		}
		request.setRequestHeader("If-Modified-Since",
			"Sat, 1 Jan 2000 00:00:00 GMT");
		request.send(null);
	}
}

ProteoSAFeInputModule_uploader.prototype.getToggleUploadWarningHandler =
function() {
	var moduleInstance = this;
	return function(display) {
		var div = document.getElementById(moduleInstance.id + "_uploadWarning");
		if (display)
			div.innerHTML = "WARNING: Please do not close this " +
				"window before your uploads are complete. Doing so " +
				"will cause any pending uploads to fail.";
		else div.innerHTML = "";
	}
}

CCMSForms.modules["uploader"] = ProteoSAFeInputModule_uploader;
