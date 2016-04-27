/**
 * This file contains scripts for managing user-uploaded files within the
 * ProteoSAFe web application.  This API is implemented using an adaptation
 * of the Dojo file/folder tree widget to traverse the user's file space.
 */

// static tree-related script utility package
var ProteoSAFeTreeUtils = {};

// client-tree interaction API
ProteoSAFeTreeUtils.getCreateDummyFileHandler = function(folderTree) {
	return function(name, folder) {
		if (name == null || folder == null)
			return;
		folderTree.queueUpdate(folder, "dummy", name);
	}
}

ProteoSAFeTreeUtils.getCreateNewFolderHandler = function(folderTree) {
	return function() {
		var folder = null;
		try {
			folder = folderTree.tree.getTargetFolder();
			if (folder == null)
				folder = folderTree.tree.getRootFolder();
		} catch (error) {
			if (error.message == "SELECTED_MULTIPLE")
				alert("You have selected more than one folder.\n" +
					"Please select a single parent folder " +
					"under which to create this folder.");
			else alert("There was an error assigning a parent " +
				"folder to your new folder.");
			return;
		}
		var name = prompt("Please name your new folder. " +
			"This folder will be created under parent folder\n" +
			"/" + ProteoSAFeResourceUtils.cleanFilename(
				folderTree.model.getIdentity(folder)) + "/ :", "");
		if (name != null && name != "")
			folderTree.queueUpdate(folder, "create", name);
	}
}

ProteoSAFeTreeUtils.getRenameFileHandler = function(folderTree) {
	return function() {
		var root = folderTree.tree.getRootFolder();
		var file = null;
		try {
			file = folderTree.tree.getTargetFile();
			if (file == null)
				throw new Error("SELECTED_NONE");
			else if (folderTree.model.getIdentity(file) ==
				folderTree.model.getIdentity(root))
				throw new Error("SELECTED_ROOT");
		} catch (error) {
			if (error.message == "SELECTED_MULTIPLE")
				alert("You have selected more than one item.\n" +
					"Please select a single file or folder to rename.");
			else if (error.message == "SELECTED_NONE")
				alert("Please select a file or folder to rename.");
			else if (error.message == "SELECTED_ROOT")
				alert("Sorry, you may not rename your root folder.");
			else alert("There was an error renaming your selected " +
				"file or folder.");
			return;
		}
		var name = prompt("Please rename this file or folder.", file.name);
		if (name != null && name != "" && name != file.name) {
			folderTree.queueUpdate(file, "rename", name);
			folderTree.tree.clearSelection();
		}
	}
}

ProteoSAFeTreeUtils.getDeleteSelectedHandler = function(folderTree) {
	return function() {
		var selected = null;
		try {
			selected = folderTree.tree.attr("selectedNodes");
			if (selected == null || selected.length < 1) {
				var selectedItem = folderTree.tree.getTargetFile();
				if (selectedItem == null)
					throw new Error("SELECTED_NONE");
				else selected = [selectedItem];
			}
			var root = folderTree.tree.getRootFolder();
			for (var i in selected)
				if (folderTree.model.getIdentity(selected[i]) ==
					folderTree.model.getIdentity(root))
					throw new Error("SELECTED_ROOT");
		} catch (error) {
			if (error.message == "SELECTED_NONE")
				alert("Please select one or more files or folders to delete.");
			else if (error.message == "SELECTED_ROOT")
				alert("Sorry, you may not delete your root folder.");
			else alert("There was an error deleting your selected " +
				"files or folders.");
			return;
		}
		var message = "Are you sure? If you delete these items, " +
			"you will not be able to recover them:";
		for (var i in selected)
			message += "\n" + ProteoSAFeResourceUtils.cleanFilename(
				folderTree.model.getIdentity(selected[i]));
		var sure = confirm(message);
		if (sure) {
			for (var i in selected)
				folderTree.queueUpdate(selected[i], "delete");
			folderTree.tree.clearSelection();
		}
	}
}

/**
 * This function is an adapter to convert the contents of the
 * selected nodes list of the Dojo dijit.Tree object into a proper
 * ProteoSAFe-formatted (semicolon-delimited) file list string.
 */
