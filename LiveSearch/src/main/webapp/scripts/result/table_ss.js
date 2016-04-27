/**
 * Result view table implementation
 */
// constructor
function ServerSideResultViewTable(tableXML, id, task, nested) {
	// properties
	this.id = id;
	this.div = null;
	this.index = null;
	this.task = task;
	this.file = null;
	this.description = task.description;
	this.headers = null;
	this.rows = null;
    this.page_size = this.getPageSize(tableXML);
    this.server_URL = this.getServerURL(tableXML);
    this.server_params = this.getServerParameters(tableXML);
	this.update_callback = null;
	this.sg = new ServerSideSorter();
	this.fg = new ServerSideFilterer();
	this.ng = new ServerSideNavigator(this.page_size);
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

ServerSideResultViewTable.prototype.getServerURL = function(tableXML) {
	var server_URL = "/ProteoSAFe/QueryResult";
	var blocks = tableXML.getElementsByTagName("block");
	if (blocks.length != 1)
		return server_URL;
	else if (blocks[0].attributes.server_URL != null){
		server_URL = blocks[0].attributes.server_URL.value
	}
	return server_URL;
}

ServerSideResultViewTable.prototype.getPageSize = function(tableXML) {
	var page_size = 30;
	var blocks = tableXML.getElementsByTagName("block");
	if (blocks.length != 1)
		return page_size;
	else if (blocks[0].attributes.pagesize != null)
		page_size = parseInt(blocks[0].attributes.pagesize.value);
	return page_size;
}

ServerSideResultViewTable.prototype.getServerParameters = function(tableXML) {
	var blocks = tableXML.getElementsByTagName("block");
	if (blocks.length != 1)
		return null;
	var params = blocks[0].getElementsByTagName("parameters");
	if (params.length != 1)
		return null;
	params = params[0].getElementsByTagName("parameter");
	if (params.length < 1)
		return null;
	parameters = {};
	for (var i=0; i<params.length; i++) {
		var param = params[i];
		if (param.attributes.name != null)
			parameters[param.attributes.name.value] = param.textContent;
	}
	return parameters;
}

ServerSideResultViewTable.prototype.setUpdateCallback = function(callback) {
	if (callback && typeof callback === "function")
		this.update_callback = callback;
}

// build table from XML specification
ServerSideResultViewTable.prototype.buildTable = function(tableXML) {
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
	// never show a "checked only" checkbox filter for dynamic tables
	filterHeader.push(null);
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
			} else expander.collapsed = expander.expanded = expanderIcons;

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
ServerSideResultViewTable.prototype.render = function(div, index) {
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
	var oldEnclosingDiv = document.getElementById(this.id + "_enclosing");
	if (oldEnclosingDiv != null)
		this.div.removeChild(oldEnclosingDiv);
	// set up enclosing div and modal overlay div
	var enclosingDiv = document.createElement("div");
	enclosingDiv.id = this.id + "_enclosing";
	enclosingDiv.style.position = "relative";
	var overlay = document.createElement("div");
	overlay.id = this.id + "_overlay";
	overlay.className = "overlay";
	var spinner = document.createElement("div");
	spinner.id = this.id + "_spinner";
	overlay.appendChild(spinner);
	enclosingDiv.appendChild(overlay);
	// set up table HTML
	this.html = document.createElement("table");
	this.html.id = this.id;
	this.html.setAttribute("class", "tabular");
	setupTable(this.html, decorateTable, this.headers);
	// add table to enclosing div and then to the result page
	enclosingDiv.appendChild(this.html);
	spliceChild(this.div, enclosingDiv, this.index);
	// read location hash, apply any pre-render selections encoded there
	var hash = location.hash;
	if (hash != null && this.index == 0){
		this.updateData();
		//Setting the data in the boxes appropriately
		var hash_parsed = parseLocationHash();

		var filterFound = false;
		for (var key in hash_parsed) {
			// a location hash key represents a filter if it is an ID
			// corresponding to an HTML element found on the page
			var element = document.getElementById(key);
			if (element != null) {
				if (element.type == "checkbox" && hash_parsed[key] == "checked") {
					element.checked = true;
					if (element.activate)
						element.activate();
					filterFound = true;
				} else if (element.type == "text") {
					element.value = hash_parsed[key];
					filterFound = true;
				}
			}
		}
	}

	else this.draw();
}

// draw the table
ServerSideResultViewTable.prototype.draw = function(table) {
	if (table == null)
		table = this;
	// set up sorter
	table.sg.setSource(table.filteredData);
	table.sg.setCallback(function() {
		table.ng.clear();
		table.updateData();
	});
	// set up navigator
	table.ng.setSource(table.filteredData);
	table.ng.setCallback(function(begin, end) {
		table.updateData();
	});
	// render rows - use static begin and end values,
	// since the pagination is now done on the server side
	renderRows(table.html, table.filteredData, table.rows, 0, table.page_size);
}

// update the table data by querying using the current hash contents
ServerSideResultViewTable.prototype.updateData = function() {
	// set up query parameters
	var table = this;
	var service = this.server_URL;
	var task = this.task.id;
	var file = this.file;
	var pageSize = this.page_size;
	var offset = this.ng.getBegin();
	if (offset == null || isNaN(offset))
		offset = 0;
	var query = encodeURIComponent(decodeURIComponent(location.hash));
	var parameters = {
		task: task,
		file: file,
		pageSize: pageSize,
		offset: offset,
		query: query
	};
	if (this.server_params != null) {
		for (var key in this.server_params)
			parameters[key] = this.server_params[key];
	}
	// call update callback, if present
	if (table.update_callback)
		table.update_callback(parameters);
	// start progress spinner
	enableOverlay(
		document.getElementById(this.id + "_overlay"), true, true, "100%");
	// submit query to the server
	$.ajax({
		type: "GET",
		url: service,
		data: parameters,
		async: true,
		cache: false,
		success: function(data) {
			// stop progress spinner
			enableOverlay(
				document.getElementById(table.id + "_overlay"), false);
			// update table with response data
			table.setData(data);
			table.draw(table);
			// highlight the last pre-selected sort control
			var hash = parseLocationHash();
			if (hash != null && table.index == 0) {
				var sorts = hash["table_sort_history"];
				if (sorts != null) {
					var sortsArray = sorts.split(";");
					for (var i=0; i<sortsArray.length; i++) {
						var sort = sortsArray[i];
						var element = document.getElementById(sort);
						if (element != null) {
							//element.record();
							element.select();
						}
					}
				}
			}
		},
		error: function(response, text, error) {
			// stop progress spinner
			enableOverlay(
				document.getElementById(table.id + "_overlay"), false);
			console.log("Error updating table with query: [" + text + "].");
		}
	});
}

// set data to the table
ServerSideResultViewTable.prototype.setData = function(data) {
	// the table's actual row data should be embedded as an array hashed to
	// key "table_data" at the top level of the argument object; everything
	// besides that is metadata that should be processed and discarded
	if (data != null) {
		// file name and total row count should be at the top level
		if (data["file"])
			this.file = data["file"];
		if (data["total_rows"])
			this.ng.setTotal(data["total_rows"]);
		if (data["row_data"])
			data = data["row_data"];
	}
//	// first, check data to see if there are any dynamic columns to add
//	var changed = false;
//	if (data != null) {
//        var first = data[0];
//        var irrelevant = {};
//        if (first != null) {
//        	// initialize array for dynamic columns, if any are found
//        	var columns = new Array();
//        	// add any dynamic columns found to the array
//        	for (var key in first) {
//    			var value = first[key];
//        		if (key.indexOf("_dyn_#") == 0) {
//        			var column = {};
//        			column.attributes = {
//        				"field": key,
//        				"label": key.substring(6)
//        			};
//        			// if the first element value is a number,
//        			// then this is probably a numerical column
//        			if (!isNaN(parseFloat(value)) && isFinite(value)) {
//        				column.sort = numberSorter;
//        				column.filter = rangeFilter;
//        				// if the value has an "e" or an "E" in it,
//        				// then it's an exponential number
//        				if (value.indexOf("e") >= 0 || value.indexOf("E") >= 0)
//        					column.render = renderExpNum;
//        				else column.render = renderFloat;
//        				column.attributes.width = "3";
//        			} else {
//        				column.sort = plainSorter;
//        				column.filter = plainFilter;
//        				column.render = renderPlain;
//        				column.attributes.width = "8";
//        			}
//        			// add this to the list of dynamic columns we want to add
//        			columns.push(column);
//        		}
//                // then, note any "irrelevant" column values in this row
//        		if (value == 0 || value == -1 || value == "null")
//        			irrelevant[key] = value;
//        	}
//        	// if any dynamic columns were found, add them
//        	if (columns.length > 0) {
//        		this.columns = columns;
//        		changed = true;
//        	}
//        }
//        // if any irrelevant columns were found, verify that
//        // their values are irrelevant for all rows
//        var irrelevantCount = 0;
//        for (var column in irrelevant)
//        	irrelevantCount++;
//        if (irrelevantCount >= 0) {
//            for (var i=0; i<data.length; i++) {
//            	var row = data[i];
//            	// for each potentially irrelevant column, check its value
//            	var noLongerIrrelevant = new Array();
//            	for (var column in irrelevant)
//            		if (row[column] != irrelevant[column])
//            			noLongerIrrelevant.push(column);
//                // if a column's value for this row is not irrelevant, then
//            	// remove that column from the list of columns to be checked
//            	for (var j=0; j<noLongerIrrelevant.length; j++) {
//            		delete irrelevant[noLongerIrrelevant[j]];
//            		irrelevantCount--;
//            	}
//            	// if no irrelevant columns are left, stop checking
//            	if (irrelevantCount <= 0)
//            		break;
//            }
//        }
//        // if any columns were found to be irrelevant for all rows, hide them
//        for (var column in irrelevant) {
//        	var columnState = tableManager.getColumn(this.id, column);
//        	if (columnState != null) {
//        		consoleLog("Hiding irrelevant column [" + column + "].");
//        		columnState.hidden = true;
//        		changed = true;
//        	}
//        }
//        // if any changes were made to this table's structure, rebuild it
//        if (changed) {
//        	this.buildTable();
//        	this.addColumns(this.columns);
//        }
//	}
	// finally, assign the data to the table
	this.data = data;
	this.filteredData = data;
}

// filter the table
ServerSideResultViewTable.prototype.filter = function(table) {
	if (table == null)
		table = this;
	table.filteredData = table.fg.filter(table.data);
	table.sg.setSource(table.filteredData);
	table.sg.sort();
}

// rebuild and re-render the table
ServerSideResultViewTable.prototype.rebuildTable = function() {
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
ServerSideResultViewTable.prototype.addColumns = function(columns) {
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
resultViewBlocks["table_ss"] = ServerSideResultViewTable;



function ServerSideSorter() {
	var pivot = null;
	var records = null;
	var render = null;
	var history = new Array();

	this.setSource = function(source) { records = source; };
	this.setCallback = function(callback) { render = callback; };

	// sort control-specific functions
	var activate = function() {
		//records.sort(this.comp);
		render();
		return this.select();
	}

	var select = function() {
        if (pivot && pivot != this)
        	pivot.unclick();
        pivot = this;
        this.click();
        return false;
	}

	var record = function() {
		// first, add this click to the sorting history
		history.push(this.id);
		// then write the sorting history into the location hash
		var hash = parseLocationHash();
		if (hash == null)
			hash = {};
		var value = "";
		for (var i=0; i<history.length; i++)
			value += history[i] + ";";
		if (value.length > 0)	// chomp trailing semicolon
			value = value.substring(0, value.length - 1);
		hash["table_sort_history"] = value;
		location.hash = encodeURIComponent(JSON.stringify(hash));
	}

	var onclick = function() {
		this.record();
		this.activate();
	}

	var renderSortControl = function(div, fieldname, comp) {
		var span = document.createElement('span');
		var asc = document.createElement('img');
		var dsc = document.createElement('img');
		div.appendChild(span);
		span.appendChild(asc);
		span.appendChild(dsc);
		span.style.marginLeft = '3px';
		asc.id = fieldname + "_asc";
		asc.style.position = 'relative';
		asc.style.bottom = '7px';
		asc.className = 'selectable';
		asc.click = function() { this.src = '/ProteoSAFe/images/asc-on.gif'; }
		asc.unclick = function() { this.src = '/ProteoSAFe/images/asc.gif'; }
		asc.activate = activate;
		asc.select = select;
		asc.record = record;
		asc.onclick = onclick;
		asc.comp = comp;
		asc.unclick();

		dsc.id = fieldname + "_dsc";
		dsc.style.position = 'relative';
		dsc.style.top = '2px';
		dsc.style.left = '-10px';
		dsc.className = 'selectable';
		dsc.click = function() { this.src = '/ProteoSAFe/images/dsc-on.gif'; }
		dsc.unclick = function() { this.src = '/ProteoSAFe/images/dsc.gif'; }
		dsc.activate = activate;
		dsc.select = select;
		dsc.record = record;
		dsc.onclick = onclick;
		dsc.comp = function(a, b) { return -comp(a,b); };
		dsc.unclick();
	}

	this.sort = function() {
		if (pivot)
			pivot.activate();
		else render();
	}

	this.renderPlain = function(div, fieldname) {
		renderSortControl(div, fieldname,
			function(a, b) {
				// extract first value of each record, if there are multiple
				var A = splitFieldValue(a[fieldname]);
				if (A == null)
					A = a[fieldname];
				else A = A[0];
				var B = splitFieldValue(b[fieldname]);
				if (B == null)
					B = b[fieldname];
				else B = B[0];
				// compare extracted values
				if (A == B)
					return 0;
				else return (A < B) ? -1 : 1;
			}
		);
	}

	this.renderNumber = function(div, fieldname) {
		renderSortControl(div, fieldname,
			function(a, b) {
				// extract first value of each record, if there are multiple
				var A = splitFieldValue(a[fieldname]);
				if (A == null)
					A = a[fieldname];
				else A = A[0];
				var B = splitFieldValue(b[fieldname]);
				if (B == null)
					B = b[fieldname];
				else B = B[0];
				// convert to numbers, if necessary
				if (typeof A != "number")
					A = Number(A);
				if (typeof B != "number")
					B = Number(B);
				// compare extracted values
				return A - B;
			}
		);
	}

	this.renderExp = this.renderNumber;
}

function ServerSideFilterer() {
	var keys = new Array();
	var ranges = new Array();
	var filters = new Array();
	var ids = new Array();

	var eval = function(record) {
		for (var i in filters)
			if (!filters[i](record))
				return false;
		return true;
	};

	this.renderMark = function(div, fieldname, caption) {
		var checkbox = document.createElement('input');
		checkbox.type = 'checkbox';
		checkbox.id = fieldname + "_checkbox";
		ids.push(checkbox.id);
		appendChildren(div, [checkbox, ' ' + caption]);
		filters.push(
			function(record){
				return !checkbox.checked || record[fieldname];
			}
		);
	}

	this.renderPlain = function(div, fieldname, size) {
		var key = document.createElement("input");
		key.type = "text";
		key.id = fieldname + "_input";
		ids.push(key.id);
		key.size = size ? size: "10";
		div.appendChild(key);
		keys.push(key);
		filters.push(
			function(record) {
				// ensure that the indicated column is present in the record
				var value = record[fieldname];
				if (value == null)
					return true;
				// if the column value is present, convert it to upper
				// case, to ensure case-insensitive string comparison
				else value = value.toUpperCase();
				// need to re-fetch DOM element to prevent stale JS handle
				var input = document.getElementById(key.id);
                if (input != null && input.value != null && input.value != "") {
                    search_values = input.value.split("||");
                    exact_match = false;
                    if (input.value.indexOf("EXACT") != -1){
                        exact_match = true;
                    }
                    for (var i=0; i<search_values.length; i++) {
                    	var search_value = search_values[i].toUpperCase();
                        if (exact_match == true) {
                            if (value == search_value)
                                return true;
                        } else {
                            if (value.indexOf(search_value) != -1)
                                return true;
                        }
                    }
                    return false;
                }
                // if no filter text is specified, then all records match
                return true;
			}
		);
	};

	this.renderRange = function(div, fieldname, size){
		var lower = document.createElement('input');
		lower.type = 'text';
		lower.id = fieldname + "_lowerinput";
		ids.push(lower.id);
		lower.size = size ? size : '2';

		var upper = document.createElement('input');
		upper.type = 'text';
		upper.id = fieldname + "_upperinput";
		ids.push(upper.id);
		upper.size = size ? size : '2';

		div.noWrap = 'true';
		div.appendChild(lower);
		div.appendChild(document.createTextNode('~'));
		div.appendChild(upper);

		ranges.push({upper: upper, lower: lower});
		filters.push(
			function(record){
				// need to re-fetch DOM elements to prevent stale JS handles
				var upperInput = document.getElementById(upper.id);
				var lowerInput = document.getElementById(lower.id);
				// properly extract ranges
				var upperValue = Number.NaN, lowerValue = Number.NaN;
				if (upperInput != null && upperInput.value != '')
					upperValue = Number(stripSeparators(upperInput.value));
				if (lowerInput != null && lowerInput.value != '')
					lowerValue = Number(stripSeparators(lowerInput.value));
				// if no ranges have been specified, return true
				if (isNaN(upperValue) && isNaN(lowerValue))
					return true;
				// split all values of each record, if there are multiple
				var values = splitFieldValue(record[fieldname]);
				if (values == null) {
					values = new Array();
					values.push(record[fieldname]);
				}
				// iterate over all values
				for (var i=0; i<values.length; i++) {
					var value = values[i];
					// convert value to number, if necessary
					if (typeof value != "number")
						value = Number(value);
					if ((isNaN(upperValue) || upperValue >= value) &&
						(isNaN(lowerValue) || lowerValue <= value))
						return true;
				}
				return false;
			}
		);
	}

	this.filter = function(records) {
		// first, save the current filter state into the location hash
		var hash = parseLocationHash();
		if (hash == null)
			hash = {};
		for (var i=0; i<ids.length; i++) {
			var id = ids[i];
			var element = document.getElementById(id);
			if (element == null)
				consoleLog("Could not find table filter element ID [" +
					id + "].");
			else if (element.type == "checkbox") {
				if (element.checked)
					hash[id] = "checked";
				else delete hash[id];
			}
			else if (element.type == "text") {
				if (element.value != null && element.value != "")
					hash[id] = element.value;
				else delete hash[id];
			}
		}
		location.hash = encodeURIComponent(JSON.stringify(hash));
		// then, actually evaluate the filter
		var result = new Array();
		for (var i in records) {
			var record = records[i];
			if (eval(record))
				result.push(record);
		}
		return result;
	}

	this.clean = function(){
		for (var i in keys)
			keys[i].value = '';
		for (var i in ranges) {
			ranges[i].upper = '';
			ranges[i].lower = '';
		}
	}
}

function ServerSideNavigator(cols) {
	var span, prev, next, input, button;
	var begin, end, total;
	var source = null;
	var callback = null;
	var title = null;
	var banner = null;
	var titleDisplay = null;
	var bannerDisplay = null;

	var updateDisplay = function() {
		if (title)
			titleDisplay.innerHTML = title;
		if (banner)
			bannerDisplay.innerHTML = banner(begin, end, total);
	};

	var goPrev = function() {
		if (begin - cols < 0)
			return;
		begin -= cols;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if (callback)
			callback(begin, end);
	};

	var goNext = function() {
		if (begin + cols >= total)
			return;
		begin += cols;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if (callback)
			callback(begin, end);
	};

	var gotoHit = function(){
		var target = Number(input.value);
		if (isNaN(target))
			return;
		target -= target % cols;
		if (target < 0 || target > total)
			return;
		begin = target;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if (callback)
			callback(begin, end);
	};

	this.renderControls = function(tr, tableId) {
		// table header cell
		var td = tr.insertCell(tr.cells.length);
		td.style.border = "0px";

		// top-level header div
		var div = document.createElement("div");
		td.appendChild(div);

		// header title container
		titleDisplay = document.createElement("div");
		titleDisplay.id = "title";
		titleDisplay.style.cssFloat = "left";
		titleDisplay.style.padding = "3px";

		// navigation controls container
		var ctrSpan = document.createElement("div");
		ctrSpan.style.cssFloat = "left";
		ctrSpan.style.paddingLeft = "100px";

		// navigation control widgets
		var prev = document.createElement("img");
		prev.src = "/ProteoSAFe/images/prev.gif";
		prev.className = "selectable";
		prev.onclick = goPrev;
		bannerDisplay = document.createElement("span");
		bannerDisplay.style.display = "inline-block";
		bannerDisplay.style.width = "220px";
		bannerDisplay.style.paddingLeft = "7px";
		var next = document.createElement("img");
		next.src = "/ProteoSAFe/images/next.gif";
		next.className = "selectable";
		next.onclick = goNext;
		input = document.createElement("input");
		input.type = "text";
		var button = document.createElement("button");
		button.innerHTML = "Go";
		button.onclick = gotoHit;

		// add widgets to navigation controls container
		appendChildren(ctrSpan, [prev, bannerDisplay, next,
			"\u00A0\u00A0\u00A0\u00A0Go to \u00A0\u00A0",
			input, button]);

		// column selector
		var selector = document.createElement("div");
		selector.style.clear = "both";
		var selectLink = document.createElement("a");
		selectLink.style.cursor = "pointer";
		selectLink.style.color = "blue";
		selectLink.style.padding = "0 0 0 20px";
		selectLink.innerHTML = "Select columns";
		selectLink.onclick = function(event) {
			buildColumnSelector(tableId);
			showTooltip(selectLink, event, "load:hColumnSelector");
		}
		selector.appendChild(selectLink);

		// add header containers to top-level div
		appendChildren(div, [titleDisplay, ctrSpan, selector]);

		return td;
	};

	this.setSource = function(src) {
		source = src;
		if (begin == null)
			begin = 0;
		if (end == null)
			end = Math.min(total, cols);
		updateDisplay();
	};

	this.setTotal = function(t) {
		try { total = parseInt(t); }
		catch (error) { total = cols; }
	};

	this.setCallback = function(f) { callback = f;};
	this.setTitle = function(t, f) { title = t; banner = f;};
	this.getBegin = function() { return begin; };
	this.getEnd = function() { return end; };
	this.clear = function() { begin = null; end = null; }
}
