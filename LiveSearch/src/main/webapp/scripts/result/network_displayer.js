/**
 * File stream result view block implementation
 */
// constructor
function ResultViewNetworkDisplayer(blockXML, id, task) {
    // properties
    this.id = id;
    this.div = null;
    
    this.init(blockXML);
    
    this.displayableFeatures = ["cluster index", "parent mass", "LibraryID", "EvenOdd", "Peptide"]
    this.displayableFeatureInInfoPanel = ["cluster index", "parent mass", "LibraryID", "number of spectra", "DefaultGroups", "precursor charge"]
    
    this.edgedisplayableFeatures = ["Cosine", "DeltaMZ", "None"]
    
    //These are div ids for all the overlay panels
    this.displayable_overlay_panel_names = ["library_match_display_div", "pairs_histogram_div", "ms2_peak_histogram_div"]
    //This the class name for the overlay panels
    this.displayable_overlay_panel_class = "network_overlay_panel"

    //This is the class for all Network Modification Things that can be hidden
    this.displayable_network_control_class = "network_control_panels"
    this.control_panels_hidden = false;
    
    this.clusterinfo_loaded = false
    this.pairsinfo_loaded = false
    this.cytoscape_ready = false
    this.librarysearch_loaded = false
    this.cluster_peaks_loaded = false;
    
    this.node_list = null
    this.clusterinfo_data = null
    this.clusterinfo_map = null
    
    this.defaultFilename = "spectra/specs_ms.mgf"
    
    this.nodeLabelDisplayLength = 25;
    
    //State of the plotting
    this.top_plot_spectrum = new Object()
    this.bottom_plot_spectrum = new Object()
    
    //Determining whether we want to render big
    this.biglayout = new Object();
    this.biglayout.cssfile = "/ProteoSAFe/styles/networkdisplayer_big.css"
    this.biglayout.window_height = 900;
    this.biglayout.window_width = 1100;
    
    this.smalllayout = new Object();
    this.smalllayout.cssfile = "/ProteoSAFe/styles/networkdisplayer.css"
    this.smalllayout.window_height = 600;
    this.smalllayout.window_width = 800;
    
    this.layout = this.smalllayout
    
    this.taskid = task.id;
    this.workflow = task.workflow;

    this.componentindex = -1;

    //Reading the XML to determine whether or not we should be changing loading behavior
    this.loadpeptides = false
    this.loadpeptide_view = "view_all_cluster_peptide_identifications_component_sliced"
    if(blockXML.getAttribute("loadpeptides") == "true"){
        this.loadpeptides = true
    }
    
}

// initialize block from XML specification
ResultViewNetworkDisplayer.prototype.init = function(blockXML) {
}


ResultViewNetworkDisplayer.prototype.checkready = function(){
    if(this.clusterinfo_loaded == true && this.pairsinfo_loaded == true && this.cytoscape_ready == true && this.librarysearch_loaded == true){        
        //$("#network_ready_light").css("background", "green");
        this.ready_button_green();
        
        //Do other things when ready
        
        //Associating Library Search Data with Clusters
        
        //Saving smiles information
        for(var i in this.library_search_data){
        	if(this.library_search_data[i]["#Scan#"] in this.clusterinfo_map){
            	this.clusterinfo_map[this.library_search_data[i]["#Scan#"]].Smiles = this.library_search_data[i].Smiles
            }
        }
        
        //Saving library ID information
        for(var i in this.library_search_data){
        	if(this.library_search_data[i]["#Scan#"] in this.clusterinfo_map){
            	this.clusterinfo_map[this.library_search_data[i]["#Scan#"]].library_SpectrumID = this.library_search_data[i].SpectrumID
        	}
        }
        
        //Showing the pies
        //$("#pierenderbutton").click();
        
        
        //If this is in two pass workflow, lets go ahead and import identifications
        if(this.loadpeptides){
            this.ready_button_red();
            $.ajax({
                url: "/ProteoSAFe/result_json.jsp",
                data: { task: this.taskid, 
                    view: this.loadpeptide_view,  
                    show: 'true',
                    componentindex: this.componentindex},
                cache: false,
                success: get_multipass_result_callback_network_displayer_gen(this)
            });
        }
        
        //Rendering Cytoscape
        this.renderCytoscapeFinalize()
        
        return true
    }
    return false
}




ResultViewNetworkDisplayer.prototype.ready_button_red = function(){
    //$("#network_ready_light").css("background", "red");
    $("#network_ready_light").empty()
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-spinner fa-spin"
    icon_object.title = "Interface Waiting"
    $("#network_ready_light").append(icon_object)
}

ResultViewNetworkDisplayer.prototype.ready_button_green = function(){
    //$("#network_ready_light").css("background", "green");
    $("#network_ready_light").empty()
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-check-circle"
    icon_object.title = "Interface Ready"
    $("#network_ready_light").append(icon_object)
}

// render the streamed file
ResultViewNetworkDisplayer.prototype.render = function(div, index) {
    //Adding the appropriate CSS file
    var link = document.createElement('link')
    link.setAttribute('rel', 'stylesheet')
    link.setAttribute('type', 'text/css')
    link.setAttribute('href', this.layout.cssfile + "?cache=" + makeRandomString(10))
    document.getElementsByTagName('head')[0].appendChild(link)
    
    //Adding font awesome CSS
    var link = document.createElement('link')
    link.setAttribute('rel', 'stylesheet')
    link.setAttribute('type', 'text/css')
    link.setAttribute('href', "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css")
    document.getElementsByTagName('head')[0].appendChild(link)
    

    visualization_container = document.createElement("div");
    visualization_container.id = "visualization_container"
    div.appendChild(visualization_container)

    //Container for Network
    container = document.createElement("div");
    container.id = "networkcontainer"

    

    visualization_container.appendChild(container)
    
    //Rendering Powerview Items
    this.renderPowerView(visualization_container)
    

    var task_id = get_taskid();
    var parameters_map = getURLParameters();
    var component_index = parameters_map["componentindex"];
    this.componentindex = component_index
    //Getting Pairs Info Page
    
    //Constructing URL
    var result_url = '/ProteoSAFe/result.jsp'
    
    
    if(component_index == null){
        //Grabbing Pairs
        $.ajax({
            url: result_url,
            data: { task: task_id, view: 'network_pairs_specnets_allcomponents', show: 'true'},
            cache: false,
            success: network_displayer_network_pairs_cytoscape_callback(this)
        });
    }
    else{
        //Grabbing Pairs
        $.ajax({
            url: result_url,
            data: { task: task_id, view: 'network_pairs_specnets_componentsliced', show: 'true', componentindex: component_index},
            cache: false,
            success: network_displayer_network_pairs_cytoscape_callback(this)
        });
    }
    
    //Grabbing Cluster Info
    //Does this have to be asynchronous?
    $.ajax({
        url: result_url,
        //data: { task: task_id, view: 'view_all_clusters_withID_beta', show: 'true'},
        data: { task: task_id, view: 'cluster_info_sliced', show: 'true', componentindex: component_index},
        cache: false,
        success: view_all_clusters_data_beta_callback_gen(this)
    });
    
    //Grabbing Library Data
    $.ajax({
        url: result_url,
        data: { task: task_id, view: 'view_all_annotations_DB', show: 'true'},
        cache: false,
        success: function(result_object){
            return function(html){
                library_search_data = get_block_data_from_page(html);
                result_object.library_search_data = library_search_data
                
                result_object.librarysearch_loaded = true;
                
                console.log("Library Search Data Ready");
                result_object.checkready()
            }
        }(this),
        error: function(result_object){
        	return function(){
        		result_object.library_search_data = new Array()
                
                result_object.librarysearch_loaded = true;
                
                console.log("Library Search Data Error");
                result_object.checkready()
        	}
        }(this)
    });
    
    //Ready Display
    ready_display_light = document.createElement("div")
    ready_display_light.id = "network_ready_light"
    visualization_container.appendChild(ready_display_light)
    
    this.ready_button_red();
	
	this.renderCytoscapeInitial(visualization_container);
    
}

ResultViewNetworkDisplayer.prototype.renderCytoscapeFinalize = function(){
    console.log("Cytoscape Rendering Data");
    //Creating the nodes, assuming all the data is available
    for(var key in this.node_list){
        cy.add({
            group: "nodes",
            data: { id: this.node_list[key], name: this.node_list[key] },
            position: { x:  0, y:  0 }
        });
    }
    
    edge_index = 0
    for(var i in this.pairs_data){
        pair_obj = this.pairs_data[i]
        edge_index += 1
        //Determining Whether we want to change the edge direciton
        if(parseFloat(this.clusterinfo_map[pair_obj["CLUSTERID2"]]["parent mass"]) > parseFloat(this.clusterinfo_map[pair_obj["CLUSTERID1"]]["parent mass"])){
            cy.add({
                group: "edges",
                //data: { id: "e_" + edge_index.toString(), source: pair_obj["CLUSTERID1"], target: pair_obj["CLUSTERID2"], matchscore: pair_obj["Cosine"], deltaMZ: Math.abs(parseFloat(pair_obj["DeltaMZ"])).toString()}
                data: { id: "e_" + edge_index.toString(), source: pair_obj["CLUSTERID1"], target: pair_obj["CLUSTERID2"], edgecolor: "#bbb", edgedisplay: "", score: parseFloat(pair_obj["Cosine"])}
            });
        }
        else{
            cy.add({
                group: "edges",
                //data: { id: "e_" + edge_index.toString(), source: pair_obj["CLUSTERID2"], target: pair_obj["CLUSTERID1"], matchscore: pair_obj["Cosine"], deltaMZ: Math.abs(parseFloat(pair_obj["DeltaMZ"])).toString()}
                data: { id: "e_" + edge_index.toString(), source: pair_obj["CLUSTERID2"], target: pair_obj["CLUSTERID1"], edgecolor: "#bbb", edgedisplay: "", score: parseFloat(pair_obj["Cosine"])}
            });
        }
    }
    
    cy.panningEnabled(true)
    cy.userPanningEnabled(true)
    cy.zoomingEnabled(true)
    cy.userZoomingEnabled(true)
    cy.boxSelectionEnabled(false)
    cy.elements().unselectify()
    
    result_object = this;
    
    cy.on('tap', 'node', function(e){
        var node = e.cyTarget; 
        var neighborhood = node.neighborhood().add(node);
        
        cy.elements().addClass('faded');
        neighborhood.removeClass('faded');
        
        result_object.renderTopInfoPanelPopulate(node.id())
    });
    
    cy.on('cxttap', 'node', function(e){
        var node = e.cyTarget; 
        result_object.renderBottomInfoPanelPopulate(node.id())
    });
    
    cy.on('tap', 'edge', function(e){
        var edge = e.cyTarget;
        result_object.renderTopInfoPanelPopulate(edge.source().id())
        result_object.renderBottomInfoPanelPopulate(edge.target().id())
    });
    
    cy.on('tap', function(e){
        if( e.cyTarget === cy ){
            cy.elements().removeClass('faded');
        }
    });
    
    //Mouse over a node
    cy.on('mouseover', function(e){
        var element = e.cyTarget;
        if( e.cyTarget === cy ){
            //Background
        }
        else{
            if(element.group() == "nodes"){
                scanNumber = element.id()
                result_object.renderStructurePanelPopulate($("#hoverstructurediv"), scanNumber)
                result_object.renderInfoPanelPopulate($("#hovercontentdiv"), scanNumber, false)
                
                $("#popup_hover_wrapper").show();
                mouseLeft = e.cyRenderedPosition.x
                mouseDown = e.cyRenderedPosition.y
                
                $("#popup_hover_wrapper").css('top', mouseDown).css('left', mouseLeft + 15);
            }
        }
    });
    
    cy.on('mouseout', function(e){
        var element = e.cyTarget;
        if( e.cyTarget === cy ){
            //Background
        }
        else{
            if(element.group() == "nodes"){
                scanNumber = element.id()
                $("#hoverstructurediv").empty()
                $("#hovercontentdiv").empty()
                
                $("#popup_hover_wrapper").hide();
            }
        }
    });

    //Setting Node Color
    for(var i in this.node_list){
        if(this.clusterinfo_map[this.node_list[i]].LibraryID.length > 4){
            cy.$('#' + this.node_list[i]).addClass('haslibraryidentification');
        }
    }
    
    
    
    sleep_start_layout(200, this.layout.window_width, this.layout.window_height)
}

