var CCMSUtils = {};
CCMSUtils.logTracker = {};

function consoleLog(message) {
	if (message == null)
		return;
	else if (console && console.log)
		console.log(message);
}

function consoleError(message) {
	if (message == null)
		message = "";
	throw(message);
}

function trackedConsoleLog(message, token) {
	if (token == null) {
		consoleLog(message);
		return;
	}
	var index = CCMSUtils.logTracker[token];
	if (index == null)
		index = CCMSUtils.logTracker[token] = 0;
	consoleLog("[" + token + "] " + index + ". " + message);
	index++;
	CCMSUtils.logTracker[token] = index;
}

function extractResponseMessage(responseText) {
	if (responseText == null)
		return null;
	var parsed = responseText;
	var start = parsed.indexOf("[:");
	if (start < 0)
		return null;
	else parsed = parsed.substring(start + 2)
	var end = parsed.indexOf(":]");
	if (end < 0)
		return null;
	else return parsed.substring(0, end);
}

function decodeHTMLEntities(content) {
	if (content == null)
		return null;
	// this functionality requires jQuery!
	else return $('<div/>').html(content).text();
}

// function to parse the location hash JSON object - if it is one
function parseLocationHash() {
	var hash = location.hash;
	if (hash == null || hash.length < 2)
		return null;
	// decode URI'ed hash
	hash = decodeURIComponent(hash);
	// slice off leading "#" character
	hash = hash.substring(1);
	// try to parse
	try { return JSON.parse(hash); }
	catch (error) {
		consoleLog("Error parsing location hash: [" + error + "]");
		return null;
	}
}

function addHashQueryToRequest() {
	// get query from the location hash
	var query = location.hash;
	// strip off leading hash character, as it's irrelevant to the query
	if (query.charAt(0) == '#')
		query = query.substring(1);
	// strip off enclosing curly braces to prevent
	// recursive parameter resolution
	if (query.charAt(0) == '{')
		query = query.substring(1);
	if (query.charAt(query.length - 1) == '}')
		query = query.substring(0, query.length - 1);
	// rebuild request parameter string, excluding old query, if one exists
	var newSearch = "?";
	var search = location.search;
	if (search.charAt(0) == '?')
		search = search.substring(1);
	var parameters = search.split("&");
	if (parameters != null && parameters.length > 0) {
		for (var i=0; i<parameters.length; i++) {
			var parameter = parameters[i];
			var tokens = parameter.split("=");
			if (tokens != null && tokens.length > 0 &&
				tokens[0].trim() == "query")
				continue;
			else newSearch += parameter + "&";
		}
	}
	// append new query to the request parameter string
	newSearch += "query=" + query;
	// reload page with the correct query encoded in the request
	location.search = newSearch;
}

function enableOverlay(div, enable, showSpinner, height) {
	if (div == null)
		return;
	// show or hide the overlay
	if (enable) {
		div.style.visibility = "visible";
		// if a spinner was specified, show it
		if (showSpinner) {
			var spinner = div.firstElementChild;
			if (spinner == null)
				return;
			else if (showSpinner)
				enableSpinner(spinner, true, height);
			else enableSpinner(spinner, false);
		}
	} else div.style.visibility = "hidden";
}

function enableSpinner(div, enable, height) {
	if (div == null)
		return;
	// clear out div
	removeChildren(div);
	// build and display spinner, if indicated
	if (!height)
		height = "200%";
	if (enable) {
		var spinner = new Spinner({
			lines: 13,				// The number of lines to draw
			length: 7,				// The length of each line
			width: 4,				// The line thickness
			radius: 10,				// The radius of the inner circle
			corners: 1,				// Corner roundness (0..1)
			rotate: 0,				// The rotation offset
			color: "#FFF",			// #rgb or #rrggbb
			speed: 1,				// Rounds per second
			trail: 23,				// Afterglow percentage
			shadow: false,			// Whether to render a shadow
			hwaccel: false,			// Whether to use hardware acceleration
			className: "spinner",	// The CSS class to assign to the spinner
			zIndex: 2e9,			// The z-index (defaults to 2000000000)
			top: height,			// Top position relative to parent in px
			left: "auto"			// Left position relative to parent in px
		}).spin(div);
	}
}

