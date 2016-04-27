/**
 * Result view table implementation
 */
// constructor
function ResultViewTable(tableXML, id, task, nested) {
	// properties
	this.id = id;
	this.div = null;
	this.index = null;
	this.task = task;
	this.description = task.description;
	this.headers = null;
	this.rows = null;
	this.sg = new SortingGroup();
	this.fg = new FilterGroup();
    page_size = this.getPageSize(tableXML);
	this.ng = new NavigationGroup(page_size);
	this.html = null;
	this.data = null;
	this.filteredData = null;
	this.builder = null;
	this.nested = nested;
	this.width = 0;
	// dynamic column stuff
	this.sortHeader = null;
	this.filterHeader = null;
	this.rowCache = null;
	this.columns = null;
	// build the table
	this.buildTable(tableXML);
	// register the table instance, in case it needs to be rebuilt
	tableManager.setBlock(this.id, this);
}

ResultViewTable.prototype.getPageSize = function(tableXML){
    page_size = 30;
    blocks = tableXML.getElementsByTagName("block")
    if(blocks.length != 1){
        return page_size;
    }

    if(blocks[0].attributes.pagesize != null){
        page_size = parseInt(blocks[0].attributes.pagesize.value)
    }

    return page_size;
}

// build table from XML specification
ResultViewTable.prototype.buildTable = function(tableXML) {
	// initialize basic table properties
	this.headers = new Array();
	this.rows = new Array();
	// set up builder properties, in case this table needs to be rebuilt
	var builder = this.builder;
	if (builder == null)
		builder = this.builder = {};
	if (tableXML == null)
		tableXML = builder.tableXML;
	else builder.tableXML = tableXML;
	if (this.nested == null)
		this.nested = builder.nested;
	else builder.nested = this.nested;
	if (tableXML == null)
		return;
	// set up column titles and sort controls
	var sortHeader = new Array();
	var filterCallback = this.filter;
	var table = this;
	if (this.nested)
		sortHeader.push(null);
	else sortHeader.push(
		renderButton("Filter", "filter",
			function() { filterCallback(table); }));
	// filter controls
	var filterHeader = new Array();
//	if (this.nested)
		filterHeader.push(null);
//	else filterHeader.push(markFilter(this.fg, "selected", "checked only"));
	// set up row renderer cache, since we can't actually instantiate any
	// row renderer functions until after they're all processed, to ensure that
	// they all line up and such
	this.rowCache = new Array();
	var rowExpanders = new Array();
	this.width = 1;
	// set up row renderers
	var rows = tableXML.getElementsByTagName("row");
	for (var i=0; i<rows.length; i++) {
		var rowNode = rows[i];
		var rowId = "" + i;
		var row = new Array();
		// if an expander is specified for this row, set it up
		var expanderIcons = getAttribute(rowNode, "expander");
		if (expanderIcons != null) {
			var expander = {rowId: rowId};
			var icons = expanderIcons.split(":");
			if (icons.length > 1) {
				expander.collapsed = icons[0];
				expander.expanded = icons[1];
			}
			else{
				expander.collapsed = expander.expanded = expanderIcons;
			}

			expanderIconType = getAttribute(rowNode, "expandericontype");
			if(expanderIconType != null){
				expander.icontype = expanderIconType;
			}
			else{
				expander.icontype = "image";
			}

			rowExpanders.push(expander);
		}
		// set up controls for this row
		var columnLoaders = new Array();
		// set up column renderers
		columns = rowNode.getElementsByTagName("column");
		for (var j=0; j<columns.length; j++) {
			var column = columns[j];
			var columnId = "" + j;
			var type = getAttribute(column, "type");
			// retrieve attributes for this column
			var attributes = {};
			var nodeAttributes = column.attributes;
			for (var k=0; k<nodeAttributes.length; k++) {
				var attribute = nodeAttributes.item(k);
				attributes[attribute.nodeName] = attribute.nodeValue;
			}
			attributes.blockId = this.id;
			attributes.task = this.task;
			// ensure that this column's state is known
			var columnState = tableManager.getColumn(this.id, attributes.field);
			if (columnState == null) {
				var label = attributes.label;
				if (label == null)
					label = attributes.field;
				tableManager.addColumn(this.id, attributes.field, label);
			}
			// check this column's hidden state
			if (tableManager.isColumnHidden(this.id, attributes.field))
				continue;
			// if this is the main row, determine the table's width
			if (i == 0) {
				var colspan = parseInt(attributes.colspan);
				if (isNaN(colspan) || colspan < 1)
					colspan = 1;
				this.width += colspan;
			}
			// if a loader is specified for this column, set it up
			var loaderIcons = getAttribute(column, "loader");
			if (loaderIcons != null) {
				var loader = {columnId: columnId};
				var icons = loaderIcons.split(":");
				if (icons.length > 1) {
					loader.unloaded = icons[0];
					loader.loaded = icons[1];
				} else loader.unloaded = loader.loaded = loaderIcons;
				columnLoaders.push(loader);
			}
			// retrieve parameter mappings for this column, if any
			var parameters = null;
			var parameterNodes = column.getElementsByTagName("parameter");
			for (var k=0; k<parameterNodes.length; k++) {
				var parameter = parameterNodes.item(k);
				var name = getAttribute(parameter, "name");
				if (name == null)
					continue;
				var value = getAttribute(parameter, "value");
				if (value == null) {
					value = getAttribute(parameter, "file");
					if (value != null)
						value = "FILE->" + value;
					else {
						value = getAttribute(parameter, "folder");
						if (value != null)
							value = "FOLDER->" + value;
					}
				}
				if (value == null)
					continue;
				else if (parameters == null)
					parameters = {};
	    		// if it's a hash value, then don't try to resolve it
				if (value[0] == "#")
					parameters[name] = value;
				else parameters[name] = resolveParameters(value);
			}
			if (parameters != null)
				attributes.parameters = parameters;
			// retrieve proper column handler for this column
			var handler = getColumnHandler(type);
			// if this is the first (main) row, assign proper
			// header controls for this column
			if (i == 0) {
				// sort header control
				if (handler == null || handler.sort == null)
					sortHeader.push(null);
				else {
					sortHeader.push(
						handler.sort(this.sg,
							attributes.label, attributes.field,
							attributes.tooltip));
				}
				// filter header control
				if (!this.nested && filterHeader != null) {
					if (handler == null || handler.filter == null)
						filterHeader.push(null);
					else filterHeader.push(
						handler.filter(this.fg,
							attributes.field, attributes.width));
				}
			}
			// assign proper renderer for this column
			if (handler == null || handler.render == null)
				row.push(null);
			else row.push(handler.render(this.id, rowId, columnId, attributes));
		}
		// if this is not the first (main) row, set up this row's control cell
		if (i != 0) {
			row.splice(0, 0,
				renderRowControl(
					this.id, rowId, "control", null, columnLoaders, this.nested)
			);
		}
		// cache row details, to instantiate its renderer function later
		var rowElement = {
			"row": row,
			"id": rowId,
			"columnLoaders": columnLoaders,
			"expander": expander
		};
		this.rowCache.push(rowElement);
	}
	// now that we've collected all the row information,
	// finalize all headers and row renderers
	// TODO: come up with a better way of deciding whether this table
	// needs a title and navigation header
	if (this.id == "main")
		this.headers.push(getTitleHeaderRenderer(
			this.id, this.ng, this.description, this.width));
	this.headers.push(createCaptionHeader(null, sortHeader, this.width));
	this.headers.push(createExtraHeader(null, filterHeader, this.width));
	for (var i=0; i<this.rowCache.length; i++) {
		var rowElement = this.rowCache[i];
		// set up the main row's control cell
		if (i == 0) {
			rowElement.row.splice(0, 0,
				renderRowControl(
					this.id, rowElement.id, "control",
					rowExpanders, rowElement.columnLoaders, this.nested)
			);
		}
		// instantiate the row renderer
		if (rowElement.expander != null)
			this.rows.push(createRowRender(this.id, rowElement.id,
				expandableRow, rowElement.row, this.width));
		else this.rows.push(createRowRender(this.id, rowElement.id,
				defaultRow, rowElement.row, this.width));
	}
	// save the headers, in case we need to modify
	// them later when adding new dynamic columns
	this.sortHeader = sortHeader;
	this.filterHeader = filterHeader;
}