ResultViewNetworkDisplayer.prototype.renderCytoscapeInitial = function(div){
    console.log("Cytoscape Initial Render");
    $('#networkcontainer').cytoscape({
        style: cytoscape.stylesheet()
            .selector('node')
            .css({
                'content': 'data(name)',
                'width': '20px',
                'height': '20px',
                //'text-valign': 'center',
                'color': 'black',
                //'text-outline-width': 2,
                //'text-outline-color': '#888'
            })
            .selector('edge')
            .css({
                'content': 'data(edgedisplay)',
                'font-size': '11',
                'line-color': 'data(edgecolor)',
                'target-arrow-shape': 'triangle',
                'target-arrow-color' : 'data(edgecolor)',
                'target-arrow-fill' : 'fill',
                //'opacity': 0.80,
                'color' : 'blue'
                //'curve-style' : 'haystack'
            })
            .selector(':selected')
            .css({
                'background-color': 'black',
                'line-color': 'black',
                'target-arrow-color': 'black',
                'source-arrow-color': 'black'
            })
            .selector('.faded')
            .css({
                'opacity': 0.50,
                'text-opacity': 0
            })
            .selector('.haslibraryidentification')
            .css({
                'background-color': '#000099',
            })
            .selector('.shapehighlight')
            .css({
                'shape': 'triangle',
            })
            .selector('.colorhighlight')
            .css({
                'background-color': 'red',
            }),
                                        
                                        
        elements: {
            nodes: [],
            edges: []
        },
        
        pixelRatio: 'auto',
        motionBlur: false,
        wheelSensitivity: 0.5,
        
        ready: function(context){
            return function(){
                window.cy = this
                context.cytoscape_ready = true
                
                //Adding Cytoscape Pan Zoom
                // the default values of each option are outlined below:
                var defaults = ({
                    zoomFactor: 0.05, // zoom factor per zoom tick
                    zoomDelay: 45, // how many ms between zoom ticks
                    minZoom: 0.1, // min zoom level
                    maxZoom: 10, // max zoom level
                    fitPadding: 50, // padding when fitting
                    panSpeed: 10, // how many ms in between pan ticks
                    panDistance: 10, // max pan distance per tick
                    panDragAreaSize: 75, // the length of the pan drag box in which the vector for panning is calculated (bigger = finer control of pan speed and direction)
                    panMinPercentSpeed: 0.25, // the slowest speed we can pan by (as a percent of panSpeed)
                    panInactiveArea: 8, // radius of inactive area in pan drag box
                    panIndicatorMinOpacity: 0.5, // min opacity of pan indicator (the draggable nib); scales from this to 1.0
                    autodisableForMobile: true, // disable the panzoom completely for mobile (since we don't really need it with gestures like pinch to zoom)

                    // icon class names
                    sliderHandleIcon: 'fa fa-minus',
                    zoomInIcon: 'fa fa-plus',
                    zoomOutIcon: 'fa fa-minus',
                    resetIcon: 'fa fa-expand'
                });

                cy.panzoom( defaults );
                
                var styleSheet = $('<link type="text/css" href="'+ "/ProteoSAFe/styles/cytoscape.js-panzoom.css" + "?cache=" + makeRandomString(10)+'" rel="stylesheet" />');
                styleSheet.load(function(){});
                $('head').append(styleSheet);
                
                console.log("Cytoscape Ready");
                context.checkready();
            }
        }(this)
    });
}


ResultViewNetworkDisplayer.prototype.renderPowerView = function(div){
    this.renderNetworkNodeLabelPanel(div)
    this.renderNetworkEdgeLabelPanel(div)
    this.renderNodeVisualsPanel(div)
    this.renderNetworkEdgeColorsPanel(div)
    this.renderClusterInformationPanel(div)
    this.renderIDImport(div)
    this.renderPieEnabler(div)
    this.renderLayoutButton(div)
    this.renderAlignmentCalculate(div)
    this.renderExpandArea(div)
    this.renderPopout(div)
    

    this.renderMS2PeakHighlight(div)
    this.renderLibraryMatchWindows(div)
    this.renderKeyHandler(div)
    this.renderControlPanelHide(div)
    this.renderScreenshotNetwork(div)
    this.renderDebugFeature(div)
    
}

ResultViewNetworkDisplayer.prototype.renderScreenshotNetwork = function(div){
    var screenshot_div = document.createElement("div");
    screenshot_div.id = "screenshot_div"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-picture-o"
    icon_object.title = "Download Screenshot of Network"

    icon_object.onclick = function(context_object, context_div){
        return function(){
            var download = document.createElement('a');
            download.href = cy.png();
            download.download = "network_" + context_object.taskid + "_" + context_object.componentindex + ".png";
            download.style.display = 'none';
            document.body.appendChild(download);
            download.click();
            document.body.removeChild(download);
        }

    }(this, screenshot_div)
    
    screenshot_div.appendChild(icon_object);
    
    div.appendChild(screenshot_div)
}





ResultViewNetworkDisplayer.prototype.renderControlPanelHide = function(div){
    var toggle_div = document.createElement("div");
    toggle_div.id = "control_panel_toggle_div"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-toggle-on"
    icon_object.title = "Toggle Control Panels"
    icon_object.id = "control_panel_toggle_icon"

    icon_object.onclick = function(context_object, context_div){
        return function(){
            if(context_object.control_panels_hidden == false){
                context_object.control_panels_hidden = true
                $("#control_panel_toggle_icon").removeClass("fa fa-toggle-on").addClass("fa fa-toggle-off")
                $("." + context_object.displayable_network_control_class).hide()
            }
            else{
                context_object.control_panels_hidden = false
                $("#control_panel_toggle_icon").removeClass("fa fa-toggle-off").addClass("fa fa-toggle-on")
                $("." + context_object.displayable_network_control_class).show()
            }
        }

    }(this, div)
    
    toggle_div.appendChild(icon_object);
    
    div.appendChild(toggle_div)
}


ResultViewNetworkDisplayer.prototype.renderKeyHandler = function(){
    escape_handler = function(context){
        return function(e){
            if(e.which == 27){
                $("." + context.displayable_overlay_panel_class).hide();
            }
        }
    }(this)
    
    $('body').keydown(escape_handler);
}

ResultViewNetworkDisplayer.prototype.renderLibraryMatchWindows = function(div){
    library_match_display_div = document.createElement("div");
    library_match_display_div.id = "library_match_display_div"
    library_match_display_div.className = this.displayable_overlay_panel_class
    library_match_display_div.style.display = "none"
    div.appendChild(library_match_display_div)
}

ResultViewNetworkDisplayer.prototype.renderAlignmentCalculate = function(div){
    alignment_display_box = document.createElement("div")
    alignment_display_box.id = "alignment_box"
    alignment_display_box.className = this.displayable_network_control_class

    alignment_display_area = document.createElement("div")
    alignment_display_area.id = "alignment_box_display"

    structure_tani_display_area = document.createElement("div")
    structure_tani_display_area.id = "structure_tani_display_area"
    
    var show_alignment_calc = document.createElement("button");
    show_alignment_calc.innerHTML = "Align Spec";
    show_alignment_calc.id = "alignment_box_button"
    
    show_alignment_calc.onclick = function(context){
        return function(){
            context.alignTopBottomPanel()
        }
    }(this)
    
    div.appendChild(alignment_display_box)
    alignment_display_box.appendChild(show_alignment_calc);
    alignment_display_box.appendChild(alignment_display_area);
    alignment_display_box.appendChild(structure_tani_display_area);
    
    
}

ResultViewNetworkDisplayer.prototype.renderIDImport = function(div){
    importID_div = document.createElement("div");
    importID_div.style.width = "350px";
    importID_div.id = "importID_div";
    importID_div.className = "popbox"
    
    importID_selection_div = document.createElement("div")
    importID_selection_div.id = "importID_selection_div";
    
    close_dialog_button = document.createElement("img")
    close_dialog_button.src = "/ProteoSAFe/images/hide.png"
    close_dialog_button.id = "close_dialog_button"
    close_dialog_button.onclick = function(){
        $("#importID_div").hide();
    }
    
    
    importID_div.appendChild(importID_selection_div)
    importID_div.appendChild(close_dialog_button)
    
    div.appendChild(importID_div)
    
    
    //Defining characteristics of importing different workflows
    workflow_to_view = new Object();
    workflow_to_view["MSGFDB"] = "group_by_spectrum"
    workflow_to_view["PEPNOVO"] = "group_by_spectrum"
    
    workflow_to_handler = new Object();
    workflow_to_handler["MSGFDB"] = get_msgfdb_result_callback_network_displayer_gen
    workflow_to_handler["PEPNOVO"] = get_pepnovo_result_callback_network_displayer_gen
    
    
    //Adding button to show it
    //show_dialog_button = document.createElement("img")
    //show_dialog_button.src = "/ProteoSAFe/images/plus.png"
    //show_dialog_button.id = "show_dialog_button"
    
    var show_dialog_button = document.createElement("div");
    show_dialog_button.id = "show_import_dialog_button"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-plus-square-o"
    icon_object.title = "Import Identifications"
    
    show_dialog_button.appendChild(icon_object);
    
    show_dialog_button.onclick = function(render_context){
        return function(){
            $("#importID_selection_div").empty();
            $("#importID_div").show();
            
            render_context.ready_button_red()
            
            //Getting all of my tasks
            $.ajax({
                url: "/ProteoSAFe/QueryTaskList",
                cache: false,
                success: function(context){
                    return function(json){
                        all_tasks = JSON.parse(json);
                        if(all_tasks.status == "success"){
                            
                            my_tasks = all_tasks.tasks;
                            
                            //iterating through all workflows
                            for(workflow in workflow_to_view){
                                console.log(workflow);
                                
                                
                                //Text Box
                                var import_url_box = document.createElement("INPUT");
                                import_url_box.id = "import_url_box_" + workflow;
                                var import_url_button = document.createElement("button");
                                import_url_button.innerHTML = "Import " + workflow + " IDs";
                                
                                $("#importID_selection_div").append(import_url_box)
                                $("#importID_selection_div").append(import_url_button)
                                
                                import_url_button.onclick = function(workflow_current, context){
                                    return function(){
                                        context.ready_button_red()
                                        $.ajax({
                                            url: $("#import_url_box_" + workflow_current).val(),
                                            cache: false,
                                            success: workflow_to_handler[workflow_current](context)
                                        });
                                    }
                                }(workflow, context);
                                
                                
                                //Adding the selection dropdown
                                workflow_tasks = new Array();
                                
                                
                                for(i in my_tasks){
                                    task = my_tasks[i];
                                    if(task.workflow == workflow && task.status == "DONE" && task.desc.indexOf(get_taskid()) != -1){
                                        workflow_tasks.push(task);
                                    }
                                }
                                
                                if(workflow_tasks.length == 0){
                                    continue;
                                }
                                
                                var dropdown_select = document.createElement("select");
                                dropdown_select.className = "workflow_import_selection";
                                
                                dropdown_select.onchange = function(){
                                    $("#import_url_box_" + workflow).val(this.value);
                                }
                                
                                select_workflow_div = document.createElement("div")
                                select_workflow_div.id = "import_select_div_" + workflow;
                                $("#importID_selection_div").append(select_workflow_div);
                                
                                
                                for(i in workflow_tasks){
                                    var option = document.createElement('option');
                                    option.text = workflow_tasks[i].desc
                                    results_url = "/ProteoSAFe/result.jsp?task=" + workflow_tasks[i].task + "&view=" + workflow_to_view[workflow]
                                    option.value = results_url;
                                    dropdown_select.add(option);
                                }
                                
                                $("#import_select_div_" + workflow ).append(dropdown_select);
                                
                                results_url = "/ProteoSAFe/result.jsp?task=" + workflow_tasks[0].task + "&view=" + workflow_to_view[workflow]
                                $("#import_url_box_" + workflow).val(results_url);
                            }
                            //Ready light
                            render_context.ready_button_green()
                        }
                    }
                }(render_context)
            });
        }
    }(render_object)
    
    div.appendChild(show_dialog_button);
    
}

