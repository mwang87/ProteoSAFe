<%@ page language="java" contentType="text/xml; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
	import="edu.ucsd.saint.commons.http.HttpParameters"
	import="edu.ucsd.saint.commons.http.HttpParameters.UploadListener"
	import="edu.ucsd.livesearch.util.FormatUtils"
%><%
	String uuid = request.getParameter("uuid");
	UploadListener upload = HttpParameters.getUploaListener(uuid);
	long elapsed = 0, bytes = 1, read = 0, remaining = 0, rate = 0, percentage = 0;
	if(upload != null){
		elapsed = upload.getElapsed();
		read = upload.getBytesRead();
		bytes = upload.getTotalBytes();
		if(bytes > 0)
			percentage = (read * 100 / bytes);
		if(elapsed > 0)
			rate = (read / 1024) * 1000 / (elapsed);
		if(rate > 0)
			remaining = ((bytes - read) / (rate * 1024)) * 1000;
	}
%><?xml version="1.0" encoding="ISO-8859-1" ?>
<status>
	<percentage><%= percentage %></percentage>
	<rate><%= rate %> </rate>
	<bytes><%= bytes %> </bytes>
	<read><%=read%> </read>
	<elapsed><%= FormatUtils.formatTimePeriod(elapsed) %> </elapsed>
	<remaining><%= FormatUtils.formatTimePeriod(remaining) %>  </remaining>
</status>