// render the table
ResultViewTable.prototype.render = function(div, index) {
	if (div != null)
		this.div = div;
	if (this.div == null) {
		alert("No div was provided under which to render this result block.");
		return;
	}
	if (index != null)
		this.index = index;
	if (this.index == null)
		this.index = 1;
	// remove old table, if present
	var oldTable = document.getElementById(this.id);
	if (oldTable != null)
		this.div.removeChild(oldTable);
	// set up table HTML
	this.html = document.createElement("table");
	this.html.id = this.id;
	this.html.setAttribute("class", "tabular");
	setupTable(this.html, decorateTable, this.headers);
	spliceChild(this.div, this.html, this.index);
	// draw table
	this.draw();
	// read location hash, apply any pre-render selections encoded there
	var hash = parseLocationHash();
	if (hash != null && this.index == 0) {
		// apply any pre-selected filters
		var filterFound = false;
		for (var key in hash) {
			// a location hash key represents a filter if it is an ID
			// corresponding to an HTML element found on the page
			var element = document.getElementById(key);
			if (element != null) {
				if (element.type == "checkbox" && hash[key] == "checked") {
					element.checked = true;
					if (element.activate)
						element.activate();
					filterFound = true;
				} else if (element.type == "text") {
					element.value = hash[key];
					filterFound = true;
				}
			}
		}
		if (filterFound)
			this.filter();
		// apply any pre-selected sorts
		var sorts = hash["table_sort_history"];
		if (sorts != null) {
			var sortsArray = sorts.split(";");
			for (var i=0; i<sortsArray.length; i++) {
				var sort = sortsArray[i];
				var element = document.getElementById(sort);
				if (element != null)
					element.activate();
			}
		}
	}
}

// draw the table
ResultViewTable.prototype.draw = function(table) {
	if (table == null)
		table = this;
	// set up sorter
	table.sg.setSource(table.filteredData);
	table.sg.setCallback(function() {
		table.draw(table);
	});
	// set up navigator
	table.ng.setSource(table.filteredData);
	table.ng.setCallback(function(begin, end) {
		renderRows(table.html, table.filteredData, table.rows, begin, end);
	});
	// render rows
	renderRows(table.html, table.filteredData, table.rows,
		table.ng.getBegin(), table.ng.getEnd());
}