ProteoSAFeTreeUtils.formatTreeSelection = function(items) {
	if (items == null)
		return "";
	var selection = "";
	for (var key in items) {
		var item = items[key];
		if (item == null || item.path == null)
			continue;
		// for some reason, the Dojo selected item implementation
		// sometimes assigns a boolean value, and sometimes a
		// string, to the "directory" property of tree items
		else if (item.directory == true || item.directory == "true")
			selection += "d.";
		else selection += "f.";
		selection += item.path + ";";
	}
	return selection;
}

// hash to maintain constructors for ProteoSAFe resource manager tree classes
var ProteoSAFeTreeClasses = {};

/**
 * Implementation for the main folder tree used in the ProteoSAFe resource
 * manager popup window.  This tree is used to display selectable files and
 * folders from the user's upload space on the ProteoSAFe server.
 */
ProteoSAFeTreeClasses.folderTree = function(id) {
	// assign basic properties
	if (id == null) {
		// TODO: report error, fail
		return;
	}
	this.id = id;
	
	// method implementations to be passed into the Dojo object constructors
	/*
	 * Queries the server to retrieve any children files and folders belonging
	 * to the specified file.
	 * 
	 * It is necessary to override this method when dealing with file items that
	 * do not maintain references to their own children, since the default
	 * implementation fetches a file's children simply by looking for child
	 * references in the file object, rather than submitting a new query.
	 * 
	 * This function returns a request object as defined in the
	 * dojo.data.api.Request API.  The actual result items are made available
	 * through the "onComplete" callback function, and are not present in this
	 * function's return value.
	 * 
	 * In this case, the callback function will be provided by the tree widget,
	 * since this function will only actually called by the widget itself to
	 * populate its display.
	 */
	var getChildren = function(item, onComplete) {
		var query = null;
		if (this.store.isItem(item)) {
			// this hack is necessary to solve a bug in the getIdentity
			// method, when dealing with "forest" type folder trees
			var identity = this.getIdentity(item);
            if (identity == null || identity == "null")
                    identity = item.name;
            query = {"parentDir": identity};
		} else query = {};
		return this.store.fetch({
			query: query,
			onComplete: onComplete
		});
	}
	
	/*
	 * Determines whether or not the specified file may contain any children
	 * files or folders.
	 * 
	 * Because only directories can contain other files, this function simply
	 * returns true if the specified file is a directory, and false otherwise.
	 * Not only does this make sense for directories, but it is necessary when
	 * dealing with file items that do not maintain references to their own
	 * children, since this method's default implementation returns true if
	 * and only if the specified file contains child references.
	 * 
	 * This function will only actually called by the tree widget, to determine
	 * whether or not an expand/collapse control should be rendered in the
	 * specified file's tree display.
	 */
	var mayHaveChildren = function(item) {
		if (this.store.isItem(item))
			return this.store.getValue(item, "directory");
		else return false;
	}
	
	var getNode = function(item) {
		if (item == null)
			return null;
		var domNode = dojo.byId(this.domNode.id);
		var treeNodes = dojo.query("[widgetid]", domNode);
		if (treeNodes == null || treeNodes.length < 1)
			return null;
		else {
			var model = this.model;
			var id = model.getIdentity(item);
			var node = null;
			treeNodes.forEach(
				function(treeNode) {
					var widgetNode =
						dijit.byId(treeNode.getAttribute("widgetid"));
					if (model.getIdentity(widgetNode.item) == id)
						node = widgetNode;
				}
			);
			return node;
		}
	}
	
	var getSelectedNode = function() {
		var domNode = dojo.byId(this.domNode.id);
		var selected = dojo.query("span[aria-selected=\"true\"]", domNode);
		if (selected == null || selected.length < 1)
			return null;
		var treeNode = selected[0].parentNode.parentNode.parentNode;
		return dijit.byId(treeNode.getAttribute("widgetid"));
	}
	
	var getSelectedItem = function() {
		var widgetNode = this.getSelectedNode();
		if (widgetNode == null)
			return null;
		else return widgetNode.item;
	}
	
	var clearSelection = function() {
		// clear tree's single selected node
		var selectedNode = this.getSelectedNode();
		if (selectedNode != null)
			selectedNode.setSelected(false);
		// clear all multiple selected nodes, if any
		var selected = this.attr("selectedNodes");
		if (selected != null && selected.length > 0) {
			// build a local copy of the selected nodes array,
			// to avoid concurrency issues
			var checked = new Array();
			for (var i in selected)
				checked.push(selected[i]);
			// remove all items in the copied array
			for (var i in checked)
				this.unselect(checked[i], this.getNode(checked[i]));
		}
	}
	
	var getTargetFolder = function() {
		var selected = this.attr("selectedNodes");
		if (selected == null || selected.length < 1)
			return this.getSelectedItem();
		else {
			var folder = null;
			for (var i in selected) {
				if (selected[i].directory) {
					// if no other folders have been seen, remember this one
					if (folder == null)
						folder = selected[i];
					// otherwise, more than one folder has been selected
					else throw new Error("SELECTED_MULTIPLE");
				}
			}
			return folder;
		}
	}
	
	var getTargetFile = function() {
		var selected = this.attr("selectedNodes");
		if (selected == null || selected.length < 1)
			return this.getSelectedItem();
		else if (selected.length > 1)
			throw new Error("SELECTED_MULTIPLE");
		else return selected[0];
	}
	
	var getRootFolder = function() {
		var rootNode = this.getNode(this.model.root);
		if (rootNode == null)
			return null;
		var children = rootNode.getChildren();
		if (children == null || children[0] == null || children[0].item == null)
			return null;
		else return children[0].item;
	}
	
	/*
	 * Selects the specified file in the user interface tree widget.
	 * 
	 * It is presumed, but not necessary, that this file is not currently
	 * selected.
	 * 
	 * This function will only actually be called by other widget event
	 * handlers, to facilitate simultaneous selection of multiple items in the
	 * folder tree.
	 */
	var select = function(item, node) {
		// add item to selected nodes array
		if (item != null && this.isSelected(item) == false) {
			var selected = this.attr("selectedNodes");
			if (selected == null)
				selected = new Array();
			selected.push(item);
			this.attr("selectedNodes", selected);
		}
		// activate node's selected style
		if (node != null) {
			var labelNodes =
				dojo.query("span.dijitTreeLabel", dojo.byId(node.id));
			if (labelNodes != null && labelNodes.length > 0)
				labelNodes[0].className =
					"dijitTreeLabel selectedTreeNode";
		}
	}
	
	/*
	 * Un-selects the specified file in the user interface tree widget.
	 * 
	 * It is presumed, but not necessary, that this file is currently selected.
	 * 
	 * This function will only actually be called by other widget event
	 * handlers, to facilitate simultaneous selection of multiple items in the
	 * folder tree.
	 */
	var unselect = function(item, node) {
		// remove item from selected nodes array
		if (item != null) {
			var id = this.model.getIdentity(item);
			if (id != null) {
				var selected = this.attr("selectedNodes");
				if (selected != null) {
					for (var i in selected) {
						var selectedId =
							this.model.getIdentity(selected[i]);
						if (selectedId == id) {
							selected.splice(i, 1);
							break;
						}
					}
					this.attr("selectedNodes", selected);
				}
			}
		}
		// deactivate node's selected style
		if (node != null) {
			var labelNodes =
				dojo.query("span.dijitTreeLabel", dojo.byId(node.id));
			if (labelNodes != null && labelNodes.length > 0)
				labelNodes[0].className = "dijitTreeLabel";
		}
	}
	
	/*
	 * Toggles the selection state of the specified file in the user interface
	 * tree widget.
	 * 
	 * This function will only actually be called by other widget event
	 * handlers, to facilitate simultaneous selection of multiple items in the
	 * folder tree.
	 */
	var toggle = function(item, node) {
		if (this.isSelected(item))
			this.unselect(item, node);
		else this.select(item, node);
	}
	
	/*
	 * Determines whether or not the specified file has been selected in the
	 * user interface tree widget.
	 * 
	 * This function will only actually be called by other widget event
	 * handlers, to facilitate simultaneous selection of multiple items in the
	 * folder tree.
	 */
	var isSelected = function(item) {
		if (item == null)
			return false;
		var selected = this.attr("selectedNodes");
		if (selected == null || selected.length < 1)
			return false;
		else {
			var id = this.model.getIdentity(item);
			for (var i in selected)
				if (this.model.getIdentity(selected[i]) == id)
					return true;
			return false;
		}
	}
	
	/*
	 * "onClick" event handler for the user interface tree widget.
	 * 
	 * Basically just toggles the selection state of the clicked file, but also
	 * deliberately deactivates the tree widget's native (one file at a time)
	 * selection state, since it is not meaningful in this implementation.
	 */
	var onClick = function(item, node) {
		this.toggle(item, node);
		node.setSelected(false);
	}
	
	var onOpen = function(item, node) {
		node.setSelected(false);
	}
	
	var onClose = function(item, node) {
		node.setSelected(false);
	}
	
	var focusNode = function(node) {
		node.setSelected(false);
	}
	
	// Dojo object declarations and properties initialization
	/*========================================================================
	 * Backing data store containing file and folder data returned from the
	 * server.
	 * 
	 * This data store is populated with queries to a service specified by the
	 * store's "url" property.  This service must implement the protocol defined
	 * at http://docs.dojocampus.org/dojox/data/FileStore/protocol.
	 * 
	 * From the programmer's perspective, as long as the target service
	 * correctly implements the expected protocol, the tree widget will
	 * automatically drive the query process and do all the work.  The
	 * programmer typically does not need to manually execute any queries for
	 * data.
	 * 
	 * See dojox.data.FileStore
	 *========================================================================*/
	this.store = null;
	this.storeArgs = {
		url: "ManageFiles",
		pathAsQueryParam: true
	};
	
	/*========================================================================
	 * Tree data model to organize data from the backing data store into a
	 * hierarchical tree structure, to populate the user interface tree widget.
	 * 
	 * To properly traverse the data returned by the backing data store, this
	 * data model instance will need to override these methods:
	 * 
	 * mayHaveChildren(item)
	 * getChildren(item, onComplete)
	 * 
	 * By default, items returned by the data store are assumed to maintain
	 * references to their own children.  This default expectation is based on
	 * an assumption that directory structures exposed by this interface are
	 * typically small, and therefore should be eagerly loaded. This is because
	 * the presence of child references in a data item causes the model to
	 * automatically instantiate tree nodes for those references, exposing
	 * further child references that are pursued recursively.
	 * 
	 * However, to implement efficient lazy loading, the data store used by this
	 * model returns objects that refer only to their parent folder, rather than
	 * their children.  This requires a subsequent request to fetch the children
	 * of any item, which implies the need to override the default logic of
	 * "mayHaveChildren" and "getChildren", both of which rely on the assumed
	 * presence of child references in their default implementation.
	 * 
	 * Aside from this, all that needs to be done is to attach the data store to
	 * this data model using the model's "store" property.  The model will then
	 * automatically handle dispatching and processing all of the top-level
	 * widget's requests for data.
	 * 
	 * See dijit.tree.TreeStoreModel
	 *========================================================================*/
	this.model = null;
	this.modelArgs = {
		query: {},
		getChildren: getChildren,
		mayHaveChildren: mayHaveChildren
	};
	
	/*========================================================================
	 * Folder tree widget, to display the user's file and folder data from the
	 * server in a nifty user interface.
	 * 
	 * By default, the tree widget maintains awareness of a single currently
	 * selected node.  In order to allow multiple nodes to be selected at the
	 * same time, special logic will need to be added to maintain awareness of
	 * all of the nodes that have been toggled to a selected state.  To this
	 * end, this widget is enhanced with some extra methods, to facilitate
	 * this feature:
	 * 
	 * isSelected(item)
	 * select(item, node)
	 * unselect(item, node)
	 * toggle(item, node)
	 * 
	 * Furthermore, the following event handler methods need to be overridden,
	 * to both implement the multiple selection feature, and to disable the
	 * default concept of single selection:
	 * 
	 * onClick(item, node)
	 * onOpen(item, node)
	 * onClose(item, node)
	 * 
	 * Aside from this, all that needs to be done is to attach the data model to
	 * this tree widget using the widget's "model" property, and then to bind
	 * the widget to the proper DOM node on the page.  The widget will then
	 * handle populating and displaying the folder tree automatically.
	 * 
	 * see dijit.Tree
	 *========================================================================*/
	this.tree = null;
	this.treeArgs = {
		showRoot: false,
		getNode: getNode,
		getSelectedNode: getSelectedNode,
		getSelectedItem: getSelectedItem,
		clearSelection: clearSelection,
		getTargetFolder: getTargetFolder,
		getTargetFile: getTargetFile,
		getRootFolder: getRootFolder
	};
	this.optTreeArgs = {
		select: select,
		unselect: unselect,
		toggle: toggle,
		isSelected: isSelected,
		onClick: onClick,
		onOpen: onOpen,
		onClose: onClose,
		focusNode: focusNode
	};
}

