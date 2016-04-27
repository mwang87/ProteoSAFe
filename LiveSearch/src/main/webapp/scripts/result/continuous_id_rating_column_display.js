
function continuousidrating_summarycolumn_renderstream(tableId, rowId, columnId, attributes) {
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
        rating_div.style.width = "100px"
        div_id_name = "rating" + rowId + "_" + columnId + "_" + index
        rating_div.id = div_id_name
        
        td.appendChild(rating_div);
        
        $.ajax({
            type: "GET",
            url: "/ProteoSAFe/ContinuousIDRatingSummaryServlet",
            data: { summary_type: "per_dataset_spectrum_scan", spectrum_id: spectrum_id, scan: scan, dataset_id: dataset_id},
            cache: false,
            success: continuousrating_summarycolumn_construct_prepopulated_star_callback_gen(spectrum_id, scan, dataset_id, div_id_name)
        });
        
        
    }
}

function continuousrating_summarycolumn_construct_prepopulated_star_callback_gen(spectrum_id, scan, dataset_id, div_name){
    return function(json){
        obj = JSON.parse(json);
        //Calculating average
        rating_total = 0
        ratings_count = 0
        
        for(var i in obj["ratings"]){
            rating_total += parseInt(obj["ratings"][i]["rating"])
            ratings_count += 1
        }
        
        if(ratings_count == 0){
            $("#" + div_name).raty({path: '/ProteoSAFe/images/plugins', numberMax: 4, readOnly: true, hints: ['Incorrect ID', 'Cannot Tell', 'Compound Class ID', 'Correct ID']})
        }
        else{
            average_rating = rating_total / ratings_count
            $("#" + div_name).raty({path: '/ProteoSAFe/images/plugins', numberMax: 4, readOnly: true, score: average_rating, half: true, hints: ['Incorrect ID', 'Cannot Tell', 'Compound Class ID', 'Correct ID']})
        }
    }
}

var continuosidratingSummaryColumnHandler = {
    render: continuousidrating_summarycolumn_renderstream,
    sort: plainSorter
};



columnHandlers["continuousidrating_summarycolumn"] = continuosidratingSummaryColumnHandler;