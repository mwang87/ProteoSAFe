<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
        import="edu.ucsd.livesearch.parameter.ResourceManager"
        import="edu.ucsd.livesearch.servlet.DownloadWorkflowInterface"
        import="edu.ucsd.livesearch.servlet.ManageParameters"
        import="edu.ucsd.livesearch.servlet.ServletUtils"
        import="edu.ucsd.livesearch.storage.FileManager"
        import="edu.ucsd.livesearch.storage.SequenceRepository"
        import="edu.ucsd.livesearch.storage.SequenceFile"
        import="edu.ucsd.livesearch.task.Task"
        import="edu.ucsd.livesearch.task.TaskManager"
        import="edu.ucsd.livesearch.util.AminoAcidUtils.PTM"
        import="edu.ucsd.livesearch.util.AminoAcidUtils.PTMType"
        import="edu.ucsd.livesearch.util.Commons"
        import="edu.ucsd.livesearch.account.AccountManager"
%>
<%
    String user = (String)session.getAttribute("livesearch.user");


%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
    <link href="styles/main.css" rel="stylesheet" type="text/css" />
    <link href="styles/datasetnetwork.css" rel="stylesheet" type="text/css" />
    <link href="images/favicon.ico" rel="shortcut icon" type="image/icon" />
    <script src="scripts/util.js" language="javascript" type="text/javascript"></script>
    
    
    <!-- Result view rendering scripts -->
    <script src="/ProteoSAFe/scripts/render.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/stream.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/updateResource.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/mingplugin.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/massivepage.js?5" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/powerview_columnhandlers.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/spectrumpage.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/parameterlink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/absolutelink.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/molecular_dataset_linkout.js?2" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/fileViewLinkList.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/continuous_id_rating.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/continuous_id_rating_column_display.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/network_displayer.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>

    
    <!-- Third-party utility scripts -->
    <!-- Third-party utility scripts -->
    <script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="http://ajax.googleapis.com/ajax/libs/jqueryui/1.8.4/jquery-ui.min.js" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.min.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/cytoscape.js-panzoom.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/cytoscape.js/arbor.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery/jquery.raty.js" language="javascript" type="text/javascript"></script>
    <link href="styles/jquery/jquery.raty.css" rel="stylesheet" type="text/css" />
    
    
    <script language="javascript" type="text/javascript">
        function init() {
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>
            
            
            //Javascript For Summary
            table_header = document.createElement("h2");
            table_header.innerHTML = "Dataset Network";
            $("#maindisplay").append(table_header);
            
            network_div = document.createElement("div");
            network_div.id = "networkcontainer"
            $("#maindisplay").append(network_div);
            
            //Adding font awesome CSS
            var link = document.createElement('link')
            link.setAttribute('rel', 'stylesheet')
            link.setAttribute('type', 'text/css')
            link.setAttribute('href', "//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css")
            document.getElementsByTagName('head')[0].appendChild(link)
            
            context = new Object();
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/result.jsp",
                data: { task: "698fc5a09db74c7492983b3673ff5bf6", view: "view_aggregate_dataset_network"},
                cache: false,
                async: false,
                success: function(context){
                    return function(html){
                        results_data = get_block_data_from_page(html);
                        context.network_data = results_data;
                        
                        //Splitting out the unique nodes
                        dataset_names_map = new Object()
                        for(i = 0; i < results_data.length; i++){
                            dataset_id1 = parseInt(results_data[i].Dataset1.substring(7));
                            dataset_id2 = parseInt(results_data[i].Dataset2.substring(7));
                            dataset_names_map[dataset_id1] = 1
                            dataset_names_map[dataset_id2] = 1
                        }
                        
                        dataset_names = Object.keys(dataset_names_map)
                        context.dataset_names = dataset_names
                    }
                }(context)
            });
            
            
            //Initializing the network
            
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
                        
                        console.log("Cytoscape Rendering Data");
                        
                        
                        filtered_nodes_map = new Object()
                        for(i in context.network_data){
                            if(parseInt(context.network_data[i].SharedCompounds) < 10){
                                continue;
                            }
                            
                            dataset_id1 = parseInt(context.network_data[i].Dataset1.substring(7));
                            dataset_id2 = parseInt(context.network_data[i].Dataset2.substring(7));
                            filtered_nodes_map[dataset_id1] = 1
                            filtered_nodes_map[dataset_id2] = 1   
                        }
                        filtered_nodes = Object.keys(filtered_nodes_map)
                        
                        
                        for(name in filtered_nodes){
                            cy.add({
                                group: "nodes",
                                data: { id: filtered_nodes[name], name: filtered_nodes[name] },
                                position: { x:  0, y:  0 }
                            });
                        }
                        
                        //adding pairs
                        edge_index = 0
                        for(i in context.network_data){
                            if(parseInt(context.network_data[i].SharedCompounds) < 10){
                                continue;
                            }
                            
                            edge_index += 1
                            cy.add({
                                group: "edges",
                                data: { id: "e_" + edge_index.toString(), 
                                        source: parseInt(context.network_data[i].Dataset1.substring(7)), 
                                        target: parseInt(context.network_data[i].Dataset2.substring(7)), edgecolor: "#bbb", edgedisplay: "", score: parseInt(context.network_data[i].SharedCompounds)}
                            });
                        }
                        
                        
                        cy.panningEnabled(true)
                        cy.userPanningEnabled(true)
                        cy.zoomingEnabled(true)
                        cy.userZoomingEnabled(true)
                        cy.boxSelectionEnabled(false)
                        cy.elements().unselectify()
                        
                        cy.on('tap', 'node', function(e){
                            var node = e.cyTarget; 
                            var neighborhood = node.neighborhood().add(node);
                            
                            cy.elements().addClass('faded');
                            neighborhood.removeClass('faded');
                        });
                        
                        cy.on('tap', function(e){
                            if( e.cyTarget === cy ){
                                cy.elements().removeClass('faded');
                            }
                        });
                        
                        sleep_start_layout(200, 1000, 800)
                    }
                }(context)
            });
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
        
    </script>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <title>Dataset Network</title>
</head>
<body onload="init()">
<div id="bodyWrapper">
    <a href="${livesearch.logo.link}"><div id="logo"></div></a>
    
    <div id="maindisplay"></div>
    
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
