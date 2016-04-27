/**
 * File stream result view block implementation
 */
// constructor
function ResultViewHistogramDisplayer(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewHistogramDisplayer.prototype.init = function(blockXML) {
    this.viewname = blockXML.getAttribute("viewname")
    this.columnname = blockXML.getAttribute("columnname")
    this.absolutevalue = blockXML.getAttribute("absolutevalue")
}


// render the streamed file
ResultViewHistogramDisplayer.prototype.render = function(div, index) {
    if (div != null)
        this.div = div;
    if (this.div == null) {
        alert("No div was provided under which to render this result block.");
        return;
    }

    visualization_container = document.createElement("div");
    visualization_container.id = "visualization_container"
    visualization_container.style.width = "990px"
    visualization_container.style.marginLeft = "auto"
    visualization_container.style.marginRight = "auto"
    div.appendChild(visualization_container)

    this.getHistogramData(visualization_container);

}

ResultViewHistogramDisplayer.prototype.getHistogramData = function(div){
    result_url = "/ProteoSAFe/result_json.jsp"
    task_id = get_taskid()

    chart_container = document.createElement("div");
    chart_container.id = makeRandomString(10)
    chart_container.style.height = "600px"

    this.chart_container_id = chart_container.id

    waiting_icon = document.createElement("img")
    waiting_icon.src = "/ProteoSAFe/images/inProgress_big.gif"
    waiting_icon.className = "chartinprogress"
    //chart_container.appendChild(waiting_icon)

    div.appendChild(chart_container);


    $.ajax({
        url: result_url,
        data: { task: task_id, view: this.viewname, show: 'true'},
        cache: false,
        success: function(display_object, render_div, task_id){
            return function(json_data){
                table_data = JSON.parse(json_data);

                if("row_data" in table_data.blockData){
                    serverside_url = "/ProteoSAFe/QueryResult";
                    $.ajax({
                        url: serverside_url,
                        data: { task: task_id, file: table_data.blockData.file, offset: "0", pageSize: "-1"},
                        success: function(display_object, render_div){
                            return function(json_data){
                                table_data = json_data
                                data_list = new Array()
                                for(var i in table_data.row_data){
                                    if(display_object.absolutevalue == "true"){
                                        data_list.push(Math.abs(parseFloat(table_data.row_data[i][display_object.columnname])))
                                    }
                                    else{
                                        data_list.push(parseFloat(table_data.row_data[i][display_object.columnname]))
                                    }
                                }
                                display_object.data_list = data_list
                                display_object.initialrender(render_div);
                            }
                        }(display_object, render_div),
                    })
                }
                else{
                    data_list = new Array()
                    for(var i in table_data.blockData){
                        if(display_object.absolutevalue == "true"){
                            data_list.push(Math.abs(parseFloat(table_data.blockData[i][display_object.columnname])))
                        }
                        else{
                            data_list.push(parseFloat(table_data.blockData[i][display_object.columnname]))
                        }
                    }
                    display_object.data_list = data_list
                    display_object.initialrender(render_div);
                }
            }
        }(this, div, task_id)
    });
}

ResultViewHistogramDisplayer.prototype.initialrender = function(div){
    data_list = this.data_list
    //Determining default values
    max_value = Math.floor(Math.max.apply(Math, data_list))
    min_value = Math.floor(Math.min.apply(Math, data_list))
    histogram_range = max_value - min_value;
    bucket_size = 1
    histogram_buckets = Math.floor(histogram_range / bucket_size)
    display_min = Math.floor(min_value/bucket_size)  * bucket_size
    display_max = display_min + histogram_buckets * bucket_size

    this.data_low_input = display_min
    this.data_high_input = display_max
    this.bins_input = histogram_buckets

    this.prepareHistogramOptions(div)
    this.renderHistogram(div)
}

//Must be called after data is present
ResultViewHistogramDisplayer.prototype.prepareHistogramOptions = function(div){
    data_low_input = document.createElement("input")
    data_low_input.id = "data_low_input"
    data_low_input.size = 12
    data_low_input.defaultValue = this.data_low_input
    data_low_input.placeholder = "Data Low Input"

    data_high_input = document.createElement("input")
    data_high_input.id = "data_high_input"
    data_high_input.size = 12
    data_high_input.defaultValue = this.data_high_input
    data_high_input.placeholder = "Data High Input"

    bins_input = document.createElement("input")
    bins_input.id = "bins_input"
    bins_input.size = 12
    bins_input.defaultValue = this.bins_input
    bins_input.placeholder = "Bins Input"

    //Adding update button
    update_hist_button = document.createElement("button");
    update_hist_button.innerHTML = "Update"

    update_hist_button.onclick = function(context, div){
        return function(){
            context.renderHistogram(div);
        }
    }(this, div);


    div.appendChild(data_low_input)
    div.appendChild(data_high_input)
    div.appendChild(bins_input)
    div.appendChild(update_hist_button)

}

ResultViewHistogramDisplayer.prototype.renderHistogram = function(div){
    display_min = parseFloat($("#data_low_input").val())
    display_max = parseFloat($("#data_high_input").val())
    histogram_buckets = parseInt($("#bins_input").val())

    histogram_result_data = create_histogram(display_min, display_max, histogram_buckets, this.data_list)

    hist_buckets = histogram_result_data[0]
    hist_values = histogram_result_data[1]

    console.log(histogram_result_data)

    histogram_zipped = new Array()

    for(var i in hist_buckets){
        histogram_zipped.push([hist_buckets[i], hist_values[i]])
    }


    $('#' + this.chart_container_id).highcharts({
        chart: {
            type: 'column',
            zoomType: 'xy'
        },
        title: {
            text: 'Histogram'
        },
        xAxis: {
            //categories: mz_buckets,
            labels:{
                //rotation:-90,
                //y:40,
                style: {
                    fontSize:'14px',
                    fontWeight:'normal',
                    color:'#333'
                },

            },
            title: {
                text: this.columnname,
                style: {
                    fontSize:'16px',
                    fontWeight:'normal',
                    color:'#333'
                }
            },

        },
        yAxis: {
            min: 0,
            title: {
                text: 'Density',
                style: {
                    fontSize:'16px',
                    fontWeight:'normal',
                    color:'#333'
                }
            },
            minTickInterval: 1,
            allowDecimals: false
        },
        tooltip: {
            headerFormat: '<span style="font-size:14px"><b>{point.key} Bucket</b></span><table>',
            pointFormat: '<tr>' +
                '<td style="padding:0">{point.y} Count</td></tr>',
            footerFormat: '</table>',
            //shared: true,
            useHTML: true
        },
        plotOptions: {
            column: {
                pointPadding: 0,
                groupPadding: 0,
                borderWidth: 0
            }
        },
        series: [{
            showInLegend: false,
            name: 'Histogram',
            data: histogram_zipped
        }]
    });
}

function create_histogram(low_limit, high_limit, buckets, data, scaling_data){
    width = high_limit - low_limit
    bucket_width = width / buckets

    histogram = new Array();

    buckets_label = new Array();

    for(var i = 0; i < buckets; i++){
        histogram.push(0)
        buckets_label.push(low_limit + i * bucket_width)
    }

    for(var i in data){
        value = data[i]
        bucket_index = Math.floor((value - low_limit + bucket_width/2)/bucket_width)
        if(bucket_index >= 0 && bucket_index < buckets){
            if(scaling_data == null){
                histogram[bucket_index]++
            }
            else{
                histogram[bucket_index] += scaling_data[i]
            }
        }
    }

    return [buckets_label, histogram]
}


// set data to the file streamer
ResultViewHistogramDisplayer.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["histogramdisplayer"] = ResultViewHistogramDisplayer;