function enableDiv(div_name, enable, style) {
	var div = document.getElementById(div_name);
	if (style == null)
		style = "block";
	if (div != null)
		div.style.display = enable ? style : "none";
}

function toggleDiv(div_name){
	var div = document.getElementById(div_name);
	if(div) div.style.display = div.style.display == "none" ? "block" : "none";
}

function enableRow(div_name, enable){
	var div = document.getElementById(div_name);
	if(div){
		try{
			div.style.display = enable ? "table-row" : "none";
		}
		catch(e){
			div.style.display = "block";
		}
	}
}

function toggleRow(div_name){
	var div = document.getElementById(div_name);
	if(div){
		try{
			div.style.display = (div.style.display == "none") ? "table-row" : "none";
		}
		catch(e){
			div.style.display = "block";
		}
	}
}

function enableInline(div_name, enable){
	var div = document.getElementById(div_name);
	if(div)
		div.style.display = enable ? "inline" : "none";
}

var injectedScripts = new Array();

function injectScripts(html) {
	if (html == null)
		return;
	var token = "<script"
	for (var i=html.indexOf(token); i>=0; i=html.indexOf(token)) {
		try {
			// extract script element's attributes
			html = html.substring(i + token.length);
			var script = html.substring(0, html.indexOf(">"));
			// extract "src" attribute from script element
			var source = script.indexOf("src=");
			if (source >=0) {
				script = script.substring(source + 4);
				// strip off enclosing characters
				var quote = script.charAt(0);
				script = script.substring(1);
				script = script.substring(0, script.indexOf(quote));
				// inject script source
				injectScript(script);
			}
		} catch (error) {
			return;
		}
	}
}

function injectScript(source) {
	if (source == null || injectedScripts.indexOf(source) >= 0)
		return;
	var script = document.createElement("script");
	script.setAttribute("type", "text/javascript");
	script.setAttribute("language", "javascript");
	script.setAttribute("src", source);
	document.getElementsByTagName("head")[0].appendChild(script);
	injectedScripts.push(source);
}

function resizeFlash(div_name, width, height) {
	var div = document.getElementById(div_name);
	if (div) {
		div.style.width = width;
		div.style.height = height;
	}
}

function modifyHref(div_name, regex, replacement){
	var div = document.getElementById(div_name);
	if(div)
		div.href = div.href.replace(regex, replacement);
}

function stripSeparators(value, separator) {
	if (value == null)
		return null;
	else if (separator == null)
		separator = ",";
	// escape separator string for regular expressions
	separator = separator.replace(/([.?*+^$[\]\\(){}|-])/g, "\\$1");
	// build regular expression to globally remove
	// all occurrences of the separator string
	var finder = new RegExp(separator, "g");
	return value.replace(finder, "");
}

function getRequestParameter(parameter) {
	if (parameter == null)
		return null;
	parameter = parameter.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
	var pattern = "[\\?&]" + parameter + "=([^&#]*)";
	var regex = new RegExp(pattern);
	var result = regex.exec(window.location.href);
	if (result == null)
		return null;
	else return result[1];
}

function resolveParameters(value) {
	if (value == null)
		return null;
	// parse out all parameter references
	while (true) {
		// parse out parameter name
		var parameterName = null;
		var start = value.indexOf("{");
		if (start < 0)
			break;
		var end = value.indexOf("}");
		if (end <= start)
			break;
		else if (end == start + 1)
			parameterName = "";
		else parameterName = value.substring(start + 1, end);
		// retrieve parameter value
		var parameterValue = getRequestParameter(parameterName);
		if (parameterValue == null)
			parameterValue = parameterName;
		var paramStart = parameterValue.indexOf("{");
		var paramEnd = parameterValue.indexOf("}");
		if (paramStart >= 0 && paramEnd > paramStart) {
			alert("Request parameter values should not themselves " +
				"be references to other parameters. You run the risk " +
				"of entering an infinite loop!");
			throw "Request parameter recursive reference";
		} else value = value.substring(0, start) + parameterValue +
			value.substring(end + 1);

	}
	return value;
}

