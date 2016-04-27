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
%>
<%
    String redirectURL = "/ProteoSAFe/result.jsp?task=698fc5a09db74c7492983b3673ff5bf6&view=view_aggregate_molecule_dataset#{\"table_sort_history\":\"UnidentifiedPrecursorNeighbor_dsc\"}";
    response.sendRedirect(redirectURL);

%>
