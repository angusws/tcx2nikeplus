package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.garmin.GarminDataType;
import com.awsmithson.tcx2nikeplus.util.Log;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;

@Deprecated
public class Garmin {

	private static final Log log = Log.getInstance();

	private static final String GARMIN_ID_ERROR = "Unable to download garmin activity.  Please ensure your garmin workout is not marked as private.<br /><br />" +
			"If the workout was recorded on a non-garmin device and you have an original GPX file, please upload that using the 'GPX File' checkbox.";


	@Deprecated
	public static Document downloadGarminTcx(CloseableHttpClient client, int garminActivityId) throws IOException {
		return downloadAndCreateDocument(client, garminActivityId, GarminDataType.TCX);
	}

	@Deprecated
	public static Document downloadGarminGpx(CloseableHttpClient httpClient, int garminActivityId) throws IOException {
		return downloadAndCreateDocument(httpClient, garminActivityId, GarminDataType.GPX);
	}

	@Deprecated
	private static Document downloadAndCreateDocument(@Nonnull CloseableHttpClient httpClient, int garminActivityId, @Nonnull GarminDataType garminDataType) throws IOException {
		try (CloseableHttpResponse response = garminDataType.executeGarminHttpRequest(httpClient, garminActivityId)) {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.getEntity().getContent());
		} catch (IOException | ParserConfigurationException | SAXException | URISyntaxException e) {
			log.out(e);
			throw new IOException(GARMIN_ID_ERROR);
		}
	}
}
