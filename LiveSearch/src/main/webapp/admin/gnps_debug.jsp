<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="edu.ucsd.livesearch.account.*"
    import="edu.ucsd.livesearch.servlet.ManageSharing"
    import="edu.ucsd.livesearch.servlet.ServletUtils"
    import="edu.ucsd.livesearch.storage.FileManager"    
    import="edu.ucsd.livesearch.account.AccountManager"
    import="edu.ucsd.livesearch.dataset.Dataset"
    import="edu.ucsd.livesearch.dataset.DatasetManager"
    import="edu.ucsd.livesearch.parameter.ResourceManager"
    import="edu.ucsd.livesearch.libraries.AnnotationManager"
    import="edu.ucsd.livesearch.libraries.SpectrumInfo"
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotation"
    import="edu.ucsd.livesearch.libraries.SpectrumAnnotationSet"
    import="edu.ucsd.livesearch.libraries.SpectrumRating"
    import="edu.ucsd.livesearch.libraries.SpectrumRatingManager"
    import="edu.ucsd.livesearch.task.Task"
    import="edu.ucsd.livesearch.task.TaskManager.TaskStatus"
    import="edu.ucsd.livesearch.task.TaskManager"
    import="edu.ucsd.livesearch.util.Commons"
    import="edu.ucsd.livesearch.util.FormatUtils"
    import="edu.ucsd.livesearch.subscription.SubscriptionManager"
    import="java.util.*"    
    import="org.apache.commons.lang3.StringEscapeUtils"
%>


<%!
    public static List<Dataset> get_gnps_datasets(){
        Map<Task, Dataset> tasks_map = DatasetManager.queryAllDatasets();
        
        List<Dataset> gnps_datasets = new ArrayList<Dataset>();
        
        
        for(Task task : tasks_map.keySet()){
            String description = task.getDescription();
            if(description.toUpperCase().indexOf("GNPS") != -1){
                gnps_datasets.add(tasks_map.get(task));
            }
        }
        return gnps_datasets;
    }
%>
    
<%
    List<Dataset> gnps_datasets = get_gnps_datasets();
    String output = " { \"datasets\": [";
    
    for(Dataset dataset : gnps_datasets){
        output += "{";
        output += "\"name\"" + ":" + "\"" + dataset.getDatasetID() + "\"" + ",";
        output += "}";
    }
    
    output += "] }";
    
    List<SpectrumRating> ratings = SpectrumRatingManager.Get_All_Ratings();
    List<Integer> ratings_counts = new ArrayList<Integer>();
    ratings_counts.add(0);
    ratings_counts.add(0);
    ratings_counts.add(0);
    ratings_counts.add(0);
    ratings_counts.add(0);
    
    for(SpectrumRating rating : ratings){
        ratings_counts.set(rating.getRating(), ratings_counts.get(rating.getRating()) + 1);
    }
    
    String ratings_histogram = " { \"ratings_hist\": [";
    for(int i = 0; i < ratings_counts.size(); i++){
        ratings_histogram += ratings_counts.get(i) + ", ";
    }
    ratings_histogram += "] }";
%>
<!DOCTYPE html>
<html>
<head>
    <script language="javascript" type="text/javascript">
        var dataset_data = <%= output %>;
        var rating_hist = <%= ratings_histogram %>
    </script>
</head>
<div id="bodyWrapper">
    
</div>
</body>
</html>