ResultViewNetworkDisplayer.prototype.resizeNodesByFeatureName = function(div, feature_name){
    cy.style().selector('.customNodeSize').css({
        'width' : 'mapData(normalized_nodesize, 0, 100, 10, 40)',
        'height' : 'mapData(normalized_nodesize, 0, 100, 10, 40)'
    })

    if(feature_name == "default"){
        for(var i in context.node_list){
            cy.$('#' + context.node_list[i]).removeClass('customNodeSize');
        }
        return;
    }

    min_count = -1
    max_count = -1

    for(var i in this.node_list){
        feature_count = parseInt(this.clusterinfo_map[this.node_list[i]][feature_name])
        if(min_count == -1){
            min_count = feature_count;
            max_count = feature_count;
        }

        if(feature_count < min_count){
            min_count = feature_count;
        }
        if(feature_count > max_count){
            max_count = feature_count;
        }
    }

    spectrum_count_range = max_count - min_count

    //Now lets do the normalization
    for(var i in this.node_list){
        normalized_count = parseInt(this.clusterinfo_map[this.node_list[i]][feature_name])
        normalized_count -= min_count
        if(spectrum_count_range == 0){
            normalized_count = 33;
        }
        else{
            normalized_count = normalized_count/spectrum_count_range * 100
        }
        
        cy.$('#' + this.node_list[i]).data("normalized_nodesize", normalized_count)
    }

    for(var i in context.node_list){
        cy.$('#' + context.node_list[i]).addClass('customNodeSize');
    }
}

ResultViewNetworkDisplayer.prototype.colorNodesByFeatureName = function(div, feature_name){
    cy.style().selector('.customNodeColor').css({
        'background-color' : 'mapData(normalized_color, 0, 100, red, green)'
    })

    if(feature_name == "default"){
        //Clearing the highlights
        for(var i in context.node_list){
            cy.$('#' + context.node_list[i]).removeClass('customNodeColor');
        }
        return;
    }

    min_count = -1
    max_count = -1

    for(var i in this.node_list){
        feature_count = parseInt(this.clusterinfo_map[this.node_list[i]][feature_name])
        if(min_count == -1){
            min_count = feature_count;
            max_count = feature_count;
        }

        if(feature_count < min_count){
            min_count = feature_count;
        }
        if(feature_count > max_count){
            max_count = feature_count;
        }
    }

    spectrum_count_range = max_count - min_count

    //Now lets do the normalization
    for(var i in this.node_list){
        normalized_count = parseInt(this.clusterinfo_map[this.node_list[i]][feature_name])
        normalized_count -= min_count
        if(spectrum_count_range == 0){
            normalized_count = 33;
        }
        else{
            normalized_count = normalized_count/spectrum_count_range * 100
        }
        cy.$('#' + this.node_list[i]).data("normalized_color", normalized_count)
    }

    for(var i in context.node_list){
        cy.$('#' + context.node_list[i]).addClass('customNodeColor');
    }
}

ResultViewNetworkDisplayer.prototype.renderPieEnabler = function(div){
    color_mapping = new Object()
    color_mapping["color_box_1"] = "#E8747C"
    color_mapping["color_box_2"] = "#74CBE8"
    color_mapping["color_box_3"] = "#74E883"
    color_mapping["color_box_4"] = "#FFA500"
    color_mapping["color_box_5"] = "#FFFF00"
    color_mapping["color_box_6"] = "#808080"
    
    default_group_to_color = new Object();
    default_group_to_color["color_box_1"] = "G1"
    default_group_to_color["color_box_2"] = "G2"
    default_group_to_color["color_box_3"] = "G3"
    default_group_to_color["color_box_4"] = "G4"
    default_group_to_color["color_box_5"] = "G5"
    default_group_to_color["color_box_6"] = "G6"
    
    
    var render_button_1 = document.createElement("button");
    render_button_1.innerHTML = "Draw Pies";
    render_button_1.style.marginLeft = "5px"
    render_button_1.id = "pierenderbutton";
    
    render_object = this;
    render_button_1.onclick = function(context_object){
        return function(){
            //Adding Style
            cy.style().selector('.customPieColor').css({
                'pie-size': '80%',
                'pie-1-background-color': '#E8747C',
                'pie-1-background-size': 'mapData(G1_normalized, 0, 100, 0, 100)',
                'pie-2-background-color': '#74CBE8',
                'pie-2-background-size': 'mapData(G2_normalized, 0, 100, 0, 100)',
                'pie-3-background-color': '#74E883',
                'pie-3-background-size': 'mapData(G3_normalized, 0, 100, 0, 100)',
                'pie-4-background-color': '#FFA500',
                'pie-4-background-size': 'mapData(G4_normalized, 0, 100, 0, 100)',
                'pie-5-background-color': '#FFFF00',
                'pie-5-background-size': 'mapData(G5_normalized, 0, 100, 0, 100)',
                'pie-6-background-color': '#808080',
                'pie-6-background-size': 'mapData(G6_normalized, 0, 100, 0, 100)'
            })

            //Populating Cytoscape Nodes with Information 
            pie_display_mapping = new Object()
            pie_display_mapping["color_box_1"] = "G1_normalized"
            pie_display_mapping["color_box_2"] = "G2_normalized"
            pie_display_mapping["color_box_3"] = "G3_normalized"
            pie_display_mapping["color_box_4"] = "G4_normalized"
            pie_display_mapping["color_box_5"] = "G5_normalized"
            pie_display_mapping["color_box_6"] = "G6_normalized"
            
            for(var i in context_object.node_list){
                
                spectrum_count = 0
                for(box_id in pie_display_mapping){
                    feature_name = $("#" + box_id).val();
                    if(context_object.clusterinfo_map[context_object.node_list[i]][feature_name] != null){
                        feature_value = parseInt(context_object.clusterinfo_map[context_object.node_list[i]][feature_name])
                        spectrum_count += feature_value
                    }
                }
                
                for(box_id in pie_display_mapping){
                    feature_name = $("#" + box_id).val();
                    feature_value = parseInt(context_object.clusterinfo_map[context_object.node_list[i]][feature_name])
                    normalized_feature_value = feature_value/spectrum_count * 100;
                    console.log(feature_name + "\t" + normalized_feature_value)
                    cy.$('#' + context_object.node_list[i]).data(pie_display_mapping[box_id], normalized_feature_value)
                }
            }
            
            for(var i in context.node_list){
                cy.$('#' + context.node_list[i]).addClass('customPieColor');
            }
            
        };
    }(this);
    
    //Displaying groups filtering
    var legend_div = document.createElement("div");
    legend_div.id = "grouplegend_div";
    
    div.appendChild(legend_div)
    
    //Adding Header to Legend
    var legend_header = document.createElement("h3");
    legend_header.innerHTML = "Node Coloring"
    legend_header.id = "group_legend_heading"
    
    legend_div.appendChild(legend_header)

    //Adding shit to legend div
    var table_thing = document.createElement("table")
    
    for(var key in color_mapping){
        tr_1 = document.createElement("tr")
        table_thing.appendChild(tr_1)
        td_1_1 = document.createElement("td")
        td_1_2 = document.createElement("td")
        tr_1.appendChild(td_1_1)
        tr_1.appendChild(td_1_2)
        
        var input_box_1 = document.createElement("input");
        input_box_1.type = "text"
        input_box_1.id = key
        input_box_1.style.width = "80px"
        input_box_1.value = default_group_to_color[key]
        
        td_1_1.appendChild(input_box_1)
        
        var box_1_color = document.createElement("div");
        box_1_color.style.width = "40px"
        box_1_color.style.height = "25px"
        box_1_color.style.background = color_mapping[key]
        
        td_1_2.appendChild(box_1_color)
    }
    
    legend_div.appendChild(table_thing)
    
    legend_div.appendChild(render_button_1)

    //Adding a color Reset Button
    var reset_color_button = document.createElement("button");
    reset_color_button.innerHTML = "Reset";
    reset_color_button.style.marginLeft = "0px"
    reset_color_button.id = "pieresetbutton";
    reset_color_button.onclick = function(){
        for(var i in context.node_list){
            cy.$('#' + context.node_list[i]).removeClass('customPieColor');
        }   
    }
    legend_div.appendChild(reset_color_button)

    legend_div.className = this.displayable_network_control_class
}

ResultViewNetworkDisplayer.prototype.renderLayoutButton = function(div){
    var rerun_layout_icon = document.createElement("div");
    rerun_layout_icon.id = "rerun_icon"
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-refresh"
    icon_object.title = "Rerun Layout"
    rerun_layout_icon.appendChild(icon_object);
    
    
    rerun_layout_icon.onclick = function(context){
        return function(){
            sleep_start_layout(0, context.layout.window_width, context.layout.window_height);
        }
    }(this);
    div.appendChild(rerun_layout_icon)
}

