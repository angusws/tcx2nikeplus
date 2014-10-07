package com.awsmithson.tcx2nikeplus.http;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.TimeZone;

public class GeonamesSlowTest {

	@Test
	public void testGetTimeZone() throws IOException, ParserConfigurationException, SAXException {
		TimeZone expected = TimeZone.getTimeZone("Europe/London");
		TimeZone actual = Geonames.getTimeZone(new BigDecimal("-3.292960999533534"), new BigDecimal("55.97914927639067"));
		Assert.assertEquals("Wrong timezone returned", expected, actual);
	}
}
