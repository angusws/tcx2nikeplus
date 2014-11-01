package com.awsmithson.tcx2nikeplus.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Log {
	private static Log _context;
	private static Logger _log;

	private static final @Nonnull Predicate<StackTraceElement> CALLER_PREDICATE = new Predicate<StackTraceElement>() {
		@Override
		public boolean apply(@Nullable StackTraceElement element) {;
			return element != null && !(element.getClassName().equals(Thread.class.getName()) || element.getClassName().equals(Log.class.getName()));
		}
	};

	private Log() {
		_log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	public static synchronized Log getInstance() {
		if (_context == null)
			_context = new Log();

		return _context;
	}

	public void out(Object message) {
		out(Level.INFO, null, message.toString(), (Object[])null);
	}

	public void out(String message, Object ... args) {
		out(Level.INFO, null, message, args);
	}

	public void out(Level level, Object message) {
		out(level, null, message.toString(), (Object[])null);
	}

	public void out(Level level, String message, Object ... args) {
		out(level, null, message, args);
	}

	public void out(Throwable throwable) {
		out(Level.SEVERE, throwable, "", (Object[])null);
	}

	public void out(Level level, Throwable throwable, Object message) {
		out(level, throwable, message.toString(), (Object[])null);
	}

	public void out(Level level, Throwable throwable, String message, Object ... args) {
		if (_log.isLoggable(level)) {
			if (args != null) {
				message = String.format(message, args);
			}

			// Get the caller class and method name (this is a *disgusting* hack which I've been forced into because I've
			// mis-used java.util.logging.  Maybe I'll fix this one day, but it's not important for now.
			StackTraceElement caller = Iterables.find(Arrays.asList(Thread.currentThread().getStackTrace()), CALLER_PREDICATE);
			_log.logp(level, caller.getClassName(), caller.getMethodName(), message, throwable);
		}
	}
}