ProteoSAFeTreeClasses.folderTree.prototype.build = function(
	dirsOnly, multipleSelect
) {
	// get tree div's parent node
	var div = document.getElementById(this.id);
	var parent = div.parentNode;
	// if tree objects were present before, get rid of them
	if (this.tree != null)
		this.tree.destroyRecursive(true);
	if (this.model != null)
		this.model.destroy();
	// re-create the tree div, if necessary
	if (div != null) {
		div = document.createElement("div");
		div.id = this.id;
		parent.appendChild(div);
	}
	// create and initialize store
	var storeArgs = copyHash(this.storeArgs);
	if (dirsOnly)
		storeArgs.options = "dirsOnly";
	this.store = new dojox.data.FileStore(storeArgs);
	// create and initialize model
	var modelArgs = copyHash(this.modelArgs);
	modelArgs.store = this.store;
	this.model = new dijit.tree.ForestStoreModel(modelArgs);
	// create and initialize tree
	var treeArgs = copyHash(this.treeArgs);
	treeArgs.model = this.model;
	if (multipleSelect) {
		for (var key in this.optTreeArgs)
			treeArgs[key] = this.optTreeArgs[key];
	}
	this.tree = new dijit.Tree(treeArgs, this.id);
	this.tree.clearSelection();
}