var BR = new Object();

function appendChildren(div, children){
	for(var i = 0; i < children.length; i++){
		var child = children[i];
		if(child == BR)
			div.appendChild(document.createElement('br'));
		else if(typeof child == 'string')
			div.appendChild(document.createTextNode(child));
		else div.appendChild(child);
	}  
}

function removeChildren(div) {
	if (div == null)
		return;
	if (div.hasChildNodes()) {
		while (div.childNodes.length >= 1) {
			div.removeChild(div.firstChild);
		}
	}
}

function spliceChild(div, child, index) {
	if (div == null || child == null)
		return;
	if (index == null ||
		div.hasChildNodes() == false || div.childNodes.length <= index) {
		div.appendChild(child);
		return;
	} else div.insertBefore(child, div.childNodes[index]);
}

function getChildById(div, id) {
	if (div == null || id == null)
		return;
	if (div.hasChildNodes()) {
		for (var i=0; i<div.childNodes.length; i++) {
			var child = div.childNodes[i];
			if (getAttribute(child, "id") == id)
				return child;
		}
	}
	return null;
}

function getAttribute(element, attribute) {
	if (element == null || attribute == null)
		return null;
	try {
		return element.attributes.getNamedItem(attribute).nodeValue;
	} catch (error) {
		return null;
	}
}

function copyHash(hash) {
	if (hash == null)
		return null;
	var copy = {};
	for (var key in hash) {
		if (hash.hasOwnProperty(key))
			copy[key] = hash[key];
	}
	return copy;
}

function countHashElements(hash) {
	var count = 0;
	for (var key in hash) {
		if (hash.hasOwnProperty(key))
			++count;
	}
	return count;
}

function isHashEmpty(hash) {
	return countHashElements(hash) <= 0;
}

function encodeRequestParameter(name, value) {
	return escape(name).replace(/\+/g, "%2B") + "=" +
		escape(value ? value : "").replace(/\+/g, "%2B");
}

function serializeForm(form) {
	if (form == null)
		return null;
	var query = "";
	function appendFormValue(name, value) {
		query += (query.length > 0 ? "&" : "") +
			encodeRequestParameter(name, value);
	}
	var elements = form.elements;
    for (var i=0; i<elements.length; i++) {
    	var element = elements[i];
    	var type = element.type.toUpperCase();
    	var name = element.name;
    	if (name) {
    		if (type == "TEXT" || type == "TEXTAREA" || type == "PASSWORD" ||
    			type == "BUTTON" || type == "RESET" || type == "SUBMIT" ||
    			type == "FILE" || type == "IMAGE" || type == "HIDDEN")
    			appendFormValue(name, element.value);
    		else if (type == "CHECKBOX" && element.checked)
    			appendFormValue(name, element.value ? element.value : "On");
    		else if (type == "RADIO" && element.checked)
    			appendFormValue(name, element.value);
    		else if (type.indexOf("SELECT") != -1)
            	for (var j=0; j<element.options.length; j++) {
            		var option = element.options[j];
            		if (option.selected)
            			appendFormValue(
            				name, option.value ? option.value : option.text);
            	}
    	}
    }
    return query;
}

function createForm(method, action, children){
	var form = document.createElement('form');
	form.method = method;
	form.action = action;
	appendChildren(form, children);
	return form;
}

function createInput(name, size){
	var input = document.createElement('input');
	input.type = 'text';
	input.name = name;
	input.size = size;
	return input;
}

function createPassword(name, size){
	var input = document.createElement('input');
	input.type = 'password';
	input.name = name;
	input.size = size;
	return input;
}

