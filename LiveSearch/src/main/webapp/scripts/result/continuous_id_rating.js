/**
 * File stream result view block implementation
 */
// constructor
function ContinuousIDRating(blockXML, id, task) {
        // properties
        this.id = id;
        this.task = task;
        
        // set up the file retrieval
        this.init(blockXML);
}

// initialize block from XML specification
ContinuousIDRating.prototype.init = function(blockXML) {
        
}

// render the streamed file
ContinuousIDRating.prototype.render = function(div, index) {
}

// set data to the file streamer
ContinuousIDRating.prototype.setData = function(data) {
        this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["continuousidrating"] = ContinuousIDRating;


function continuousidrating_renderstream(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        
        //Spectrum ID
        spectrum_id = invokeParameters["SpectrumID"]
        scan = invokeParameters["Scan"]
        SpectrumFile = invokeParameters["SpectrumFile"]
        dataset_id = SpectrumFile.split("_")[0]
        
        rating_div = document.createElement("div");
        rating_div.style.width = "120px"
        div_id_name = "rating" + rowId + "_" + columnId + "_" + index
        rating_div.id = div_id_name

        task_id = get_taskid()
        
        td.appendChild(rating_div);
        
        $.ajax({
            type: "GET",
            url: "/ProteoSAFe/ContinuousIDRatingServlet",
            data: { task: task_id, spectrumID: spectrum_id, scan: scan, massiveID: dataset_id},
            cache: false,
            success: continuousrating_construct_prepopulated_star_callback_gen(spectrum_id, scan, dataset_id, div_id_name)
        });
        
        
    }
}

function continuousrating_construct_prepopulated_star_callback_gen(spectrum_id, scan, dataset_id, div_name){
    return function(json){
        obj = JSON.parse(json);
        if(obj['rating'] != null && obj['rating'] != '0' && parseInt(obj['rating']) < 5 ){
            
            $("#" + div_name).raty({path: '/ProteoSAFe/images/plugins', 
                                   score: parseInt(obj['rating']),  
                                   numberMax: 4, 
                                   click: continuousrating_callback_gen(spectrum_id, scan, dataset_id) , 
                                   hints: ['Incorrect ID', 'Cannot Tell', 'This is a match that may be correct based but not 100% certain. It could be as isomer or have a similar substructure and the details can be specified in the comments', 'Correct ID'], 
                                   cancel: true})
                                   
            $("#" + div_name).append(" ")
            var img = $('<img>');
            img.attr('src', '/ProteoSAFe/images/plugins/comment.png');
            img.attr('title', 'Add Comment - Rating Required');
            img.appendTo("#" + div_name)
            if(obj['rating_comment'] == "null" || obj['rating_comment'] == "undefined" || obj['rating_comment'] == null){
                img.ratingcomment = ""
            }
            else{
                img.ratingcomment = obj['rating_comment']
            }
            img.click(continuousrating_comment_callback_gen(spectrum_id, scan, dataset_id, img))
        }
        else{
            $("#" + div_name).raty({path: '/ProteoSAFe/images/plugins', 
                                   numberMax: 4, 
                                   click: continuousrating_callback_gen(spectrum_id, scan, dataset_id) , 
                                   hints: ['Incorrect ID', 'Cannot Tell', 'This is a match that may be correct based but not 100% certain. It could be as isomer or have a similar substructure and the details can be specified in the comments', 'Correct ID'] , 
                                   cancel: true})
            $("#" + div_name).append(" ")
            var img = $('<img>');
            img.attr('src', 'images/plugins/comment.png');
            img.attr('title', 'Add Comment - Rating Required');
            img.appendTo("#" + div_name)
            if(obj['rating_comment'] != "null"){
                img.ratingcomment = obj['rating_comment']
            }
            else{
                img.ratingcomment = ""
            }
            img.click(continuousrating_comment_callback_gen(spectrum_id, scan, dataset_id, img))
        }
    }
}

function continuousrating_comment_callback_gen(spectrum_id, scan, dataset_id, div){
    return function(){
        var new_comment = prompt("Rating Comment", div.ratingcomment);
        
        if(new_comment == null){
            return;
        }

        //alert(spectrum_id + " " + scan + " " + dataset_id + div.ratingcomment + " " + new_comment);
        div.ratingcomment = new_comment
        
        $.ajax({
            type: "POST",
            url: "/ProteoSAFe/ContinuousIDRatingServlet",
            data: { spectrumID: spectrum_id, scan: scan, massiveID: dataset_id, function: "add_comment", rating_comment: new_comment},
            cache: false,
            success: function(return_val){},
            error: function(){alert("Error In Comment Saving")}
        });
    }
}

function continuousrating_callback_gen(spectrum_id, scan, dataset_id){
    return function(score, evt){
        task_id = get_taskid()
        if(score == null){
            //Cancelling Rating
            $.ajax({
                type: "POST",
                url: "/ProteoSAFe/ContinuousIDRatingServlet",
                data: { task: task_id, spectrumID: spectrum_id, scan: scan, rating: 0, massiveID: dataset_id, function: "delete_rating"},
                cache: false,
                success: function(){}
            });
        }
        else{
            //alert(spectrum_id + " " + scan + " " + dataset_id + " " + score + " " + task_id)
            $.ajax({
                type: "POST",
                url: "/ProteoSAFe/ContinuousIDRatingServlet",
                data: { task: task_id, spectrumID: spectrum_id, scan: scan, rating: score, massiveID: dataset_id, function: "add_rating"},
                cache: false,
                success: function(){}
            });
        }        
    }
}

var continuosidratingColumnHandler = {
    render: continuousidrating_renderstream,
    sort: plainSorter
};



columnHandlers["continuousidrating"] = continuosidratingColumnHandler;