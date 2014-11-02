package com.awsmithson.tcx2nikeplus.convert;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;

public class ConverterException extends Exception {
	public ConverterException(@Nonnull String message) {
		super(Preconditions.checkNotNull(message, "message argument is null."));
	}

	public ConverterException(String message, Throwable throwable) {
		super(Preconditions.checkNotNull(message, "message argument is null."), Preconditions.checkNotNull(throwable, "throwable argument is null."));
	}
}
