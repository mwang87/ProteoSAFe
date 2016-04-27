package edu.ucsd.liveadmin;

import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsd.saint.commons.Helpers;
import edu.ucsd.saint.commons.WebAppProps;

public class UpdateSequences {
	private static Object monitor = new Object();
	private static boolean forceUpdate = false;
	private static final Logger logger = LoggerFactory.getLogger(UpdateSequences.class);
	
	private static Thread updator = null;
	
	public static void start(){
		if(updator == null){
			updator = new Thread("Sequence Updator"){
				public void run(){
					doUpdate();
				}
			};
			Helpers.startAsDaemon(updator);
		}			
	}
	
	public static void stop(){
		if(updator != null){
			updator.interrupt();
			updator = null;
		}		
	}

	private static void doUpdate(){
		try{
			while(true){
				SequenceRepository.updateSequences();			
				Calendar now = Calendar.getInstance();
				int day = now.get(Calendar.DAY_OF_MONTH);
				int hour = now.get(Calendar.HOUR_OF_DAY);
				synchronized(monitor){
					boolean toUpdate = 
						WebAppProps.get("liveadmin.sequences.update", "false").equals("true"); 
					if(toUpdate && (forceUpdate || (day == 1 && hour == 4))){
						execUpdateScript();
						SequenceRepository.updateSequences();
						forceUpdate = false;
					}
					monitor.wait(3600000l);
				}
			}
		}
		catch(Exception e){
			logger.info("Failed to update sequences");
		}
	}		

	public static void forceUpdate(){
		synchronized(monitor){
			forceUpdate = true;
			monitor.notify();
		}
	}
	
	private static void execUpdateScript(){
		try{
			String script = WebAppProps.getPath("liveadmin.sequences.updateScript");			
			String dir = WebAppProps.getPath("liveadmin.sequences.path");

			ProcessBuilder pb = new ProcessBuilder("perl", script, dir);
			Commons.executeProcess(null, System.out, pb);
			StringBuffer buffer = new StringBuffer();
			for(String token: pb.command())
				buffer.append("[").append(token).append("] ");
			logger.info(buffer.toString()); 
		}
		catch(Exception e){
			logger.info("Failed to execute update scripts");
		}		
	}
}
