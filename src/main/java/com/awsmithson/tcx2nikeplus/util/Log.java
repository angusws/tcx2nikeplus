package com.awsmithson.tcx2nikeplus.util;

import java.util.logging.Level;
import java.util.logging.Logger;


public class Log {

	private static Log _context;
	private static Logger _log;
	

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


	public void out(Throwable t) {
		out(Level.SEVERE, t, "", (Object[])null);
	}

	public void out(Level level, Throwable t, Object message) {
		out(level, t, message.toString(), (Object[])null);
	}

	public void out(Level level, Throwable t, String message, Object ... args) {
		if (args != null) message = String.format(message, args);
		_log.log(level, message, t);
	}
}
