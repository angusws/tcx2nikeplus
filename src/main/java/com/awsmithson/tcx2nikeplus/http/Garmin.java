package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.base.Preconditions;
import com.topografix.gpx._1._1.GpxType;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class Garmin {

	// Load "garmin.properties" file.
	private static final @Nonnull Properties garminProperties = new Properties();
	static {

		try (InputStream inputStream = Garmin.class.getResourceAsStream("/garmin.properties")) {
			garminProperties.load(inputStream);
		}
		catch (IOException ioe) {
			throw new ExceptionInInitializerError(ioe);
		}
	}
	
	private static final String	URL_GARMIN_ACTIVITIES = "https://connect.garmin.com/activities";
	private static final String	URL_GARMIN_SIGN_IN = "https://connect.garmin.com/signin";
	private static final String	URL_GARMIN_TCX = "https://connect.garmin.com/proxy/activity-service-1.0/tcx/activity/%d?full=true";
	private static final String	URL_GARMIN_GPX = "https://connect.garmin.com/proxy/activity-service-1.1/gpx/activity/%s?full=true";

	private static final Log log = Log.getInstance();

	private static final String GARMIN_ID_ERROR = "Invalid Garmin Activity.  Please ensure your garmin workout is not marked as private.";


	public static @Nonnull CloseableHttpClient getGarminHttpSession() throws IOException {
		CloseableHttpClient client = HttpClientBuilder.create().build();

		// 1 - Initial attempt to view activities, which will set cookies and redirect us to sign-in page.
		HttpGet get = new HttpGet(URL_GARMIN_ACTIVITIES);
		try (CloseableHttpResponse response1 = client.execute(get)) {
			EntityUtils.consume(response1.getEntity());

			// 2 - Sign-in attempt
			HttpPost post = new HttpPost(URL_GARMIN_SIGN_IN);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("login", "login"));
			nameValuePairs.add(new BasicNameValuePair("login:loginUsernameField", garminProperties.getProperty("GARMIN_USERNAME")));
			nameValuePairs.add(new BasicNameValuePair("login:password", garminProperties.getProperty("GARMIN_PASSWORD")));
			nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState", "j_id1"));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			try (CloseableHttpResponse response2 = client.execute(post)) {
				EntityUtils.consume(response2.getEntity());
			}

			return client;
		}
	}


	public static @Nonnull TrainingCenterDatabaseT downloadTcx(@Nonnull CloseableHttpClient httpClient, int garminActivityId) throws IOException, JAXBException {
		Preconditions.checkNotNull(httpClient, "httpClient argument was null.");

		HttpGet get = new HttpGet(String.format(URL_GARMIN_TCX, garminActivityId));
		try (CloseableHttpResponse response = httpClient.execute(get)) {
			JAXBContext jaxbContext = JAXBContext.newInstance(TrainingCenterDatabaseT.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			return unmarshaller.unmarshal(new StreamSource(response.getEntity().getContent()), TrainingCenterDatabaseT.class).getValue();
		}
	}


	public static @Nonnull GpxType downloadGpx(@Nonnull CloseableHttpClient httpClient, int garminActivityId) throws IOException, JAXBException {
		Preconditions.checkNotNull(httpClient, "httpClient argument was null.");

		HttpGet get = new HttpGet(String.format(URL_GARMIN_GPX, garminActivityId));
		try (CloseableHttpResponse response = httpClient.execute(get)) {
			JAXBContext jaxbContext = JAXBContext.newInstance(GpxType.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			return unmarshaller.unmarshal(new StreamSource(response.getEntity().getContent()), GpxType.class).getValue();
		}
	}






	public static Document downloadGarminTcx(HttpClient client, int garminActivityId) throws IOException {
		return downloadGarminDocument(client, URL_GARMIN_TCX, garminActivityId);
	}
	public static Document downloadGarminGpx(HttpClient client, int garminActivityId) throws IOException {
		return downloadGarminDocument(client, URL_GARMIN_GPX, garminActivityId);
	}


	private static Document downloadGarminDocument(HttpClient client, String url, int garminActivityId) throws IOException {
		url = String.format(url, garminActivityId);
		Document doc = null;

		try {
			HttpGet get = new HttpGet(String.format(url, garminActivityId));
			HttpResponse response = client.execute(get);
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.getEntity().getContent());
		}
		catch (Exception e) {
			log.out(e);
			throw new IOException(GARMIN_ID_ERROR);
		}

		if (doc == null) throw new IOException(GARMIN_ID_ERROR);

		log.out("Successfully downloaded data for garmin activity %d.", garminActivityId);
		return doc;
	}
}