ProteoSAFeTreeClasses.folderTree.prototype.queueUpdate = function(
	item, action, name
) {
	if (item == null || action == null)
		return;
	var queue = this.tree.attr("updateQueue");
	if (queue == null)
		queue = new Array();
	var queued = {"item": item, "action": action};
	if (name != null)
		queued.name = name;
	queue.push(queued);
	this.tree.attr("updateQueue", queue);
	this.pollUpdateQueue();
}

ProteoSAFeTreeClasses.folderTree.prototype.pollUpdateQueue = function() {
	// if something else is currently being updated, do nothing - it
	// will trigger its own poll of the update queue when it's done
	var current = this.tree.attr("updating");
	if (current != null)
		return;
	// if the queue is empty, there's nothing left to update
	var queue = this.tree.attr("updateQueue");
	if (queue == null || queue.length < 1)
		return;
	// otherwise, retrieve the next item in the queue
	var next = queue[0];
	// remove the item from the queue and mark it as updating
	queue.splice(0, 1);
	this.tree.attr("updating", next);
	// update the item
	if (next.action == "refresh")
		this.refresh(next.item);
	else if (next.action == "dummy" || next.action == "create")
		this.createItem(next.item, next.action, next.name);
	else if (next.action == "rename")
		this.renameItem(next.item, next.name);
	else if (next.action == "delete")
		this.deleteItem(next.item);
	// unrecognized update action, move on and start over
	else {
		this.tree.attr("updating", undefined);
		this.pollUpdateQueue();
	}
}