function createLink(text, url){
	var link = document.createElement('a');
	if(url) link.href = url;
	link.innerHTML = text;
	return link;
}

function createCheckbox(name){
	var check = document.createElement('input');
	check.type = 'checkbox';
	check.name = name;
	return check;
}

function createRadio(name, value){

	try{
		radio = document.createElement('<input type="radio" ' + 'name="' + name +'" />');
	}catch(err){
		radio = document.createElement('input');
		radio.type = 'radio';
		radio.name = name;
		radio.value = value;
	}
	return radio;
}

function createButton(name, text){
	var button = document.createElement('button');
	button.name = name;
	if(text) button.innerHTML = text;
	return button;
}

function createHidden(name, value){
	var hidden = document.createElement('input');
	hidden.type = 'hidden';
	hidden.name = name;
	hidden.value = value ? value : '';
	return hidden;
}

function createSpan(id, children){
	var span = document.createElement('span');
	if(id) span.id = id;
	if(children)
		appendChildren(span, children);
	return span;
}

/* Interactive Help support */

var _help_active = null;
var _help_timer =  null;

function help_show(divname, fixed)
{
	var mydiv = document.getElementById(divname);
	help_hold();  // reset timer 
	
	if (_help_active != null) {
		if (_help_active == mydiv) {
				return; // we are already showing the right div
		}
		else {
			help_hide_now();
		}
	}

	_help_active = mydiv;
	if (_help_active != null) {
			help_position(divname, fixed);
			mydiv.style.display = "block";
	}
	help_hold();
}

function help_position(divname, fixed) {
	var mydiv = document.getElementById(divname);
	var mylink = document.getElementById(divname + "-");

	
	if(fixed != true){
		var wholeBody = document.getElementsByTagName((document.compatMode && document.compatMode == "CSS1Compat") ? "HTML" : "BODY")[0];
		var posY = mylink.offsetTop;
		var posX = mylink.offsetLeft + mylink.offsetWidth;

		var bodyWidth = (wholeBody.clientWidth) ? wholeBody.clientWidth + wholeBody.scrollLeft : window.innerWidth + window.pageXOffset;
		var bodyHeight = (window.innerHeight) ? window.innerHeight + window.pageYOffset : wholeBody.clientHeight + wholeBody.scrollTop;

		mydiv.style.top = (posY - 10 ) + "px";
		mydiv.style.left = (posX ) + "px";
	
		if( posX + mydiv.offsetWidth > bodyWidth ){	//  body bounds 
			mydiv.style.left = (bodyWidth - mydiv.offsetWidth) + "px";
		}
		if( posY + mydiv.offsetHeight > bodyHeight ){
			mydiv.style.top = (bodyHeight - mydiv.offsetHeight) + "px";
		}
	}
	else{
		mydiv.style.position = "fixed";
		var posY = getY(mylink);
		var posX = getX(mylink) + mylink.offsetWidth;
		mydiv.style.top = (posY - 10) + "px";
		mydiv.style.left = (posX) + "px";
	}
}

function getY(emt){
	var ret = 0;
	while(emt != null){
		ret += emt.offsetTop;
		emt = emt.offsetParent;
	}
	return ret;
}

function getX(emt){
	var ret = 0;
	while(emt != null){
		ret += emt.offsetLeft;
		emt = emt.offsetParent;
	}
	return ret;
}


function help_hide()
{
	if(_help_active != null){
			_help_timer = setTimeout("help_hide_now();", 500);
	}
}

function help_hide_now()
{
	if (_help_active != null) {
		_help_active.style.display = 'none';
		_help_active = null;
	}
}

function help_hold()
{
		if (_help_timer != null) {
			clearTimeout(_help_timer);
		}
}

function toggleHelp(divname, fixed){
	var div = document.getElementById(divname);
	if(div.style.display != 'block')
		help_show(divname, fixed);
	else help_hide();
}

