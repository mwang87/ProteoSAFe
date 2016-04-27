
/**
 * Result view table column handler for streaming peaks from the database
 */
var psmViewerColumnHandler = {
    render: renderPsmViewer
};

columnHandlers["chemical_spectrum_viewer"] = psmViewerColumnHandler;

function renderPsmViewer(tableId, rowId, columnId, attributes) {
    return function(td, record, index) {
        td.id = getColumnElementId(tableId, record.id, rowId, columnId);
        var cell_id = "" + tableId + "record" + record.id + "row" + rowId + "col" + columnId;
        if (attributes.colspan != null)
            td.colSpan = attributes.colspan;
        // get loader parameters
        var task = attributes.task.id;
        var invokeParameters = null;
        var parameters = attributes.parameters;
        if (parameters != null) {
            invokeParameters = {};
            for (var parameter in parameters)
                invokeParameters[parameter] =
                    resolveFields(parameters[parameter], record);
        }
        var contentType = attributes.contentType;
        var panelHeight = this.panelHeight;
        var block = attributes.blockId;
        // set up on-demand loader function
        var columnLoader = function() {
            removeChildren(td);

            displayPsmViewer(
                td, task, invokeParameters, 600, 300, cell_id);
        };
        // if this column is already loaded, just invoke the loader function
        if (tableManager.isColumnLoaded(tableId, record.id, rowId, columnId))
            columnLoader();
        // otherwise, assign the loader function to this record,
        // so that it can be invoked when the column is loaded
        else tableManager.setColumnLoader(
            tableId, record.id, rowId, columnId, columnLoader);
    }
}



function displayPsmViewer(div, task, parameters, panelWidth, panelHeight, cell_id){
    //var url = "/ProteoSAFe/SpectrumCommentServlet?SpectrumID=" + parameters.spectrumid

    if (parameters != null) {
        for (var parameter in parameters) {
            if (parameter == "jsonID") {
                var jsonID = parameters["jsonID"];
            }
        }
    }

    removeChildren(div);
    if (jsonID != 'None') {
        var child = document.createElement("img");
        child.src = "/ProteoSAFe/images/inProgress.gif";
        div.appendChild(child);

        var url = "/ProteoSAFe/DownloadResultFile?fetchStaticFile&task=" + task + "&file=jsons/" + jsonID + ".json";
        $.ajax({
            url: url,
            cache: true,
            success: function(render_div, width, height){
                return function(response){
                    removeChildren(render_div);

                    psm_details = JSON.parse(response);
                    var mol = ChemDoodle.readMOL(psm_details.molfile);

                    var child = document.createElement("div");
                    child.id = "child" + cell_id;
                    render_div.appendChild(child);

                    $("#" + child.id).specview_dereplicator({
                                    peaks2: psm_details.peaks,
                                    allPeaks2: psm_details.allpeaks,
                                    massErrors2: psm_details.masserrors,
                                    spectrum2: psm_details.spectrum,
                                    mol: mol,
                                    atom_list: psm_details.atomlist,
                                    peptide_bonds: psm_details.peptidebonds,
                                    width: width,
                                    height: height
                    });
                }
            }(div, panelWidth, panelHeight),
            failure: function(render_div){
                return function(){
                    var text_label = document.createElement("p");
                    text_label.innerHTML = "Error loading PSM visualization";
                    render_div.appendChild(text_label);
                }
            }(div)
        });
    } else {
        var text_label = document.createElement("p");
        text_label.innerHTML = "Visualization for this PSM is not available";
        text_label.align = "left";
        div.appendChild(text_label);
    }
}
