/**
 * File stream result view block implementation
 */
// constructor
function ParameterLinkClass(blockXML, id, task) {
        // properties
        this.id = id;
        this.task = task;
        
        // set up the file retrieval
        this.init(blockXML);
}

// initialize block from XML specification
ParameterLinkClass.prototype.init = function(blockXML) {
        
}

// render the streamed file
ParameterLinkClass.prototype.render = function(div, index) {
}

// set data to the file streamer
ParameterLinkClass.prototype.setData = function(data) {
        this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["parameterlink"] = ParameterLinkClass;

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

function parameterlink_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        var all_parameters_string = ""
        var json_string = ""
        if (parameters != null){
            for (var parameter in parameters){
                all_parameters_string += parameter + "=" +
                    resolveFields(parameters[parameter], record) + "&";
                json_string += "\"" + parameter + "\":\"" + resolveFields(parameters[parameter], record) + "\",";
            }
        }
        json_string = json_string.replace("\+", "%2B");
        //json_string = encodeURIComponent(json_string);
        //alert(json_string);
        //alert(all_parameters_string);
        json_string = json_string.slice(0, -1);
        
        task_id = get_taskid();
        
        var full_url = "";
        if(attributes.includetask == "true"){
            full_url = attributes.target + "" + "?" + "task=" + task_id + "&" + all_parameters_string
        }
        else{
            full_url = attributes.target + "" + "?" + all_parameters_string
        }
        
        //alert(json_string);
        
        var json_link = document.createElement("a");
        json_link.href = full_url;
        json_link.innerHTML = attributes.label;
        td.appendChild(json_link);
    }
}

var parameterlinkColumnHandler = {
    render: parameterlink_renderstream,
    sort: plainSorter
};



columnHandlers["parameterlink"] = parameterlinkColumnHandler;