package com.awsmithson.tcx2nikeplus.http;

import com.google.common.base.Predicate;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HttpClients {

	// The "URL_DATA_SYNC" nike+ service seems to intermittently return 503, HttpComponents can deal with that nicely.
	private static final @Nonnull HttpStatusCodeRetryStrategy DEFAULT_RETRY_STRATEGY = new HttpStatusCodeRetryStrategy(10, 200, new Predicate<HttpResponse>() {
		@Override
		public boolean apply(@Nullable HttpResponse httpResponse) {
			return (httpResponse == null) || (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE);
		}
	});

	public static HttpClientBuilder createDefaultHttpClientBuilder() {
		return HttpClientBuilder
				.create()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectTimeout(2500)
						.setConnectionRequestTimeout(2500)
						.setSocketTimeout(2500)
						.build())
				.setRetryHandler(new HttpTimeoutRetryHandler(10))
				.setServiceUnavailableRetryStrategy(DEFAULT_RETRY_STRATEGY);
	}
}
