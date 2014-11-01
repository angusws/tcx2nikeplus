package com.awsmithson.tcx2nikeplus.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


public class LogFormatter extends Formatter
{

	private static final String FORMAT_NORMAL = "%1$tFT%1$tTZ [thread-%2$d] (%3$s) %4$s.%5$s:\t%6$s\n";
	private static final String FORMAT_EXCEPTION = "%1$tFT%1$tTZ [thread-%2$d] (%3$s) %4$s.%5$s:\t%6$s\n%7$s\n";

	@Override
	public String format(LogRecord record) {


		@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
		Throwable throwable = record.getThrown();
		if (throwable != null) {
			StringWriter sw = new StringWriter();
			throwable.printStackTrace(new PrintWriter(sw));
			return String.format(FORMAT_EXCEPTION, record.getMillis(), record.getThreadID(), record.getLevel(), record.getSourceClassName(), record.getSourceMethodName(), record.getMessage(), sw.toString());
		} else {
			return String.format(FORMAT_NORMAL, record.getMillis(), record.getThreadID(), record.getLevel(), record.getSourceClassName(), record.getSourceMethodName(), record.getMessage());
		}
	}
}
