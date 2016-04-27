var counter = 0;

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
