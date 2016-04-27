/**
 * File view link list - result view block implementation
 */
// constructor
function ResultViewFileLinkList(blockXML, id, task) {
	// properties
	this.id = id;
	this.div = null;
	this.index = null;
	this.task = task;
	this.data = null;
}

ResultViewFileLinkList.prototype.render = function(div, index) {
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
	// if this file is already present, remove it
	var child = getChildById(this.div, this.id);
	if (child != null)
		this.div.removeChild(child);
	// add a new child div for this file
	child = document.createElement("div");
	child.id = this.id;
	spliceChild(this.div, child, this.index);
	// add header text
	var header = document.createElement("h3");
	header.innerHTML =
		"Click on a link below to browse results from that file.";
	// build file table
	var links = document.createElement("table");
	links.className = "result";
	// build table header row
	var row = document.createElement("tr");
	// filename column
	var cell = document.createElement("th");
	cell.innerHTML = "Result File";
	row.appendChild(cell);
	// group by spectrum view column
	cell = document.createElement("th");
	cell.innerHTML = "Total PSMs";
	row.appendChild(cell);
	// invalid row count column
	cell = document.createElement("th");
	cell.innerHTML = "Invalid PSMs";
	row.appendChild(cell);
	// group by peptide view column
	cell = document.createElement("th");
	cell.innerHTML = "Peptides";
	row.appendChild(cell);
	// group by protein view column
	cell = document.createElement("th");
	cell.innerHTML = "Proteins";
	row.appendChild(cell);
	links.appendChild(row);
	var i = 0;
	// add a new view link for each file in the result data hash
	for (var file in this.data) {
		var fileData = this.data[file];
		row = document.createElement("tr");
		// filename column
		cell = document.createElement("td");
		cell.innerHTML = fileData.filename;
		row.appendChild(cell);
		// group by spectrum view column
		cell = document.createElement("td");
		var link = document.createElement("a");
		link.href = "result.jsp?task=" + task.id +
			"&view=group_by_spectrum&file=" + file;
		link.innerHTML = fileData.psms;
		cell.appendChild(link);
		row.appendChild(cell);
		// invalid row count column
		cell = document.createElement("td");
		var link = document.createElement("a");
		link.href = "result.jsp?task=" + task.id +
			"&view=group_by_spectrum&file=" + file +
			"#{\"valid_input\"%3A\"INVALID\"}";
		link.innerHTML = fileData.invalid + " (" + fileData.percent + ")";
		cell.appendChild(link);
		row.appendChild(cell);
		// peptide view column
		cell = document.createElement("td");
		var link = document.createElement("a");
		link.href = "result.jsp?task=" + task.id +
			"&view=group_by_peptide&file=" + file;
		link.innerHTML = fileData.peptides;
		cell.appendChild(link);
		row.appendChild(cell);
		// protein view column
		cell = document.createElement("td");
		var link = document.createElement("a");
		link.href = "result.jsp?task=" + task.id +
			"&view=group_by_protein&file=" + file;
		link.innerHTML = fileData.proteins;
		cell.appendChild(link);
		row.appendChild(cell);
		links.appendChild(row);
		i++;
	}
	// if we wanted to redirect straight to the view when there's only one,
	// we could do that here with a simple window.location = url;
	child.appendChild(header);
	child.appendChild(links);
}

ResultViewFileLinkList.prototype.setData = function(data) {
	this.data = data;
}

// assign this view implementation to block type "fileViewLinkList"
resultViewBlocks["fileViewLinkList"] = ResultViewFileLinkList;
