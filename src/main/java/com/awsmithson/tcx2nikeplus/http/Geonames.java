package com.awsmithson.tcx2nikeplus.http;


import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.google.common.base.Preconditions;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.TimeZone;
import java.util.logging.Level;

public class Geonames {

	private static final @Nonnull Log logger = Log.getInstance();
	private static final @Nonnull String URL_TIMEZONE = "http://ws.geonames.org/timezone?username=tcx2nikeplus_app&lat=%.4f&lng=%.4f";

	private static final @Nonnull BigDecimal MIN_LONGITUDE_DEGREES = new BigDecimal("-180");
	private static final @Nonnull BigDecimal MAX_LONGITUDE_DEGREES = new BigDecimal("180");
	private static final @Nonnull BigDecimal MIN_LATITUDE_DEGREES = new BigDecimal("-90");
	private static final @Nonnull BigDecimal MAX_LATITUDE_DEGREES = new BigDecimal("90");

	public static @Nonnull	TimeZone getTimeZone(@Nonnull BigDecimal longitudeDegrees, @Nonnull BigDecimal latitudeDegrees) throws IOException, ParserConfigurationException, SAXException {
		Preconditions.checkNotNull(longitudeDegrees, "longitudeDegrees argument is null.");
		Preconditions.checkNotNull(latitudeDegrees, "latitudeDegrees argument is null.");
		ensureInRange(longitudeDegrees, MIN_LONGITUDE_DEGREES, MAX_LONGITUDE_DEGREES, "longitudeDegrees");
		ensureInRange(latitudeDegrees, MIN_LATITUDE_DEGREES, MAX_LATITUDE_DEGREES, "latitudeDegrees");

		logger.out("Looking up timezone for lon: %.4f, lat: %.4f", longitudeDegrees, latitudeDegrees);
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(String.format(URL_TIMEZONE, latitudeDegrees, longitudeDegrees));

			try (CloseableHttpResponse response = client.execute(post)) {
				logger.out(Level.FINE, " - response code: %d", response.getStatusLine().getStatusCode());
				HttpEntity httpEntity = response.getEntity();
				if (httpEntity != null) {
					try (InputStream inputStream = httpEntity.getContent()) {
						Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
						document.normalize();
						logger.out(Level.FINER, "\t%s", Util.documentToString(document));

						String timeZoneId = Util.getSimpleNodeValue(document, "timezoneId");
						logger.out(" - lon: %.4f, lat: %.4f = %s", longitudeDegrees, latitudeDegrees, timeZoneId);
						return TimeZone.getTimeZone(timeZoneId);
					}
				} else {
					throw new NullPointerException("Http response empty");
				}
			}
		}
	}

	private static void ensureInRange(@Nonnull BigDecimal value, @Nonnull BigDecimal minAcceptable, @Nonnull BigDecimal maxAcceptable, @Nonnull String argumentName) {
		Preconditions.checkArgument(value.compareTo(minAcceptable) > 0, "%s argument [%s] must be >= %s.", argumentName, value, minAcceptable);
		Preconditions.checkArgument(value.compareTo(maxAcceptable) < 0, "%s argument [%s] must be <= %s.", argumentName, value, maxAcceptable);
	}
}