/* AJAX XML HTTP request generation */
function createRequest() {
	var xmlHttp;
	if (window.XMLHttpRequest) {
		xmlHttp = new XMLHttpRequest(); // IE7, Safari, Firefox
		//alert("IE7");
	} else if (window.ActiveXObject) {
		xmlHttp = new ActiveXObject("Microsoft.XMLHTTP"); // IE6
		//alert("IE6");                
	}
	return xmlHttp;
}

/* Non-AJAX XML string parsing */
function parseXML(xml) {
	var xmlDoc;
	if (window.DOMParser) {
		var parser = new DOMParser();
		xmlDoc = parser.parseFromString(xml,"text/xml");
	} else {
		xmlDoc = new ActiveXObject("Microsoft.XMLDOM");	// IE
		xmlDoc.async = "false";
		xmlDoc.loadXML(xml);
	}
	return xmlDoc;
}

/* Logo block */

function createSubmenu(caption, items){
	var li = document.createElement('li');
	var ul = document.createElement('ul');
	appendChildren(ul, items);
	appendChildren(li, [createLink(caption, null), ul]);
	return li;
}

function createMenuItem(caption, link){
	var li = document.createElement('li');
	li.appendChild(createLink(caption, link));
	return li;
}

/*function initLogoBlock(divname, authenticated, admin, site, context, current){
	var div = document.getElementById(divname);
	var login = document.createElement('div');
	var button = createButton('login', 'Sign in');
	var banner = document.createElement('div');
	var menu = document.createElement('ul');
	button.onclick = function(){ this.form.submit(); };
	login.className = 'loginbox';
	banner.className = 'pdmenu';
	banner.appendChild(menu);
	appendChildren(div, [login, banner]);
	
	if(!authenticated)
		login.appendChild(
			createForm('post', 'https://' + site + context + '/user/login.jsp',
				[	'User: ', createInput('user', '8'),
					' Pass: ', createPassword('password', '8'),
					button, BR,
					createHidden('url', current),
					"Don't have a username? ",
					createLink('Register!', context + '/user/register.jsp')
				]
			)
		);
	if(authenticated){
		var items = [
			createMenuItem('Logout', context + '/user/logout.jsp'),
			createMenuItem('User Profile', context + '/user/profile.jsp'),
			createMenuItem('Jobs', context + '/jobs.jsp')
		];
		if(admin) items.push(createMenuItem('All jobs', context + '/jobs.jsp?user=all'));
		menu.appendChild(createSubmenu('Account', items));
	}
	appendChildren(menu,[
		createSubmenu('About', [
			createMenuItem('General Info', context + '/help.jsp'),
			createMenuItem('UCSD Proteomics', 'http://proteomics.bioprojects.org/'),
			createMenuItem('Future Tools', 'http://proteomics.bioprojects.org/Software.html')
		]),
		createSubmenu('Help', [
			createMenuItem('Demo', null),
			createMenuItem('Contact', 'http://proteomics.bioprojects.org/People.html')
		])
	]);
}*/

