package com.awsmithson.tcx2nikeplus.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


public class LogFormatter extends Formatter
{

	private static final String FORMAT_NORMAL = "%1$tF %1$tT (%2$s) [thread-%3$d]\t%4$s\n";
	private static final String FORMAT_EXCEPTION = "%1$tF %1$tT (%2$s) [thread-%3$d]\t%4$s\n%4$s\n";

	@Override
	public String format(LogRecord record) {
		Throwable t = record.getThrown();
		if (t != null) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			return String.format(FORMAT_EXCEPTION, record.getMillis(), record.getLevel(), record.getThreadID(), record.getMessage(), sw.toString());
		}
		else return String.format(FORMAT_NORMAL, record.getMillis(), record.getLevel(), record.getThreadID(), record.getMessage());
	}
}
