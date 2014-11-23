package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

import javax.annotation.Nonnull;

class HttpStatusCodeRetryStrategy implements ServiceUnavailableRetryStrategy {
	private static final @Nonnull Log logger = Log.getInstance();

	private final int maxRetries;
	private final int retryInterval;
	private final Predicate<HttpResponse> retryPredicate;

	HttpStatusCodeRetryStrategy(int maxRetries, int retryInterval, Predicate<HttpResponse> retryPredicate) {
		Preconditions.checkArgument(maxRetries > 0, "maxRetries argument must be greater than 0.");
		Preconditions.checkArgument(retryInterval > 0, "retryInterval argument must be greater than 0.");
		this.maxRetries = maxRetries;
		this.retryInterval = retryInterval;
		this.retryPredicate = Preconditions.checkNotNull(retryPredicate, "retryPredicate argument is null.");
	}

	@Override
	public boolean retryRequest(HttpResponse httpResponse, int executionCount, HttpContext httpContext) {
		boolean retry = (executionCount <= maxRetries) && (retryPredicate.apply(httpResponse));
		if (retry) {
			logger.out(" - retrying, response code: %d", (httpResponse != null) ? httpResponse.getStatusLine().getStatusCode() : -1);
		}
		return retry;
	}

	@Override
	public long getRetryInterval() {
		return retryInterval;
	}
}