function initLogoBlock(
	divname, authenticated, admin, showDatasets, context, current
) {
	var div = document.getElementById(divname);
	var login = document.createElement('div');
	var banner = document.createElement('div');
//	var menu = document.createElement('ul');
	login.className = 'loginbox';
//	banner.className = 'pdmenu';
	banner.className = 'banner';
//	banner.appendChild(menu);
	appendChildren(div, [login, banner]);
	
	if (!authenticated) {
		login.innerHTML = 
			'<iframe ' +
			'src="' + context + '/user/login_box.jsp?url=' + current  + '" '+
			'frameborder="0" allowtransparency="true" style="height: 26px">' +
			'</iframe>' +
			'<div class="loginframe">' +
				'Don\'t have an account? ' +
				'<a href="' + context + '/user/register.jsp">Register!</a>' +
			'</div>';
	}
	// render user account links
	var items = null;
	if (authenticated) {
		appendChildren(banner,
			[createLink('Logout', context + '/user/logout.jsp'), ' | ']);
	    if ('${livesearch.build}' == 'GNPS')
			appendChildren(banner,
				[createLink('My User', context + '/user/summary.jsp'), ' | ']);
		appendChildren(banner, [
			createLink('Update Profile', context + '/user/profile.jsp'), ' | '
		]);
		// render workflow task links
		items = [createLink('Jobs', context + '/jobs.jsp'), ' | '];
		if (admin) {
			items.push(createLink('All Jobs', context + '/jobs.jsp?user=all'));
		 	items.push(' | ');
		}
		appendChildren(banner, items);
	}
	// render MassIVE repository links
	if (showDatasets) {
		items = [
			//createLink('MassIVE Datasets', context + '/datasets.jsp'), ' | '
            createLink('MassIVE Datasets', context + '${livesearch.datasets.link}'), ' | '
		];
		if (authenticated && admin) {
			items.push(
				createLink('All Datasets', context + '/datasets.jsp?user=all'));
			items.push(' | ');
		}
		appendChildren(banner, items);
	}
	// render common links
    
    if('${livesearch.build}' == 'GNPS'){
        appendChildren(banner, [
            createLink('Documentation', 'https://bix-lab.ucsd.edu/display/Public/GNPS+Documentation+Page'), ' | ',
            createLink('Forum', 'https://groups.google.com/forum/#!forum/molecular_networking_bug_reports'), ' | ',
            createLink('Contact', context + '/contact.jsp')
        ]);
    }
    else{
        appendChildren(banner, [
            createLink('General Info', context + '/help.jsp'), ' | ',
            createLink('UCSD Proteomics', 'http://proteomics.ucsd.edu/'), ' | ',
            createLink('Future Tools', 'http://proteomics.ucsd.edu/Software.html'), ' | ',
            createLink('Demo', context + '/${livesearch.demo.jsp}'), ' | ',
            createLink('Contact', context + '/contact.jsp')
        ]);
    }
}

/**
 * Balloon tooltips
 */
function createTooltip() {
	var tooltip = new Box;
	BalloonConfig(tooltip, "GBox");
	tooltip.opacity = 0.95;
	tooltip.hOffset = 0;
	tooltip.vOffset = 0;
	tooltip.fontSize = "10pt";
	tooltip.allowEventHandlers = true;
	return tooltip;
}

function showTooltip(caption, event, tooltipText) {
	event = window.event || event;					// ridiculous IE workaround
	// show the (non-"sticky") tooltip
	tooltip.showTooltip(event, tooltipText);
	// if this caption doesn't have an onclick event, register a new
	// one to show a "sticky" version of this tooltip when clicked
	if (caption.onclick == null) {
		caption.onclick = function(newEvent) {
			newEvent = window.event || newEvent;	// IE workaround, again
			tooltip.nukeTooltip();
			tooltip.showTooltip(newEvent, tooltipText);
		};
	}
}

function attachTooltip(caption, tooltipText) {
	caption.onmouseover = function(event) {
		showTooltip(this, event, tooltipText);
	};
}

//For Show Hide Buttons
function generate_hide_input_callback(row_classname, button_object){
    return function(){
        $(row_classname).hide();
        button_object.innerHTML = "Show Fields";
        button_object.onclick = generate_show_input_callback(row_classname, button_object);
    }
}

function generate_show_input_callback(row_classname, button_object){
    return function(){
        $(row_classname).show();
        button_object.innerHTML = "Hide Fields";
        button_object.onclick = generate_hide_input_callback(row_classname, button_object);
    }
}

