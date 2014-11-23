package com.awsmithson.tcx2nikeplus.http;

import javax.annotation.Nonnull;
import java.io.IOException;

public class HttpException extends IOException {

	private final @Nonnull int statusCode;

	public HttpException(@Nonnull String message, @Nonnull int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}

	public @Nonnull int getStatusCode() {
		return statusCode;
	}
}
