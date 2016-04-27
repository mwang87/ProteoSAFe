/**
 * Custom SWFUpload event handler functions
 */
function getFileQueuedHandler(uploader) {
	return function(file) {
		try {
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.setStatus("Upload Queued...");
			progress.toggleCancel(true, this);
			// add the file detail string to the queue to send to the server
			uploader.queuedUploads.push(
				file.id + ":" + file.name + ":" + file.size);
		} catch (error) {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"file queued\" handler: " + error, "UPLOAD");
		}
	}
}

function getFileQueueErrorHandler(uploader) {
	return function(file, errorCode, message) {
		try {
			if (errorCode === SWFUpload.QUEUE_ERROR.QUEUE_LIMIT_EXCEEDED) {
				alert("You have attempted to queue too many files.\n" +
					(message === 0 ? "You have reached the upload limit." :
					"You may select " + (message > 1 ?
						"up to " + message + " files." : "one file.")));
				return;
			}
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.toggleCancel(true, this);
			progress.setError();
			switch (errorCode) {
				case SWFUpload.QUEUE_ERROR.FILE_EXCEEDS_SIZE_LIMIT:
					progress.setStatus("File is too big.");
					trackedConsoleLog("Error Code: File too big, File name: " +
						file.name + ", File size: " + file.size +
						", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.QUEUE_ERROR.ZERO_BYTE_FILE:
					progress.setStatus("Cannot upload Zero Byte files.");
					trackedConsoleLog("Error Code: Zero byte file, " +
						"File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.QUEUE_ERROR.INVALID_FILETYPE:
					progress.setStatus("Invalid File Type.");
					trackedConsoleLog("Error Code: Invalid File Type, " +
						"File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
				default:
					if (file !== null)
						progress.setStatus("Unhandled Error");
					trackedConsoleLog("Error Code: " + errorCode +
						", File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
			}
		} catch (error) {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"file queue error\" handler: " + error, "UPLOAD");
		}
		// refresh the queue array
		uploader.queuedUploads = new Array();
	}
}

function getFileDialogCompleteHandler(
	folderTree, folderModel, uploader,
	queueUploads, toggleUploadWarning
) {
	return function(numFilesSelected, numFilesQueued) {
		try {
//
trackedConsoleLog(
	"upload_handlers.js - calling \"file dialog complete\" handler, with " +
	numFilesSelected + " file(s) selected, and " + numFilesQueued +
	" file(s) queued.", "UPLOAD");
//
			if (numFilesSelected > 0) {
				// add target upload folder to post params
				var folder = null;
				try {
					folder = folderTree.getTargetFolder();
				} catch (error) {}
				if (folder == null)
					folder = folderTree.getRootFolder();
				folder = folderModel.getIdentity(folder);
				this.addPostParam("folder", folder);
				// refresh cookies
				this.refreshCookies(true);
				// send all queued files to the server, which should
				// trigger an upload after the server responds
				var files = "";
				for (var file in uploader.queuedUploads)
					files += uploader.queuedUploads[file] + ";";
				if (files.length > 0) {
					files = files.substr(0, files.length - 1);
					queueUploads(files, folder, this);
				}
				document.getElementById(
					this.customSettings.cancelButtonId).disabled = false;
				toggleUploadWarning(true);
			}
		} catch (error)  {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"file dialog complete\" handler: " + error, "UPLOAD");
		}
		// refresh the queue array
		uploader.queuedUploads = new Array();
	}
}

function getUploadStartHandler() {
	return function(file) {
		try {
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.setStatus("Uploading (0% Done)...");
			progress.toggleCancel(true, this);
		} catch (error) {}
		return true;
	}
}

function getUploadProgressHandler(progressUpload, uploader) {
	return function(file, bytesLoaded, bytesTotal) {
		try {
			var percent = Math.ceil((bytesLoaded / bytesTotal) * 100);
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.setProgress(percent);
			progress.setStatus("Uploading (" + percent + "% Done)...");
			// inform the server how this file upload is doing, but
			// only do it at most once per whole percent progress
			if (uploader.lastProgress < percent) {
				uploader.lastProgress = percent;
				progressUpload(file.id, bytesLoaded);
			}
		} catch (error) {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"upload progress\" handler: " + error, "UPLOAD");
		}
	}
}

function getUploadSuccessHandler() {
	return function(file, serverData) {
		try {
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.setComplete();
			progress.setStatus("Upload Complete.");
		} catch (error) {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"upload success\" handler: " + error, "UPLOAD");
		}
	}
}

function getUploadErrorHandler(cancelUpload) {
	return function(file, errorCode, message) {
		try {
			var progress =
				new FileProgress(file, this.customSettings.progressTarget);
			progress.toggleCancel(true, this);
			progress.setError();
			switch (errorCode) {
				case SWFUpload.UPLOAD_ERROR.HTTP_ERROR:
					progress.setStatus("Upload Error: " + message);
					trackedConsoleLog("Error Code: HTTP Error, File name: " +
						file.name + ", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.UPLOAD_FAILED:
					progress.setStatus("Upload Failed.");
					trackedConsoleLog("Error Code: Upload Failed, File name: " +
						file.name + ", File size: " + file.size +
						", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.IO_ERROR:
					progress.setStatus("Server (IO) Error.");
					trackedConsoleLog("Error Code: IO Error, File name: " +
						file.name + ", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.SECURITY_ERROR:
					progress.setStatus("Security Error.");
					trackedConsoleLog("Error Code: Security Error, " +
						"File name: " + file.name + ", Message: " + message,
						"UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.UPLOAD_LIMIT_EXCEEDED:
					progress.setStatus("Upload limit exceeded.");
					trackedConsoleLog("Error Code: Upload Limit Exceeded, " +
						"File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.FILE_VALIDATION_FAILED:
					progress.setStatus("Failed Validation.  Upload skipped.");
					trackedConsoleLog("Error Code: File Validation Failed, " +
						"File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
				case SWFUpload.UPLOAD_ERROR.FILE_CANCELLED:
					// If there aren't any files left (they were all cancelled)
					// disable the cancel button
					if (this.getStats().files_queued === 0) {
						document.getElementById(
							this.customSettings.cancelButtonId).disabled = true;
					}
					progress.setStatus("Upload Cancelled.");
					progress.setCancelled();
					break;
				case SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED:
					progress.setStatus("Upload Stopped.");
					break;
				default:
					progress.setStatus("Unhandled Error: " + errorCode);
					trackedConsoleLog("Error Code: " + errorCode +
						", File name: " + file.name + ", File size: " +
						file.size + ", Message: " + message, "UPLOAD");
					break;
			}
		} catch (error) {
			trackedConsoleLog("upload_handlers.js - error in " +
				"\"upload error\" handler: " + error, "UPLOAD");
		} finally {
			cancelUpload(file.id);
		}
	}
}

function getUploadCompleteHandler(
	queuedTokens, toggleUploadWarning, uploader
) {
	return function(file) {
		if (this.getStats().files_queued === 0) {
			document.getElementById(
				this.customSettings.cancelButtonId).disabled = true;
			toggleUploadWarning(false);
		}
		queuedTokens[file.id] = undefined;
		uploader.lastProgress = 0;
	}
}

// This event comes from the Queue Plugin
function getQueueCompleteHandler(id) {
	return function(numFilesUploaded) {
		var status = document.getElementById(id + "_divStatus");
		status.innerHTML = numFilesUploaded + " file" +
			(numFilesUploaded === 1 ? "" : "s") + " uploaded.";
	}
}
