var counter = 0;
INDEX = 'Position';

/* Data Entry Constructors */
function renderControl(task){
	return function(td, hit, seq){
		hit.task = task;
		td.noWrap = 'true';

		var checkbox = document.createElement('input');
		checkbox.type = 'checkbox';
		checkbox.style.verticalAlign = 'middle';
		checkbox.onclick = function(){
			hit.selected = checkbox.checked;
		}
		appendChildren(td, [checkbox, ' ' + (seq + 1)]);
		if(hit.selected)
			checkbox.checked = true;
	}
}

function renderDisplayControl(task){
	return function(td, hit, seq){
		var id = hit.Counter;
		hit.task = task;
		td.noWrap = 'true';		
		var control = document.createElement('img');
		control.id = id + '_control';
		control.src = '../images/sp.png';
		control.className = 'selectable';
		control.style.verticalAlign = 'middle';
		control.onclick = function(){
			if(hit.loaded)
				hit.expanded = ! hit.expanded;
			else{
				hit.loaded = true;
				hit.expanded = true;
				inspectImage(document.getElementById(id + '_image'), hit);
				inspectDetailText(document.getElementById(id + '_text'), hit);
			}
			toggleRow(id + '_row');
			return false;
		}
		var checkbox = document.createElement('input');
		checkbox.type = 'checkbox';
		checkbox.style.verticalAlign = 'middle';
		checkbox.onclick = function(){
			hit.selected = checkbox.checked;
		}
		td.appendChild(checkbox);
		td.appendChild(document.createTextNode(' '));
		td.appendChild(control);
		td.appendChild(document.createTextNode(' ' + (seq + 1)));		
		if(hit.selected)
			checkbox.checked = true;
	}
}

function renderClusterDisplayControl(task){
	return function(td, hit, seq){
		hit.task = task;
		td.noWrap = 'true';
		td.style.textDecoration = 'none';
		
		var id = hit.Counter;
		var expand = document.createElement('a');
		expand.href='';
		expand.id = id + '_expand';
		expand.onclick= function(){
			loadClusterMatches(task, hit, id);
			return false;
		}
		expand.innerHTML = '<img src="../images/dn.gif" class="selectable" />';
		var collapse = document.createElement('a');
		collapse.href = '';
		collapse.id = id + '_collapse';
		collapse.style.display = 'none';
		collapse.onclick = function(){ return false; };
		collapse.innerHTML = '<img src="../images/up.gif" class="selectable"/>';
			
		var control = document.createElement('img');
		control.id = id + '_control';
		control.src = '../images/sp.png';
		control.className = 'selectable';
		control.style.verticalAlign = 'middle';
		control.onclick = function(){
			if(hit.loaded)
				hit.expanded = ! hit.expanded;
			else{
				hit.loaded = true;
				hit.expanded = true;
				inspectImage(document.getElementById(id + '_image'), hit);
				inspectDetailText(document.getElementById(id + '_text'), hit);
			}
			toggleRow(id + '_row');
			return false;
		}
		var checkbox = document.createElement('input');
		checkbox.type = 'checkbox';
		checkbox.style.verticalAlign = 'middle';
		checkbox.onclick = function(){
			hit.selected = checkbox.checked;
		}
		td.appendChild(checkbox);
		td.appendChild(document.createTextNode(' '));
		td.appendChild(control);
		td.appendChild(document.createTextNode(' '));
		td.appendChild(expand);
		td.appendChild(collapse);
		td.appendChild(document.createTextNode(' ' + (seq + 1)));		
		if(hit.selected)
			checkbox.checked = true;
	}
}

function renderSubDisplayControl(task){
	return function(td, hit, seq){
		var id = hit.Counter;
		hit.task = task;
		td.noWrap = 'true';		
		var control = document.createElement('img');
		control.id = id + '_control';
		control.src = '../images/sp.png';
		control.className = 'selectable';
		control.style.verticalAlign = 'middle';
		control.onclick = function(){
			if(hit.loaded)
				hit.expanded = ! hit.expanded;
			else{
				hit.loaded = true;
				hit.expanded = true;
				inspectImage(document.getElementById(id + '_image'), hit);
				inspectDetailText(document.getElementById(id + '_text'), hit);
			}
			toggleRow(id + '_row');
			return false;
		}
		td.appendChild(control);
		td.appendChild(document.createTextNode(' ' + (seq + 1)));
	}
}

