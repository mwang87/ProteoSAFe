/**
 * File stream result view block implementation
 */
// constructor
function ResultViewDummyDownload(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewDummyDownload.prototype.init = function(blockXML) {

}

// render the streamed file
ResultViewDummyDownload.prototype.render = function(div, index) {
}



// set data to the file streamer
ResultViewDummyDownload.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["dummydownload"] = ResultViewDummyDownload;


/**
 * <block id="makedatasetlink" type="dummylinkout">
          <data>
             <parsers>
                <parser type="stream" contentType="text/xml"/>
            </parsers>
          </data>
            <parameter name="URLBASE" value="https://bix-lab.ucsd.edu/display/Public/GNPS+MassIVE+Dataset+Creation"/>
        </block>
 */
// constructor
function ResultViewDummyLinkout(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewDummyLinkout.prototype.init = function(blockXML) {
	all_parameters = blockXML.getElementsByTagName("parameter")
	parameters = new Object
	for(i=0; i<all_parameters.length; i++){
		parameter_key = all_parameters[i].getAttribute("name")
		parameter_value = all_parameters[i].getAttribute("value")
		parameters[parameter_key] = parameter_value
	}

	url_parameter_prefix = "URLBASE"

	url = ""
    //Looking for url base
    for (var parameter in parameters){
        if(parameter == url_parameter_prefix){
            url += parameters[parameter]
            break;
        }
    }

	this.target_url = url
	window.location.replace(this.target_url)
}

// render the streamed file
ResultViewDummyLinkout.prototype.render = function(div, index) {
}



// set data to the file streamer
ResultViewDummyLinkout.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["dummylinkout"] = ResultViewDummyLinkout;


