package com.awsmithson.tcx2nikeplus.http;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;

public class GeonamesFastTest {

    @Test
    public void testGetTimeZoneIllegalArguments() throws IOException, ParserConfigurationException, SAXException {
        // Longitude - minimum
        try {
            Geonames.getTimeZone(new BigDecimal("-180.00001"), new BigDecimal("0"));
            Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("longitudeDegrees argument [-180.00001] must be >= -180.", iae.getMessage());
        }

        // Longitude - maximum
        try {
            Geonames.getTimeZone(new BigDecimal("180.00001"), new BigDecimal("0"));
            Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("longitudeDegrees argument [180.00001] must be <= 180.", iae.getMessage());
        }

        // Latitude - minimum
        try {
            Geonames.getTimeZone(new BigDecimal("0"), new BigDecimal("-90.00001"));
            Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("latitudeDegrees argument [-90.00001] must be >= -90.", iae.getMessage());
        }

        // Latitude - maximum
        try {
            Geonames.getTimeZone(new BigDecimal("0"), new BigDecimal("90.00001"));
            Assert.fail(String.format("Expected %s", IllegalArgumentException.class.getCanonicalName()));
        } catch (IllegalArgumentException iae) {
            Assert.assertEquals("latitudeDegrees argument [90.00001] must be <= 90.", iae.getMessage());
        }
    }
}
