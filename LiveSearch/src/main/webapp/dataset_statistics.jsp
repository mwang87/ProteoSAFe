<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
%>

<?xml version="1.0" encoding="ISO-8859-1" ?>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <title>UCSD/CCMS - MassIVE Dataset Statistics - Mass Spectrometry Repository Dataset List</title>
    <link href="styles/main.css" rel="stylesheet" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/icon"/>
    
    <!-- General ProteoSAFe scripts -->
    <script src="scripts/form.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/render.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/util.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/result.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/result/table.js" language="javascript" type="text/javascript"></script>
    
    <!-- Help text tooltip scripts -->
    <script src="scripts/tooltips/balloon.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/balloon.config.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/box.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/tooltips/yahoo-dom-event.js" language="javascript" type="text/javascript"></script>
    <script src="scripts/jquery/jquery-1.10.2.min.js" language="javascript" type="text/javascript"></script>
    
    <!-- Special script code pertaining exclusively to this page -->
    <script language="javascript" type="text/javascript">
        function init(){
            <%= ServletUtils.JSLogoBlock("logo", request, session) %>
            
            $.ajax({
                type: "GET",
                url: "/ProteoSAFe/MassiveServlet",
                data: { 
                'function': 'massivestatistics'
                },
                success: function(result){
                    console.log(result);
                    var results_data = JSON.parse(result)['user_dataset_count'].reverse();
                    
                    var table = $('<table></table>').addClass('foo');
                    for(i=0; i<results_data.length; i++){
                        var row = $('<tr></tr>').addClass('bar').text(results_data[i]['username'] +  " " + results_data[i]['count']);
                        table.append(row);
                    }

                    $('#stats_div').append(table);
                }
            });
        }
        
        
        
    </script>
</head>
<body onload="init()">
<div id="bodyWrapper">
    <a href="${livesearch.logo.link}"><div id="logo"></div></a>
    <div id="textWrapper">
        <h4><a href="index.jsp">Back to main page</a>&nbsp;</h4>
        <div id="stats_div"></div>
    </div>
</div>

<!-- Column selector form -->
<div class="helpbox" id="hColumnSelector" style="left:-5000px;">
    <form name="columnSelector" method="get" action="">
        <table id="columnSelector">
            <tr>
                <td/>
                <td>
                    <input value="Submit" type="button"
                        onclick="evaluateColumns(this.form);"/>
                </td>
            </tr>
        </table>
    </form>
</div>
<jsp:include page="/filtered/analytics.jsp"/>
</body>
</html>