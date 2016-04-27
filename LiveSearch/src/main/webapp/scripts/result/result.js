/**
 * This file contains scripts used by the ProteoSAFe UI result view pages.
 */

/**
 * Function to build a result view object for a particular result view of
 * a particular workflow task.
 * 
 * This function asynchronously retrieves the appropriate result view
 * specification file before attempting to build the view, and therefore
 * the completed view object cannot be returned directly by the function.
 * Instead, this function will invoke the provided callback function
 * with the completed view object as its argument, as a means of providing
 * access to it.
 */
function buildResultView(task, view, callback) {
	var request = createRequest();
    var url = "/ProteoSAFe/DownloadWorkflowInterface?workflow=" +
    	task.workflow + "&task=" + task.id + "&type=result";
	request.open("GET", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				var result =
					new ResultView(request.responseXML, view, task);
				if (result)
					callback(result);
			} else alert(
				"Could not find result specification for view \"" + view +
				"\" of workflow \"" + task.workflow + "\".");
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");    
	request.send(null);
}

function getViewSpecification(resultXML, view) {
	if (resultXML == null || view == null)
		return null;
	var views = resultXML.getElementsByTagName("view");
	for (var i=0; i<views.length; i++) {
		var viewNode = views[i];
		var id = getAttribute(viewNode, "id");
		if (id == view)
			return viewNode;
	}
}

function getBlockSpecification(resultXML, block) {
	if (resultXML == null || block == null)
		return null;
	var blocks = resultXML.getElementsByTagName("block");
	for (var i=0; i<blocks.length; i++) {
		var blockNode = blocks[i];
		var id = getAttribute(blockNode, "id");
		if (id == block)
			return blockNode;
	}
}

/**
 * Result view object
 */
// constructor
function ResultView(resultXML, view, task) {
	// retrieve proper XML element for this view
	var viewXML = getViewSpecification(resultXML, view);
	if (viewXML == null) {
		alert("Could not find result specification for view \"" + view +
			"\" of workflow \"" + task.workflow + "\".");
		return false;
	}
	// properties
	this.id = view;
	this.div = null;
	this.blocks = new Array();
	// initialize the view by building its blocks
	var blockNodes = viewXML.getElementsByTagName("blockRef");
	for (var i=0; i<blockNodes.length; i++) {
		var blockNode = blockNodes[i];
		// get this block's specification
		var block = getAttribute(blockNode, "type");
		var blockXML = getBlockSpecification(resultXML, block);
		if (blockXML == null) {
			alert("Could not find result specification for block \"" + block +
				"\" of workflow \"" + task.workflow + "\".");
			return false;
		}
		// set up this block and add it to the list
		var blockType = getAttribute(blockXML, "type");
		var blockConstructor = getResultViewBlock(blockType);
		if (blockConstructor != null) {
			this.blocks.push(
				new blockConstructor(
					blockXML, getAttribute(blockNode, "id"), task));
		} else {
			alert("Could not find implementation for result block type \"" +
				blockType + "\".");
			return false;
		}
	}
	return true;
}

// get the specified block in this view
ResultView.prototype.getBlock = function(name) {
	if (name == null)
		return null;
	for (var i=0; i<this.blocks.length; i++) {
		var block = this.blocks[i];
		if (block.id == name)
			return block;
	}
	return null;
}

// set data to the specified block in this view
ResultView.prototype.setData = function(name, data) {
	var block = this.getBlock(name);
	if (block != null)
		block.setData(data);
}

// render the specified block in this view
ResultView.prototype.renderBlock = function(name) {
	var block = this.getBlock(name);
	if (block != null)
		block.render(this.div);
}

// render all blocks in this view
ResultView.prototype.render = function(div) {
	if (div != null)
		this.div = div;
	if (this.div == null) {
		alert("No div was provided under which to render this result view.");
		return;
	}
	// clear all current blocks, since they will all be re-rendered
	// (note that this implementation assumes that all children of
	// the argument div are in fact result view blocks)
	removeChildren(this.div);
	// render all blocks
	for (var i=0; i<this.blocks.length; i++)
		this.blocks[i].render(this.div, i);
}

/**
 * Table registering result view block types with their appropriate
 * implementations.
 * 
 * The purpose of this table is to assign any new result view implementations
 * to their appropriate (string) types, as referenced in result.xml.  Any new
 * type of result view block will need to be implemented and registered to its
 * type in this manner.
 * 
 * A result view block implementation should be a Javascript class with the
 * following members:
 * 
 * Properties:
 *     id
 *     task
 *     description
 * 
 * Methods:
 *     constructor(blockXML, id, task)
 *     render(div, index)
 *     setData(data)
 */
var resultViewBlocks = {};

/**
 * Retrieves the appropriate result view class for the specified block type.
 */
function getResultViewBlock(type) {
	if (type == null)
		return;
	else return resultViewBlocks[type];
}
