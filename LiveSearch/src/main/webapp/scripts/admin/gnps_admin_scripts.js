function ConvertToCSV(objArray) {
    var array = typeof objArray != 'object' ? JSON.parse(objArray) : objArray;
    var str = '';

    for (var i = 0; i < array.length; i++) {
        var line = '';
        for (var index in array[i]) {
            if (line != '') line += ';'

            line += array[i][index];
        }

        str += line + '\r\n';
    }

    return str;
}


function create_users_table(div, users_data){
    child_table = document.createElement("div");
    div.appendChild(child_table);

    var task = new Object();
    task.id = "1234";
    task.workflow = "All GNPS Users";
    task.description = "All GNPS Users";
    var generic_table = new ResultViewTableGen(users_tableXML(), "users", task, 0);
    generic_table.setData(users_data.users);
    generic_table.render(child_table, 0);

    child_text_area = document.createElement("TEXTAREA");
    child_text_area.value = ConvertToCSV(users_data.users)
    div.appendChild(child_text_area)
}

function create_spectra_table(div, spectra_data){
    child_table = document.createElement("div");
    div.appendChild(child_table);

    var task = new Object();
    task.id = "1234";
    task.workflow = "All GNPS Spectra";
    task.description = "All GNPS Spectra";
    var generic_table = new ResultViewTableGen(spectra_tableXML(), "spectra", task, 0);
    generic_table.setData(spectra_data.spectra);
    generic_table.render(child_table, 0);
}


function create_unique_compound_name_count(div, spectra_data){
    compound_names = new Object;
    for( i = 0 ; i < spectra_data.spectra.length; i++){
        key = spectra_data.spectra[i].annotation
        compound_names[key] = 1
    }

    child_table = document.createElement("div");
    child_table.innerHTML = "Unique Compound names = " + String(Object.keys(compound_names).length)
    div.appendChild(child_table);
}

function create_GNPS_Annotation_revision_count(div, spectra_data){
    revision_count = 0
    for( i = 0 ; i < spectra_data.spectra.length; i++){
        revision_count += spectra_data.spectra[i].annotationcount - 1
    }

    child_table = document.createElement("div");
    child_table.innerHTML = "Total Revisions = " + String(revision_count)
    div.appendChild(child_table);
}


