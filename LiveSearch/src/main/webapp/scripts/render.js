/* Resizable table */
        var markerHTML = " ";
        var minWidth = 80;
        var dragingColumn = null;
        var startingX = 0;
        var currentX = 0;

        function getNewWidth () {
            var newWidth = minWidth;
            if (dragingColumn != null) {
                newWidth = parseInt (dragingColumn.parentNode.style.width);
                if (isNaN (newWidth)) {
                    newWidth = 0;
                }
                newWidth += currentX - startingX;
                if (newWidth < minWidth) {
                    newWidth = minWidth;

                }
            }
            return newWidth;
        }

        function columnMouseDown (event) {
            if (!event) {
                event = window.event;
            }
            /*if (dragingColumn != null) {
                ColumnGrabberMouseUp ();
            }*/
            startingX = event.clientX;
            currentX = startingX;
            dragingColumn = this;
            return true;
        }

        function columnMouseUp () {
            if (dragingColumn != null) {
                dragingColumn.parentNode.style.width = getNewWidth ();
                dragingColumn = null;
            }
			return true;
        }

        function columnMouseMove (event) {
            if (!event) {
                event = window.event;
            }
            if (dragingColumn != null) {
			    currentX = event.clientX;
                dragingColumn.parentNode.style.width = getNewWidth ();
                startingX = event.clientX;
                currentX = startingX;
			}
			return true;
        }
        document.onmouseup = columnMouseUp;
        document.onmousemove = columnMouseMove;

function splitFieldValue(value) {
	if (value == null)
		return null;
	var values = null;
	if (typeof value == "string")
		values = value.split("!");
	else {
		values = new Array();
		values.push(value);
	}
	return values;
}

/* Sorting */

