/**
 * File stream result view block implementation
 */
// constructor
function ResultViewChartDisplayer(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewChartDisplayer.prototype.init = function(blockXML) {

}


//Blocking AJAX call will populate object with the latest annotation
ResultViewChartDisplayer.prototype.get_annotation_comments = function(div){
    
}


// render the streamed file
ResultViewChartDisplayer.prototype.render = function(div, index) {
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
    
    render_pairs_histogram_button = document.createElement("button");
    render_pairs_histogram_button.innerHTML = "Show Pairs Histogram"
    render_precursormz_button = document.createElement("button");
    render_precursormz_button.innerHTML = "Show Precursor MZ Histogram"
    render_rarefactioncurve_button = document.createElement("button");
    render_rarefactioncurve_button.innerHTML = "Show Rarefaction Curve"
    
    render_pairs_histogram_button.onclick = function(button_to_hide, div, context){
        return function(){
            context.renderPairsHistogram(div);
            button_to_hide.style.visibility = "hidden"
        }
    }(render_pairs_histogram_button, visualization_container, this);
    
    
    render_precursormz_button.onclick = function(button_to_hide, div, context){
        return function(){
            context.renderPrecursorMZHistogram(div);
            button_to_hide.style.visibility = "hidden"
        }
    }(render_precursormz_button, visualization_container, this);
    
    render_rarefactioncurve_button.onclick = function(button_to_hide, div, context){
        return function(){
            context.renderRarefactionCurve(div);
            button_to_hide.style.visibility = "hidden"
        }
    }(render_rarefactioncurve_button, visualization_container, this);
    
    visualization_container.appendChild(render_pairs_histogram_button);
    visualization_container.appendChild(render_precursormz_button);
    visualization_container.appendChild(render_rarefactioncurve_button);
    
}

ResultViewChartDisplayer.prototype.renderPrecursorMZHistogram = function(div){
    result_url = "/ProteoSAFe/result.jsp"
    
    task_id = get_taskid()
    
    chart_container = document.createElement("div");
    chart_container.id = makeRandomString(10)
    chart_container.style.height = "600px"
    
    waiting_icon = document.createElement("img")
    waiting_icon.src = "/ProteoSAFe/images/inProgress_big.gif"
    waiting_icon.className = "chartinprogress"
    chart_container.appendChild(waiting_icon)

    div.appendChild(chart_container);
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_all_clusters_withID', show: 'true'},
        cache: false,
        success: function(container_id){
            return function(html){
                cluster_data = get_block_data_from_page(html);
                
                data_list = new Array()
                
                cluster_count_list = new Array()
                
                for(var i in cluster_data){
                    data_list.push(Math.abs(parseFloat(cluster_data[i]["precursor mass"])))
                    cluster_count_list.push(Math.abs(parseInt(cluster_data[i]["number of spectra"])))
                }
                
                max_value = Math.floor(Math.max.apply(Math, data_list))
                min_value = Math.floor(Math.min.apply(Math, data_list))
                
                histogram_range = max_value - min_value;
                bucket_size = 2
                histogram_buckets = Math.floor(histogram_range / bucket_size)
                display_min = Math.floor(min_value/bucket_size)  * bucket_size
                display_max = display_min + histogram_buckets * bucket_size
                
                mz_histograms = create_histogram(display_min, display_max, histogram_buckets, data_list)
                
                mz_spectra_counts_scaled_histogram = create_histogram(display_min, display_max, histogram_buckets, data_list, cluster_count_list)
                
                mz_scaled_values = mz_spectra_counts_scaled_histogram[1]
                
                mz_buckets = mz_histograms[0]
                mz_values = mz_histograms[1]
                
                
                
                mz_histogram_zipped = new Array()
                mz_scaled_histogram_scaled = new Array()
                
                for(var i in mz_buckets){
                    mz_histogram_zipped.push([mz_buckets[i], mz_values[i]])
                    mz_scaled_histogram_scaled.push([mz_buckets[i], mz_scaled_values[i]])
                }
                
                
                $(function () {
                    $('#' + container_id).highcharts({
                        chart: {
                            type: 'column',
                            zoomType: 'xy'
                        },
                        title: {
                            text: 'Network MZ Histogram'
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
                                text: 'MZ',
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
                                text: 'Clusters',
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
                            headerFormat: '<span style="font-size:14px"><b>{point.key} Da</b></span><table>',
                            pointFormat: '<tr>' +
                                '<td style="padding:0">{point.y} Clusters</td></tr>',
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
                            showInLegend: true,
                            name: 'Node Histogram',
                            data: mz_histogram_zipped
                        },
                        {
                            showInLegend: true,
                            name: 'Spectra Histogram',
                            data: mz_scaled_histogram_scaled,
                            visible: false
                        }]
                    });
                });
            }
        }(chart_container.id)
    });
}

