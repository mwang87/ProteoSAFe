importScripts('/ProteoSAFe/scripts/result/rarefaction_utils.js');

onmessage = function (oEvent) {
    cluster_data = JSON.parse(oEvent.data);
    rarefaction_curve = generate_clustering_rarefaction(cluster_data);
    
    postMessage(JSON.stringify(rarefaction_curve));
};