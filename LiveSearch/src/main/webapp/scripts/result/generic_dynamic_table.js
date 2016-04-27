function ResultViewTableGen(tableXML, id, task, nested, columnhandler) {
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
    // build the table
    this.buildTable(tableXML, nested, columnhandler);
    // register the table instance, in case it needs to be rebuilt
    tableManager.setBlock(this.id, this);
}

ResultViewTableGen.prototype.getPageSize = function(tableXML){
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
ResultViewTableGen.prototype.buildTable = function(tableXML, nested, columnhandler) {
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
        if (nested == null)
                nested = builder.nested;
        else builder.nested = nested;
        if (tableXML == null)
                return;
        // set up header renderers
        // title and navigation controls
        // TODO: come up with a better way of deciding whether this table
        // needs a title and navigation header
        //if (this.id == "main")
                this.headers.push(
                        getTitleHeaderRenderer(this.id, this.ng, this.description));
        // column titles and sort controls
        var sortHeader = new Array();
        var filterCallback = this.filter;
        var table = this;
        if (nested)
                sortHeader.push(null);
        else sortHeader.push(
                renderButton("Filter", "filter",
                        function() { filterCallback(table); }));
        // filter controls
        var filterHeader = null;
        if (!nested) {
                filterHeader = new Array();
                filterHeader.push(markFilter(this.fg, "selected", "checked only"));
        }
        // set up main row renderers and controls
        var mainRow = null;
        var mainRowId = null;
        var mainRowColumnLoaders = null;
        var rowExpanders = new Array();
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
                                parameters[name] = resolveParameters(value);
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
                                if (!nested && filterHeader != null) {
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
                        else{
                            row.push(handler.render(this.id, rowId, columnId, attributes, columnhandler));
                        }
                }
                // if this is the first (main) row, finalize headers
                // and wait until the end to finalize column renderers
                if (i == 0) {
                        this.headers.push(createCaptionHeader(null, sortHeader));
                        this.headers.push(createExtraHeader(null, filterHeader));
                        mainRow = row;
                        mainRowId = rowId;
                        mainRowColumnLoaders = columnLoaders;
                }
                // otherwise, set up this row's control cell
                // and finalize its column renderers
                else {
                        row.splice(0, 0,
                                renderRowControl(
                                        this.id, rowId, "control", null, columnLoaders, nested)
                        );
                        if (expander != null)
                                this.rows.push(
                                        createRowRender(this.id, rowId, expandableRow, row));
                        else this.rows.push(
                                        createRowRender(this.id, rowId, defaultRow, row));
                }
        }
        // now that we've collected all the row expander information,
        // set up the main row's control cell and finalize its column renderers
        mainRow.splice(0, 0,
                renderRowControl(
                        this.id, mainRowId, "control",
                        rowExpanders, mainRowColumnLoaders, nested)
        );
        this.rows.splice(0, 0, createRowRender(this.id, mainRowId, null, mainRow));
}

ResultViewTableGen.prototype.getupdateddata = function(newdata) {
    this.data = newdata;
    this.filteredData = newdata;
}

// render the table
ResultViewTableGen.prototype.render = function(div, index) {
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
}

// draw the table
ResultViewTableGen.prototype.draw = function(table) {
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
ResultViewTableGen.prototype.setData = function(data) {
        this.data = data;
        this.filteredData = data;
        //alert("setting data");
}

// filter the table
ResultViewTableGen.prototype.filter = function(table) {
        if (table == null)
                table = this;
        table.filteredData = table.fg.filter(table.data);
        table.sg.setSource(table.filteredData);
        table.sg.sort();
}

// rebuild and re-render the table
ResultViewTableGen.prototype.rebuildTable = function() {
        this.buildTable();
        this.render();
}
