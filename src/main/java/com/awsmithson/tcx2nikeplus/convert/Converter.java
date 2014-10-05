package com.awsmithson.tcx2nikeplus.convert;

import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public interface Converter<INPUT, OUTPUT> {
	public OUTPUT convert(@Nonnull INPUT input) throws ParserConfigurationException, SAXException, IOException;
}
