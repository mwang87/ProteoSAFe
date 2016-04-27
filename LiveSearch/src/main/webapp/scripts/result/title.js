/**
 * Title result view block implementation
 */
// constructor
function ResultViewTitle(blockXML, id, task) {
	// properties
	this.id = id;
	this.task = task;
	this.description = task.description;
	this.data = null;
}

// render the title
ResultViewTitle.prototype.render = function(div, index) {
	div.innerHTML = "<h2>" + this.data + "</h2>";
}

// assign text to the title
ResultViewTitle.prototype.setData = function(data) {
	this.data = data;
}

// assign this view implementation to block type "title"
resultViewBlocks["title"] = ResultViewTitle;