ResultViewChartDisplayer.prototype.renderPairsHistogram = function(div){
    result_url = "/ProteoSAFe/result.jsp"
    
    task_id = get_taskid()
    
    chart_container = document.createElement("div");
    chart_container.id = makeRandomString(10)
    chart_container.style.height = "600px"
    
    waiting_icon = document.createElement("img")
    waiting_icon.src = "/ProteoSAFe/images/inProgress_big.gif"
    waiting_icon.className = "chartinprogress"
    chart_container.appendChild(waiting_icon)

    div.appendChild(chart_container);
    
    //Adding displayer for table
    components_table_div = document.createElement("div");
    components_table_div.id = "components_table_div"
    div.appendChild(components_table_div)
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_network_pairs', show: 'true'},
        cache: false,
        success: function(container_id, components_table_div){
            return function(html){
                pairs_data = get_block_data_from_page(html);
                
                mz_delta_list = new Array()
                
                for(var i in pairs_data){
                    mz_delta_list.push(Math.abs(parseFloat(pairs_data[i].MzDiff)))
                }
                
                max_value = Math.floor(Math.max.apply(Math, mz_delta_list))
                min_value = Math.floor(Math.min.apply(Math, mz_delta_list))
                
                histogram_range = max_value - min_value;
                bucket_size = 1
                histogram_buckets = Math.floor(histogram_range / bucket_size)
                display_min = Math.floor(min_value/bucket_size)  * bucket_size
                display_max = display_min + histogram_buckets * bucket_size
                
                mz_histograms = create_histogram(display_min, display_max, histogram_buckets, mz_delta_list)
                
                mz_buckets = mz_histograms[0]
                mz_values = mz_histograms[1]
                
                mz_histogram_zipped = new Array()
                
                for(var i in mz_buckets){
                    mz_histogram_zipped.push([mz_buckets[i], mz_values[i]])
                }
                
                
                $(function () {
                    $('#' + container_id).highcharts({
                        chart: {
                            type: 'column',
                            zoomType: 'xy'
                        },
                        title: {
                            text: 'Network MZ Delta Histogram'
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
                                text: 'MZ Delta',
                                style: {
                                    fontSize:'16px',
                                    fontWeight:'normal',
                                    color:'#333'
                                }
                            }
                        },
                        yAxis: {
                            min: 0,
                            title: {
                                text: 'Pairs',
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
                            headerFormat: '<span style="font-size:14px"><b>{point.key} Da</b></span><table>',
                            pointFormat: '<tr>' +
                                '<td style="padding:0">{point.y} pairs</td></tr>',
                            footerFormat: '</table>',
                            //shared: true,
                            useHTML: true
                        },
                        plotOptions: {
                            column: {
                                pointPadding: 0,
                                groupPadding: 0,
                                borderWidth: 0
                            },
                            series: {
                                cursor: 'pointer',
                                point: {
                                    events: {
                                        click: function (table_div) {
                                            return function(){
                                                mzdelta = this.x;
                                                //Getting data from network pairs
                                                $.ajax({
                                                    url: '/ProteoSAFe/result.jsp',
                                                    data: { task: get_taskid(), view: 'network_pairs_specnets_allcomponents', show: 'true'},
                                                    cache: false,
                                                    success: function(mzdelta, table_div){
                                                        return function(html){
                                                            pairs_data = get_block_data_from_page(html);
                                                            found_components = new Array();
                                                            
                                                            for(i = 0; i < pairs_data.length; i++){
                                                                if( Math.abs(mzdelta - Math.abs(parseFloat(pairs_data[i].DeltaMZ)) ) < 0.5 ){
                                                                    component = parseInt(pairs_data[i].ComponentIndex)
                                                                    found_components.push(component);
                                                                }
                                                            }
                                                            
                                                            component_counts = new Object()
                                                            unique_components = $.unique(found_components.map(function(element){return element}))
                                                            
                                                            for(i = 0; i < unique_components.length; i++){
                                                                component_counts[unique_components[i]] = 0
                                                            }
                                                            
                                                            for(i = 0; i < found_components.length; i++){
                                                                component_counts[found_components[i]]++
                                                            }
                                                            
                                                            table_data_object = new Array()
                                                            
                                                            unique_keys = Object.keys(component_counts)
                                                            for(i = 0; i < unique_keys.length; i++){
                                                                row_object = new Object();
                                                                row_object.component_index = String(unique_keys[i])
                                                                row_object.delta_counts = String(component_counts[unique_keys[i]])
                                                                row_object.task = get_taskid();
                                                                table_data_object.push(row_object);
                                                            }
                                                            
                                                            var task = new Object();
                                                            task.id = "1234";
                                                            task.workflow = "Components containing " + String(mzdelta) + " mz Deltas";
                                                            task.description = "Components containing " + String(mzdelta) + " mz Deltas";
                                                            var generic_table = new ResultViewTable(get_chart_displayer_components_for_delta_tableXML(), "main", task);
                                                            generic_table.setData(table_data_object);
                                                            generic_table.render(table_div, 0);
                                                            
                                                        }
                                                        
                                                    }(mzdelta, table_div)
                                                });
                                            }
                                        }(components_table_div)
                                    }
                                }
                            }
                        },
                        series: [{
                            showInLegend: false,
                            name: 'mz delta',
                            data: mz_histogram_zipped

                        }]
                    });
                });
            }
        }(chart_container.id, components_table_div)
    });
}


