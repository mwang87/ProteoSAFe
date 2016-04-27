/**
 * File stream result view block implementation
 */
// constructor
function ResultViewScatterplotDisplayer(blockXML, id, task) {
    // properties
    this.id = id;
    this.task = task;
    // set up the file retrieval
    this.init(blockXML);
}

// initialize block from XML specification
ResultViewScatterplotDisplayer.prototype.init = function(blockXML) {
    this.viewname = blockXML.getAttribute("viewname")
    this.xname = blockXML.getAttribute("xname")
    this.yname = blockXML.getAttribute("yname")
}


// render the streamed file
ResultViewScatterplotDisplayer.prototype.render = function(div, index) {
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

    this.getScatterplotData(visualization_container);

}

ResultViewScatterplotDisplayer.prototype.getScatterplotData = function(div){
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
        success: function(display_object, render_div){
            return function(json_data){
                table_data = JSON.parse(json_data);

                data_list = new Array()
                for(var i in table_data.blockData){
                    data_list.push([parseFloat(table_data.blockData[i][display_object.xname]), parseFloat(table_data.blockData[i][display_object.yname])])
                }

                display_object.data_list = data_list

                display_object.renderPlot(render_div)
            }
        }(this, div)
    });
}

ResultViewScatterplotDisplayer.prototype.renderPlot = function(div){
    $('#' + this.chart_container_id).highcharts({
        chart: {
            type: 'scatter',
            zoomType: 'xy'
        },
        title: {
            text: 'Scatter Plot'
        },
        subtitle: {
            text: ''
        },
        xAxis: {
            title: {
                enabled: true,
                text: this.xname
            },
            startOnTick: true,
            endOnTick: true,
            showLastLabel: true
        },
        yAxis: {
            title: {
                text: this.yname
            }
        },
        legend: {
            layout: 'vertical',
            align: 'left',
            verticalAlign: 'top',
            x: 100,
            y: 70,
            floating: true,
            backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF',
            borderWidth: 1
        },
        plotOptions: {
            scatter: {
                marker: {
                    radius: 5,
                    states: {
                        hover: {
                            enabled: true,
                            lineColor: 'rgb(100,100,100)'
                        }
                    }
                },
                states: {
                    hover: {
                        marker: {
                            enabled: false
                        }
                    }
                },
                tooltip: {
                    headerFormat: '<b>{series.name}</b><br>',
                    pointFormat: '{point.x} , {point.y} '
                }
            }
        },
        series: [{
            name: 'Data',
            color: 'rgba(223, 83, 83, .5)',
            data: this.data_list
        }]
    });
}


// set data to the file streamer
ResultViewScatterplotDisplayer.prototype.setData = function(data) {
    this.data = data;
}

// assign this view implementation to block type "stream"
resultViewBlocks["scatterplotdisplayer"] = ResultViewScatterplotDisplayer;
