package com.awsmithson.tcx2nikeplus.convert;

import javax.annotation.Nonnull;

public interface Converter<INPUT, OUTPUT> {
	public OUTPUT convert(@Nonnull INPUT input) throws ConverterException;
}
