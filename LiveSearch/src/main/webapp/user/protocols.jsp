<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.*"
    import="edu.ucsd.livesearch.servlet.ManageSharing"
    import="edu.ucsd.livesearch.servlet.ManageParameters"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.storage.FileManager"    
    import="edu.ucsd.livesearch.account.AccountManager"
    import="edu.ucsd.livesearch.dataset.Dataset"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.libraries.AnnotationManager"
    import="edu.ucsd.livesearch.libraries.SpectrumInfo"
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotation"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="java.util.*"    
    import="org.apache.commons.lang3.StringEscapeUtils"
%>

<%
    String identity = (String)session.getAttribute("livesearch.user");
    boolean isAdmin = AccountManager.getInstance().checkRole(identity, "administrator");
    String protocol_json = "";
    if(identity == null){
        
    }
    else{
        List<String> protocols = ManageParameters.getOwnedProtocols(identity);
        protocol_json = "[";
        for(String protocol : protocols){
            protocol_json += "{" + "\"protocol_name\" : \"" + protocol + "\"},";
        }
        protocol_json += "]";
    }
%>


<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>UCSD/CCMS - User Summary</title>
    <link href="/ProteoSAFe/styles/main.css" rel="stylesheet" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>
    
    <!-- General ProteoSAFe scripts -->
    <script src="/ProteoSAFe/scripts/form.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/render.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/util.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/generic_dynamic_table.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/misc_column_render_widgets.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/result/js_column_spectrum_viewer.js" language="javascript" type="text/javascript"></script>
    
    <!-- Help text tooltip scripts -->
    <script src="/ProteoSAFe/scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="/ProteoSAFe/scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    <script src="https://platform.twitter.com/widgets.js" language="javascript" type="text/javascript"></script>
    
    
    <script src="/ProteoSAFe/scripts/result/table_ming.js" language="javascript" type="text/javascript"></script>
    
    
    <script language="javascript" type="text/javascript">
        var protocols = <%= protocol_json %>;
        
        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>
            
            content_div = document.createElement("div")
            $("#textWrapper").append(content_div)
            create_protocols_table(content_div)
        }
        
        function create_protocols_table(div){
            child_table = document.createElement("div");
            div.appendChild(child_table);
            
            var task = new Object();
            task.id = "1234";
            task.workflow = "My Protocols";
            task.description = "My Protocols";
            var generic_table = new ResultViewTableGen(my_protocol_tableXML(), "myprotocols", task, 0);
            generic_table.setData(protocols);
            generic_table.render(child_table, 0);
        }
        
        function my_protocol_tableXML(){
            var tableXML_str = '<block id="dataset_compounds" type="table" pagesize="15"> \
                                    <row>  \
                                        <column field="protocol_name" label="Protocol Name" type="text" width="5"/> \
                                        <column label="Delete Protocol" type="deleteprotocolwidget" width="5"> \
                                            <parameter name="protocol_name" value="[protocol_name]"/>\
                                        </column>\
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
        
        //Custom Column Widget
        function renderDeleteProtocol(tableId, rowId, columnId, attributes) {
            return function(td, record, index) {
                parameters = resolve_parameters_to_map(attributes.parameters, record)
                
                protocol_name = parameters["protocol_name"]
                
                
                remove_button = document.createElement("BUTTON");
                remove_button.id = makeRandomString(10)
                remove_button.innerHTML = "Delete Protocol"
                remove_button.style.width = "150px"
                
                remove_button.onclick = function(protocol_name){
                    return function(){
                        $.ajax({
                            method: 'post',
                            url: "/ProteoSAFe/ManageParameters",
                            data: { operation: "delete", protocol: protocol_name},
                            success: function(response){
                                location.reload();
                            }
                        });
                    }
                }(protocol_name)
                
                td.appendChild(remove_button)
            }
        }



        var deleteProtocolDisplayer = {
            render: renderDeleteProtocol
        };

        columnHandlers["deleteprotocolwidget"] = deleteProtocolDisplayer;
        
        
    </script>
    
    
</head>
<body onload="init()">
<div id="bodyWrapper">
<%--      <a href="${livesearch.logo.link}"><div id="logo"></div></a>  --%>
    <a href="../index.jsp"><div id="logo"></div></a>
    
    <div id="titleHeader" style="text-align: justify;margin-left:auto; margin-right:auto">
        <h1>
            User Protocol Page
        </h1>
    </div>
    
    <div id="textWrapper">
        
    </div>
    <br>
    <br>
    <br>
    <br>
    <br>
    <br>
</div>

<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>
