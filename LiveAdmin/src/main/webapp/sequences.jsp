<%@ page language="java" contentType="text/xml; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="edu.ucsd.liveadmin.Commons" 
    import="edu.ucsd.liveadmin.SequenceRepository"
    import="edu.ucsd.liveadmin.SequenceFile"
    import="java.util.Collection"
%><%
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<sequences>
<%
	Collection<SequenceFile> seqs = SequenceRepository.getSequences();
	for(SequenceFile seq: seqs){
%>
	<sequence>
		<code><%= seq.getCode() %></code>
		<display><%= seq.getDisplay() %></display>
	</sequence>
<%
	}
%>
</sequences>