ResultViewNetworkDisplayer.prototype.renderExpandArea = function(div){
    var expand_div = document.createElement("div");
    expand_div.id = "expand_div"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-arrows-alt"
    icon_object.onclick = this.expandCallback()
    icon_object.title = "Expand Viewer"
    
    expand_div.appendChild(icon_object);
    
    div.appendChild(expand_div)
}

ResultViewNetworkDisplayer.prototype.renderDebugFeature = function(div){
    var debug_div = document.createElement("div");
    debug_div.id = "debug_div"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-exclamation-triangle"
    icon_object.title = "Debug Feature"

    icon_object.onclick = function(context_object, context_div){
        return function(){
            
            
            //import_family_number = parseInt(prompt("Import Family Number", ""))
            //console.log("Importing Family Number " + import_family_number)
        }

    }(this, div)
    
    debug_div.appendChild(icon_object);
    
    div.appendChild(debug_div)
}

ResultViewNetworkDisplayer.prototype.renderMS2PeakHighlight = function(div){
	node_color_buttons_div = document.createElement("div");
    node_color_buttons_div.id = "nodeMS2colorsentry_div"
    node_color_buttons_div.className = this.displayable_network_control_class
    div.appendChild(node_color_buttons_div);
    
    label = document.createElement("h3");
    label.innerHTML = "Node MS2 Peaks Highlight"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    node_color_buttons_div.appendChild(label)
    
    download_node_button = document.createElement("i");
    download_node_button.className = "fa fa-download"
    download_node_button.title = "Download Node MS/MS"
    download_node_button.id = "download_nodes_ms2_button"
    download_node_button.onclick = this.loadAllNetworkSpectrumPeaks(this)
    
    node_ms2_highlight_input = document.createElement("input")
    node_ms2_highlight_input.id = "node_MS2_input_box"
    node_ms2_highlight_input.size = 12
    node_ms2_highlight_input.placeholder = "Cluster MZ Highlight"
    
    node_color_buttons_div.appendChild(download_node_button)
    node_color_buttons_div.appendChild(node_ms2_highlight_input)
    
    update_node_button = document.createElement("i");
    update_node_button.className = "fa fa-refresh"
    update_node_button.title = "Update Node Highlight"
    update_node_button.id = "update_node_highlight_img"
    
    update_node_button.onclick = function(context){
        return function(){
            mz_highlight = parseFloat($("#node_MS2_input_box").val())
            found_nodes = new Array()
            
            //Clearing the highlights
            for(var i in context.node_list){
                cy.$('#' + context.node_list[i]).removeClass('colorhighlight');
            }
            
            
            for(i in context.cluster_peaks){
                for(peak_idx in context.cluster_peaks[i]){
                    if(Math.abs(context.cluster_peaks[i][peak_idx][0] - mz_highlight) < 0.5){
                        //console.log("Found Peak + " + i );
                        found_nodes.push(i);
                        cy.$('#' + i).addClass('colorhighlight');
                    }
                }
                //console.log(context.cluster_peaks[i]);
            }
            console.log(found_nodes);
            //alert("highlight " + mz_highlight);
        }
    }(this)
    
    node_color_buttons_div.appendChild(update_node_button)
    
    
    //Creating window for histogram
    ms2_peak_histogram_div = document.createElement("div");
    ms2_peak_histogram_div.id = "ms2_peak_histogram_div"
    ms2_peak_histogram_div.className = this.displayable_overlay_panel_class
    div.appendChild(ms2_peak_histogram_div);
    
    render_button_1 = document.createElement("i");
    render_button_1.className = "fa fa-bar-chart"
    render_button_1.id = "show_ms2_histogram_button"
    render_button_1.title = "Node MS2 Peak Histogram"
    
    render_button_1.onclick = function(context, render_div, render_div_name){
        return function(){
            peaks_data = context.cluster_peaks
            
            ms2_fragment_list = new Array()
                
            for(var i in peaks_data){
                for(j in peaks_data[i]){
                    ms2_fragment_list.push(peaks_data[i][j][0])
                }
            }
            
            max_value = Math.floor(Math.max.apply(Math, ms2_fragment_list))
            min_value = Math.floor(Math.min.apply(Math, ms2_fragment_list))
            
            histogram_range = max_value - min_value;
            bucket_size = 1
            histogram_buckets = Math.floor(histogram_range / bucket_size)
            display_min = Math.floor(min_value/bucket_size)  * bucket_size
            display_max = display_min + histogram_buckets * bucket_size
            
            histograms = create_histogram(min_value, max_value, histogram_range, ms2_fragment_list)
            
            buckets = histograms[0]
            values = histograms[1]
            
            histogram_zipped = new Array()
                
            for(var i in buckets){
                histogram_zipped.push([buckets[i], values[i]])
            }
            
            $("#" + render_div_name).empty()
            
            chart_div = document.createElement("div");
            chart_div.id = makeRandomString(10);
            render_div.appendChild(chart_div);
            
            $(function () {
                $('#' + chart_div.id).highcharts({
                    chart: {
                        type: 'column',
                        zoomType: 'xy'
                    },
                    title: {
                        text: 'Network M2 Peak Histogram'
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
                            text: 'M2 Peak Histogram',
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
                            text: 'Spectra Counts',
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
                            '<td style="padding:0">{point.y} MS2 Spectra</td></tr>',
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
                                    click: function (render_div_name) {
                                        return function(){
                                            value_to_filter = this.x
                                            $("#node_MS2_input_box").val(value_to_filter);
                                            $("#update_node_highlight_img")[0].onclick()
                                            $("#" + render_div_name).hide()
                                        }
                                    }(render_div_name)
                                }
                            }
                        }
                    },
                    series: [{
                        showInLegend: false,
                        name: 'MS2 Peak Histogram',
                        data: histogram_zipped
                    }]
                });
            });
        
            
            //Adding close 
            close_dialog_button = document.createElement("img")
            close_dialog_button.src = "/ProteoSAFe/images/hide.png"
            close_dialog_button.id = "close_histogram_button"
            close_dialog_button.onclick = function(div_name){
                return function(){
                    $("#" + render_div_name).hide()
                }
            }(render_div_name)
            
            render_div.appendChild(close_dialog_button);
            
            $("#" + render_div_name).show()
        }
    }(this, ms2_peak_histogram_div, ms2_peak_histogram_div.id)
    
    node_color_buttons_div.appendChild(render_button_1)
}


ResultViewNetworkDisplayer.prototype.renderPopout = function(div){
    var popout_div = document.createElement("div");
    popout_div.id = "popout_div"
    
    icon_object = document.createElement("i");
    icon_object.className = "fa fa-external-link"
    icon_object.onclick = function(){
        url = document.URL;
        url = url.replace("result.jsp", "result_bare.jsp")
        width = screen.availWidth-10
        height = screen.availHeight-55
        
        window.open(
        url, "result_popout",
        "width=" + width + ",height=" + height + ",toolbar=0,location=0,directories=0," +
        "status=0,menubar=0,scrollbars=yes,resizeable=0")
    }
    icon_object.title = "Popout Viewer"
    
    popout_div.appendChild(icon_object);
    
    div.appendChild(popout_div)
}

ResultViewNetworkDisplayer.prototype.expandCallback = function(){
    context = this;
    return function(){
        context.layout = context.biglayout
            

        var styleSheet = $('<link type="text/css" href="'+context.layout.cssfile + "?cache=" + makeRandomString(10)+'" rel="stylesheet" />');
        styleSheet.load(function(){cy.resize();});
        $('head').append(styleSheet);

        
        //Updating Icon
        $("#expand_div").empty();
        
        icon_object = document.createElement("i");
        icon_object.className = "fa fa-compress"
        icon_object.onclick = context.shrinkCallback()
        icon_object.title = "Shrink Viewer"
        $("#expand_div").append(icon_object)
        
        
    }
}

ResultViewNetworkDisplayer.prototype.shrinkCallback = function(){
    context = this;
    return function(){
        context.layout = context.smalllayout
        
        var styleSheet = $('<link type="text/css" href="'+context.layout.cssfile + "?cache=" + makeRandomString(10)+'" rel="stylesheet" />');
        styleSheet.load(function(){cy.resize();});
        $('head').append(styleSheet);
        
        $("#expand_div").empty();
        
        icon_object = document.createElement("i");
        icon_object.className = "fa fa-arrows-alt"
        icon_object.onclick = context.expandCallback()
        icon_object.title = "Expand Viewer"
        $("#expand_div").append(icon_object)
    }
}

ResultViewNetworkDisplayer.prototype.renderClusterInformationPanel = function(div){
    //Initially rendering top panel
    top_content_wrapper = document.createElement("div")
    top_content_wrapper.id = "topcontentwrapper"
    
    
    
    //Initially rendering Bottom Panel
    bottom_content_wrapper = document.createElement("div")
    bottom_content_wrapper.id = "bottomcontentwrapper"
    
    
    
    
    //Making a right div to hold stuff
    top_content_div = document.createElement("div");
    top_content_div.id = "topcontentdiv";
    
    //Add an initial text to describe box
    top_panel_initial_text = document.createElement("div")
    top_panel_initial_text.className = "initial_panel_text"
    top_panel_initial_text.innerHTML = "Node Information Panel <br> Left click to populate"
    top_content_div.appendChild(top_panel_initial_text);
    
    
    bottom_content_div = document.createElement("div");
    bottom_content_div.id = "bottomcontentdiv";
    
    //Add an initial text to describe bottom box
    bot_panel_initial_text = document.createElement("div")
    bot_panel_initial_text.className = "initial_panel_text"
    bot_panel_initial_text.innerHTML = "Node Information Panel <br> Right click to populate"
    bottom_content_div.appendChild(bot_panel_initial_text);
    
    
    top_structure_div = document.createElement("div");
    top_structure_div.id = "topstructurediv";
    top_structure_div.className += "structurediv"
    $("#topstructurediv").hide();
    
    bottom_structure_div = document.createElement("div");
    bottom_structure_div.id = "bottomstructurediv";
    bottom_structure_div.className += "structurediv"
    $("#bottomstructurediv").hide();
    
    
    hover_content_div = document.createElement("div");
    hover_content_div.id = "hovercontentdiv";
    
    hover_structure_div = document.createElement("div");
    hover_structure_div.id = "hoverstructurediv";
    
    
    //Creating popup box holder
    hover_special_div = document.createElement("div");
    hover_special_div.id = "popup_hover_wrapper"
    hover_special_div.className = "popbox"
    hover_special_div.appendChild(hover_content_div)
    hover_special_div.appendChild(hover_structure_div)
    
    
    
    div.appendChild(top_content_div)
    div.appendChild(bottom_content_div)
    
    top_content_wrapper.appendChild(top_content_div)
    top_content_wrapper.appendChild(top_structure_div)
    
    bottom_content_wrapper.appendChild(bottom_content_div)
    bottom_content_wrapper.appendChild(bottom_structure_div)
    
    div.appendChild(top_content_wrapper);
    div.appendChild(bottom_content_wrapper);
    
    
    div.appendChild(hover_special_div);
}



