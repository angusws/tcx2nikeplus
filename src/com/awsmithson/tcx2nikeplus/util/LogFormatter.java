package com.awsmithson.tcx2nikeplus.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;


public class LogFormatter extends Formatter
{

	private static final String FORMAT_NORMAL = "%1$tF %1$tT (%2$s)\t%3$s\n";
	private static final String FORMAT_EXCEPTION = "%1$tF %1$tT (%2$s)\t%3$s\n%4$s\n";

	@Override
	public String format(LogRecord record) {

		Throwable t = record.getThrown();

		return (t == null)
			? String.format(FORMAT_NORMAL, record.getMillis(), record.getLevel(), record.getMessage())
			: String.format(FORMAT_EXCEPTION, record.getMillis(), record.getLevel(), record.getMessage(), t.toString())
		;
	}
}