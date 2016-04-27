package edu.ucsd.livesearch.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.livesearch.account.AccountManager;
import edu.ucsd.livesearch.libraries.AnnotationManager;
import edu.ucsd.livesearch.libraries.SpectrumAnnotation;
import edu.ucsd.livesearch.libraries.SpectrumInfo;
import edu.ucsd.livesearch.result.parsers.TabularResult;
import edu.ucsd.livesearch.task.Task;
import edu.ucsd.livesearch.task.TaskManager;
import edu.ucsd.saint.commons.WebAppProps;


public class SplashServlet extends BaseServlet{

	private Logger logger = LoggerFactory.getLogger(LibraryServlet.class);
	//Getting the Annotations and Comments
	@Override
	protected void doGet(
		HttpServletRequest request, HttpServletResponse response
	) throws ServletException, IOException {
        String splash_block1 = request.getParameter("splash_block1");
        String splash_block2 = request.getParameter("splash_block2");
        String splash_block3 = request.getParameter("splash_block3");

        List<SpectrumInfo> spectra = AnnotationManager.QuerySpectrumSplash(splash_block1, splash_block2, splash_block3);

        StringBuilder render_string_builder = new StringBuilder();
	    render_string_builder.append("{ \"spectra\": [");
	    int spectrum_count = 0;
	    for(SpectrumInfo spec : spectra){
	    	spectrum_count++;


	        if(spectrum_count < spectra.size()){
                render_string_builder.append(spec.toJSON() + ",");
	        }
	        else{
	        	render_string_builder.append(spec.toJSON());
	        }
	    }
	    render_string_builder.append("]}");
	    String render_string = render_string_builder.toString();

	    response.getOutputStream().print(render_string);

	}



}