ResultViewNetworkDisplayer.prototype.renderTopInfoPanelPopulate = function(scanNumber){
    this.renderInfoPanelPopulate($("#topcontentdiv"), scanNumber, true, true)
    this.renderStructurePanelPopulate($("#topstructurediv"), scanNumber, 250, 200)
}

ResultViewNetworkDisplayer.prototype.renderBottomInfoPanelPopulate = function(scanNumber){
    this.renderInfoPanelPopulate($("#bottomcontentdiv"), scanNumber, true, false)
    this.renderStructurePanelPopulate($("#bottomstructurediv"), scanNumber, 250, 200)
}

ResultViewNetworkDisplayer.prototype.renderInfoPanelPopulate = function(div, scanNumber, plotspectrum, isTopPanel){
    div.empty()
    
    cluster_object = this.clusterinfo_map[scanNumber]
    full_string = ""
    
    table_left = 0
    
    var plotwindow = document.createElement("div");
    plotwindow.id = "plotwindow";
    plotwindow.style.border = "1px solid"
    plotwindow.style.width = "530px"
    
    var table = $('<table></table>');
    
    
    if(plotspectrum == null || plotspectrum == true){
        if(div[0].id == "topcontentdiv"){
            this.genericBackendPlotter(this.defaultFilename, cluster_object['cluster index'], cluster_object['Peptide'], plotwindow, true)
        }
        else{
            this.genericBackendPlotter(this.defaultFilename, cluster_object['cluster index'], cluster_object['Peptide'], plotwindow, false)
        }
        div.append(plotwindow)
        
        table_left = 535
        
        table.css({top: 0, left: table_left, position: "absolute"});

        //Adding element to download the spectra
        download_node_button = document.createElement("i");
        download_node_button.className = "fa fa-download download_user_spectra_icon"
        download_node_button.title = "Download MS/MS Peaks"
        
        download_node_button.onclick = function(context, isTopPanel){
            return function(){
                peaks_to_render = null;
                if(isTopPanel){
                    peaks_to_render = context.top_plot_spectrum.peaks
                }
                else{
                    peaks_to_render = context.bottom_plot_spectrum.peaks
                }

                output_string = ""
                for(i = 0; i < peaks_to_render.length; i++){
                    output_string += peaks_to_render[i][0] + "\t" + peaks_to_render[i][1] + "\n"
                }
                var download = document.createElement('a');
                

  
                download.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(output_string)
                download.download = "peaks.txt";
                download.style.display = 'none';
                document.body.appendChild(download);
                download.click();
                document.body.removeChild(download);
            }
        }(context, isTopPanel)

        div.append(download_node_button)

    }
    else{
        table.css({top: 0, left: table_left});
    }
    
    
    
    
    for(var i in this.displayableFeatureInInfoPanel){
        row = $('<tr></tr>');
        var column1 = $('<td></td>').text(this.displayableFeatureInInfoPanel[i]);
        var column2 = $('<td></td>')
        
        display_div = document.createElement("div")
        if(plotspectrum == null || plotspectrum == true){
            display_div.className = ("node_info_panel_item")
        }
        
        display_div.textContent = (cluster_object[this.displayableFeatureInInfoPanel[i]])
        display_div.title = (cluster_object[this.displayableFeatureInInfoPanel[i]])
        column2.append(display_div)
        //var column2 = $('<td></td>').text(cluster_object[this.displayableFeatureInInfoPanel[i]]);
        
        row.append(column1);
        row.append(column2);
        
        table.append(row);
    }
    
    if(plotspectrum == null || plotspectrum == true){
        //Adding Peptide field that is editable
        row = $('<tr></tr>');
        var column1 = $('<td></td>').text("Peptide");
        var column2 = $('<td></td>');
        peptide_input_box = document.createElement("input")
        peptide_input_box.id = makeRandomString(10);
        peptide_input_box.className = "peptide_input_field"
        peptide_input_box.size = 14
        if(cluster_object["Peptide"] != null){
            peptide_input_box.value = cluster_object["Peptide"]
        }
        column2.append(peptide_input_box)
        //Adding Refresh Button
        //var update_peptide_button = document.createElement("img");
        //update_peptide_button.id = "update_peptide_img"
        //update_peptide_button.src = "/ProteoSAFe/images/plugins/circle_arrow.png"
        
        update_peptide_button = document.createElement("i");
        update_peptide_button.className = "fa fa-refresh"
        update_peptide_button.title = "Update Peptide Plot"
        update_peptide_button.id = "update_peptide_img"
        
        update_peptide_button.onclick = function(context, render_div, isTopPanel, input_box_id){
            return function(){
                peaks_to_render = null;
                if(isTopPanel){
                    peaks_to_render = context.top_plot_spectrum.peaks
                }
                else{
                    peaks_to_render = context.bottom_plot_spectrum.peaks
                }
                
                new_peptide = $("#" + input_box_id).val()
                
                displayJSLibrarySpectrumViewer_Standalone(render_div, 298, 500, peaks_to_render, null, new_peptide);
                
                
                
            }
        }(this, plotwindow, isTopPanel, peptide_input_box.id)
        
        detailed_peptide_button = document.createElement("i");
        detailed_peptide_button.className = "fa fa-plus-square"
        detailed_peptide_button.title = "Detailed Peptide Plot"
        detailed_peptide_button.id = "update_peptide_img"
        
        detailed_peptide_button.onclick = function(context, render_div, isTopPanel, input_box_id){
            return function(){
                peaks_to_render = null;
                if(isTopPanel){
                    peaks_to_render = context.top_plot_spectrum.peaks
                }
                else{
                    peaks_to_render = context.bottom_plot_spectrum.peaks
                }
                
                new_peptide = $("#" + input_box_id).val()

                
                library_match_div_name = "library_match_display_div"
                $("#" + library_match_div_name).empty();
                
                $("#" + library_match_div_name).show()
                
                displayJSLibrarySpectrumViewer_Standalone_divname(library_match_div_name, 500, 500, peaks_to_render, null, new_peptide, true);
                
                //Adding close 
                close_dialog_button = document.createElement("img")
                close_dialog_button.src = "/ProteoSAFe/images/hide.png"
                close_dialog_button.id = "close_library_match_button"
                close_dialog_button.onclick = function(div_name){
                    return function(){
                        $("#" + div_name).hide()
                    }
                }(library_match_div_name)
            
                $("#" + library_match_div_name).append(close_dialog_button);
                
                
            }
        }(this, plotwindow, isTopPanel, peptide_input_box.id)
        column2.append(update_peptide_button)
        column2.append(detailed_peptide_button)
    
        row.append(column1);
        row.append(column2);
        
        table.append(row);
    }
    else{
        //Adding Peptide field that is not editable
        row = $('<tr></tr>');
        var column1 = $('<td></td>').text("Peptide");
        var column2 = $('<td></td>').text(cluster_object["Peptide"]);
        row.append(column1);
        row.append(column2);
        
        table.append(row);
    }
    
    
    //Adding Retention time information
    row = $('<tr></tr>');
    var column1 = $('<td></td>').text("RT Info");
    var column2 = $('<td></td>').html(parseFloat(cluster_object["RTMean"]).toFixed(2) + ", &sigma; = " + parseFloat(cluster_object["RTStdErr"]).toFixed(2));
    
    row.append(column1);
    row.append(column2);
    
    table.append(row);
    
    //Creating link to cluster info
    row = $('<tr></tr>');
    var column1 = $('<td></td>').text("ClusterSpectra");
    var column2 = $('<td></td>');
    
    var clusterinfo_url = $('<a>',{
            text: 'Cluster Spectra',
            title: 'Cluster Spectra',
            href: '/ProteoSAFe/result.jsp?task=' + get_taskid() + '&view=cluster_details' +  '&protein=' + scanNumber,
            target: '_blank'
        })
    column2.append(clusterinfo_url)
    row.append(column1);
    row.append(column2);
    table.append(row);
    
    //Creating link to Library if it exists, and if it is in side panel
    if(this.clusterinfo_map[scanNumber] != null && this.clusterinfo_map[scanNumber].library_SpectrumID != null && (plotspectrum == null || plotspectrum == true) ){
        spectrum_id = this.clusterinfo_map[scanNumber].library_SpectrumID

        show_library_page_button = document.createElement("button");
        show_library_page_button.innerHTML = "View Lib Spec"
        show_library_page_button.onclick = function(spectrum_id){
            return function(){
                url = '/ProteoSAFe/gnpslibraryspectrum.jsp?SpectrumID=' + spectrum_id
                var win = window.open(url, '_blank');
                win.focus();
            }
        }(spectrum_id)
            
        
        
        //Adding Button to Compare Spectrum to Library Spectrum
        var show_library_match_button = document.createElement("button");
        show_library_match_button.innerHTML = "View Match";
        
        show_library_match_button.onclick = function(context, isTopPanel, library_SpectrumID){
            return function(){
                library_match_div_name = "library_match_display_div"
                $("#" + library_match_div_name).empty();
                
                peaks_to_render_object = new Object()
                
                if(isTopPanel){
                    peaks_to_render_object.query_spectrum_peaks = context.top_plot_spectrum.peaks
                }
                else{
                    peaks_to_render_object.query_spectrum_peaks = context.bottom_plot_spectrum.peaks
                }
                
                $("#" + library_match_div_name).show()
                
                //Grabbing Library Spectrum
                library_url = "/ProteoSAFe/SpectrumCommentServlet"
                $.ajax({
                    url: library_url,
                    cache: true,
                    data: {
                        SpectrumID: library_SpectrumID
                    },
                    success: function(peaks_to_render_object, library_match_div_name, library_SpectrumID){
                        return function(response){
                            spectrum_object = JSON.parse(response)
                            spectrum_info = spectrum_object.spectruminfo
                            peaks_str = spectrum_info.peaks_json
                            peaks_to_render_object.library_spectrum_peaks = JSON.parse(peaks_str)
                            
                            
                            //Dressing up the Div
                            title_item = document.createElement("h2");
                            title_item.innerHTML = library_SpectrumID + " Match"
                            title_item.style.textAlign = "center"
                            title_item.style.height = "0px"
                            $("#" + library_match_div_name).append(title_item)
                            
                            var child = document.createElement("div");
                            child.id = makeRandomString(10);
                            $("#" + library_match_div_name).append(child);
                            
                            width = 700
                            height = 500
                            
                            display_lorikeet_comparison_spectra(child.id, width, height, peaks_to_render_object.query_spectrum_peaks, peaks_to_render_object.library_spectrum_peaks)
                        }
                    }(peaks_to_render_object, library_match_div_name, library_SpectrumID)
                })
                
                //Adding close 
                close_dialog_button = document.createElement("img")
                close_dialog_button.src = "/ProteoSAFe/images/hide.png"
                close_dialog_button.id = "close_library_match_button"
                close_dialog_button.onclick = function(div_name){
                    return function(){
                        $("#" + div_name).hide()
                    }
                }(library_match_div_name)
            
                $("#" + library_match_div_name).append(close_dialog_button);
                
            }
        }(this, isTopPanel, spectrum_id)
        
        row = $('<tr></tr>');
        column1 = $('<td></td>');
        column2 = $('<td></td>');
        column1.append(show_library_page_button)
        column2.append(show_library_match_button)
        row.append(column1);
        row.append(column2);
        table.append(row);
        
        //row = $('<tr></tr>');
        //var column1 = $('<td></td>').text("Viz Library Match");
        //var column2 = $('<td></td>');
        //column2.append(show_library_match_button)
        //row.append(column1);
        //row.append(column2);
        //table.append(row);
        
    }
    
    div.append(table)
}