function get_chart_displayer_components_for_delta_tableXML(){
    var tableXML_str = '<block id="chart_displayer_components_for_delta" type="table"> \
                            <row>  \
                                <column field="component_index" label="Component Index" type="integer" width="5"/> \
                                <column label="View Network" type="genericurlgenerator" width="16"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/result.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=view" value="network_displayer"/>\
                                    <parameter name="REQUESTPARAMETER=task" value="[task]"/>\
                                    <parameter name="REQUESTPARAMETER=componentindex" value="[component_index]"/>\
                                    <parameter name="LABEL" value="View Network"/>\
                                </column>\
                                <column field="delta_counts" label="delta_counts" type="integer" width="5"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

ResultViewChartDisplayer.prototype.renderRarefactionCurve = function(div){
    result_url = "/ProteoSAFe/result.jsp"
    
    task_id = get_taskid()
    /*
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'network_statistics', show: 'true'},
        cache: false,
        success: function(render_div){
            return function(html){
                var parsed_data =  $.parseHTML(html);
                var dom = $(html);
                var javascript_element_with_block_data  = dom.filter("#renderjsdata")[0];
                var lines_array = javascript_element_with_block_data.text.split("\n");
                
                var lines_array_truncated = lines_array[2];
                
                
                data = lines_array_truncated.split("=")[1]
                
                data = data.replace("\"", "")
                data = data.replace(" ", "")
                data = data.replace(";", "")
                data = data.replace("\"", "")
                
                streamed_url = "/ProteoSAFe/DownloadResultFile"
                
                $.ajax({
                    url: streamed_url,
                    data: { task: task_id, file: data, block: 'main'},
                    cache: false,
                    success: function(render_div){
                        return function(value){
                            rarefaction_data = new Array();
                            
                            //Parsing out the value
                            line_splits = value.split("\n")
                            parse_curve = false;
                            for(var i in line_splits){
                                if(line_splits[i].indexOf("Rarefaction Curve") != -1){
                                    parse_curve = true;
                                    continue
                                }
                                if(parse_curve){
                                    if(line_splits[i].indexOf("------") != -1){
                                        continue;
                                    }
                                    
                                    if(line_splits[i].indexOf("Filename") != -1){
                                        continue;
                                    }
                                    
                                    if(line_splits[i].length < 2){
                                        break;
                                    }
                                    
                                    rarefaction_splits = line_splits[i].split("\t")
                                    
                                    rarefaction_data.push([rarefaction_splits[0], parseInt(rarefaction_splits[3])])
                                }
                            }
                            
                            //date_labels = rarefaction_data.map(function(value,index) { return value[0]; });
                            
                            chart_container = document.createElement("div");
                            chart_container.id = makeRandomString(10)
                            chart_container.style.height = "600px"
                            
                            render_div.appendChild(chart_container);
                            
                            //rarefaction_data = rarefaction_data.slice(500);
                            
                            $('#' + chart_container.id).highcharts({
                                title: {
                                    text: 'Dataset Rarefaction Curve'
                                },
                                chart: {
                                    zoomType: 'xy'
                                },
                                xAxis: {
                                    title: {
                                        text: 'Dataset Counts',
                                        style: {
                                            fontSize:'16px',
                                            fontWeight:'normal',
                                            color:'#333'
                                        }
                                    },
                                    labels:{
                                        enabled: false
                                    },
                                    minorTickLength: 0,
                                    minorGridLineWidth: 0,
                                    lineColor: 'transparent',
                                    tickInterval: 10000,
                                    tickLength: 0
                                },
                                yAxis: {
                                    min: 0,
                                    title: {
                                        text: 'Clusters Affected',
                                        style: {
                                            fontSize:'16px',
                                            fontWeight:'normal',
                                            color:'#333'
                                        }
                                    }
                                },
                                tooltip: {
                                    headerFormat: '<span style="font-size:14px"><b>{point.key}</b></span><table>',
                                    pointFormat: '<tr>' +
                                        '<td style="padding:0">{point.y} Clusters</td></tr>',
                                    footerFormat: '</table>',
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
                                    turboThreshold: 0,
                                    showInLegend: false,
                                    name: 'Rarefaction',
                                    data: rarefaction_data

                                }]
                            });
                            
                        }
                    }(render_div)
                });
                
            }
        }(div)
    });*/
    
    result_url = "/ProteoSAFe/result.jsp"
    
    task_id = get_taskid()
    
    chart_container = document.createElement("div");
    chart_container.id = makeRandomString(10)
    chart_container.style.height = "600px"
    
    waiting_icon = document.createElement("img")
    waiting_icon.src = "/ProteoSAFe/images/inProgress_big.gif"
    waiting_icon.className = "chartinprogress"
    chart_container.appendChild(waiting_icon)
    
    div.appendChild(chart_container);
    
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_all_clusters_withID', show: 'true'},
        cache: false,
        success: function(container_id){
            return function(html){
                cluster_data = get_block_data_from_page(html);
                
                
                //Spawing worker
                var myWorker = new Worker("/ProteoSAFe/scripts/result/webworker/rarefaction_worker.js");
                myWorker.onmessage = function(chart_id){
                    return function (oEvent) {
                        rarefaction_curve = JSON.parse(oEvent.data);
                        
                        rarefaction_means = rarefaction_curve.map(function(value,index) { return value[0]; });
                        rarefaction_errors = rarefaction_curve.map(function(value,index) { return [value[0] - value[1], value[0] + value[1]]; });
                        
                        
                        $('#' + chart_id).highcharts({
                            title: {
                                text: 'Dataset Rarefaction Curve Calculated'
                            },
                            chart: {
                                zoomType: 'xy'
                            },
                            xAxis: {
                                title: {
                                    text: 'Dataset Counts',
                                    style: {
                                        fontSize:'16px',
                                        fontWeight:'normal',
                                        color:'#333'
                                    }
                                },
                                labels:{
                                    enabled: false
                                },
                                minorTickLength: 0,
                                minorGridLineWidth: 0,
                                lineColor: 'transparent',
                                tickInterval: 10000,
                                tickLength: 0
                            },
                            yAxis: {
                                min: 0,
                                title: {
                                    text: 'Clusters Affected',
                                    style: {
                                        fontSize:'16px',
                                        fontWeight:'normal',
                                        color:'#333'
                                    }
                                }
                            },
                            tooltip: {
                                headerFormat: '<span style="font-size:14px"><b>{point.key}</b></span><table>',
                                pointFormat: '<tr>' +
                                    '<td style="padding:0">{point.y} Clusters</td></tr>',
                                footerFormat: '</table>',
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
                                turboThreshold: 0,
                                type: 'spline', 
                                showInLegend: false,
                                name: 'Rarefaction',
                                data: rarefaction_means
                            },
        //                     {
        //                         turboThreshold: 0,
        //                         type: 'arearange',
        //                         showInLegend: false,
        //                         name: 'Rarefaction Error',
        //                         data: rarefaction_errors
        //                     }
                            ]
                        });
                        
                        
                    }
                }(container_id);
                myWorker.postMessage(JSON.stringify(cluster_data));
            }
        }(chart_container.id)
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
    
    //console.log(buckets_label)
    //console.log(histogram)
    
    return [buckets_label, histogram]
}


// set data to the file streamer
ResultViewChartDisplayer.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["chartdisplayer"] = ResultViewChartDisplayer;