function users_tableXML(){
    var tableXML_str = '<block id="users" type="table" pagesize="15"> \
                            <row>  \
                                <column label="user" field="user" type="genericurlgenerator" width="10"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/user/summary.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=user" value="[user]"/>\
                                    <parameter name="LABEL" value="[user]"/>\
                                </column>\
                                <column field="org" label="org" type="text" width="8"/> \
                                <column field="jobscount" label="jobscount" type="integer" width="8"/> \
                                <column field="privatedatasets" label="privatedatasets" type="integer" width="8"/> \
                                <column field="publicdatasets" label="publicdatasets" type="integer" width="8"/> \
                                <column field="alldatasets" label="alldatasets" type="integer" width="8"/> \
                                <column field="numberspectra" label="numberspectra" type="integer" width="8"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}

function spectra_tableXML(){
    var tableXML_str = '<block id="spectra" type="table" pagesize="15"> \
                            <row>  \
                                <column label="user" field="spectrumid" type="genericurlgenerator" width="10"> \
                                    <parameter name="URLBASE" value="/ProteoSAFe/gnpslibraryspectrum.jsp"/>\
                                    <parameter name="REQUESTPARAMETER=SpectrumID" value="[spectrumid]"/>\
                                    <parameter name="LABEL" value="[spectrumid]"/>\
                                </column>\
                                <column field="annotationcount" label="annotationcount" type="integer" width="2"/> \
                                <column field="annotation" label="annotation" type="text" width="10"/> \
                            </row>\
                        </block>' ;
    return (parseXML(tableXML_str));
}


function decorateTable(table) {
    table.cellSpacing = "1";
    table.cellPadding = "4";
    table.className = "result";
    table.border = "0";
    table.width = "100%";
}

function create_discrete_histogram(data_list, fieldname){
    histogram_map = new Object();

    for(var i in data_list){
        key_value = data_list[i][fieldname]

        if(!(key_value in histogram_map)){
            histogram_map[key_value] = 0
        }

        histogram_map[key_value]++;
    }

    histogram_list = new Array()

    for(var key in histogram_map){
        list_item = [key, histogram_map[key]]
        histogram_list.push(list_item)
    }

    return histogram_list
}

function library_size_throughout_time(div){
    //Creating histogram on spectra_data

    spectrum_list = spectra_data.spectra

    for(var i in spectrum_list){
        creation_time = spectrum_list[i].creationtime
        creation_time = creation_time.substring(0, 10)
        spectrum_list[i].creationtimeabbrev = creation_time
    }

    histogram_list = create_discrete_histogram(spectrum_list, "creationtimeabbrev")

    histogram_list = histogram_list.sort();

    date_labels = histogram_list.map(function(value,index) { return value[0]; });

    chart_container = document.createElement("div");
    chart_container.id = "chartcontainer"
    chart_container.style.height = "600px"

    div.appendChild(chart_container);

    $('#chartcontainer').highcharts({
        chart: {
            type: 'column',
            zoomType: 'xy'
        },
        title: {
            text: 'Library Additions By Date'
        },
        xAxis: {
            categories: date_labels,
            labels:{
                rotation:-90,
                //y:40,
                style: {
                    fontSize:'12px',
                    fontWeight:'normal',
                    color:'#333'
                },
            },
            title: {
                text: 'Date',
                style: {
                    fontSize:'16px',
                    fontWeight:'normal',
                    color:'#333'
                }
            }
        },
        yAxis: {
            //type: 'logarithmic',
            min: 0,
            title: {
                text: '#Spectra',
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
                '<td style="padding:0">{point.y} Spectra Additions</td></tr>',
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
                        click: function () {
                            date = this.name;
                            url = '/ProteoSAFe/gnpslibrary.jsp?library=all#%7B%22create_time_input%22%3A%22' + date + '%22%7D'

                            var win = window.open(url, '_blank')
                            win.focus();

                            //location.href = url;
                        }
                    }
                }
            }
        },
        series: [{
            showInLegend: false,
            name: 'mz delta',
            data: histogram_list
        }],

    });

    //plot_cumulative_library_size(div, histogram_list)
    plot_cumulative_library_size_lineseries(div, histogram_list)
}

function plot_cumulative_library_size(div, histogram_list){
    cumulative_histogram_list = new Array();

    cumulative_count = 0
    for(var i in histogram_list){
        list_item = histogram_list[i]
        cumulative_count += list_item[1]
        new_list_item = [list_item[0], cumulative_count]
        cumulative_histogram_list.push(new_list_item);
    }


    chart_container = document.createElement("div");
    chart_container.id = "chartcontainer_cumulative"
    chart_container.style.height = "600px"

    div.appendChild(chart_container);

    $('#chartcontainer_cumulative').highcharts({
        chart: {
            type: 'column'
        },
        title: {
            text: 'Library Additions By Date'
        },
        xAxis: {
            categories: date_labels,
            labels:{
                rotation:-90,
                //y:40,
                style: {
                    fontSize:'12px',
                    fontWeight:'normal',
                    color:'#333'
                },
            },
            title: {
                text: 'Date',
                style: {
                    fontSize:'16px',
                    fontWeight:'normal',
                    color:'#333'
                }
            }
        },
        yAxis: {
            //type: 'logarithmic',
            min: 0,
            title: {
                text: '#Spectra',
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
                '<td style="padding:0">{point.y} Spectrum Additions</td></tr>',
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
            name: 'mz delta',
            data: cumulative_histogram_list
        }]
    });
}



