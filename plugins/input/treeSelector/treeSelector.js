/**
 * Resource manager module for selecting files from the ProteoSAFe user space
 * to be added as input to the current workflow.  Closely related to the
 * ProteoSAFe "fileSelector" input form module, which launches this one in
 * the resource manager window.
 * 
 * Because this module makes extensive use of folder tree UI elements,
 * it is dependent on ProteoSAFe script file "fileManager.js".
 */
function ProteoSAFeInputModule_treeSelector(div, id) {
	// set argument properties
	this.div = div;
	this.id = id;
	// initialize other properties
	this.folderTree = null;
	this.selectionTrees = {};
}

/**
 * Initializes this treeSelector module by constructing all of its trees
 */
ProteoSAFeInputModule_treeSelector.prototype.init = function() {
	// create folder tree
	this.folderTree =
		new ProteoSAFeTreeClasses.folderTree(this.id + "_folderTree");
	// set file management button callbacks
	document.getElementById(this.id + "_createButton").onclick =
		ProteoSAFeTreeUtils.getCreateNewFolderHandler(this.folderTree);
	document.getElementById(this.id + "_renameButton").onclick =
		ProteoSAFeTreeUtils.getRenameFileHandler(this.folderTree);
	document.getElementById(this.id + "_deleteButton").onclick =
		ProteoSAFeTreeUtils.getDeleteSelectedHandler(this.folderTree);
	// look up all initialized treeSelector modules in the input
	// form, and create a selection tree for each of them
	// TODO: legitimately find the form name of the associated input modules
	var form = opener.CCMSFormUtils.getFormRecord("mainform");
	if (form == null || form.modules == null)
		return;
	for (var field in form.modules) {
		// fileSelector modules have a "fileSelector" property set to true
		if (form.modules[field].fileSelector)
			this.selectionTrees[field] =
				new ProteoSAFeTreeClasses.selectionTree(
					field, form.modules[field].label);
	}
	// ensure that necessary HTML is present for all trees
	for (var treeName in this.selectionTrees)
		this.initSelectionTree(treeName);
	// build all trees
	this.buildFolderTree();
	this.buildSelectionTrees();
}

/**
 * Initializes this treeSelector module by rebuilding the folder tree
 */
ProteoSAFeInputModule_treeSelector.prototype.refresh = function() {
	this.buildFolderTree();
}

/**
 * Creates and initializes all necessary HTML for the indicated selection tree
 */
ProteoSAFeInputModule_treeSelector.prototype.initSelectionTree = function(
	treeName
) {
	if (treeName == null)
		return;
	// get the selection tree
	var tree = this.selectionTrees[treeName];
	// make sure that this tree's selection button is present
	var button = document.getElementById(treeName + "_fileSelector");
	if (button == null && tree != null) {
		// get the selection button table cell
		var td = document.getElementById(this.id + "_fileSelectors");
		// add selection button
		var button = document.createElement("button");
		button.title = "Add " + tree.label;
		button.style.width = "120px";
		var module = this;
		var field = tree.id;
		button.onclick = function() {
			module.addSelection(field);
		};
		var span = document.createElement("span");
		span.style.fontSize = "75%";
		span.appendChild(document.createTextNode(tree.label + " "));
		button.appendChild(span);
		var img = document.createElement("img");
		img.src = "images/add_spec.png";
		img.style.cssFloat = "right";
		button.appendChild(img);
		td.appendChild(button);
		// add line break
		td.appendChild(document.createElement("br"));
	}
	// make sure that this tree's rendering div is present
	var div = document.getElementById(treeName + "_tree");
	if (div == null && tree != null) {
		// get the selection tree table
		var table = document.getElementById(this.id + "_selectionTrees");
		// splice a new row into next-to-last position under parent tree
		var tr = table.insertRow(table.rows.length - 1);
		// add selection removal button
		var td = tr.insertCell(0);
		td.style.verticalAlign = "top";
		var button = document.createElement("button");
		button.title = "Remove " + tree.label;
		button.style.width = "40px";
		var module = this;
		var field = tree.id;
		button.onclick = function() {
			module.removeSelection(field);
		};
		var img = document.createElement("img");
		img.src = "images/remove_spec.png";
		img.alt = button.title;
		button.appendChild(img);
		td.appendChild(button);
		// add tree div
		td = tr.insertCell(1);
		td.style.maxWidth = "396px";
		div = document.createElement("div");
		div.id = tree.id + "_tree";
		td.appendChild(div);
	}
}

/**
 * Builds the folder tree
 */
ProteoSAFeInputModule_treeSelector.prototype.buildFolderTree = function() {
	this.folderTree.build(false, true);
}

/**
 * Builds all selection trees
 */
ProteoSAFeInputModule_treeSelector.prototype.buildSelectionTrees =
function() {
	for (var treeName in this.selectionTrees)
		this.selectionTrees[treeName].build();
}

/**
 * Adds the currently selected items from the
 * folder tree to the indicated selection tree
 */
ProteoSAFeInputModule_treeSelector.prototype.addSelection = function(field) {
	// update fileSelector field in parent form
	// TODO: legitimately find the form name of the associated input module
	var selector = opener.CCMSFormUtils.getModuleInstance(field, "mainform");
	if (selector != null)
		selector.addSelectedFiles(
			ProteoSAFeTreeUtils.formatTreeSelection(
				this.folderTree.tree.attr("selectedNodes")));
	// update selection tree in resource manager UI
	var tree = this.selectionTrees[field];
	if (tree != null)
		tree.build();
	// clear selection in left panel
	this.folderTree.tree.clearSelection();
}

/**
 * Removes the currently selected item
 * from the indicated selection tree
 */
ProteoSAFeInputModule_treeSelector.prototype.removeSelection = function(
	field
) {
	// update fileSelector field in parent form
	// TODO: legitimately find the form name of the associated input module
	var selector = opener.CCMSFormUtils.getModuleInstance(field, "mainform");
	var tree = this.selectionTrees[field];
	if (selector != null && tree != null)
		selector.removeSelectedFiles(
			ProteoSAFeTreeUtils.formatTreeSelection(
				[tree.tree.getSelectedItem()]));
	// update selection tree in resource manager UI
	if (tree != null)
		tree.build();
	// clear selection in left panel
	this.folderTree.tree.clearSelection();
}

/**
 * Clears the current selections from the indicated selection tree
 */
ProteoSAFeInputModule_treeSelector.prototype.clearSelection = function(
	field
) {
	// update fileSelector field in parent form
	// TODO: legitimately find the form name of the associated input module
	var selector = opener.CCMSFormUtils.getModuleInstance(field, "mainform");
	if (selector != null)
		selector.clearSelectedFiles();
	// update selection tree in resource manager UI
	var tree = this.selectionTrees[field];
	if (tree != null)
		tree.build();
	// clear selection in left panel
	this.folderTree.tree.clearSelection();
}

/**
 * Clears the current selections from all selection trees
 */
ProteoSAFeInputModule_treeSelector.prototype.clearAllSelections =
function() {
	for (var field in this.selectionTrees)
		this.clearSelection(field);
}

CCMSForms.modules["treeSelector"] = ProteoSAFeInputModule_treeSelector;
