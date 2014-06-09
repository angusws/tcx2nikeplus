package com.awsmithson.tcx2nikeplus.http;


import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class NikePlusSlowTest {

	// Load "test.properties" file.
	private static final @Nonnull Properties testProperties;
	static {
		testProperties = new Properties();
		try (InputStream inputStream = NikePlus.class.getResourceAsStream("/test.properties")) {
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
		NikePlus nikePlus = new NikePlus();
		String accessToken = nikePlus.login(testProperties.getProperty(PROPERTY_NIKEPLUS_EMAIL), testProperties.getProperty(PROPERTY_NIKEPLUS_PASSWORD).toCharArray());
		Assert.assertNotNull("accessToken was null", accessToken);
		Assert.assertTrue("accessToken length was 0", accessToken.length() > 0);
	}


}