ResultViewNetworkDisplayer.prototype.renderStructurePanelPopulate = function(div, scanNumber, width, height){
    div.empty()
    div.hide();
    
    cluster_object = this.clusterinfo_map[scanNumber]
    
    smiles_string = cluster_object.Smiles
    
    if(width == null){
        width = 250
    }
    if(height == null){
        height = 250
    }
    
    if(smiles_string != null && smiles_string.length > 5){
        div.show();
        image_element = document.createElement("img")
        cache_flag = "2"
        structure_url_prefix = "http://ccms-support.ucsd.edu:5000/smilesstructure?smiles=" + encodeURIComponent(smiles_string) + "&width=" + width + "&height=" + height + "cache=" + cache_flag
        image_element.src = structure_url_prefix
        image_element.height = height;
        
        div.append(image_element)
    }
}

ResultViewNetworkDisplayer.prototype.renderNetworkNodeLabelPanel = function(div){
    node_labels_buttons_div = document.createElement("div");
    node_labels_buttons_div.id = "nodelabelsbuttons_div"
    node_labels_buttons_div.className = this.displayable_network_control_class
    div.appendChild(node_labels_buttons_div);


    label = document.createElement("h3");
    label.innerHTML = "Node Labels"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    node_labels_buttons_div.appendChild(label)
    
    var table_thing = document.createElement("table")
    
    for(var i in this.displayableFeatures){
        tr_1 = document.createElement("tr")
        table_thing.appendChild(tr_1)
        td_1_1 = document.createElement("td")
        tr_1.appendChild(td_1_1)
        
        item_name = this.displayableFeatures[i]
        var render_button_1 = document.createElement("button");
        render_button_1.className += "NetworkLabelButton";
        render_button_1.innerHTML = item_name;
        
        //render_button_1.style.width = "95px"
        
        render_object = this;
        render_button_1.onclick = render_network_label_button_callback_gen(this, item_name)
        
        td_1_1.appendChild(render_button_1)
    }
    
    node_labels_buttons_div.appendChild(table_thing)
}


ResultViewNetworkDisplayer.prototype.renderNodeVisualsPanel = function(div){
    node_size_div = document.createElement("div");
    node_size_div.id = "node_size_div"
    node_size_div.className = this.displayable_network_control_class
    div.appendChild(node_size_div);

    buttons_to_display_array = [["Default", "default"],
                                ["Spec Count", "number of spectra"], 
                                ["Precursor Int", "sum(precursor intensity)"], 
                                ["Num Files", "UniqueFileSourcesCount"], 
                                ["Paren Mass", "parent mass"],
                                ["Even Odd", "EvenOdd"],
                                ["Prec Charge", "precursor charge"]]


    //Rendering for Node Sizes
    this.renderNodeSizePanel(node_size_div, buttons_to_display_array);

    //Now rendering for Node Colors
    this.renderNodeColorPanel(node_size_div, buttons_to_display_array);
}

ResultViewNetworkDisplayer.prototype.renderNodeSizePanel = function(div, buttons_to_display_array){
    label = document.createElement("h3");
    label.innerHTML = "Node Size"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    div.appendChild(label)

    var table_thing = document.createElement("table")
    
    div.appendChild(table_thing)

    //Adding a dropdown selection for Node Size
    dropdown_select = document.createElement("select")
    dropdown_select.className += "NetworkDisplaySelectDropdown";
    for(var i in buttons_to_display_array){
        display_value = buttons_to_display_array[i][0];
        select_value = buttons_to_display_array[i][1];

        select_option = document.createElement("option")
        select_option.innerHTML = display_value
        select_option.value = select_value

        dropdown_select.appendChild(select_option)
    }

    dropdown_select.onchange = function(context_object, context_div){
        return function(){
            context_object.resizeNodesByFeatureName(context_div, this.selectedOptions[0].value);
        }
        
    }(this, div)

    table_thing.appendChild(dropdown_select)
}

ResultViewNetworkDisplayer.prototype.renderNodeColorPanel = function(div, buttons_to_display_array){
    label = document.createElement("h3");
    label.innerHTML = "Node Color"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    div.appendChild(label)

    var table_thing = document.createElement("table")
    
    div.appendChild(table_thing)

    //Adding a dropdown selection for Node Size
    dropdown_select = document.createElement("select")
    dropdown_select.className += "NetworkDisplaySelectDropdown";
    for(var i in buttons_to_display_array){
        display_value = buttons_to_display_array[i][0];
        select_value = buttons_to_display_array[i][1];

        select_option = document.createElement("option")
        select_option.innerHTML = display_value
        select_option.value = select_value

        dropdown_select.appendChild(select_option)
    }

    dropdown_select.onchange = function(context_object, context_div){
        return function(){
            console.log("Change Color");
            context_object.colorNodesByFeatureName(context_div, this.selectedOptions[0].value);
        }
        
    }(this, div)

    table_thing.appendChild(dropdown_select)
}

function render_network_label_button_callback_gen(render_object, item_name){
    return function(){
        render_object.updateDisplayFeature(item_name);
    }
}

// set data to the file streamer
ResultViewNetworkDisplayer.prototype.setData = function(data) {
    this.data = data;
}

ResultViewNetworkDisplayer.prototype.updateDisplayFeature = function(featureName){
    for(var i in this.node_list){
        new_name = " "
        if(this.clusterinfo_map[this.node_list[i]][featureName] != null){
            new_name = this.clusterinfo_map[this.node_list[i]][featureName]
            
            //Removing the framing quotes
            if(new_name[0] == new_name[new_name.length - 1] && new_name[0] == '"'){
                new_name = new_name.substring(1, new_name.length - 1)
            }
            
            //Removes Peptide Sentinel
            if(new_name.indexOf("Peptide:") == 0){
                new_name = new_name.substring("Peptide:".length);
            }
            
            new_name = new_name.substring(0,this.nodeLabelDisplayLength);
            
            
        }
        
        if(new_name == "N/A"){
            new_name = ""
        }
        
        cy.$('#' + this.node_list[i]).data("name", new_name)
    }
}