// set data to the table
ResultViewTable.prototype.setData = function(data) {
	// first, check data to see if there are any dynamic columns to add
	var changed = false;
	if (data != null) {
        var first = data[0];
        var irrelevant = {};
        if (first != null) {
        	// initialize array for dynamic columns, if any are found
        	var columns = new Array();
        	// add any dynamic columns found to the array
        	for (var key in first) {
    			var value = first[key];
        		if (key.indexOf("_dyn_#") == 0) {
        			var column = {};
        			column.attributes = {
        				"field": key,
        				"label": key.substring(6)
        			};
        			// if the first element value is a number,
        			// then this is probably a numerical column
        			if (!isNaN(parseFloat(value)) && isFinite(value)) {
        				column.sort = numberSorter;
        				column.filter = rangeFilter;
        				// if the value has an "e" or an "E" in it,
        				// then it's an exponential number
        				if (value.indexOf("e") >= 0 || value.indexOf("E") >= 0)
        					column.render = renderExpNum;
        				else column.render = renderFloat;
        				column.attributes.width = "3";
        			} else {
        				column.sort = plainSorter;
        				column.filter = plainFilter;
        				column.render = renderPlain;
        				column.attributes.width = "8";
        			}
        			// add this to the list of dynamic columns we want to add
        			columns.push(column);
        		}
                // then, note any "irrelevant" column values in this row
        		if (value == 0 || value == -1 || value == "null")
        			irrelevant[key] = value;
        	}
        	// if any dynamic columns were found, add them
        	if (columns.length > 0) {
        		this.columns = columns;
        		changed = true;
        	}
        }
        // if any irrelevant columns were found, verify that
        // their values are irrelevant for all rows
        var irrelevantCount = 0;
        for (var column in irrelevant)
        	irrelevantCount++;
        if (irrelevantCount >= 0) {
            for (var i=0; i<data.length; i++) {
            	var row = data[i];
            	// for each potentially irrelevant column, check its value
            	var noLongerIrrelevant = new Array();
            	for (var column in irrelevant)
            		if (row[column] != irrelevant[column])
            			noLongerIrrelevant.push(column);
                // if a column's value for this row is not irrelevant, then
            	// remove that column from the list of columns to be checked
            	for (var j=0; j<noLongerIrrelevant.length; j++) {
            		delete irrelevant[noLongerIrrelevant[j]];
            		irrelevantCount--;
            	}
            	// if no irrelevant columns are left, stop checking
            	if (irrelevantCount <= 0)
            		break;
            }
        }
        // if any columns were found to be irrelevant for all rows, hide them
        for (var column in irrelevant) {
        	var columnState = tableManager.getColumn(this.id, column);
        	if (columnState != null) {
        		consoleLog("Hiding irrelevant column [" + column + "].");
        		columnState.hidden = true;
        		changed = true;
        	}
        }
        // if any changes were made to this table's structure, rebuild it
        if (changed) {
        	this.buildTable();
        	this.addColumns(this.columns);
        }
	}
	// finally, assign the data to the table
	this.data = data;
	this.filteredData = data;
}

// filter the table
ResultViewTable.prototype.filter = function(table) {
	if (table == null)
		table = this;
	table.filteredData = table.fg.filter(table.data);
	table.sg.setSource(table.filteredData);
	table.sg.sort();
}

// rebuild and re-render the table
ResultViewTable.prototype.rebuildTable = function() {
	this.buildTable();
	// re-render any previously added dynamic columns
	this.addColumns(this.columns);
	this.render();
}

/**
 * Add new columns to the table.
 *
 * The argument "columns" object is assumed to be a Javascript Array,
 * containing hashes each with the following required members:
 *
 * sort:       column sorting function, e.g. plainSorter from render.js
 *
 * filter:     column filtering function, e.g. plainFilter from render.js
 *
 * render:     table cell rendering function, e.g. renderPlain from render.js
 *
 * attributes: column attributes, such as those extracted from <column> elements
 *             in result.xml - at minimum, these attributes should be defined:
 *
 *             field:   data record field name
 *             label:   displayed column header label
 *
 *             Also, these optional attributes are supported:
 *
 *             tooltip: help text displayed when hovering over the column header
 *             width:   width of the rendered table cell
 */
ResultViewTable.prototype.addColumns = function(columns) {
	if (columns == null || columns.length == null || columns.length < 1 ||
		this.rowCache == null || this.rowCache.length < 1)
		return;
	// get main row and its ID
	var mainRow = this.rowCache[0];
	var row = mainRow.row;
	var rowId = mainRow.id;
	if (row == null || rowId == null)
		return;
	// save added columns, in case the table needs to be rebuilt
	this.columns = columns;
	// add the new columns to the rendered table
	for (var i=0; i<columns.length; i++) {
		var column = columns[i];
		var columnId = "_dyn_#_" + i;
		var attributes = column.attributes;
		// ensure that this column's state is known
		var columnState = tableManager.getColumn(this.id, attributes.field);
		if (columnState == null) {
			var label = attributes.label;
			if (label == null)
				label = attributes.field;
			tableManager.addColumn(this.id, attributes.field, label);
		}
		// check this column's hidden state
		if (tableManager.isColumnHidden(this.id, attributes.field))
			continue;
		// if the column is not hidden, add to the table's width
		var colspan = parseInt(attributes.colspan);
		if (isNaN(colspan) || colspan < 1)
			colspan = 1;
		this.width += colspan;
		// add sort header for this new column
		this.sortHeader.push(column.sort(this.sg,
			attributes.label, attributes.field, attributes.tooltip));
		// add filter header for this new column
		if (!this.nested)
			this.filterHeader.push(
				column.filter(this.fg, attributes.field, attributes.width));
		// add table cell renderer for this new column
		row.push(column.render(this.id, rowId, columnId, attributes));
	}
	// replace old headers with the updated ones
	this.headers = new Array();
	// TODO: come up with a better way of deciding whether this table
	// needs a title and navigation header
	if (this.id == "main")
		this.headers.push(getTitleHeaderRenderer(
			this.id, this.ng, this.description, this.width));
	this.headers.push(
		createCaptionHeader(null, this.sortHeader, this.width));
	if (!this.nested)
		this.headers.push(
			createExtraHeader(null, this.filterHeader, this.width));
	// replace old rows with the updated ones
	this.rows = new Array();
	for (var i=0; i<this.rowCache.length; i++) {
		var rowElement = this.rowCache[i];
		// splice in the updated main row
		if (i == 0)
			rowElement.row = row;
		if (rowElement.expander != null)
			this.rows.push(createRowRender(this.id, rowElement.id,
				expandableRow, rowElement.row, this.width));
		else this.rows.push(createRowRender(this.id, rowElement.id,
				defaultRow, rowElement.row, this.width));
	}
}