function loadClusterMatches(taskID, hit, div_name) {
	var url = 'cluster_matches.jsp?task=' + taskID + '&position=' + hit.Position;
	var req = createRequest();
	req.open("GET", url, true);
	req.onreadystatechange = function() {
		if (req.readyState == 4) {
			var div = document.getElementById(div_name);
			// TODO: "req.responseText" will in fact be a javascript table
			// containing the cluster match hits.  This table will need to be
			// injected into the javascript environment correctly, and the
			// "div.innerHTML" below will have to be set to the table rendering
			// function that will use the retrieved data
			div.innerHTML = req.responseText;
			div.style.display = "block";
			enableInline(div_name + '_expand', false);
			div = document.getElementById(div_name + '_expand');
			div.onclick = function() {
				enableInline(div_name + '_expand', false);
				enableRow(div_name + '_matches', true);
				enableInline(div_name + '_collapse', true);
				return false;						
			}
			div = document.getElementById(div_name + '_collapse');					
			div.onclick = function() {
				enableInline(div_name + '_collapse', false);
				enableRow(div_name + '_matches', false);
				enableInline(div_name + '_expand', true);
				return false;
			}
			enableRow(div_name + '_matches', true);
			enableInline(div_name + '_collapse', true);
		}
	}
	req.setRequestHeader( "If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT" );    
	req.send(null);
}

function clusterMatchesRow(tr, hit, seq) {
	tr.id = hit.Counter + '_matches';
	tr.style.display = 'none';
	if(hit.loaded)
		enableRow(tr.id, true);
}

function inspectDisplayRow(tr, hit, seq){
	tr.id = hit.Counter + '_row';
	tr.style.display = "none";
	if(hit.loaded)
		enableRow(tr.id, true);
}

function inspectImage(td, hit, seq){
	td.colSpan='3';
	td.style.margin = '0px';
	td.style.padding = '5px';
	td.id = hit.Counter + '_image';
	if(hit.loaded){
		td.innerHTML = "";
		var img = document.createElement('img');
		var url = 'Image?task=' + hit.task +
			'&spec=' + encodeURIComponent(hit.Internal) +
			'&offset=' + hit.SpecFilePos +
			'&scan=' + hit["Scan#"] +
			'&anno=' + encodeURIComponent(hit.Annotation);
		img.src =  url + '&thumb=on';
		img.style.border = '0px';
		img.style.margin = '0px';
		img.style.padding = '0px';
		img.className = 'selectable';
		img.onclick = function(){ loadBigImage(url); return false; };    
	    td.appendChild(img);
	}
}

function inspectDetailText(td, hit, seq){
	td.colSpan='6';
	td.id = hit.Counter + '_text';
	if(hit.loaded){
		td.innerHTML = "";	
		var iframe = document.createElement('iframe');
		var url = 'Image?task=' + hit.task +
			'&spec=' + encodeURIComponent(hit.Internal) +
			'&offset=' + hit.SpecFilePos +
			'&scan=' + hit["Scan#"] +
			'&anno=' + encodeURIComponent(hit.Annotation) +
			'&label=on';
		iframe.src = url; 
		iframe.scrolling = 'no';
		iframe.style.border = '0px';
		iframe.style.margin = '0px';
		iframe.style.padding = '0px';
		td.appendChild(iframe);	
	}
}
		
function loadImage(div_name, url){
	var div = document.getElementById(div_name + '_image');
	var img = document.createElement('img');
	img.src =  url + '&thumb=on';
	img.width = '200';
	img.style.border = '0px';
	img.style.margin = '0px';
	img.style.padding = '0px';
	img.className = 'selectable';
	img.onclick = function(){ loadBigImage(url); return false; };    
	div.appendChild(img);
	
	div = document.getElementById(div_name + '_detail');
	var iframe = document.createElement('iframe');
	iframe.src = url + '&label=on';
	iframe.scrolling = 'no';
	iframe.style.border = '0px';
	iframe.style.margin = '0px';
	iframe.style.padding = '0px';
	div.appendChild(iframe);	
	
	div = document.getElementById(div_name + '_control');
	if(div){
		div.onclick = function(){toggleRow(div_name + '_row'); return false;};
		toggleRow(div_name + '_row');
	}
}

/* Fullscreen Image Popup */

function loadBigImage(url){
    var mask = document.getElementById('popup_mask');
	var popup = new PopupWindow('popup_img');
    if(mask && popup.div){
		popup.makeCentral(600, 400);    
		popup.div.src = url;
		popup.show();
		mask.style.display = 'block';

		popup.div.onclick = mask.onclick= function(){
			mask.style.display = 'none';
			popup.hide();
			return false;
		}
	}
}
