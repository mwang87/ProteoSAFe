<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.dataset.Dataset"
%>

<%
    String massive_id = request.getParameter("massiveid");
    String massive_task_id = null;
    Dataset massive_dataset = null;
    if(massive_id != null){
        massive_dataset = DatasetManager.queryDatasetByID(massive_id);
    }
    
    if(massive_dataset != null){
        //Checking if dataset is private
        if(!massive_dataset.isPrivate()){
            massive_task_id = massive_dataset.getTaskID();
        }
    }
    
    String redirect_url = "";
    
    if(massive_task_id == null){
        redirect_url = "${livesearch.logo.link}";
    }
    else{
        redirect_url = "/ProteoSAFe/result.jsp?task=" + massive_task_id + "&view=advanced_view";
    }
    
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
        var redirect_url = "<%= redirect_url %>";
        window.location.replace(redirect_url);
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