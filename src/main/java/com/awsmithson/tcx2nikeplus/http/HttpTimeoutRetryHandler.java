package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.util.Log;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.SocketTimeoutException;

class HttpTimeoutRetryHandler implements HttpRequestRetryHandler {
	private static final @Nonnull Log logger = Log.getInstance();

	private final int maxRetries;

	HttpTimeoutRetryHandler(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		boolean retry = (executionCount <= maxRetries) && (exception instanceof SocketTimeoutException);
		if (retry) {
			logger.out(" - retrying, %s", exception.getClass().getSimpleName());
		}
		return retry;
	}
}