ProteoSAFeTreeClasses.folderTree.prototype.refresh = function(item) {
	if (item == null) {
		this.tree.attr("updating", undefined);
		return;
	}
	// get item's updated list of children
	var folderModel = this.model;
	var folderTree = this.tree;
	var pollUpdateQueue = this.pollUpdateQueue;
	this.store.fetch({
		query: {"parentDir": folderModel.getIdentity(item)},
		onComplete: function(items) {
			// refresh the tree view
			folderModel.onChildrenChange(item, items);
			// clear any automatic selection
			folderTree.clearSelection();
			// indicate that this update is complete
			folderTree.attr("updating", undefined);
			// trigger the next update
			pollUpdateQueue();
		},
		onError: function() {
			folderTree.attr("updating", undefined);
		}
	});
}

ProteoSAFeTreeClasses.folderTree.prototype.createItem = function(
	parent, action, name
) {
	if (name == null || parent == null || action == null) {
		this.tree.attr("updating", undefined);
		return;
	}
	var id = this.model.getIdentity(parent);
	// assign members to local variables for function closure below
	var folderStore = this.store;
	var folderModel = this.model;
	var folderTree = this.tree;
	var pollUpdateQueue = this.pollUpdateQueue;
	// create and launch file creation request
	var url = "ManageFiles";
	var params = "action=" + action + "&name=" + name + "&folder=" + id;
	var request = createRequest();
	request.open("POST", url, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				// get parent item's updated list of children
				folderStore.fetch({
					query: {"parentDir": id},
					onComplete: function(items) {
						// refresh the tree view
						folderModel.onChildrenChange(parent, items);
						// indicate that this update is complete
						folderTree.attr("updating", undefined);
						// trigger the next update
						pollUpdateQueue();
					},
					onError: function() {
						folderTree.attr("updating", undefined);
					}
				});
			} else {
				alert(request.responseText);
				folderTree.attr("updating", undefined);
			}
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

ProteoSAFeTreeClasses.folderTree.prototype.renameItem = function(
	item, newName
) {
	if (item == null || newName == null) {
		this.tree.attr("updating", undefined);
		return;
	}
	var id = this.model.getIdentity(item);
	// assign members to local variables for function closure below
	var folderStore = this.store;
	var folderTree = this.tree;
	var refresh = this.refresh;
	// create and launch file creation request
	var url = "ManageFiles";
	var params = "path=" + id + "&newName=" + newName;
	var request = createRequest();
	request.open("PUT", url + "?" + params, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				// get parent item
				folderStore.fetchItemByIdentity({
					identity: item.parentDir,
					onItem: function(parent) {
						// refresh parent
						refresh(parent);
					},
					onError: function() {
						folderTree.attr("updating", undefined);
					}
				});
			} else {
				alert(request.responseText);
				folderTree.attr("updating", undefined);
			}
		}
	}
	request.setRequestHeader("If-Modified-Since",
		"Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(null);
}

