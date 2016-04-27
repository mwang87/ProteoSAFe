function DownloadBlock(div) {
	// properties
	this.form = null;
	this.download = null;				// submit button
	this.hidden = null;					// filtered hit IDs
	this.blocks = {};					// form blocks
	// build and render the form
	this.render(document.getElementById(div));
}

DownloadBlock.prototype.render = function(div) {
	if (div == null)
		return;
	// create form
	this.form = document.createElement("form");
	this.form.method = "post";
	// create radio buttons
	var radioParis = createRadio("option", "paris");
	var radioResult = createRadio("option", "delimit");
//	var radioChecked = createRadio("content", "checked");
	var radioFiltered = createRadio("content", "filtered");
	var radioAll = createRadio("content", "all");
	// create form blocks
	var spanOption = createSpan(null, 
		[" Download Option: ", radioResult, " Tab-Delimited Result Only"]);
//	var spanChecked = createSpan(null, [radioChecked, " Checked "]);
	var spanFiltered = createSpan(null, [radioFiltered, " Filtered "]);
	var spanContent = createSpan(null,
		[" Include Entries: ", spanFiltered, radioAll, " All "]);
	// set up content block visibility callbacks
	radioParis.onclick = function() {
		spanContent.style.visibility = "hidden";
	}
	radioResult.onclick = function() {
		spanContent.style.visibility = "visible";
	}
	// register form blocks
	this.blocks["option"] = spanOption;
	this.blocks["content"] = spanContent;
//	this.blocks["checked"] = spanChecked;
	this.blocks["filtered"] = spanFiltered;
	// create submit button
	this.download = createButton("download", "Download");
	// create hidden form elements
	this.hidden = createHidden("entries");
	// create form element labels
	// add elements to form and initialize them
	appendChildren(this.form, [
		spanOption, BR, spanContent, this.download, this.hidden]
	);
	div.appendChild(this.form);
	// initialize radio buttons
	radioAll.checked = true;
	radioResult.checked = true;
	this.getContent = function() {
		if (radioAll.checked)
			return "all";
		else if (radioFiltered.checked)
			return "filtered";
//		else if (radioChecked.checked)
//			return "checked";
	}
}

DownloadBlock.prototype.enableBlock = function(name, enable) {
	if (name == null)
		return;
	var block = this.blocks[name];
	if (block != null)
		block.style.display = enable ? "block" : "none";
}

DownloadBlock.prototype.setTarget = function(url) {
	this.form.action = url;
}

DownloadBlock.prototype.submit = function() {
	this.form.submit();
}

DownloadBlock.prototype.setDownloadCallback = function(callback) {
	this.download.onclick = callback;
}

DownloadBlock.prototype.setEntries = function(entries) {
	this.hidden.value = entries;
}
