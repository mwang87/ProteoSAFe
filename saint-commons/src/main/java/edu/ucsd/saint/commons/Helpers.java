package edu.ucsd.saint.commons;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

public class Helpers {
	public static String getUUID(boolean withHyphen){
		String result = UUID.randomUUID().toString();
		return withHyphen ? result : result.replaceAll("-", "");
	}

	public static void startAsDaemon(Thread thread){
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.setDaemon(true);
        thread.start();
	}
	
	public static void runSimpleProcess(Process process) {
		if (process == null)
			return;
		try {
			// gobble the stream's thread to ensure that it doesn't deadlock
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
			while ((reader.readLine()) != null) {}
			process.waitFor();
		} catch (Throwable error) {
			throw new RuntimeException(error);
		} finally {
			try { process.getInputStream().close(); } catch (Throwable error) {}
			try { process.getOutputStream().close(); } catch (Throwable error) {}
			try { process.getErrorStream().close(); } catch (Throwable error) {}
			try { process.destroy(); } catch (Throwable error) {}
		}
	}
}