ProteoSAFeTreeClasses.folderTree.prototype.deleteItem = function(item) {
	if (item == null) {
		this.tree.attr("updating", undefined);
		return;
	}
	// assign members to local variables for function closure below
	var folderStore = this.store;
	var folderModel = this.model;
	var folderTree = this.tree;
	var pollUpdateQueue = this.pollUpdateQueue;
	// create and launch file creation request
	var url = "ManageFiles";
	var params = "path=" + folderModel.getIdentity(item);
	var request = createRequest();
	request.open("DELETE", url + "?" + params, true);
	request.onreadystatechange = function() {
		if (request.readyState == 4) {
			if (request.status == 200) {
				// get parent item's updated list of children
				folderStore.fetch({
					query: {"parentDir": item.parentDir},
					onComplete: function(siblings) {
						// refresh the tree view
						folderModel.onDelete(item, siblings);
						// clear any automatic selection
						folderTree.clearSelection();
						// indicate that this update is complete
						folderTree.attr("updating", undefined);
						// trigger the next update
						pollUpdateQueue();
					},
					onError: function() {
						folderTree.attr("updating", undefined);
					}
				});
			} else {
				alert(request.responseText);
				folderTree.attr("updating", undefined);
			}
		}
	}
	request.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
	request.send(null);
}

/**
 * Implementation for the user-selection folder trees used in the ProteoSAFe
 * resource manager popup window.
 */
