/**
 * This file contains utility scripts used by the ProteoSAFe
 * resource manager popup window.
 */

// static resource manager script utility package
var ProteoSAFeResourceUtils = {};

// bookkeeping hash to maintain resource manager window state
var ProteoSAFeResources = {};
ProteoSAFeResources.window = null;	// resource manager popup window

/******************************************************************************
 * Resource manager window functions
 ******************************************************************************/

/**
 * Creates and/or opens the ProteoSAFe resource manager window
 */
ProteoSAFeResourceUtils.openResourceWindow = function() {
	if (ProteoSAFeResources.window != null &&
		ProteoSAFeResources.window.closed == false)
		ProteoSAFeResources.window.focus();
	else ProteoSAFeResources.window = window.open(
		"upload.jsp", "resource_window",
		"height=500,width=835,toolbar=0,location=0,directories=0," +
		"status=0,menubar=0,scrollbars=yes,resizeable=0");
}

/**
 * Blurs the ProteoSAFe resource manager window
 * (returning focus to the main input form window)
 */
ProteoSAFeResourceUtils.blurResourceWindow = function() {
    window.close();
	//ProteoSAFeResources.window.blur();
	//opener.focus();
}

/******************************************************************************
 * Resource manager filename functions
 ******************************************************************************/

ProteoSAFeResourceUtils.isDirectory = function(fileDescriptor) {
	if (fileDescriptor == null)
		return false;
	else if (fileDescriptor.length <= 2)
		return false;
	else return (fileDescriptor.substring(0, 2) == "d.");
}

ProteoSAFeResourceUtils.cleanFilename = function(fileDescriptor) {
	if (fileDescriptor == null)
		return null;
	var cleanFile = fileDescriptor;
	// strip off ProteoSAFe format prefix
	if (cleanFile.length > 2) {
		var prefix = cleanFile.substring(0, 2);
		if (prefix == "d." || prefix == "f.")
			cleanFile = cleanFile.substring(2);
	}
	// the "guest user" rule
	cleanFile = cleanFile.replace(/Guest\.(\w)+-(\d)+/g, "Guest");
	return cleanFile;
}