// assign this implementation to block type "table"
resultViewBlocks["table"] = ResultViewTable;

/**
 * In the ProteoSAFe UI, functionality for rendering tabular result data of
 * various types is meant to be both reusable and extensible.  This is achieved
 * by encapsulating the logic for rendering table columns of a particular type
 * into what is referred to as a "column handler" object.  Each column type in
 * the ProteoSAFe UI tabular result view is associated with a handler class
 * implemented to handle the unique processing requirements of that column's
 * data type.
 *
 * Any time a new column type is introduced, with new UI features and
 * requirements, a new column handler class should be implemented to process
 * the new column type properly.
 *
 * Each column handler class should contain at least a cell rendering function.
 * This function should return a Javascript closure according to the following
 * function signature:
 *
 * render(tableId, rowId, columnId, [attributes])
 *   returns function(td, record, index)
 *
 * If the column type is meant to be part of the main row for each tabular
 * data item, then its handler class should also contain rendering functions
 * for the sorting and filtration headers:
 *
 * sort(sortingGroup, label, fieldname, tooltipText)
 *   returns function(td)
 *
 * filter(filterGroup, fieldname, width)
 *   returns function(td)
 */
var textColumnHandler = {
	render: renderPlain,
	sort: plainSorter,
	filter: plainFilter
};

var integerColumnHandler = {
	render: renderInteger,
	sort: numberSorter,
	filter: rangeFilter
};

var floatColumnHandler = {
	render: renderFloat,
	sort: numberSorter,
	filter: rangeFilter
};

var exponentialColumnHandler = {
	render: renderExpNum,
	sort: numberSorter,
	filter: rangeFilter
};

var expandableColumnHandler = {
    render: renderExpandable,
    sort: plainSorter,
    filter: plainFilter
};

var linkColumnHandler = {
	render: renderLink,
	sort: plainSorter,
	filter: plainFilter
};

/**
 * Structure registering result view table column types with their appropriate
 * implementations.
 *
 * The purpose of this object is to assign any new column handler
 * implementations to their appropriate (string) types, as referenced in
 * result.xml.  Any new type of result view table column will need to be
 * implemented and registered to its type in this manner.
 */
var columnHandlers = {
	text: textColumnHandler,
	integer: integerColumnHandler,
	float: floatColumnHandler,
	exponential: exponentialColumnHandler,
	expandable: expandableColumnHandler,
	link: linkColumnHandler
};

/**
 * Retrieves the appropriate column handler object for the specified column
 * type.
 */
function getColumnHandler(type) {
	if (type == null)
		return;
	else return columnHandlers[type];
}

/**
 * Table row decorator functions should implement the following function
 * signature:
 *
 * decorate(tr, record, tableId, rowId)
 */
function defaultRow(tr, record, tableId, rowId) {
	tr.id = getRowElementId(tableId, record.id, rowId);
}

function expandableRow(tr, record, tableId, rowId) {
	defaultRow(tr, record, tableId, rowId);
	tr.style.display = "none";
	if (tableManager.isRowExpanded(tableId, record.id, rowId))
		enableRow(tr.id, true);
}

/**
 * Header renderers
 */
function getTitleHeaderRenderer(
	tableId, navigationGroup, taskDescription, colspan
) {
	return function(tr) {
		var td = navigationGroup.renderControls(tr, tableId);
		if (colspan == null)
			td.colSpan = "20";
		else td.colSpan = colspan;
		navigationGroup.setTitle(
			taskDescription,
			function(begin, end, total) {
				return " Hits " + (begin + 1) + " ~ " + end +
					" out of " + total + " ";
			}
		);
	}
}

/**
 * Column renderers
 */
function renderRowControl(
	tableId, rowId, columnId, expanders, loaders, nested
) {
	return function(td, record, index) {
		td.id = getColumnElementId(tableId, record.id, rowId, columnId)
		td.noWrap = "true";
		// if row expanders are present, add expander controls
		if (expanders != null) {
			for (var i in expanders) {
				var expander = expanders[i];
				// create and initialize expander control
				var expandControl = document.createElement("BUTTON");
				if(expander.icontype == "text"){
					var expandControl = document.createElement("BUTTON");
					var icon = tableManager.isRowExpanded(
						tableId, record.id, expander.rowId)
						? expander.expanded : expander.collapsed;
					expandControl.textContent = icon;
				}
				else if(expander.icontype == "image"){
					var expandControl = document.createElement("img");
					var icon = tableManager.isRowExpanded(
						tableId, record.id, expander.rowId)
						? expander.expanded : expander.collapsed;
					expandControl.src = "/ProteoSAFe/images/" + icon + ".png";
				}
				else{
					var expandControl = document.createElement("BUTTON");
					var icon = tableManager.isRowExpanded(
						tableId, record.id, expander.rowId)
						? expander.expanded : expander.collapsed;
					expandControl.textContent = icon;
				}


				expandControl.className = "selectable";
				expandControl.style.verticalAlign = "middle";
				expandControl.onclick =
					getExpanderControl(tableId, record.id, expander.rowId);
				// register expander control with table manager
				tableManager.setRowExpander(tableId, record.id,
					expander.rowId, expander, expandControl);
				// add expander control to this table cell
				td.appendChild(expandControl);
				td.appendChild(document.createTextNode(" "));
			}
		}
		// if column loaders are present, add loader controls
		if (loaders != null) {
			for (var i in loaders) {
				var loader = loaders[i];
				var loaded = tableManager.isColumnLoaded(
					tableId, record.id, rowId, loader.columnId);
				var icon = loaded ? loader.loaded : loader.unloaded;
				var loadControl = document.createElement("img");
				loadControl.src = "/ProteoSAFe/images/" + icon + ".png";
				loadControl.style.verticalAlign = "middle";
				if (loaded)
					loadControl.onclick = undefined;
				else {
					loadControl.className = "selectable";
					loadControl.onclick = function() {
						tableManager.loadColumn(
							tableId, record.id, rowId, loader.columnId);
						loadControl.src =
							"/ProteoSAFe/images/" + loader.loaded + ".png";
						loadControl.className = "";
						loadControl.onclick = undefined;
						return false;
					};
				}
				td.appendChild(loadControl);
				td.appendChild(document.createTextNode(" "));
			}
		}
		// if this is the main row's control cell, add row index
		if (rowId == "0")
			td.appendChild(document.createTextNode("" + (index + 1)));
	}
}

