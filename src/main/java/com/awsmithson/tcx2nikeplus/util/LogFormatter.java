package com.awsmithson.tcx2nikeplus.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


public class LogFormatter extends Formatter {
	private static final String FORMAT_NORMAL = "%1$tFT%1$tTZ [thread-%2$03d] %3$-9s %4$-55s %5$s\n";
	private static final String FORMAT_EXCEPTION = "%s\n";

	@Override
	public String format(LogRecord record) {
		@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
		Throwable throwable = record.getThrown();

		String level = String.format("(%s)", record.getLevel());
		String caller = String.format("%s.%s", record.getSourceClassName().replace("com.awsmithson.tcx2nikeplus.", "..."), record.getSourceMethodName());

		StringBuilder output = new StringBuilder(String.format(FORMAT_NORMAL, record.getMillis(), record.getThreadID(), level, caller, record.getMessage()));

		if (throwable != null) {
			StringWriter sw = new StringWriter();
			throwable.printStackTrace(new PrintWriter(sw));
			output.append(String.format(FORMAT_EXCEPTION, sw));
		}

		return output.toString();
	}
}
