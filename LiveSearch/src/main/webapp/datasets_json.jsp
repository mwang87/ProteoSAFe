<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
	import="edu.ucsd.livesearch.account.AccountManager"
	import="edu.ucsd.livesearch.dataset.Dataset"
	import="edu.ucsd.livesearch.dataset.DatasetManager"
	import="edu.ucsd.livesearch.parameter.ResourceManager"
	import="edu.ucsd.livesearch.servlet.ServletUtils"
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
	/*========================================================================
	 * Convenience methods
	 *========================================================================*/
	private static String resolveCVLabels(
		String list, String resource, Map<String, String> cache
	) {
		if (list == null)
			return "";
		StringBuffer resolved = new StringBuffer("");
		Collection<String> labels =
			ResourceManager.getCVResourceLabels(list, resource, cache);
		boolean first = true;
		for (String label : labels) {
			// prepend <hr> tag if this isn't the first item
			if (first == false)
				resolved.append(
					StringEscapeUtils.escapeJson("<hr class='separator'/>"));
			resolved.append(StringEscapeUtils.escapeJson(label));
			first = false;
		}
		return resolved.toString();
	}
%>
<%
	Map<Task, Dataset> datasets;
	// determine the identity of the requesting user and the request type
	String identity = (String)session.getAttribute("livesearch.user");
	boolean isAdmin =
		AccountManager.getInstance().checkRole(identity, "administrator");
	String target = request.getParameter("user");
	// fetch datasets accordingly
	if (isAdmin && target != null && target.isEmpty() == false)
		datasets = (target.equals("all")) ?
			DatasetManager.queryAllDatasets() :
			DatasetManager.queryOwnedDatasets(target);
	else datasets = DatasetManager.queryDatasetsByPrivacy(false);
	// obtain dataset metadata CV resource maps
	Map<String, String> species = new LinkedHashMap<String, String>();
	Map<String, String> instrument = new LinkedHashMap<String, String>();
	Map<String, String> modification = new LinkedHashMap<String, String>();
	// update CV fields appropriately
	for (Dataset dataset : datasets.values()) {
		dataset.setSpecies(resolveCVLabels(
			dataset.getSpecies(), "species", species));
		dataset.setInstrument(resolveCVLabels(
			dataset.getInstrument(), "instrument", instrument));
		dataset.setModification(resolveCVLabels(
			dataset.getModification(), "modification", modification));
		if (dataset.getPI() == null)
			dataset.setPI("");
	}
	
	List<Integer> dataset_subscriptions = new ArrayList<Integer>();
    String dataset_subscription_string = "[";
    if(identity != null){
            dataset_subscriptions = SubscriptionManager.get_all_user_subscriptions(identity);
            for(Integer dataset : dataset_subscriptions){
                    dataset_subscription_string += "\"" + dataset + "\",";
            }
    }
    dataset_subscription_string += "]";

%>
{ "datasets" : <%= DatasetManager.getDatasetTaskListJSON(datasets) %> }