ProteoSAFeTreeClasses.selectionTree = function(id, label) {
	// assign basic properties
	if (id == null) {
		// TODO: report error, fail
		return;
	}
	this.id = id;
	if (label == null)
		this.label = "Resources";
	else this.label = label;
	
	// method implementations to be passed into the Dojo object constructors
	var mayHaveChildren = function(item) {
		if (this.store.getValue(item, "id") == "ROOT")
			return true;
		else return false;
	}
	
	var getNode = function(item) {
		if (item == null)
			return null;
		var domNode = dojo.byId(this.domNode.id);
		var treeNodes = dojo.query("[widgetid]", domNode);
		if (treeNodes == null || treeNodes.length < 1)
			return null;
		else {
			var model = this.model;
			var id = model.getIdentity(item);
			var node = null;
			treeNodes.forEach(
				function(treeNode) {
					var widgetNode =
						dijit.byId(treeNode.getAttribute("widgetid"));
					if (model.getIdentity(widgetNode.item) == id)
						node = widgetNode;
				}
			);
			return node;
		}
	}
	
	var getSelectedNode = function() {
		var domNode = dojo.byId(this.domNode.id);
		var selected = dojo.query("span[aria-selected=\"true\"]", domNode);
		if (selected == null || selected.length < 1)
			return null;
		var treeNode = selected[0].parentNode.parentNode.parentNode;
		return dijit.byId(treeNode.getAttribute("widgetid"));
	}
	
	var getSelectedItem = function() {
		var widgetNode = this.getSelectedNode();
		if (widgetNode == null)
			return null;
		else return widgetNode.item;
	}
	
	var clearSelection = function() {
		// clear tree's single selected node
		var selectedNode = this.getSelectedNode();
		if (selectedNode != null)
			selectedNode.setSelected(false);
		// clear all multiple selected nodes, if any
		var selected = this.attr("selectedNodes");
		if (selected != null && selected.length > 0) {
			// build a local copy of the selected nodes array,
			// to avoid concurrency issues
			var checked = new Array();
			for (var i in selected)
				checked.push(selected[i]);
			// remove all items in the copied array
			for (var i in checked)
				this.unselect(checked[i], this.getNode(checked[i]));
		}
	}
	
	var getIconClass = function(item, opened) {
		if (this.model.store.getValue(item, "directory"))
			if (opened)
				return "dijitFolderOpened";
			else return "dijitFolderClosed";
		else return "dijitLeaf";
	}
	
	// Dojo objects
	this.store = null;
	this.model = null;
	this.modelArgs = {
		query: {id: "ROOT"},
		mayHaveChildren: mayHaveChildren
	};
	this.tree = null;
	this.treeArgs = {
		getNode: getNode,
		getSelectedNode: getSelectedNode,
		getSelectedItem: getSelectedItem,
		clearSelection: clearSelection,
		getIconClass: getIconClass
	};
}

ProteoSAFeTreeClasses.selectionTree.prototype.build = function() {
	// get tree div's parent node
	var div = document.getElementById(this.id + "_tree");
	var parent = div.parentNode;
	// if tree objects were present before, get rid of them
	if (this.tree != null)
		this.tree.destroyRecursive(true);
	if (this.model != null)
		this.model.destroy();
	// re-create the tree div
	// re-create the tree div, if necessary
	if (div != null) {
		div = document.createElement("div");
		div.id = this.id + "_tree";
		parent.appendChild(div);
	}
	// collect the currently selected files of this type
	var children = new Array();
	// TODO: legitimately find the form name of the associated input module
	var fileSelector =
		opener.CCMSFormUtils.getModuleInstance(this.id, "mainform");
	if (fileSelector != null) {
		var selectedFiles = fileSelector.getSelectedFiles();
		if (selectedFiles != null && selectedFiles.length > 0) {
			for (var i=0; i<selectedFiles.length; i++) {
				var file = selectedFiles[i];
				children.push({
					id: file,
					path: ProteoSAFeResourceUtils.cleanFilename(file),
					directory: ProteoSAFeResourceUtils.isDirectory(file)
				});
			}
		}
	}
	// instantiate tree objects, and assign relevant properties
	var root = {id: "ROOT", path: "Selected " + this.label, directory: true};
	if (children.length > 0)
		root.children = children;
	var resources = {
		identifier: "id",
		label: "path",
		items: [root]
	};
	// create and initialize store
	this.store = new dojo.data.ItemFileReadStore({data: resources});
	// create and initialize model
	var modelArgs = copyHash(this.modelArgs);
	modelArgs.store = this.store;
	this.model = new dijit.tree.TreeStoreModel(modelArgs);
	// create and initializeup tree
	var treeArgs = copyHash(this.treeArgs);
	treeArgs.model = this.model;
	this.tree = new dijit.Tree(treeArgs, this.id + "_tree");
	this.tree.clearSelection();
}

dojo.require("dijit.Tree");
dojo.require("dojox.data.FileStore");
dojo.require("dojo.data.ItemFileReadStore");