ResultViewNetworkDisplayer.prototype.renderNetworkEdgeColorsPanel = function(div){
    edge_color_buttons_div = document.createElement("div");
    edge_color_buttons_div.id = "edgecolorsentry_div"
    edge_color_buttons_div.className = this.displayable_network_control_class
    div.appendChild(edge_color_buttons_div);
    
    label = document.createElement("h3");
    label.innerHTML = "Edge Highlight"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    edge_color_buttons_div.appendChild(label)
    
    edge_delta_input = document.createElement("input")
    edge_delta_input.id = "edge_delta_input_box"
    edge_delta_input.size = 12
    edge_delta_input.placeholder = "Edge MZ Delta"
    
    edge_color_buttons_div.appendChild(edge_delta_input)
    
    //Adding Refresh Button
    //update_edge_button = document.createElement("div");
    //update_edge_button.id = "update_edge_color_img"
    //update_edge_button.src = "/ProteoSAFe/images/plugins/circle_arrow.png"
    
    update_edge_button = document.createElement("i");
    update_edge_button.className = "fa fa-refresh"
    update_edge_button.title = "Update MZ Delta Edge Highlight"
    update_edge_button.id = "update_edge_color_img"
    
    //update_edge_button.appendChild(icon_object)
    
    update_edge_button.onclick = function(context, input_box_id){
        return function(){
            mz_delta_user_string = $("#" + input_box_id).val()
            
            //Array full of filters, each element will be a 2 element array of low and high values
            mz_filters = new Array();
            
            if(mz_delta_user_string.length == 0){
                //Setting color back to normal
                edge_index = 0
                for(var i in context.pairs_data){
                    pair_obj = context.pairs_data[i]
                    edge_index += 1
                    
                    cy.$('#e_' + edge_index).data("edgecolor", "#bbb")
                }
                return;
            }
            
            default_tolerance = 0.5
            
            user_delta_splits = mz_delta_user_string.split("||")
            for(filters_idx = 0; filters_idx < user_delta_splits.length; filters_idx++){
                mz_low = 0
                mz_high = 0
                if(user_delta_splits[filters_idx].indexOf("-") != -1){
                    mz_low = parseFloat(user_delta_splits[filters_idx].split("-")[0])
                    mz_high = parseFloat(user_delta_splits[filters_idx].split("-")[1])
                }
                else{
                    mz_delta_user = parseFloat(user_delta_splits[filters_idx])
                    mz_low = mz_delta_user - default_tolerance;
                    mz_high = mz_delta_user + default_tolerance;
                }
                mz_filters.push([mz_low, mz_high])
            }
            
            
            edge_index = 0
            for(var i in context.pairs_data){
                pair_obj = context.pairs_data[i]
                edge_index += 1
                
                edge_delta = parseFloat(pair_obj["DeltaMZ"])
                
                color_edge = false;
                
                for(filters_idx in mz_filters){
                    if( edge_delta > mz_filters[filters_idx][0] && edge_delta < mz_filters[filters_idx][1]){
                        color_edge = true;
                        break;
                    }
                }
                
                if(color_edge == true){
                    cy.$('#e_' + edge_index).data("edgecolor", "red")
                }
                else{
                    cy.$('#e_' + edge_index).data("edgecolor", "#bbb")
                }
                
                /*if( edge_delta > mz_low && edge_delta < mz_high){
                    cy.$('#e_' + edge_index).data("edgecolor", "red")
                }
                else{
                    cy.$('#e_' + edge_index).data("edgecolor", "#bbb")
                }*/
            }
            
        }
    }(this, edge_delta_input.id)
    
    edge_color_buttons_div.appendChild(update_edge_button)
    
    
    
    //Creating window for histogram
    pairs_histogram_div = document.createElement("div");
    pairs_histogram_div.id = "pairs_histogram_div"
    pairs_histogram_div.className = this.displayable_overlay_panel_class
    div.appendChild(pairs_histogram_div);
    
    //Button to show the panel
    //var render_button_1 = document.createElement("button");
    //render_button_1.id = "show_histogram_button";
    //render_button_1.innerHTML = "Pairs Histogram";
    
    render_button_1 = document.createElement("i");
    render_button_1.className = "fa fa-bar-chart"
    render_button_1.id = "show_histogram_button"
    render_button_1.title = "Edge Histogram"
    
    render_button_1.onclick = function(context, render_div, render_div_name){
        return function(){
            pairs_data = context.pairs_data
            
            mz_delta_list = new Array()
                
            for(var i in pairs_data){
                mz_delta_list.push(Math.abs(parseFloat(pairs_data[i].DeltaMZ)))
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
            
            $("#" + render_div_name).empty()
            
            chart_div = document.createElement("div");
            chart_div.id = "histogram_chart_object";
            render_div.appendChild(chart_div);
            
            $(function () {
                $('#' + chart_div.id).highcharts({
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
                                    click: function (render_div_name) {
                                        return function(){
                                            delta_to_filter = this.x
                                            $("#edge_delta_input_box").val(delta_to_filter);
                                            $("#update_edge_color_img")[0].onclick()
                                            $("#" + render_div_name).hide()
                                        }
                                    }(render_div_name)
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
        
            
            //Adding close 
            close_dialog_button = document.createElement("img")
            close_dialog_button.src = "/ProteoSAFe/images/hide.png"
            close_dialog_button.id = "close_histogram_button"
            close_dialog_button.onclick = function(div_name){
                return function(){
                    $("#" + render_div_name).hide()
                }
            }(render_div_name)
            
            render_div.appendChild(close_dialog_button);
            
            $("#" + render_div_name).show()
        }
    }(this, pairs_histogram_div, pairs_histogram_div.id)
    
    edge_color_buttons_div.appendChild(render_button_1)
    
    //Adding Field for Cosine Filtering
    edge_score_input = document.createElement("input")
    edge_score_input.id = "edge_cosine_input_box"
    edge_score_input.size = 12
    edge_score_input.placeholder = "Edge Score Min."
    
    edge_color_buttons_div.appendChild(edge_score_input)
    
    update_cosine_edge_button = document.createElement("i");
    update_cosine_edge_button.className = "fa fa-refresh"
    update_cosine_edge_button.title = "Update Score Edge Highlight"
    update_cosine_edge_button.id = "update_edge_cosine_color_img"
    
    update_cosine_edge_button.onclick = function(context, input_box_id){
        return function(){
            cosine_user_string = $("#" + input_box_id).val()
            
            if(cosine_user_string.length == 0){
                //Setting color back to normal
                edge_index = 0
                for(var i in context.pairs_data){
                    pair_obj = context.pairs_data[i]
                    edge_index += 1
                    
                    cy.$('#e_' + edge_index).data("edgecolor", "#bbb")
                }
                return;
            }
            
            minimum_score_value = parseFloat(cosine_user_string) - 0.01

            edge_index = 0
            for(var i in context.pairs_data){
                pair_obj = context.pairs_data[i]
                edge_index += 1
                
                edge_score = parseFloat(pair_obj["Cosine"])
                
                
                if(edge_score >= minimum_score_value){
                    cy.$('#e_' + edge_index).data("edgecolor", "red")
                }
                else{
                    cy.$('#e_' + edge_index).data("edgecolor", "#bbb")
                }
            }
            
        }
    }(this, edge_score_input.id)
    
    edge_color_buttons_div.appendChild(update_cosine_edge_button)
    
    
    render_cosine_histogram = document.createElement("i");
    render_cosine_histogram.className = "fa fa-bar-chart"
    render_cosine_histogram.id = "show_cosine_histogram_button"
    render_cosine_histogram.title = "Edge Cosine Histogram"
    
    
    render_cosine_histogram.onclick = function(context, render_div, render_div_name){
        return function(){
            pairs_data = context.pairs_data
            
            mz_cosine_list = new Array()
                
            for(var i in pairs_data){
                mz_cosine_list.push(Math.abs(parseFloat(pairs_data[i].Cosine)))
            }
            
            bucket_size = 0.02
            
            max_value = Math.max.apply(Math, mz_cosine_list) + 2 * bucket_size
            min_value = Math.min.apply(Math, mz_cosine_list) - bucket_size
            
            
            histogram_range = max_value - min_value;
            histogram_buckets = Math.floor(histogram_range / bucket_size)
            display_min = Math.floor(min_value/bucket_size)  * bucket_size
            display_max = display_min + histogram_buckets * bucket_size
            
            mz_histograms = create_histogram(display_min, display_max, histogram_buckets, mz_cosine_list)
            
            mz_buckets = mz_histograms[0]
            mz_values = mz_histograms[1]
            
            mz_histogram_zipped = new Array()
                
            for(var i in mz_buckets){
                mz_histogram_zipped.push([Math.round((mz_buckets[i]*100))/100, mz_values[i]])
            }
            
            $("#" + render_div_name).empty()
            
            chart_div = document.createElement("div");
            chart_div.id = "histogram_chart_object";
            render_div.appendChild(chart_div);
            
            $(function () {
                $('#' + chart_div.id).highcharts({
                    chart: {
                        type: 'column',
                        zoomType: 'xy'
                    },
                    title: {
                        text: 'Network Cosine Score Histogram'
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
                            text: 'Cosine Score',
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
                        headerFormat: '<span style="font-size:14px"><b>{point.key} Cosine Score</b></span><table>',
                        //headerFormat: function(){
                        //    return '<span style="font-size:14px"><b>' + Highcharts.numberFormat(this.point.key, 1) + ' Cosine Score</b></span><table>'
                        //},
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
                                    click: function (render_div_name) {
                                        return function(){
                                            cosine_to_filter = this.x
                                            $("#edge_cosine_input_box").val(cosine_to_filter);
                                            $("#update_edge_cosine_color_img")[0].onclick()
                                            $("#" + render_div_name).hide()
                                        }
                                    }(render_div_name)
                                }
                            }
                        }
                    },
                    series: [{
                        showInLegend: false,
                        name: 'Cosine Score',
                        data: mz_histogram_zipped
                    }]
                });
            });
        
            
            //Adding close 
            close_dialog_button = document.createElement("img")
            close_dialog_button.src = "/ProteoSAFe/images/hide.png"
            close_dialog_button.id = "close_histogram_button"
            close_dialog_button.onclick = function(div_name){
                return function(){
                    $("#" + render_div_name).hide()
                }
            }(render_div_name)
            
            render_div.appendChild(close_dialog_button);
            
            $("#" + render_div_name).show()
        }
    }(this, pairs_histogram_div, pairs_histogram_div.id)
    
    edge_color_buttons_div.appendChild(render_cosine_histogram)
}


ResultViewNetworkDisplayer.prototype.loadAllNetworkSpectrumPeaks = function(context){
    return function(){
        console.log("Loading spectrum peaks, this'll take a while");
        context.cluster_peaks = new Object()
        context.cluster_peaks_loaded_count = 0
        var requests = [];
        context.ready_button_red();
        for(var key in context.node_list){
            requests.push(function(current_key){
                return function(){
                    $.ajax({
                        url: "/ProteoSAFe/DownloadResultFile",
                        data: { task: get_taskid(), invoke: 'annotatedSpectrumImageText', block: '0', file: "FILE->" + context.defaultFilename, scan: context.node_list[current_key], peptide: "*..*", force: "false"},
                        cache: false,
                        success: function(context){
                            return function(data){
                                //console.log(data);
                                peaklist = parseSpecplotPeaksToArray(data)
                                context.cluster_peaks[context.node_list[current_key]] = peaklist
                                context.cluster_peaks_loaded_count++;
                                if(context.cluster_peaks_loaded_count == context.node_list.length){
                                    context.cluster_peaks_loaded = true;
                                    context.ready_button_green();
                                }
                                //console.log(peaklist)
                            }
                        }(context)
                    });
                }
            }(key))
        }
        
        process_request = function() {
            if(requests.length > 0) {
                var request = requests.pop();
                if(typeof request === "function") {
                    request();
                }
            }
            if(requests.length > 0) {
                setTimeout(process_request, 200);
            }
        }
        
        setTimeout(process_request, 200);
        
    }
}

//Rendering the buttons for edges
ResultViewNetworkDisplayer.prototype.renderNetworkEdgeLabelPanel = function(div){
    edge_labels_buttons_div = document.createElement("div");
    edge_labels_buttons_div.id = "edgelabelsbuttons_div"
    edge_labels_buttons_div.className = this.displayable_network_control_class
    div.appendChild(edge_labels_buttons_div);

    label = document.createElement("h3");
    label.innerHTML = "Edge Labels"
    label.style.marginTop = "2px"
    label.style.marginBottom = "2px"
    label.style.textAlign = "center"
    label.style.fontSize = "12pt"
    edge_labels_buttons_div.appendChild(label)
    
    var table_thing = document.createElement("table")
    
    for(var i in this.edgedisplayableFeatures){
        tr_1 = document.createElement("tr")
        table_thing.appendChild(tr_1)
        td_1_1 = document.createElement("td")
        tr_1.appendChild(td_1_1)
        
        item_name = this.edgedisplayableFeatures[i]
        var render_button_1 = document.createElement("button");
        render_button_1.className += "NetworkLabelButton";
        render_button_1.innerHTML = item_name;
        
        //render_button_1.style.width = "95px"
        
        render_object = this;
        render_button_1.onclick = function(render_object, item_name){
            return function(){
                render_object.updateEdgeDisplayFeature(item_name);
            }
        }(this, item_name);
        
        td_1_1.appendChild(render_button_1)
    }
    
    edge_labels_buttons_div.appendChild(table_thing)
}


ResultViewNetworkDisplayer.prototype.updateEdgeDisplayFeature = function(featureName){
    edge_index = 0
    for(var i in pairs_data){
        pair_obj = pairs_data[i]
        edge_index += 1
        
        new_name = pair_obj[featureName]
        
        if(featureName == "None"){
            new_name = "";
        }
        
        if(featureName == "Cosine"){
            new_name = parseFloat(new_name).toFixed(2)
        }
        
        cy.$('#e_' + edge_index).data("edgedisplay", new_name)
    }
}


function network_displayer_network_pairs_cytoscape_callback(result_object){
    return function(html){
        
        pairs_data = get_block_data_from_page(html);
        node_list = {}
        for(var i in pairs_data){
            pair_obj = pairs_data[i]
            node_list[pair_obj["CLUSTERID1"]] = true;
            node_list[pair_obj["CLUSTERID2"]] = true;
        }
        
        node_list_array = new Array()
        for(key in node_list){
            node_list_array.push(key)
        }
        
        //Fixing values in Pairs Data, normalizing the deltaMZ
        for(i in pairs_data){
            pairs_data[i]["DeltaMZ"] = Math.abs(parseFloat(pairs_data[i]["DeltaMZ"])).toString()
        }
        
        //Adding to a global object
        result_object.node_list = node_list_array
        result_object.pairs_data = pairs_data
		
		result_object.pairsinfo_loaded = true;
        console.log("Pairs Data Ready");
		result_object.checkready()
    }
}


function sleep_start_layout(millis, render_width, render_height){
    setTimeout(function()
            { 
                cy.resize();
                width = render_width
                height = render_height
                zoom = cy.zoom()
                nodes_count = cy.nodes().size()
                liveUpdateFlag = true
                if (nodes_count > 200){
                    liveUpdateFlag = false;
                }
                options = {
                    name: 'arbor',

                    liveUpdate: liveUpdateFlag, // whether to show the layout as it's running
                    ready: undefined, // callback on layoutready 
                    stop: undefined, // callback on layoutstop
                    maxSimulationTime: 4000, // max length in ms to run the layout
                    fit: false, // reset viewport to fit default simulationBounds
                    padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
                    //simulationBounds: undefined, // [x1, y1, x2, y2]; [0, 0, width, height] by default
                    //simulationBounds: [0,0,width/zoom,height/zoom], // [x1, y1, x2, y2]; [0, 0, width, height] by default
                    boundingBox: {x1:0,y1:0,x2:width/zoom,y2:height/zoom}, // constrain layout bounds; { x1, y1, x2, y2 } or { x1, y1, w, h }
                    ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

                    // forces used by arbor (use arbor default on undefined)
                    repulsion: undefined,
                    stiffness: undefined,
                    friction: undefined,
                    gravity: true,
                    fps: undefined,
                    precision: undefined,

                    // static numbers or functions that dynamically return what these
                    // values should be for each element
                    nodeMass: undefined, 
                    edgeLength: function(data){
                        return data.score;
                    },

                    stepSize: 1.0, // size of timestep in simulation

                    // function that returns true if the system is stable to indicate
                    // that the layout can be stopped
                    stableEnergy: function( energy ){
                        var e = energy; 
                        return (e.max <= 0.5) || (e.mean <= 0.3);
                    }
                };

                
                cy.layout( options );
                cy.pan({x: 0, y: 0});
            }
    , millis);
}


function view_all_clusters_data_beta_callback_gen(result_object){
    return function(html){
        clusterinfo_data = get_block_data_from_page(html);
        result_object.clusterinfo_data = clusterinfo_data
        
        clusterinfo_map = {}
        for(var i in clusterinfo_data){
            clusterinfo_map[clusterinfo_data[i]['cluster index']] = clusterinfo_data[i]
        }
        
        result_object.clusterinfo_map = clusterinfo_map
        result_object.clusterinfo_loaded = true
        console.log("Cluster Information Loaded");
        result_object.checkready()
    }
}

function get_multipass_result_callback_network_displayer_gen(view_object){
    return function(json_data){
        peptide_results_data = JSON.parse(json_data).blockData;
        for(var i in peptide_results_data){
            scanNumber = peptide_results_data[i]["Cluster_index"]
            if (scanNumber in view_object.clusterinfo_map){
                current_peptide = peptide_results_data[i]["Peptide"]
                view_object.clusterinfo_map[scanNumber]["Peptide"] = current_peptide
                cy.$('#' + scanNumber).addClass('haslibraryidentification');
            }
        }
      
        view_object.ready_button_green()
        
        $("#importID_div").hide();
    }
}

function get_msgfdb_result_callback_network_displayer_gen(view_object){
    return function(html){
        peptide_results_data = get_block_data_from_page(html);
        for(var i in peptide_results_data){
            scanNumber = peptide_results_data[i]["SpecIndex"]
            if (scanNumber in view_object.clusterinfo_map){
                current_peptide = peptide_results_data[i]["Peptide"]
                view_object.clusterinfo_map[scanNumber]["Peptide"] = current_peptide
            }
        }
        
        //alert("MSGFDB IDs Imported");
        
        view_object.ready_button_green()
        
        $("#importID_div").hide();
    }
}

function get_pepnovo_result_callback_network_displayer_gen(view_object){
    return function(html){
        peptide_results_data = get_block_data_from_page(html);
        for(var i in peptide_results_data){
            scanNumber = peptide_results_data[i]["Scan"]
            if (scanNumber in view_object.clusterinfo_map){
                current_peptide = peptide_results_data[i]["Sequence"]
                view_object.clusterinfo_map[scanNumber]["Peptide"] = current_peptide
            }
        }
        
        //alert("Pepnovo IDs Imported");
        
        view_object.ready_button_green()
        
        $("#importID_div").hide();
    }
}


//Generic Plotter on div
ResultViewNetworkDisplayer.prototype.genericBackendPlotter = function(filename, scanNumber, peptide, div, isTopPanel) {
    var task = get_taskid();
    var type = "invoke";
    //var source  = "annotatedSpectrumImageThumbnail";
    var source  = "annotatedSpectrumImage";
    var contentType = "image/png";
    var block = 0;
    
    if(peptide == null){
        peptide = "*..*"
    }
    
    var invokeParameters = {};
    invokeParameters["file"] = "FILE->" + filename;
    invokeParameters["scan"] = + scanNumber;
    invokeParameters["peptide"] = "*..*";
    invokeParameters["force"] = "false";
    
    //displayJSSpectrumViewer(div, task, invokeParameters, 298, 500, 0);
    
    getSpectrumPeaks(task, invokeParameters, function(context, render_div, isTopPanel, scan){
        return function(peaks){
            if(isTopPanel){
                console.log("Top Panel Render");
                context.top_plot_spectrum.peaks = peaks
                context.top_plot_spectrum.scan = scan
                context.top_plot_spectrum.render_div = render_div
            }
            else{
                console.log("Bottom Panel Render");
                context.bottom_plot_spectrum.peaks = peaks
                context.bottom_plot_spectrum.scan = scan
                context.bottom_plot_spectrum.render_div = render_div
            }
        
            displayJSLibrarySpectrumViewer_Standalone(render_div, 298, 500, peaks, null, peptide);
        }
    }(this, div, isTopPanel, scanNumber))
}

ResultViewNetworkDisplayer.prototype.alignTopBottomPanel = function(){
    //Checking if both peaks are present
    if(this.top_plot_spectrum.peaks == null || this.bottom_plot_spectrum.peaks == null){
        return;
    }
    
    //peaks are present, lets do align
    spec1 = this.top_plot_spectrum.peaks
    spec2 = this.bottom_plot_spectrum.peaks
    
    pm1 = parseFloat(this.clusterinfo_map[this.top_plot_spectrum.scan]["parent mass"])
    pm2 = parseFloat(this.clusterinfo_map[this.bottom_plot_spectrum.scan]["parent mass"])
    
    if(pm2 > pm1){
        alignment_info = score_alignement(spec1, spec2, pm1, pm2, 0.5)
    }
    else{
        alignment_info = score_alignement(spec2, spec1, pm2, pm1, 0.5)
        //Swap the alignment, because we're reversing it
        for(i in alignment_info.alignments){
            alignment_info.alignments[i] = [alignment_info.alignments[i][1], alignment_info.alignments[i][0]]
        }
    }
    
    //console.log("Score: " + alignment_info.score);
    //console.log("Alignments: ")
    //console.log(alignment_info.alignments);
    
    $("#alignment_box_display").text("Score: " + alignment_info.score.toFixed(2))
    
    //Redraw figures with alignments
    //making an extra series
    extraSeries_map_top = new Object()
    extraSeries_map_bot = new Object()
    top_panel_label_peaks = []
    bot_panel_label_peaks = []
    panel_labels = []
    
    panel_labels_top = []
    panel_labels_bot = []
    
    //Alignment representative for exact and shifted
    top_panel_label_peaks_exact = []
    top_panel_label_peaks_shifted = []
    
    bot_panel_label_peaks_exact = []
    bot_panel_label_peaks_shifted = []
    
    panel_labels_top_exact = []
    panel_labels_top_shifted = []
    
    panel_labels_bot_exact = []
    panel_labels_bot_shifted = []
    
    for(i in alignment_info.alignments){
        top_panel_label_peaks.push(spec1[alignment_info.alignments[i][0]])
        bot_panel_label_peaks.push(spec2[alignment_info.alignments[i][1]])
        
        top_mz = spec1[alignment_info.alignments[i][0]][0]
        bot_mz = spec2[alignment_info.alignments[i][1]][0]
        
        delta = top_mz - bot_mz
        
        if(Math.abs(delta) > 1){
            panel_labels_top.push(i.toString() + " (" + delta.toFixed(0) + ")")
            panel_labels_bot.push(i.toString() + " (" + -delta.toFixed(0) + ")")
            
            
            //Peaks
            top_panel_label_peaks_shifted.push(spec1[alignment_info.alignments[i][0]])
            bot_panel_label_peaks_shifted.push(spec2[alignment_info.alignments[i][1]])
            
            //Labels
            panel_labels_top_shifted.push(i.toString()  + " (" + delta.toFixed(0) + ")")
            panel_labels_bot_shifted.push(i.toString()  + " (" + -delta.toFixed(0) + ")")
            
        }
        else{
            panel_labels_top.push(i.toString())
            panel_labels_bot.push(i.toString())
            
            //Peaks
            top_panel_label_peaks_exact.push(spec1[alignment_info.alignments[i][0]])
            bot_panel_label_peaks_exact.push(spec2[alignment_info.alignments[i][1]])
            
            //Labels
            panel_labels_top_exact.push(i.toString())
            panel_labels_bot_exact.push(i.toString())
        }
        
        panel_labels.push(i.toString())
    }
    //console.log(top_panel_label_peaks)
    //console.log(bot_panel_label_peaks)
    //console.log(panel_labels)
    
    extraSeries_map_top.data = top_panel_label_peaks
    extraSeries_map_top.labels = panel_labels_top
    extraSeries_map_top.color = "#99004c"
    
    extraSeries_map_bot.data = bot_panel_label_peaks
    extraSeries_map_bot.labels = panel_labels_bot
    extraSeries_map_bot.color = "#99004c"
    
    
    //Objects to pass to render for exact and shifted separate
    extraSeries_map_top_exact = new Object();
    extraSeries_map_top_shifted = new Object();
    
    extraSeries_map_top_exact.data = top_panel_label_peaks_exact
    extraSeries_map_top_exact.labels = panel_labels_top_exact
    extraSeries_map_top_exact.color = "#99004c"
    
    extraSeries_map_top_shifted.data = top_panel_label_peaks_shifted
    extraSeries_map_top_shifted.labels = panel_labels_top_shifted
    extraSeries_map_top_shifted.color = "blue"
    
    extraSeries_map_bot_exact = new Object();
    extraSeries_map_bot_shifted = new Object();
    
    extraSeries_map_bot_exact.data = bot_panel_label_peaks_exact
    extraSeries_map_bot_exact.labels = panel_labels_bot_exact
    extraSeries_map_bot_exact.color = "#99004c"
    
    extraSeries_map_bot_shifted.data = bot_panel_label_peaks_shifted
    extraSeries_map_bot_shifted.labels = panel_labels_bot_shifted
    extraSeries_map_bot_shifted.color = "blue"
    
    //displayJSLibrarySpectrumViewer_Standalone
    displayJSLibrarySpectrumViewer_Standalone(this.top_plot_spectrum.render_div, 298, 500, spec1, [extraSeries_map_top_exact, extraSeries_map_top_shifted])
    displayJSLibrarySpectrumViewer_Standalone(this.bottom_plot_spectrum.render_div, 298, 500, spec2, [extraSeries_map_bot_exact, extraSeries_map_bot_shifted])
    
    
    //If both structures are present, then ping the jsonp service
    top_structure = this.clusterinfo_map[this.top_plot_spectrum.scan].Smiles
    bot_structure = this.clusterinfo_map[this.bottom_plot_spectrum.scan].Smiles
    
    $("#structure_tani_display_area").text("");
    if(top_structure != null && bot_structure != null){
        calculateStructureSimilarity(top_structure, bot_structure, function(response){
            console.log(response)
            $("#structure_tani_display_area").text("Tani: " + response.similarity.toFixed(2))
        });
    }
}

// assign this view implementation to block type "network_displayer"
resultViewBlocks["network_displayer"] = ResultViewNetworkDisplayer;