/**
 * Result view table column handler for nested block columns.
 */
function renderBlock(tableId, rowId, columnId, attributes) {
	return function(td, record, index) {
		td.id = getColumnElementId(tableId, record.id, rowId, columnId);
		if (attributes.colspan != null)
			td.colSpan = attributes.colspan;
		// set up on-demand loader function
		var columnLoader = function() {
			// see if this block has already been retrieved and initialized
			var blockId = td.id + "_block";
			var blockInstance = tableManager.getBlock(blockId);
			// if this block already exists, just render it
			if (blockInstance != null) {
				removeChildren(td);
				blockInstance.render(td);
				return;
			}
			// otherwise, set up download URL to retrieve block from scratch
			var task = attributes.task;
			var block = attributes.block;
		    var url = "DownloadBlock?task=" + task.id + "&block=" + block;
		    // get remaining request parameters
		    var parameters = attributes.parameters;
		    if (parameters != null)
		    	for (var parameter in parameters)
		    		url += "&" + parameter + "=" + encodeURIComponent(
		    			resolveFields(parameters[parameter], record));
		    // submit request
			var request = createRequest();
			request.open("GET", url, true);
			request.onreadystatechange = function() {
				if (request.readyState == 4) {
					if (request.status == 200) {
						// get this block's specification
						var blockXML =
							getBlockSpecification(request.responseXML, block);
						if (blockXML == null) {
							alert("Could not find result specification for " +
								"block \"" + block + "\" of task \"" +
								task.id + "\".");
							return;
						}
						// set up this block
						var blockInstance = null;
						var blockType = getAttribute(blockXML, "type");
						var blockConstructor = getResultViewBlock(blockType);
						if (blockConstructor != null) {
							blockInstance = new blockConstructor(
								blockXML, blockId, task, true);
						} else {
							alert("Could not find implementation for result " +
								"block type \"" + blockType + "\".");
							return;
						}
						// get this block's data
						var dataXML = null;
						var dataNodes =
							request.responseXML.getElementsByTagName(
								"blockData");
						if (dataNodes != null && dataNodes.length > 0)
							dataXML = dataNodes[0];
						if (dataXML == null) {
							alert("Could not find data for block \"" + block +
								"\" of task \"" + task.id + "\".");
							return;
						}
						var dataText = dataXML.textContent;
						var data = null;
						try {
							data = JSON.parse(dataText);
						} catch (error1) {
							try {
								data = eval(dataText);
							} catch (error2) {
								data = null;
							}
						}
						if (data != null) {
							blockInstance.setData(data);
						} else {
							alert("Could not parse data for block \"" + block +
								"\" of task \"" + task.id + "\".");
							return;
						}
						// render the block
						removeChildren(td);
						blockInstance.render(td);
					} else alert(
						"Could not retrieve block instance for block \"" +
						block + "\" of task \"" + task.id + "\".");
				}
			}
			request.setRequestHeader("If-Modified-Since",
				"Sat, 1 Jan 2000 00:00:00 GMT");


            request.send(null);

		};
		// if this column is already loaded, just invoke the loader function
		if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
			columnLoader();
		// otherwise, assign the loader function to this record,
		// so that it can be invoked when the column is loaded
		else tableManager.setColumnLoader(
			tableId, record.id, rowId, columnId, columnLoader);
	}
}

