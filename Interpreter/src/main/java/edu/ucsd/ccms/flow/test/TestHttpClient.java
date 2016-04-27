package edu.ucsd.ccms.flow.test;



import edu.ucsd.saint.commons.IOUtils;

import java.io.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class TestHttpClient{

  private static String url = "http://tharkun02.ucsd.edu/MassSpecFlow/Download?task=xyz&resource=fasta";

  public static void main(String[] args) {
    HttpClient client = new DefaultHttpClient();
    HttpGet method = new HttpGet(url);
    InputStream input = null;
    try {
        HttpResponse response = client.execute(method);
        StatusLine status = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        input = entity.getContent();
      if (status.getStatusCode() != 200)
        System.out.println("Method failed: " + status.getStatusCode());
      IOUtils.dumpToFile(input, "test.tgz");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // Release the connection.
      try{ if(input != null) input.close(); }
      catch(Exception e){e.printStackTrace(); }
    }
  }
}
