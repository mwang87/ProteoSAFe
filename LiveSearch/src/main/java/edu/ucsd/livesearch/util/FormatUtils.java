package edu.ucsd.livesearch.util;

import java.util.concurrent.TimeUnit;

public class FormatUtils {

//	private static final Pattern floatPattern = Pattern.compile("[+-]?+\\d*+\\.?\\d*+");
//	private static final Pattern integerPattern = Pattern.compile("[+-]?+\\d++");	
	
//	public static boolean isEmpty(String str){
//		return str == null || str.isEmpty();
//	}
	
//	public static boolean isFloat(String value){
//		if(value == null) return false;
//		return floatPattern.matcher(value).matches();
//	}
//	
//	public static boolean isInteger(String value){
//		if(value == null) return false;		
//		return integerPattern.matcher(value).matches();
//	}
	
	public static String formatTimePeriod(long diff){
		long sec, min, hour, day;
		diff /= 1000;
		sec = diff % 60; diff /= 60;
		min = diff % 60; diff /= 60;
		hour = diff % 24;
		day = diff / 24;
		return String.format("%d day %02d:%02d:%02d", day, hour, min, sec);
	}
	
	public static String formatShortTimePeriod(long time) {
		long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(time) -
			TimeUnit.MINUTES.toSeconds(minutes);
		if (minutes > 0)
			return String.format("%d m, %d s", minutes, seconds);
		// only show whole seconds if elapsed time is 10 seconds or more
		else if (seconds >= 10)
			return String.format("%d s", seconds);
		else return String.format("%d ms", time);
	}

//	public static String quoteForJavaScript(String s){
//		if(s == null) return "''";
//		StringBuffer buffer = new StringBuffer();
//		buffer.append('\'');
//		for(int i = 0; i < s.length(); i++){
//			char c = s.charAt(i);
//			if(c == '\'' || c == '\"' || c == '\\')
//				buffer.append("\\").append(c);
//			else if ('!' <= c && c <= '~')
//				buffer.append(c);
//			else buffer.append(String.format("\\u%04X", (int)c));
//		}
//		buffer.append('\'');
//		return buffer.toString();
//	}

//	public static String wrapHtml(String word, int width){
//		StringBuffer buffer = new StringBuffer();
//		int index = 0, length = word.length();
//		for(; index + width < length; index += width)
//			buffer.append(word.substring(index, index + width)).append("<br/>");
//		if(index < length) buffer.append(word.substring(index));
//		return buffer.toString();
//	}

	public static String formatExceptoin(Exception e, String format, Object ... args){
		StackTraceElement trace = e.getStackTrace()[0];
		return String.format("%s%n\t due to exception [%s] at [%s.%s(%s:%d)]", 
			String.format(format, args),
			e.getClass().getName(), trace.getClassName(), trace.getMethodName(),
			trace.getFileName(), trace.getLineNumber());
	}

//	public static String HTMLEntityEncode(String s){
//		if (s == null)
//			return "";
//		StringBuffer buffer = new StringBuffer();
//		for (int i = 0; i < s.length(); i++) {
//			char c = s.charAt(i);
//			if (c == ' ' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '(' && c <= ':')
//				buffer.append(c);
//			else buffer.append("&#" + (int) c + ";");
//		}
//		return buffer.toString();
//	}
}