function renderCallbackBlock(tableId, rowId, columnId, attributes, columncallback) {
    return function(td, record, index) {
        td.id = getColumnElementId(tableId, record.id, rowId, columnId);
        if (attributes.colspan != null)
            td.colSpan = attributes.colspan;
        // set up on-demand loader function
        var columnLoader = function() {
            // see if this block has already been retrieved and initialized
            var blockId = td.id + "_block";
            var blockInstance = tableManager.getBlock(blockId);
            // if this block already exists, just render it
            if (blockInstance != null) {
                removeChildren(td);
                blockInstance.render(td);
                return;
            }
            // otherwise, set up download URL to retrieve block from scratch
            var task = attributes.task;
            var block = attributes.block;
            var url = "DownloadBlock?task=" + task.id + "&block=" + block;
            // get remaining request parameters
            var parameters = attributes.parameters;
            if (parameters != null)
                for (var parameter in parameters)
                    url += "&" + parameter + "=" + encodeURIComponent(
                        resolveFields(parameters[parameter], record));




            var resolved_parameters = new Object();
            if (parameters != null){
                for (var parameter in parameters){
                    resolved_parameters[parameter] = resolveFields(parameters[parameter], record);
                }
            }

            var responseXML = columncallback(block, resolved_parameters);

            var blockXML = getBlockSpecification(responseXML, block);
            if (blockXML == null) {
                    alert("Could not find result specification for " +
                            "block \"" + block + "\" of task \"" +
                            task.id + "\".");
                    return;
            }
            // set up this block
            var blockInstance = null;
            var blockType = getAttribute(blockXML, "type");
            var blockConstructor = getResultViewBlock(blockType);
            if (blockConstructor != null) {
                    blockInstance = new blockConstructor(
                            blockXML, blockId, task, true);
            } else {
                    alert("Could not find implementation for result " +
                            "block type \"" + blockType + "\".");
                    return;
            }
            // get this block's data
            var dataXML = null;
            var dataNodes =
                    responseXML.getElementsByTagName(
                            "blockData");
            if (dataNodes != null && dataNodes.length > 0)
                    dataXML = dataNodes[0];
            if (dataXML == null) {
                    alert("Could not find data for block \"" + block +
                            "\" of task \"" + task.id + "\".");
                    return;
            }
            var dataText = dataXML.textContent;
            var data = null;
            try {
                    data = JSON.parse(dataText);
            } catch (error1) {
                    try {
                            data = eval(dataText);
                    } catch (error2) {
                            data = null;
                    }
            }
            if (data != null) {
                    blockInstance.setData(data);
            } else {
                    alert("Could not parse data for block \"" + block +
                            "\" of task \"" + task.id + "\".");
                    return;
            }
            // render the block
            removeChildren(td);
            blockInstance.render(td);

        };
        // if this column is already loaded, just invoke the loader function
        if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
            columnLoader();
        // otherwise, assign the loader function to this record,
        // so that it can be invoked when the column is loaded
        else tableManager.setColumnLoader(
            tableId, record.id, rowId, columnId, columnLoader);
    }
}

var blockHandler = {
	render: renderBlock
};

var callbackblockHandler = {
    render: renderCallbackBlock
};

// assign this column handler implementation to column type "block"
columnHandlers["block"] = blockHandler;
columnHandlers["callbackblock"] = callbackblockHandler;

/**
 * Result view table column handler for linked views.
 */
