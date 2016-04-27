function ProteoSAFeInputModule_specplot(div, id, properties) {
	// set argument properties
	this.div = div;
	this.id = id;
	if (properties != null){
		this.fileselector_id = properties.fileselector_id;
		this.scan_id = properties.scan_id;
                this.path_prefix = properties.path_prefix;
                this.peptide_id = properties.peptide_id;
		//alert(this.fileselector_id);
	}
	
    //alert("ming alert");
}


ProteoSAFeInputModule_specplot.prototype.init = function() {
    //Setting the appropriate callback functions
    var click_button = document.getElementById(this.id + "_plotbutton");
    var parent = this;
    click_button.onclick = function (){
      //this.fileselector_id
      //alert(parent.fileselector_id);
      var file_selector_thing = document.getElementById(parent.fileselector_id);
      if(file_selector_thing == null){
	alert("NOT FOUND");
      }
      else{
	//alert("FOUND");
	//alert(parent.fileselector_id);
	var field_value = CCMSFormUtils.getFieldValues(CCMSFormUtils.getEnclosingForm(document.getElementById("spec_on_server")), "spec_on_server");
	
	if(field_value.length == 1){
	  var file_names = field_value[0];
	  //alert(file_names);
	  var split_files = file_names.split(";");
	  var file_count = 0;
	  var file_to_plot = "";
	  for(var idx in split_files){
	    if(split_files[idx].length > 2){
	      //alert(split_files[idx]);
	      file_count++;
	      if(split_files[idx][0] != "f"){
		alert("Not a file");
		return -1;
	      }
	      file_to_plot = split_files[idx];
	    }
	  }
	  if(file_count == 0){
	    alert("No Files");
	    return 0;
	  }
	  if(file_count > 1){
	    alert("too many files");
	    return 0;
	  }
	  //alert(file_to_plot);	
	  //Lets plot here. 
	  
	  //Getting Scan Number
	  var scan_to_plot = parseInt(document.getElementsByName(parent.scan_id)[0].value);
	  if(isNaN(scan_to_plot)){
	    alert("Invalid Scan");
	    return 0;
	  }
	  var peptide_to_plot = "";
          if(parent.peptide_id != null){
            peptide_to_plot = document.getElementsByName(parent.peptide_id)[0].value;
            if(peptide_to_plot.length < 2){
              peptide_to_plot = "*..*";
            }
            //alert(peptide_to_plot);
          }
          else{
            //alert("No peptide in input xml");
            peptide_to_plot = "*..*";
          }
	  
	  file_to_plot_path = file_to_plot.substr(2);
      
      if(file_to_plot_path.indexOf(' ') >= 0){
          alert("File Path Has Spaces. Spaces are disallowed");
          return 0;
      }
	  
	  //document.getElementById(parent.id + "_test_place").innerHTML = file_to_plot_path + " " + scan_to_plot;
	  
	  //document.getElementById(parent.id + "_test_place").innerHTML = file_to_plot;
	  //document.getElementById(parent.id + "_picture_place").innerHTML = '<img src="http://mingxunwang.com/assets/img/ming.png" />';
      //var task = "9922d31df8db4518830f8f72613ff53b";
      var task = "a7a1afd8e26247a78097537cb163350f";
      var type = "invoke";
      //var source  = "annotatedSpectrumImageThumbnail";
      var source  = "annotatedSpectrumImage";
      var contentType = "image/png";
      var block = 0;
      var invokeParameters = {};
      invokeParameters["file"] = "FILE->" + parent.path_prefix + "/" + file_to_plot_path;
      invokeParameters["scan"] = scan_to_plot;
      invokeParameters["peptide"] = peptide_to_plot;
      invokeParameters["force"] = "true";
      
      displayFileContents(document.getElementById(parent.id + "_picture_place"), task, type, source, invokeParameters, contentType, block);
      
	}
	else{
	  alert("ERROR");
	}
      }
    };
  
    // indicate that this form element has been properly loaded and initialized
    ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
    
    
}

//register this module constructor to indicate that it has been loaded
CCMSForms.modules["specplot"] = ProteoSAFeInputModule_specplot;



function displayFileContents(
    div, task, type, source, parameters, contentType, block
) {
    if (div == null || task == null || type == null || source == null)
        return;
    // set up URL to download result file
    var url = "DownloadResultFile?task=" + task + "&" + type + "=" + source;
    if (block != null)
        url += "&block=" + block;
    if (parameters != null)
        for (var parameter in parameters)
            url += "&" + parameter + "=" +
                encodeURIComponent(parameters[parameter]);
    
    // show "in-progress" download spinner
    removeChildren(div);
    var child = document.createElement("img");
    child.src = "images/inProgress.gif";
    div.appendChild(child);
    
    // create and submit AJAX request for the file data
    var request = createRequest();
    request.open("GET", url, true);
    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            // remove "in-progress" download spinner
            removeChildren(div);
            if (request.status == 200) {
                // display text file content appropriately based on type
                if (contentType != null && contentType.indexOf("image") >= 0) {
                    child = document.createElement("img");
                    child.style.border = "0px";
                    child.style.margin = "0px";
                    child.style.padding = "0px";
                    // servlet should write image content
                    // as a properly formatted data URL
                    child.src = "data:" + contentType + ";base64," +
                        request.responseText;
                } else if (contentType == "text/plain") {
                    child = document.createElement("pre");
                    child.innerHTML = request.responseText;
                } else if (contentType == "text/html") {
                    child = document.createElement("div");
                    child.innerHTML = request.responseText;
                    injectScripts(request.responseText);
                } else {
                    child = document.createElement("div");
                    child.innerHTML = request.responseText;
                }
                div.appendChild(child);
            } else if (request.status == 410) {
                alert("The input file associated with this request could not " +
                    "be found. This file was probably deleted by its owner " +
                    "after this workflow task completed.");
            } else {
                alert("Could not download result artifact of type \"" + type +
                    "\" and value \"" + source + "\", belonging to task " +
                    task + ".");
            }
        }
    }
    request.setRequestHeader("If-Modified-Since",
        "Sat, 1 Jan 2000 00:00:00 GMT");    
    request.send(null);
}