/**
 * Synchronized queue class
 * 
 * This queue maintains a list of "processes".  A process can be anything that
 * can be assigned to a Javascript variable - a scalar value, an object, a
 * function, whatever.  Each process represents a single thing that you want to
 * do in a synchronous manner - that is, when you want to completely finish each
 * "process" before you begin the next one.
 * 
 * The queue also maintains a reference to a "processCallback".  This is
 * assumed to be a Javascript function that accepts a single argument - an
 * instance of a "process".  It is assumed that the processCallback function
 * knows what to do with a process.
 * 
 * Using the queue is simple:
 * 
 * 1. Instantiate the queue by passing the processCallback function as the sole
 *    argument to its constructor.
 * 
 * 2. Add a process to the queue by calling its queue(process) method.  This
 *    triggers an immediate "launch" of the process by invoking the
 *    queue's poll() function, which in turn calls the processCallback function
 *    with the supplied process as its argument.  Whenever processCallback
 *    is called in this manner, the queue remembers what process is running by
 *    setting a flag.  This flag will not be cleared until processCallback
 *    returns - thereby ensuring that subsequently queued processes are not
 *    handled until previously queued ones are done.
 * 
 * 3. Add more processes to the queue, via more calls to the queue(process)
 *    method.  Maybe previous processes are done, maybe they're not.  The point
 *    is that we don't know how long it will take for each to finish, so we
 *    need to be confident that processes are launched in the order in which
 *    they were queued.
 * 
 * 4. The processCallback function should always clear the current process flag
 *    by calling the advance() method of the queue, whenever it is completely
 *    done (i.e. in the callback of an AJAX request).
 */
function SynchronizedQueue(processCallback) {
	// properties
	this.processQueue = new Array();
	this.priorityQueue = new Array();
	this.currentProcess = null;
	this.processCallback = processCallback;
}

SynchronizedQueue.prototype.setProcessCallback = function(processCallback) {
	this.processCallback = processCallback;
}

SynchronizedQueue.prototype.poll = function() {
	// if another process is currently running, do nothing
	if (this.currentProcess != null)
		return;
	// if anything is in the main queue, do it first; the priority queue
	// comes last, to ensure that its operations are not overwritten
	var queue = null;
	if (this.processQueue.length > 0)
		queue = this.processQueue;
	else queue = this.priorityQueue;
	// if there are no further processes to run, do nothing
	if (queue.length < 1)
		return;
	// otherwise, launch the next process in the queue
	this.currentProcess = queue[0];
	queue.splice(0, 1);
	this.processCallback(this.currentProcess);
}

SynchronizedQueue.prototype.queue = function(process) {
	if (process == null)
		return;
	this.processQueue.push(process);
	this.poll();
}

SynchronizedQueue.prototype.force = function(process) {
	if (process == null)
		return;
	this.priorityQueue.push(process);
	this.poll();
}

SynchronizedQueue.prototype.advance = function() {
	this.currentProcess = null;
	this.poll();
}


//Utiltiy functions for grabbing url parameter values 
function get_taskid(){
  var query = window.location.search.substring(1);
  var vars = query.split("&");
  var task_id = "";
  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
    if(pair[0] == "task"){
      task_id = pair[1];
      return task_id;
    }
  }
  return "";
}

function getURLParameters(){
    var query = window.location.search.substring(1);
    var vars = query.split("&");
    var task_id = "";
    output_map = {}
    for (var i=0;i<vars.length;i++) {
        var pair = vars[i].split("=");
        output_map[pair[0]] = pair[1]
    }
    return output_map
}

function makeRandomString(length)
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < length; i++ )
        text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
}

//Pad integer to string with leading zeroes
function pad(n, width, z) {
  z = z || '0';
  n = n + '';
  return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

//Converting Massive id to massive string
function massiveIDToString(massive_id){
    return "MSV" + pad(massive_id, 9)
}

String.prototype.escapeSpecialChars = function() {
    return this.replace(/\\n/g, "\\n")
               .replace(/\\'/g, "\\'")
               .replace(/\\"/g, '\\"')
               .replace(/\\&/g, "\\&")
               .replace(/\\r/g, "\\r")
               .replace(/\\t/g, "\\t")
               .replace(/\\b/g, "\\b")
               .replace(/\\f/g, "\\f")
               .replace(/\\/g, "\\\\");
};