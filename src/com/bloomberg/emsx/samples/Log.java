package com.bloomberg.emsx.samples;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	
	public enum LogLevels {
		NONE,
		BASIC,
		DETAILED
	}
	
	public static LogLevels logLevel=LogLevels.NONE;
	
	public static void LogMessage(LogLevels level, String str) {
	
		if(logLevel==LogLevels.NONE) return;
		if(level==LogLevels.DETAILED && logLevel==LogLevels.BASIC) return;
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		Date date = new Date();
		System.out.println(dateFormat.format(date) + "\t" + str);
		
	}

}