function renderViewLink(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		// special case: if the value is 0 or -1, then don't
		// render the link, just return the scalar value
		if (value == 0 || value =="0" || value == -1 || value == "-1")
			return value;
		// special case: if the value is null, then look for a special
		// null label; if not found, then just return the scalar value
	    var parameters = attributes.parameters;
		if (value == null || value.toLowerCase() == "null") {
			if (parameters == null)
				return value;
			var nullLabel = parameters["null_label"];
			if (nullLabel == null)
				return value;
			else {
				value = nullLabel;
				// remove the null label from the parameters hash,
				// since we don't want to see it in the request URL
				delete parameters["null_label"];
			}
		}
		// set up basic link URL
		var task = attributes.task.id;
		var view = attributes.view;
	    var url = "result.jsp?task=" + task + "&view=" + view;
	    // get remaining request parameters
	    if (parameters != null) {
	    	for (var parameter in parameters) {
	    		var parameterValue = parameters[parameter];
	    		// if it's a hash value, then don't treat it as a request param
	    		if (parameterValue[0] == "#")
	    			url += parameterValue;
	    		else url += "&" + parameter + "=" + encodeURIComponent(
	    			resolveFields(parameterValue, record));
	    	}
	    }
		// create an anchor element for the link
		var link = document.createElement("a");
		link.href = url;
		// set link's text content
		if (value == null)
			link.textContent = "N/A";
		else link.textContent = value;
		return link;
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

var viewLinkHandler = {
	render: renderViewLink,
	sort: plainSorter,
	filter: plainFilter
};

var numericalViewLinkHandler = {
	render: renderViewLink,
	sort: numberSorter,
	filter: rangeFilter
};

// assign this column handler implementation to column type "view"
columnHandlers["view"] = viewLinkHandler;
columnHandlers["numberview"] = numericalViewLinkHandler;

/**
 * Global table state management structure
 */
var tableManager = new TableState();

function TableState() {
	// properties
	this.tables = {};
}

TableState.prototype.isRowExpanded = function(tableId, recordId, rowId) {
	if (tableId == null || recordId == null || rowId == null)
		return false;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		return false;
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		return false;
	var recordState = tableRows[recordId];
	if (recordState == null)
		return false;
	// return the loaded status of this row
	var rowState = recordState[rowId];
	if (rowState == null || rowState.expanded == null)
		return false;
	else return rowState.expanded;
}

TableState.prototype.expandRow = function(tableId, recordId, rowId) {
	if (tableId == null || recordId == null || rowId == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		tableRows = tableState.rows = {};
	var recordState = tableRows[recordId];
	if (recordState == null)
		recordState = tableRows[recordId] = {};
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		rowState = recordState[rowId] = {};
	// try to load all of this row's columns
	var rowColumns = rowState.columns;
	if (rowColumns != null)
		for (var columnId in rowColumns)
			this.loadColumn(tableId, recordId, rowId, columnId);
	// expand row in page
	rowState.expanded = true;
	var control = rowState.expandControl;
	var expanded = rowState.expandedIcon;
	if (control != null && expanded != null){
		if(rowState.expandericontype == "text"){
			control.textContent = expanded;
		}
		if(rowState.expandericontype == "image"){
			control.src = "/ProteoSAFe/images/" + expanded + ".png";
		}
		//else{
		//	control.textContent = expanded;
		//}

	}

	enableRow(getRowElementId(tableId, recordId, rowId), true);
}

TableState.prototype.collapseRow = function(tableId, recordId, rowId) {
	if (tableId == null || recordId == null || rowId == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		tableRows = tableState.rows = {};
	var recordState = tableRows[recordId];
	if (recordState == null)
		recordState = tableRows[recordId] = {};
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		rowState = recordState[rowId] = {};
	// collapse row in page
	rowState.expanded = false;
	var control = rowState.expandControl;
	var collapsed = rowState.collapsedIcon;
	if (control != null && collapsed != null){
		if(rowState.expandericontype == "text"){
			control.textContent = collapsed;
		}
		if(rowState.expandericontype == "image"){
			control.src = "/ProteoSAFe/images/" + collapsed + ".png";
		}
		//else{
		//	control.textContent = collapsed;
		//}

	}
	enableRow(getRowElementId(tableId, recordId, rowId), false);
}

TableState.prototype.setRowExpander = function(
	tableId, recordId, rowId, expander, control
) {
	if (tableId == null || recordId == null || rowId == null ||
		expander == null || control == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		tableRows = tableState.rows = {};
	var recordState = tableRows[recordId];
	if (recordState == null)
		recordState = tableRows[recordId] = {};
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		rowState = recordState[rowId] = {};
	// set up this row's expander
	rowState.expandedIcon = expander.expanded;
	rowState.collapsedIcon = expander.collapsed;
	rowState.expandericontype = expander.icontype;
	rowState.expandControl = control;
}

TableState.prototype.collapseAllRows = function() {
	for (var tableId in this.tables) {
		var tableState = this.tables[tableId];
		if (tableState == null)
			continue;
		var tableRows = tableState.rows;
		if (tableRows == null)
			continue;
		else for (var recordId in tableRows) {
			var recordState = tableRows[recordId];
			if (recordState == null)
				continue;
			else for (var rowId in recordState)
				this.collapseRow(tableId, recordId, rowId);
		}
	}
}

TableState.prototype.isColumnLoaded = function(
	tableId, recordId, rowId, columnId
) {
	if (tableId == null || recordId == null || rowId == null ||
		columnId == null)
		return false;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		return false;
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		return false;
	var recordState = tableRows[recordId];
	if (recordState == null)
		return false;
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		return false;
	// return the loaded status of this column
	var rowColumns = rowState.columns;
	if (rowColumns == null)
		return false;
	var columnState = rowColumns[columnId];
	if (columnState == null || columnState.loaded == null)
		return false;
	else return columnState.loaded;
}

TableState.prototype.loadColumn = function(tableId, recordId, rowId, columnId) {
	if (tableId == null || recordId == null || rowId == null ||
		columnId == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		tableRows = tableState.rows = {};
	var recordState = tableRows[recordId];
	if (recordState == null)
		recordState = tableRows[recordId] = {};
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		rowState = recordState[rowId] = {};
	// get this column's state structure
	var rowColumns = rowState.columns;
	if (rowColumns == null)
		rowColumns = rowState.columns = {};
	var columnState = rowColumns[columnId];
	if (columnState == null)
		columnState = rowColumns[columnId] = {};
	// only invoke the loader function if the column is not already loaded
	if (!columnState.loaded && columnState.loader != null)
		columnState.loader();
	columnState.loaded = true;
}

TableState.prototype.setColumnLoader = function(
	tableId, recordId, rowId, columnId, columnLoader
) {
	if (tableId == null || recordId == null || rowId == null ||
		columnId == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this record's state structure
	var tableRows = tableState.rows;
	if (tableRows == null)
		tableRows = tableState.rows = {};
	var recordState = tableRows[recordId];
	if (recordState == null)
		recordState = tableRows[recordId] = {};
	// get this row's state structure
	var rowState = recordState[rowId];
	if (rowState == null)
		rowState = recordState[rowId] = {};
	// get this column's state structure
	var rowColumns = rowState.columns;
	if (rowColumns == null)
		rowColumns = rowState.columns = {};
	var columnState = rowColumns[columnId];
	if (columnState == null)
		columnState = rowColumns[columnId] = {};
	// set up this column's loader
	columnState.loaded = false;
	columnState.loader = columnLoader;
}

TableState.prototype.getBlock = function(blockId) {
	if (blockId == null)
		return null;
	// get this block's state structure
	var blockState = this.tables[blockId];
	if (blockState == null)
		return null;
	// return this block's instance object
	return blockState.block;
}

TableState.prototype.setBlock = function(blockId, block) {
	if (blockId == null)
		return null;
	// get this block's state structure
	var blockState = this.tables[blockId];
	if (blockState == null)
		blockState = this.tables[blockId] = {};
	// set this block's instance object
	blockState.block = block;
}

TableState.prototype.getColumns = function(tableId) {
	if (tableId == null)
		return null;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		return null;
	// return this tables' column structure
	return tableState.columns;
}

TableState.prototype.getColumn = function(tableId, column) {
	if (tableId == null || column == null)
		return null;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		return null;
	// get this table's columns
	var tableColumns = tableState.columns;
	if (tableColumns == null)
		return null;
	// return this column
	return tableColumns[column];
}

TableState.prototype.addColumn = function(tableId, column, label) {
	if (tableId == null || column == null)
		return;
	else if (label == null)
		label = column;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this table's columns
	var tableColumns = tableState.columns;
	if (tableColumns == null)
		tableColumns = tableState.columns = {};
	// add this column to this table
	tableColumns[column] = {
		label: label,
		hidden: false
	};
}

TableState.prototype.isColumnHidden = function(tableId, column) {
	var tableColumn = this.getColumn(tableId, column);
	if (tableColumn == null || tableColumn.hidden == null)
		return false;
	else return tableColumn.hidden;
}

TableState.prototype.hideColumn = function(tableId, column) {
	if (tableId == null || column == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this table's columns
	var tableColumns = tableState.columns;
	if (tableColumns == null)
		tableColumns = tableState.columns = {};
	// get this column
	var tableColumn = tableColumns[column];
	if (tableColumn == null)
		tableColumn = tableColumns[column] = {};
	// update this column's hidden status
	tableColumn.hidden = true;
}

TableState.prototype.showColumn = function(tableId, column) {
	if (tableId == null || column == null)
		return;
	// get this table's state structure
	var tableState = this.tables[tableId];
	if (tableState == null)
		tableState = this.tables[tableId] = {};
	// get this table's columns
	var tableColumns = tableState.columns;
	if (tableColumns == null)
		tableColumns = tableState.columns = {};
	// get this column
	var tableColumn = tableColumns[column];
	if (tableColumn == null)
		tableColumn = tableColumns[column] = {};
	// update this column's hidden status
	tableColumn.hidden = false;
}



/**
 * Helper functions for generating and rendering table content
 */
function getRowElementId(tableId, recordId, rowId) {
	var id = "";
	if (tableId != null)
		id += "table[" + tableId + "]_";
	if (recordId != null)
		id += "record[" + recordId + "]_";
	if (rowId != null)
		id += "row[" + rowId + "]";
	// remove trailing underscore, if present
	if (id.lastIndexOf("_") == id.length - 1)
		id = id.substring(0, id.length - 1);
	return id;
}

function getColumnElementId(tableId, recordId, rowId, columnId) {
	var id = getRowElementId(tableId, recordId, rowId);
	if (columnId != null) {
		// add trailing underscore, if ID string is not currently empty
		if (id != "")
			id += "_";
		id += "column[" + columnId + "]";
	}
	return id;
}

function getExpanderControl(tableId, recordId, rowId) {
	return function() {
		if (tableManager.isRowExpanded(tableId, recordId, rowId))
			tableManager.collapseRow(tableId, recordId, rowId);
		else tableManager.expandRow(tableId, recordId, rowId);
		return false;
	}
}

function resolveFields(value, record) {
	if (value == null || record == null)
		return value;
	// convert all backslash-escaped square brackets to unicode entities
	value = value.replace(/\\\[/gi, "&#91;");
	value = value.replace(/\\\]/gi, "&#93;");
	// parse out all record field references
	while (true) {
		// parse out field name
		var fieldName = null;
		var start = value.indexOf("[");
		if (start < 0)
			break;
		var end = value.indexOf("]");
		if (end <= start)
			break;
		else if (end == start + 1)
			fieldName = "";
		else fieldName = value.substring(start + 1, end);
		// retrieve field value
		var fieldValue = record[fieldName];
		if (fieldValue == null)
			fieldValue = "";
		// escape square brackets in value
		fieldValue = fieldValue.replace(/\[/gi, "&#91;");
		fieldValue = fieldValue.replace(/\]/gi, "&#93;");
		// replace record field reference with value
		value = value.substring(0, start) + fieldValue +
			value.substring(end + 1);
	}
	// restore all square brackets in final value
	value = value.replace(/&#91;/gi, "[");
	value = value.replace(/&#93;/gi, "]");
	return value;
}

function buildColumnSelector(tableId) {
	var columnSelector = document.getElementById("columnSelector");
	if (columnSelector == null)
		return;
	// set table ID to the selector element, so that
	// the form can find it when it is evaluated
	columnSelector.tableId = tableId;
	// if any column checkbox rows are present, delete them
	while (columnSelector.childNodes.length > 1)
		columnSelector.removeChild(columnSelector.firstChild);
	// get columns for this table
	var columns = tableManager.getColumns(tableId);
	if (columns != null) {
		// build column checkbox rows
		for (var column in columns) {
			var tr = document.createElement("tr");
			// create checkbox input
			var td = document.createElement("td");
			var checkbox = document.createElement("input");
			checkbox.type = "checkbox";
			checkbox.name = checkbox.value = column;
			if (tableManager.isColumnHidden(tableId, column) == false)
				checkbox.defaultChecked = true;
			td.appendChild(checkbox);
			tr.appendChild(td);
			// create column label
			td = document.createElement("td");
			td.style.fontSize = "10pt";
			td.innerHTML = columns[column].label;
			tr.appendChild(td);
			// add this column row
			columnSelector.insertBefore(tr, columnSelector.lastChild);
		}
	}
}

function evaluateColumns(form) {
	if (form == null)
		return;
	// get table ID from the parent element
	var columnSelector = document.getElementById("columnSelector");
	if (columnSelector == null)
		return;
	var tableId = columnSelector.tableId;
	if (tableId == null)
		return;
	// go through the form and evaluate all the columns for hiddenness
	var updated = false;
	for (var i=0; i<form.elements.length; i++) {
		var element = form.elements[i];
		if (element.type == "checkbox") {
			var column = element.name;
			// stupid IE hack
			if (column == null || column == "")
				column = element.value;
			var hidden = tableManager.isColumnHidden(tableId, column);
			if (element.checked && hidden) {
				tableManager.showColumn(tableId, column);
				updated = true;
			} else if (element.checked == false && hidden == false) {
				tableManager.hideColumn(tableId, column);
				updated = true;
			}
		}
	}
	if (updated) {
		var tableInstance = tableManager.getBlock(tableId);
		if (tableInstance != null)
			tableInstance.rebuildTable();
	}
	tooltip.nukeTooltip();
}