function plot_cumulative_library_size_lineseries(div, histogram_list){
    cumulative_histogram_list = new Array();

    cumulative_count = 0
    for(var i in histogram_list){
        list_item = histogram_list[i]
        cumulative_count += list_item[1]
        date_obj = Date.parse(list_item[0])
        new_list_item = [date_obj, cumulative_count]
        cumulative_histogram_list.push(new_list_item);
    }

    chart_container = document.createElement("div");
    chart_container.id = "chartcontainer_cumulative_line"
    chart_container.style.height = "600px"

    div.appendChild(chart_container);

    $('#chartcontainer_cumulative_line').highcharts('StockChart', {
        rangeSelector : {
            selected : 1
        },

        title : {
            text : 'GNPS Library Size'
        },

        series : [{
            name : 'Library Size',
            data : cumulative_histogram_list
        }],
        tooltip: {
            pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y}</b><br/>'
        },
        rangeSelector: {
            selected: 5
        },
    });

}

//Fetching Data and plottig it
function plot_gnps_datasets_size_over_time(div){
    url = "/ProteoSAFe/datasets_json.jsp"

    $.ajax({
        url: url,
        cache: false,
        success: function(div){
            return function(response){
                datasets = JSON.parse(response).datasets
                datasets.sort(function(a,b){return Date.parse(a.created) - Date.parse(b.created)})
                console.log(datasets)



                //Iterating through it all
                GNPS_datasets = new Array()

                for(i in datasets){
                    if (datasets[i].title.toLowerCase().indexOf("gnps") != -1){
                        GNPS_datasets.push(datasets[i])
                    }
                }

                //Now Create Cumulative Field
                cumulative_gnps_list = new Array();
                cumulative_count = 0
                for(var i in GNPS_datasets){
                    list_item = GNPS_datasets[i]
                    cumulative_count += parseInt(list_item.fileSizeKB) * 1024
                    date_obj = Date.parse(list_item.created)
                    new_list_item = [date_obj, cumulative_count]
                    cumulative_gnps_list.push(new_list_item);
                }

                console.log(cumulative_gnps_list)

                chart_container = document.createElement("div");
                chart_container.id = "chartcontainer_gnps_dataset_size_through_time"
                chart_container.style.height = "600px"

                div.appendChild(chart_container);

                $('#' + chart_container.id).highcharts('StockChart', {
                    rangeSelector : {
                        selected : 1
                    },

                    title : {
                        text : 'GNPS Dataset Size'
                    },

                    series : [{
                        name : 'Datasets Total Size',
                        data : cumulative_gnps_list
                    }],
                    tooltip: {
                        pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y} B</b><br/>'
                    },
                    rangeSelector: {
                        selected: 5
                    },
                });


                //Cumulative for MassIVE
                //Now Create Cumulative Field
                cumulative_massive_list = new Array();
                cumulative_count = 0
                for(var i in datasets){
                    list_item = datasets[i]
                    cumulative_count += parseInt(list_item.fileSizeKB)  * 1024
                    date_obj = Date.parse(list_item.created)
                    if (date_obj < 1161673200000){
                        continue
                    }
                    new_list_item = [date_obj, cumulative_count]
                    cumulative_massive_list.push(new_list_item);
                }


                chart_container_massive = document.createElement("div");
                chart_container_massive.id = "chartcontainer_massive_dataset_size_through_time"
                chart_container_massive.style.height = "600px"

                div.appendChild(chart_container_massive);

                $('#' + chart_container_massive.id).highcharts('StockChart', {
                    rangeSelector : {
                        selected : 1
                    },

                    title : {
                        text : 'MassIVE Dataset Size'
                    },

                    series : [{
                        name : 'Datasets Total Size',
                        data : cumulative_massive_list
                    }],
                    tooltip: {
                        pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.y} B</b><br/>'
                    },
                    rangeSelector: {
                        selected: 5
                    },
                });
            }
        }(div)
    });
}

function display_subscription_stats(div, subscription_data){
    heading = document.createElement("h2")
    heading.innerHTML = "Subscription Stats"

    content_div = document.createElement("div")
    content_div.innerHTML = "Subscribers = " + subscription_data["subscribers"] + "<br>" + "Subscriptions: " + subscription_data["subscriptions"]


    div.appendChild(heading)
    div.appendChild(content_div)
}