function SortingGroup(){
	var pivot = null;
	var records = null;
	var render = null;
	var history = new Array();

	this.setSource = function(source){ records = source; };
	this.setCallback = function(callback) { render = callback; };

	var activate = function() {
		records.sort(this.comp);
		render();
        if(pivot && pivot != this) pivot.unclick();
        pivot = this;
        this.click();
        return false;
	}

	var onclick = function() {
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
		// finally, actually evaluate the sort
		this.activate();
	}

	var renderSortControl = function(div, fieldname, comp){
		var span = document.createElement('span');
		var asc = document.createElement('img');
		var dsc = document.createElement('img');
		div.appendChild(span);
		span.appendChild(asc);
		span.appendChild(dsc);
//		span.style.position = 'relative';
		span.style.marginLeft = '3px';

		asc.id = fieldname + "_asc";
//		asc.style.position = 'absolute';
//		asc.style.top = '-20px';
//		asc.style.left = '3px';
		asc.style.position = 'relative';
		asc.style.bottom = '7px';
		asc.className = 'selectable';
		asc.click   = function(){ this.src = '/ProteoSAFe/images/asc-on.gif'; }
		asc.unclick = function(){ this.src = '/ProteoSAFe/images/asc.gif'; }
		asc.activate = activate;
		asc.onclick = onclick;
		asc.comp = comp;
		asc.unclick();

		dsc.id = fieldname + "_dsc";
//		dsc.style.position = 'absolute';
//		dsc.style.top = '-3px';
//		dsc.style.left = '3px';
		dsc.style.position = 'relative';
		dsc.style.top = '2px';
		dsc.style.left = '-10px';
		dsc.className = 'selectable';
		dsc.click   = function(){ this.src = '/ProteoSAFe/images/dsc-on.gif'; }
		dsc.unclick = function(){ this.src = '/ProteoSAFe/images/dsc.gif'; }
		dsc.activate = activate;
		dsc.onclick = onclick;
		dsc.comp = function(a, b){return -comp(a,b);};
		dsc.unclick();
	}

	this.sort = function(){
		if(pivot) pivot.activate();
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


/* Filtering */
function FilterGroup(){
	var keys = new Array();
	var ranges = new Array();
	var filters = new Array();
	var ids = new Array();

	var eval = function(record){
		for(var i in filters)
			if(!filters[i](record))
				return false;
		return true;
	};
	this.renderMark = function(div, fieldname, caption){
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
                    //For OR operations
                    if(input.value.indexOf("||") != -1){
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

                    //For AND operations
                    if(input.value.indexOf("&&") != -1){
                        search_values = input.value.split("&&");
                        for (var i=0; i<search_values.length; i++) {
                        	var search_value = search_values[i].toUpperCase();
                            var is_negation = false
                            if(search_value[0] == "^"){
                                is_negation = true
                                search_value = search_value.slice(1)
                            }

                            if(is_negation){
                                if (value.indexOf(search_value) != -1)
                                    return false;
                                else{
                                    continue;
                                }
                            }
                            else{
                                if (value.indexOf(search_value) != -1)
                                    continue
                                else{
                                    return false;
                                }
                            }

                        }
                        return true;
                    }

                    //For no operations specified
                    search_value = input.value
		    search_value = search_value.toUpperCase();
                    var is_negation = false
                    if(search_value[0] == "^"){
                        is_negation = true
                        search_value = search_value.slice(1)
                    }
                    if(is_negation){
                        if (value.indexOf(search_value) != -1){
                            return false;
                        }
                        else{
                            return true;
                        }
                    }
                    else{
                        if (value.indexOf(search_value) != -1){
                            return true;
                        }
                        else{
                            return false;
                        }
                    }


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
	this.filter = function(records){
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
		for(var i in records){
			var record = records[i];
			if(eval(record))
				result.push(record);
		}
		return result;
	}
	this.clean = function(){
		for(var i in keys)
			keys[i].value = '';
		for(var i in ranges){
			ranges[i].upper = '';
			ranges[i].lower = '';
		}
	}
}

/* Navigation */
function NavigationGroup(cols){
	var span, prev, next, input, button;
	var begin, end, total;
	var source = null;
	var callback = null;
	var title = null;
	var banner = null;
	var titleDisplay = null;
	var bannerDisplay = null;

	var updateDisplay = function(){
		if(title)titleDisplay.innerHTML = title;
		if(banner)bannerDisplay.innerHTML = banner(begin, end, total);
	};

	var goPrev = function(){
		if(begin - cols < 0 ) return;
		begin -= cols;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if(callback) callback(begin, end);
	};

	var goNext = function(){
		if(begin + cols >= total) return;
		begin += cols;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if(callback) callback(begin, end);
	};

	var gotoHit = function(){
		var target = Number(input.value);
		if(isNaN(target)) return;
		target -= target % cols;
		if(target < 0 || target > total) return;
		begin = target;
		end = Math.min(begin + cols, total);
		updateDisplay();
		if(callback) callback(begin, end);
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

	this.setSource = function(src){
		source = src;
		if(source.length == 0) begin = end = total = 0;
		else{
			total = source.length;
			begin = 0;
			end = Math.min(total, cols);
		}
		updateDisplay();
	};

	this.setCallback = function(f){ callback = f;};
	this.setTitle = function(t, f){ title = t; banner = f;};
	this.getBegin = function(){ return begin; };
	this.getEnd = function(){ return end; };

}

/* rendering */

function setupTable(table, decorator, headers){
	if(decorator) decorator(table);
	var thead = document.createElement('thead');
	table.appendChild(thead);
	for(var i in headers){
		var tr = thead.insertRow(thead.rows.length);
		headers[i](tr);
	}
	var tbody = document.createElement('tbody');
	table.appendChild(tbody);
	table.body = tbody;
//	table.dataBeginsAt = table.rows.length;
}

function renderRows(table, records, renders, begin, end) {
	// clear previously rendered table
	for (var i=table.body.rows.length-1; i>=0; i--)
		table.body.deleteRow(i);
	// set up range of records to be displayed
	begin = begin ? Math.max(begin, 0) : begin = 0;
	end = end ? Math.min(end, records.length) : records.length;
	// render the specified range of records
	for (var i=begin; i<end; i++) {
		var record = records[i];
		for (var j in renders) {
			var tr = table.body.insertRow(table.body.rows.length);
			tr.record = record;
			renders[j](tr, record, i);
		}
	}
}

function createCaptionHeader(decorator, renders, colspan) {
	return function(tr) {
		if (decorator)
			decorator(tr);
		var width = 0;
		for (var i in renders) {
			var th = document.createElement("th");
			th.onselectstart = function(event) { return false; };
			tr.appendChild(th);
			var render = renders[i];
			if (render)
				render(th);
			if (i==1 || i==3 || i==4) {
				var span = document.createElement("span");
				span.innerHTML = ".";
				span.style.backgroundColor = "white";
				span.className = "resize";
				span.onmousedown = columnMouseDown;
				th.appendChild(span);
				th.style.width=minWidth;
			}
			var span = parseInt(th.colSpan);
			if (isNaN(span) || span < 1)
				span = 1;
			width += span;
		}
		if (colspan != null && colspan > width) {
			var th = document.createElement("th");
			th.colSpan = colspan - width;
			tr.appendChild(th);
		}
	}
}

function createExtraHeader(decorator, renders, colspan) {
	return function(tr) {
		if (decorator)
			decorator(tr);
		var width = 0;
		for (var i in renders) {
			var td = tr.insertCell(tr.cells.length);
			var render = renders[i];
			if (render)
				render(td);
			var span = parseInt(td.colSpan);
			if (isNaN(span) || span < 1)
				span = 1;
			width += span;
		}
		if (colspan != null && colspan > width) {
			var td = tr.insertCell(tr.cells.length);
			td.colSpan = colspan - width;
		}
	}
}

function createRowRender(tableId, rowId, decorator, renders, colspan) {
	return function(tr, record, index) {
		if (decorator)
			decorator(tr, record, tableId, rowId);
		var width = 0;
		for (var i in renders) {
			var td = tr.insertCell(tr.cells.length);
			var render = renders[i];
			if (render)
				render(td, record, index);
			var span = parseInt(td.colSpan);
			if (isNaN(span) || span < 1)
				span = 1;
			width += span;
		}
		if (colspan != null && colspan > width) {
			var td = tr.insertCell(tr.cells.length);
			td.colSpan = colspan - width;
		}
	}
}

function wrap(s, wrap_size){
	if (s == null)
		return;
	else if (wrap_size == 0)
		return s;
	else if (wrap_size == -1) {
		var result = '';
		for(var i = 0; i < s.length; i++)
			result += s[i] + ' ';
		return result;
	} else {
		result = '';
		var L = s.length;
		var l = Math.ceil(L / wrap_size);
		var r = l * wrap_size - L;
		var a = wrap_size - Math.floor(r / l), b = r % l, c = 0;
		for(var i = 1; i <= l - b; i++, c += a)
			result += s.substring(c, c+ a) + ' ';
		a--;
		for(var i = 1; i <= b; i++, c += a)
			result += s.substring(c, c+ a)  + ' ';
		result.substr(0, -5);
		return result;
	}
}

function createCaption(caption, tooltipText) {
	var span = document.createElement("span");
	if (tooltipText) {
		span.className = "help";
		span.onmouseover = function(event) {
			showTooltip(span, event, tooltipText);
		}
	}
	span.innerHTML = caption;
	return span;
}

function createHideButton(fieldname) {
	var button = document.createElement("img");
	button.src = "/ProteoSAFe/images/hide.png";
	button.onclick = function() {
		hideColumn(fieldname);
	}
	button.onmouseover = function(event) {
		showTooltip(button, event, "Hide this column");
	}
	return button;
}

function captionHeader(caption, tooltipText) {
	return function(td) {
		td.appendChild(createCaption(caption, tooltipText));
		td.noWrap = "true";
	}
}

function plainSorter(sg, caption, fieldname, tooltipText) {
	return function(td) {
		td.appendChild(createCaption(caption, tooltipText));
		td.noWrap = "true";
		sg.renderPlain(td, fieldname);
		// td.appendChild(createHideButton(fieldname));
	}
}

function numberSorter(sg, caption, fieldname, tooltipText) {
	return function(td) {
		td.appendChild(createCaption(caption, tooltipText));
		td.noWrap = "true";
		sg.renderNumber(td, fieldname);
		// td.appendChild(createHideButton(fieldname));
	}
}

function renderButton(text, id, callback) {
	return function(td) {
		var button = document.createElement("button");
		button.id = id;
		button.innerHTML = text;
		if (callback != null)
			button.onclick = callback;
		td.appendChild(button);
	}
}

function markFilter (fg, fieldname, caption){
	return function(td){
		fg.renderMark(td, fieldname, caption);
	}
}

function plainFilter(fg, fieldname, size){
	return function(td){
		fg.renderPlain(td, fieldname, size);
	}
}

function rangeFilter(fg, fieldname, size){
	return function(td){
		fg.renderRange(td, fieldname, size);
	}
}

function expandCell(values, record, format) {
	if (values == null || record == null)
		return null;
	else if (format == null)
		format = function(value, record) { return value; };
	// only expand a cell into a sub-table if it has more than one value
	if (values.length == null)
		return format(values, record);
	else if (values.length == 1)
		return format(values[0], record);
	// create a sub-table for the multiple values in this cell
	var table = document.createElement("table");
	table.className = "subResult";
	for (var i=0; i<values.length; i++) {
		var tr = document.createElement("tr");
		var td = document.createElement("td");
		if (i > 0)
			td.style.borderTop = "thin dotted";
		var content = format(values[i], record);
		if (content == null)
			td.innerHTML = values[i];
		else if (typeof content != "object")
			td.innerHTML = content;
		else td.appendChild(content);
		tr.appendChild(td);
		table.appendChild(tr);
	}
	return table;
}

function renderCell(format, tableId, rowId, columnId, attributes) {
	return function(td, record, index) {
		td.id = getColumnElementId(tableId, record.id, rowId, columnId);
		if (attributes.colspan != null)
			td.colSpan = attributes.colspan;
		// all columns should specify as their cell value either a record field
		// to look up, or a literal value to be shown in that cell for all rows
		var value = record[attributes.field];
		if (value == null)
			value = attributes.value;
		var content = expandCell(splitFieldValue(value), record, format);
		if (content == null)
			td.innerHTML = format(value, record);
		else if (typeof content != "object")
			td.textContent = content;
		else td.appendChild(content);
	}
}


function renderCell_OptionalShow(format, tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        td.id = getColumnElementId(tableId, record.id, rowId, columnId);
        if (attributes.colspan != null)
            td.colSpan = attributes.colspan;
        var value = record[attributes.field];

        var encapulating_div = document.createElement('div');
        var content = expandCell(splitFieldValue(value), record, format);
        if (content == null)
            encapulating_div.innerHTML = format(value, record);
        else if (typeof content != "object")
            encapulating_div.innerHTML = content;
        else encapulating_div.appendChild(content);

        if(encapulating_div.innerHTML.length > 60){
            var show_button = document.createElement('input');
            show_button.type = "button";
            show_button.value = "Show";
            show_button.onclick = renderCell_OptionalShow_Button_Generator(show_button, encapulating_div);
            td.appendChild(show_button);
            encapulating_div.style.display = "none";
            console.log(value);
            console.log(value.length);
        }
        td.appendChild(encapulating_div);
    }
}

function renderCell_OptionalShow_Button_Generator(button_obj, div_to_show){
    return function(){
        button_obj.style.display = "none";
        div_to_show.style.display = "block";
    };
}

function renderPlain(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		if (value == null)
			return "N/A";
		else return value;
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

function renderInteger(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		if (value == null)
			return "N/A";
		else if (typeof(value) != "number")
			value = parseInt(value.replace(/,/g, ''));
		if (value == null || isNaN(value))
			return "N/A";
		else return value;
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

function renderFloat(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		if (value == null)
			return "N/A";
		else if (typeof(value) != "number")
			value = parseFloat(value);
		if (value == null || isNaN(value))
			return "N/A";
		else return value.toFixed(attributes.precision);
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

function renderExpNum(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		if (value == null)
			return "N/A";
		else if (typeof(value) != "number")
			value = parseFloat(value);
		if (value == null || isNaN(value))
			return "N/A";
		else return value.toExponential(attributes.precision);
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

function renderExpandable(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
    	if (value == null)
        	return "N/A";
    	else return value;
	};
	return renderCell_OptionalShow(
		format, tableId, rowId, columnId, attributes);
}

function renderLink(tableId, rowId, columnId, attributes) {
	var buildWorkflowLinkParameters = function(parameters, record) {
		var url = "{";
		for (var parameter in parameters)
			url += "\"" + parameter + "\":\"" +
				resolveFields(parameters[parameter], record) + "\",";
		// chomp the trailing comma
		url = url.substring(0, url.length - 1);
		url += "}";
		return "params=" + encodeURIComponent(url);
	};
	return function(td, record, index) {
		var link = document.createElement("a");
		link.innerHTML = "";
		// link by default to the input form page
		var url = "index.jsp";
		// fill out the link with attributes and parameters from result.xml
		if (attributes != null) {
			// if a target is specified, link to it
			if (attributes.target != null)
				url = attributes.target;
			// if a label is specified, use it as the link's text content
			if (attributes.label != null)
				link.innerHTML = attributes.label;
			// if parameters are specified, add them
			var parameters = attributes.parameters;
			if (parameters != null) {
				url += "?";
				// if the link is pointing to the input form page, then
				// build the URL using the appropriate special logic
				if (attributes.target == null || attributes.target == "" ||
					attributes.target == "index.jsp")
					url += buildWorkflowLinkParameters(parameters, record);
				// otherwise, add the parameters generically
				else {
			    	for (var parameter in parameters)
			    		url += encodeURIComponent(parameter) + "=" +
			    			encodeURIComponent(resolveFields(
			    				parameters[parameter], record)) + "&";
			    	// chomp the trailing symbol
			    	url = url.substring(0, url.length - 1);
				}
			}
		}
		link.href = url;
		if (link.innerHTML == "")
			link.innerHTML = decodeURIComponent(url);
		td.appendChild(link);
	}
}

// for date values stored as milliseconds since the epoch
function renderDate(tableId, rowId, columnId, attributes) {
	var format = function(value, record) {
		if (typeof(value) != "number")
			value = parseInt(value);
		if (value == null || isNaN(value))
			return "N/A";
		else return new Date(value).toString();
	};
	return renderCell(format, tableId, rowId, columnId, attributes);
}

function renderEmptyColumn(colSpan) {
	return function(td, record, seq) {
		td.colSpan = colSpan;
		td.innerHTML = "";
		var div = document.createElement("div");
		div.id = record.Counter;
		td.appendChild(div);
	}
}

function renderTable(tables, colSpan) {
	return function(td, record, seq) {
		td.colSpan = colSpan;
		td.innerHTML = "";
		var div = document.createElement("div");
		div.id = record.Counter;
		td.appendChild(div);
		renderTables(tables, td);
	}
}

/* Folder Tree */

function folderEntries(path, foldersOnly, render){
	var req = createRequest();
    var url = "folder.jsp?path=" + encodeURIComponent(path);
    if (foldersOnly)
    	url += "&folders=true";
    var entries = new Array();
    req.open("GET", url, true);
    req.onreadystatechange = function(){
        if(req.readyState == 4){
			var xml = req.responseXML;
			var root = xml.getElementsByTagName('entries')[0];
			for(var i = 0; i < root.childNodes.length; i++){
				var node = root.childNodes[i];
				if(node.nodeType != 1) continue;
				var entry = new Object();
				entry.isleaf = (node.nodeName == 'file');
				entry.name = node.firstChild.nodeValue;
				entry.display = node.getAttribute('display');
				if(!entry.display) entry.display = entry.name;
				entries.push(entry);
			}
			render(entries);
		}
	}
	req.setRequestHeader( "If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT" );
    req.send(null);
    return entries;
}

function FolderGroup(divName, loadEntries){
	var checkedFiles = 0;
	var checkedFolders = 0;
	var foldersOnly = false;
	var singleSelect = false;
	var loadFolder = function (folder, checked){ // load entries and expand
		var path = (!folder.path)? '' : folder.path + '/';
		loadEntries(path, foldersOnly, function(entries){
			for(var i in entries){
				var entry = entries[i];
				var div = document.createElement('div');
				var title = document.createElement('span');
				var subtrees = document.createElement('div');
				var e = {
					div: div, title: title, subtrees: subtrees, parent: folder,
					status: 'NONE', path: path + entry.name, isleaf: entry.isleaf};

				div.className = 'FolderEntryDiv';
				div.master = e;
				title.master = e;
				title.innerHTML = entry.display;
				title.className = 'unselectable title';
				subtrees.master = e;
				subtrees.style.paddingLeft = '20px';

				title.onclick = toggleEntry;
				if(checked) updateEntryStatus(e, 'PARENT_SET');
				if(!entry.isleaf){
					var toggle = createToggle();
					toggle.master = e;
					toggle.onclick = expandFolder;
					div.appendChild(toggle);
				}

				div.appendChild(title);
				div.appendChild(subtrees);
				folder.subtrees.appendChild(div);
			}
		});
	};

	var refreshFolder = function(folder, checked) {
		// remove all subfolders
		if (folder.subtrees.hasChildNodes()) {
			while (folder.subtrees.childNodes.length >= 1) {
				folder.subtrees.removeChild(folder.subtrees.firstChild);
			}
		}
		// refresh subfolders
		loadFolder(folder, checked);
	}

	var createToggle = function(){
		var div =		document.createElement('span');
		var toggle		= document.createElement('img');
		var icon		= document.createElement('img');
		div.toggle = toggle;
		div.icon = icon;

		icon.className	= toggle.className		= 'unselectable';
		icon.style.cursor= toggle.style.cursor	= 'pointer';
		icon.style.verticalAlign = toggle.style.verticalAlign = 'middle';
		icon.style.marginLeft = '2px';
		setIcons(div, false);
		div.appendChild(toggle);
		div.appendChild(icon);
		return div;
	}

	var setIcons = function(target, open){
		target.icon.src = open ? '/ProteoSAFe/images/open.png' : '/ProteoSAFe/images/closed.png';
		target.toggle.src = open ? '/ProteoSAFe/images/minus.png' : '/ProteoSAFe/images/plus.png';
	}

	var updateEntryStatus = function(entry, status){
		var old = entry.status;
		if(old == status) return;
		entry.status = status;
		entry.title.style.backgroundColor = (status != 'NONE') ? '#5E88B2' : 'transparent';
		entry.title.style.color = (status != 'NONE' ) ? 'white' : 'black';
		if(status == 'CHECKED')
			if(entry.isleaf) checkedFiles++; else checkedFolders++;
		else if(old == 'CHECKED')
			if(entry.isleaf) checkedFiles--; else checkedFolders--;

	}

	var toggleEntry = function(){ // work on selection toggles
		var entry = this.master;
		var oldStatus = entry.status;
		var newStatus = (entry.status == 'NONE') ? 'CHECKED' : 'NONE';
		if (singleSelect)
			clearAll(entry);
		updateEntryStatus(entry, newStatus);
		if(!entry.isleaf && !singleSelect){
			var s = (newStatus == 'CHECKED') ? 'PARENT_SET' : 'NONE';
			var entries = entry.subtrees.childNodes;
			for(var i = 0; i < entries.length; i++)
				updateEntryStatus(entries[i].master, s);
		}
		if(oldStatus == 'PARENT_SET' && !singleSelect)
			unsetParentStatus(entry.parent);
		return true;
	}

	var unsetParentStatus = function(entry){
		var oldStatus = entry.status;
		updateEntryStatus(entry, 'NONE');
		if(oldStatus == 'PARENT_SET' && entry.parent != null)
			unsetParentStatus(entry.parent);
		var nodes = entry.subtrees.childNodes;
		for(var i = 0; i < nodes.length; i++){
			var child = nodes[i].master;
			if(child.status == 'PARENT_SET')
				updateEntryStatus(child, 'CHECKED');
		}

	}

	var clearAll = function(entry) {
		if (entry.parent != null)
			clearAll(entry.parent);
		else clearEntry(entry);
	}

	var clearEntry = function(entry) {
		updateEntryStatus(entry, 'NONE');
		var entries = entry.subtrees.childNodes;
		for(var i = 0; i < entries.length; i++)
			clearEntry(entries[i].master);
	}

	var expandFolder = function(){ // work on tree toggles
		this.onclick = toggleFolder;
		setIcons(this, true);
		loadFolder(this.master, this.master.status != 'NONE');
		return false;
	}

	var toggleFolder = function(){ // work on tree toggles
		var	target = this.master.subtrees;
		var display = target.style.display;
		target.style.display = (display == 'none') ? 'block' : 'none'; //toggle div
		setIcons(this, display == 'none');
		return false;
	}

	this.root = {
		subtrees: document.getElementById(divName), parent: null,
		path: '', isleaf: false, status: 'NONE'
	}

	this.init = function(){
		loadFolder(this.root, false);
	}

	this.refresh = function(){
		refreshFolder(this.root, false);
		checkedFiles = 0;
		checkedFolders = 0;
	}

	this.setFoldersOnly = function(folders) {
		foldersOnly = folders;
		singleSelect = folders;
	}

	this.getSelectedEntries = function() {
		var entries = this.root.subtrees.getElementsByTagName('div');
		var selected = new Array();
		for(var i = 0; i < entries.length; i++){
			var e = entries[i];
			if(e.className == 'FolderEntryDiv' && e.master.status == 'CHECKED')
				selected.push(e.master);
		}
		return selected;
	}

	this.getSelected = function() {
		var entries = this.getSelectedEntries();
		var selected = new Array();
		for (var i = 0; i < entries.length; i++) {
			selected.push(entries[i].path);
		}
		return selected;
	}

	this.getTargetFile = function() {
		var selected = this.getSelectedEntries();
		if (selected.length < 1)
			return null;
		else if (selected.length > 1)
			throw new Error("SELECTED_MULTIPLE");
		else return selected[0].path;
	}

	this.getTargetFolder = function() {
		var selected = this.getSelectedEntries();
		if (selected.length < 1)
			return null;
		else {
			var folders = new Array();
			for (var i in selected)
				if (selected[i].isleaf == false)
					folders.push(selected[i]);
				else folders.push(selected[i].parent);
			var folder = folders[0];
			for (var i=1; i<folders.length; i++)
				if (folders[i] != folder)
					throw new Error("SELECTED_MULTIPLE");
			return folder.path;
		}
	}

	this.getRootFolder = function() {
		var entries = this.root.subtrees.getElementsByTagName('div');
		if (entries == null || entries.length < 1)
			return null;
		else return entries[0].master.path;
	}

	this.clear = function(){
		var entries = this.root.subtrees.getElementsByTagName('div');
		for(var i = 0; i < entries.length; i++){
			var e = entries[i];
			if(e.className == 'FolderEntryDiv' && e.master.status != 'NONE')
				updateEntryStatus(e.master, 'NONE');
		}
	}

	this.show = function(){
		this.root.subtrees.style.display = 'block';
	}

	this.hide = function(){
		this.root.subtrees.style.display = 'none';
	}

	this.getCheckedFiles = function(){
		return checkedFiles;
	}

	this.getCheckedFolders = function(){
		return checkedFolders;
	}
}

/* POPUP window */
var active_popup = null;

function PopupWindow(div_name){
	var div = document.getElementById(div_name);
	div.style.display = 'none';
	div.style.position = 'absolute';
	this.div = div;

	this.show = function(){
		if (active_popup == this) return; // we are already showing the right div
		if (active_popup != null)
			active_popup.hide();
		if (div != null) {
			active_popup = this;
			div.style.display = 'block';
			if(this.reposition) this.reposition();
		}
	}

	this.hide = function(){
		div.style.display = 'none';
		if(active_popup == this)
			active_popup = null;
	}

	this.toggle = function(){
		if(div.style.display != 'block')
			this.show();
		else this.hide();
	}

	this.relativePosition = function(base){
		var posY = base.offsetTop;
		var posX = base.offsetLeft + base.offsetWidth;
		div.style.position = 'absolute';

		var wholeBody = document.getElementsByTagName(
			(document.compatMode && document.compatMode == "CSS1Compat") ? "HTML" : "BODY")[0];
		var bodyWidth = (wholeBody.clientWidth) ?
				wholeBody.clientWidth + wholeBody.scrollLeft :
				window.innerWidth + window.pageXOffset;
		var bodyHeight = (window.innerHeight) ?
				window.innerHeight + window.pageYOffset :
				wholeBody.clientHeight + wholeBody.scrollTop;

		div.style.top = (posY) + "px";
		div.style.left = (posX) + "px";
		if( posX + div.offsetWidth > bodyWidth )	//  body bounds
			div.style.left = (bodyWidth - div.offsetWidth) + 'px';
		if( posX < 0) div.style.left = 0;
		if( posY + div.offsetHeight > bodyHeight )
			div.style.top = (bodyHeight - div.offsetHeight) + 'px';
		if( posY < 0) div.style.top = 0;
	}

	this.makeRelativeTo = function(base_name){
		var baseDiv = document.getElementById(base_name);
		this.reposition = function(){ this.relativePosition(baseDiv); }
	}

	this.makeCentral = function(w, h){
		this.reposition = function(){centralize(w, h);};
	}

	var centralize = function(w, h){
		var body = document.getElementsByTagName(
			(document.compatMode && document.compatMode == "CSS1Compat") ? "HTML" : "BODY")[0];
		var bodyWidth = window.innerWidth ? window.innerWidth : body.clientWidth;
		var bodyHeight = window.innerHeight ? window.innerHeight : body.clientHeight;

		var top  = Math.ceil((bodyHeight - (h ? h : div.offsetHeight)) / 2);
		var left = Math.ceil((bodyWidth - (w ? w : div.offsetWidth)) / 2);
		div.style.position = 'fixed';
		div.style.top = (top < 0 ? 0 : top) + "px";
		div.style.left =  (left < 0 ? 0 : left) + "px";
		window.onresize = function(){
			centralize();
		}
	}
}

function buildTables(workflow, tables, div, subtype) {
	var req = createRequest();
	if (subtype == null)
		subtype = "result";
    var url = "/ProteoSAFe/DownloadWorkflowInterface?workflow=" + workflow +
    	"&task=" + taskId + "&type=" + subtype;
	req.open("GET", url, true);
	req.onreadystatechange = function() {
		if (req.readyState == 4 && showResult) {
			// build tables
			var blocks = req.responseXML.getElementsByTagName("table");
			for (i=0; i<blocks.length; i++) {
				// start table
				var table = new Array();
				table["filtered"] = hits;	// TODO: replace with correct
				table["sg"] = new SortingGroup();
				table["fg"] = new FilterGroup();
				table["ng"] = new NavigationGroup(30);
				table["doRender"] = function() {
					table["ng"].setSource(table["filtered"]);
					renderRows(table["html"], table["filtered"], table["renders"], table["ng"].getBegin(), table["ng"].getEnd());
				}
				table["doSort"] = function() {
					table["sg"].setSource(table["filtered"]);
					table["sg"].sort();
				}
				table["doFilter"] = function() {
					table["filtered"] = table["fg"].filter(hits);	// TODO
					table["doSort"]();
				}
				// build title header, if this is the main "hits" table
				var headers = new Array();
				var tableName = blocks[i].attributes.getNamedItem("name").nodeValue;
				if (tableName == "hits") {
					var title = function(tr, seq) {
						var td = table["ng"].renderControls(tr);
						td.colSpan = '20';
						table["ng"].setTitle(
							taskDescription,
							function(begin, end, total) {
								return ' Hits ' + (begin + 1) + ' ~ ' + end + ' out of ' + total + ' ';
							}
						);
					}
					headers.push(title);
				}
				// build remaining headers
				var rows = blocks[i].getElementsByTagName("header");
				for (j=0; j<rows.length; j++) {
					var header = new Array();
					var columns = rows[j].getElementsByTagName("column");
					for (k=0; k<columns.length; k++) {
						var column = columns[k];
						var type = column.attributes.getNamedItem("type").nodeValue;
						if (type == "renderButton") {
							var name = column.attributes.getNamedItem("name").nodeValue;
							var action = column.attributes.getNamedItem("action").nodeValue;
							header.push(renderButton(name, action, table[action]));
						} else if (type == "plainSorter") {
							var name = column.attributes.getNamedItem("name").nodeValue;
							var colData = column.attributes.getNamedItem("colData").nodeValue;
							if (isColumnHidden(colData) == false) {
								var tooltip = column.attributes.getNamedItem("tooltip");
								if (tooltip != null)
									tooltip = tooltip.nodeValue;
								header.push(plainSorter(table["sg"], name, colData, tooltip));
							}
						} else if (type == "numberSorter") {
							var name = column.attributes.getNamedItem("name").nodeValue;
							var colData = column.attributes.getNamedItem("colData").nodeValue;
							if (isColumnHidden(colData) == false) {
								var tooltip = column.attributes.getNamedItem("tooltip");
								if (tooltip != null)
									tooltip = tooltip.nodeValue;
								header.push(numberSorter(table["sg"], name, colData, tooltip));
							}
						} else if (type == "markFilter") {
							var fieldname = column.attributes.getNamedItem("fieldname").nodeValue;
							var name = column.attributes.getNamedItem("name").nodeValue;
							header.push(markFilter(table["fg"], fieldname, name));
						} else if (type == "plainFilter") {
							var colData = column.attributes.getNamedItem("colData").nodeValue;
							if (isColumnHidden(colData) == false)
								header.push(plainFilter(table["fg"], colData));
						} else if (type == "rangeFilter") {
							var colData = column.attributes.getNamedItem("colData").nodeValue;
							if (isColumnHidden(colData) == false) {
								var size = column.attributes.getNamedItem("size");
								if (size != null)
									header.push(rangeFilter(table["fg"], colData, size.nodeValue));
								else header.push(rangeFilter(table["fg"], colData));
							}
						}
						// special header types
						else if (type == "empty")
							header.push(null);
					}
					if (j == 0)
						headers.push(createCaptionHeader(null, header));
					else headers.push(createExtraHeader(null, header));
				}
				// build data rows
				var colSpan = 0;
				var renders = new Array();
				rows = req.responseXML.getElementsByTagName("row");
				for (j=0; j<rows.length; j++) {
					var row = new Array();
					// render nested view, if present
					var file = rows[j].attributes.getNamedItem("file");
					if (file != null) {
						row.push(null);
						table["subtables"] = new Array();
						// recursive call to this function
						buildTables(workflow, table["subtables"], null,
							file.nodeValue);
						row.push(renderEmptyColumn(colSpan - 1));
					} else {
						// otherwise render columns as normal
						var columns = rows[j].getElementsByTagName("column");
						if (columns.length > colSpan)
							colSpan = columns.length;
						for (k=0; k<columns.length; k++) {
							var column = columns[k];
							var type = column.attributes.getNamedItem("type").nodeValue;
							if (type == "renderPlain") {
								var colData = column.attributes.getNamedItem("colData").nodeValue;
								if (isColumnHidden(colData) == false) {
									var size = column.attributes.getNamedItem("size").nodeValue;
									row.push(renderPlain(colData, size));
								}
							} else if (type == "renderInteger") {
								var colData = column.attributes.getNamedItem("colData").nodeValue;
								if (isColumnHidden(colData) == false)
									row.push(renderInteger(colData));
							} else if (type == "renderFloat") {
								var colData = column.attributes.getNamedItem("colData").nodeValue;
								if (isColumnHidden(colData) == false) {
									var precision = column.attributes.getNamedItem("precision").nodeValue;
									row.push(renderFloat(colData, precision));
								}
							} else if (type == "renderExpNum") {
								var colData = column.attributes.getNamedItem("colData").nodeValue;
								if (isColumnHidden(colData) == false) {
									var precision = column.attributes.getNamedItem("precision").nodeValue;
									row.push(renderExpNum(colData, precision));
								}
							}
							// special render types
							else if (type == "renderProtein") {
								var colData = column.attributes.getNamedItem("colData").nodeValue;
								if (isColumnHidden(colData) == false) {
									var size = column.attributes.getNamedItem("size").nodeValue;
									row.push(renderProtein(colData, size));
								}
							} else if (type == "empty") {
								row.push(null);
							} else if (type == "renderControl") {
								row.push(renderControl(taskId));
							} else if (type == "renderDisplayControl") {
								row.push(renderDisplayControl(taskId));
							} else if (type == "renderClusterDisplayControl") {
								row.push(renderClusterDisplayControl(taskId));
							} else if (type == "inspectImage") {
								row.push(inspectImage);
							} else if (type == "inspectDetailText") {
								row.push(inspectDetailText);
							}
						}
					}
					var decorator = rows[j].attributes.getNamedItem("decorator");
					if (decorator != null)
						renders.push(createRowRender(window[decorator.nodeValue], row));
					else renders.push(createRowRender(null, row));
				}
				// complete table
				table["headers"] = headers;
				table["renders"] = renders;
				tables[tableName] = table;
			}
			if (div != null)
				renderTables(tables, div);
		}
	}
	req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
	req.send(null);
}

function renderTables(tables, div) {
	var currentTables = div.getElementsByTagName("table");
	for (var tableName in tables) {
		var table = tables[tableName];
		table["sg"].setSource(table["filtered"]);
		table["sg"].setCallback(table["doRender"]);
		table["html"] = document.createElement("table");
		table["html"].id = tableName;
		table["html"].setAttribute("class", "tabular");
		setupTable(table["html"], decorateTable, table["headers"]);
		// if (table["doFilter"] != null)
		//	document.getElementById("doFilter").onclick = table["doFilter"];
		table["ng"].setCallback(function(begin, end){
			renderRows(table["html"], table["filtered"], table["renders"], begin, end);
		});
		// if this table exists already, clear it before rendering it again
		for (var i=0; i<currentTables.length; i++) {
			var currentTable = currentTables.item(i);
			var id = currentTable.attributes.getNamedItem("id");
			if (id == null)
				continue;
			else if (id.nodeValue == tableName) {
				div.removeChild(currentTable);
				break;
			}
		}
		// insert this table after the first table currently in the document
		// (which should be the download dialog table)
		var previousNode = null;
		previousNode = currentTables[0];
		if (previousNode != null) {
			div.insertBefore(table["html"], previousNode.nextSibling);
			table["doRender"]();
		}
		// needed for mozilla to shrink the window
		if (window.navigator && window.navigator.userAgent.indexOf("ecko") != -1  ) {
			var tbody = table["html"].getElementsByTagName("tbody")[0];
			if (tbody.scrollHeight <= parseInt(tbody.style.height))
				tbody.style.height="auto";
		}
	}
}
