package com.awsmithson.tcx2nikeplus.http;


import com.awsmithson.tcx2nikeplus.jaxb.JAXBObject;
import com.awsmithson.tcx2nikeplus.nike.NikePlusSyncData;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.topografix.gpx._1._1.GpxType;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;

public class NikePlusSlowTest {

	private static final @Nonnull Log logger = Log.getInstance();

	public static final @Nonnull String GPX_XML_2014_06_14 = "/nikeplus/gpxXML/2014-06-14_gpxXML-original.xml";
	private static final @Nonnull String RUN_2014_06_14 = "/nikeplus/run/2014-06-14_run-original.json";
	private static final @Nonnull String RUN_2014_06_14_MINIMAL = "/nikeplus/run/2014-06-14_run-minimal.json";

	// Load "test.properties" file.
	private static final @Nonnull Properties testProperties = new Properties();
	static {
		String propertiesFile = "/test.properties";
		logger.out(Level.FINER, "loading %s", propertiesFile);
		try (InputStream inputStream = NikePlus.class.getResourceAsStream(propertiesFile)) {
			testProperties.load(inputStream);
		}
		catch (IOException ioe) {
			throw new ExceptionInInitializerError(ioe);
		}
	}

	private final static String PROPERTY_NIKEPLUS_EMAIL = "nikeplus.email";
	private final static String PROPERTY_NIKEPLUS_PASSWORD = "nikeplus.password";

	@Test
	public void testLoginSuccess() throws IOException, ParserConfigurationException, SAXException {
		String accessToken = NikePlus.login(testProperties.getProperty(PROPERTY_NIKEPLUS_EMAIL), testProperties.getProperty(PROPERTY_NIKEPLUS_PASSWORD).toCharArray());
		Assert.assertNotNull("accessToken was null", accessToken);
		Assert.assertTrue("accessToken length was 0", accessToken.length() > 0);
		NikePlus.endSync(accessToken);
	}

	@Test
	public void testNikePlusUploadOriginalRunJson() throws IOException, JAXBException {
		Assert.assertNotNull("gpxXML file missing", getClass().getResource(GPX_XML_2014_06_14));
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_2014_06_14));
		testNikePlusUpload(getClass().getResourceAsStream(GPX_XML_2014_06_14), getClass().getResourceAsStream(RUN_2014_06_14));
	}

	@Test
	public void testNikePlusUploadMinimalRunJson() throws IOException, JAXBException {
		Assert.assertNotNull("gpxXML file missing", getClass().getResource(GPX_XML_2014_06_14));
		Assert.assertNotNull("Test file missing", getClass().getResource(RUN_2014_06_14_MINIMAL));
		testNikePlusUpload(getClass().getResourceAsStream(GPX_XML_2014_06_14), getClass().getResourceAsStream(RUN_2014_06_14_MINIMAL));
	}

	private void testNikePlusUpload(@Nonnull InputStream gpxXMLInputStream, @Nonnull InputStream runJsonInputStream) throws IOException, JAXBException {
		String accessToken = NikePlus.login(testProperties.getProperty(PROPERTY_NIKEPLUS_EMAIL), testProperties.getProperty(PROPERTY_NIKEPLUS_PASSWORD).toCharArray());
		Assert.assertNotNull("accessToken was null", accessToken);
		Assert.assertTrue("accessToken length was 0", accessToken.length() > 0);

		try {
			JsonElement runJson = new JsonParser().parse(new InputStreamReader(runJsonInputStream));
			GpxType gpxXml = JAXBObject.GPX_TYPE.unmarshall(gpxXMLInputStream);
			Assert.assertEquals("trk size incorrect", 1, gpxXml.getTrk().size());
			Assert.assertTrue("Got bad status-code from NikePlus", NikePlus.syncData(accessToken, new NikePlusSyncData(runJson, gpxXml)));
		}
		finally {
			NikePlus.endSync(accessToken);
		}
	}
}
