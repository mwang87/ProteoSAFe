/**
 * File stream result view block implementation
 */
// constructor
function MingPluginTestClass(blockXML, id, task) {
        // properties
        this.id = id;
        this.task = task;

        // set up the file retrieval
        this.init(blockXML);
}

// initialize block from XML specification
MingPluginTestClass.prototype.init = function(blockXML) {

}

// render the streamed file
MingPluginTestClass.prototype.render = function(div, index) {
}

// set data to the file streamer
MingPluginTestClass.prototype.setData = function(data) {
        this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["mingplugin"] = MingPluginTestClass;



function renderStream_ming(tableId, rowId, columnId, attributes) {
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
        demangled_parameters_hash = new Object();
        if (parameters != null){
            for (var parameter in parameters){
                all_parameters_string += parameter + "=" +
                    resolveFields(parameters[parameter], record) + "\n";
                field_value = resolveFields(parameters[parameter], record)
                field_value = field_value.replace(/\"/g,'\\"')
                json_string += "\"" + parameter + "\":\"" + field_value + "\",";
                demangled_parameters_hash[parameter] = field_value
            }
        }

        json_string = json_string.replace("\+", "%2B");
        json_string = JSON.stringify(demangled_parameters_hash);
        json_string = encodeURIComponent(json_string);
        //alert(json_string);
        //alert(all_parameters_string);
        //json_string = json_string.slice(0, -1);
        var full_url = attributes.target + "ProteoSAFe" + "?params=" + json_string

        //alert(json_string);

        var json_link = document.createElement("a");
        json_link.href = full_url;
        json_link.innerHTML = attributes.label;
        td.appendChild(json_link);
    }
}

var streamColumnHandler_ming = {
    //alert("Ming");
    render: renderStream_ming
};



columnHandlers["mingplugin"] = streamColumnHandler_ming;
