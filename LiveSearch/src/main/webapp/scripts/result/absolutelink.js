/**
 * File stream result view block implementation
 */
// constructor
function AbsoluteLinkClass(blockXML, id, task) {
        // properties
        this.id = id;
        this.task = task;
        
        // set up the file retrieval
        this.init(blockXML);
}

// initialize block from XML specification
AbsoluteLinkClass.prototype.init = function(blockXML) {
        
}

// render the streamed file
AbsoluteLinkClass.prototype.render = function(div, index) {
}

// set data to the file streamer
AbsoluteLinkClass.prototype.setData = function(data) {
        this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["absolutelink"] = AbsoluteLinkClass;


function absolutelink_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        var json_link = document.createElement("a");
        json_link.href = invokeParameters["link"];
        json_link.innerHTML = attributes.label;
        td.appendChild(json_link);
    }
}

var absolutelinkColumnHandler = {
    render: absolutelink_renderstream
};



columnHandlers["absolutelink"] = absolutelinkColumnHandler;