package com.awsmithson.tcx2nikeplus.garmin;

import com.awsmithson.tcx2nikeplus.http.HttpClients;
import com.awsmithson.tcx2nikeplus.http.HttpException;
import com.awsmithson.tcx2nikeplus.jaxb.JAXBObject;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;

public enum GarminDataType {
	TCX {
		private static final @Nonnull String DOWNLOAD_URL = "https://connect.garmin.com/modern/proxy/download-service/export/tcx/activity/%d";

		@Override
		@Nonnull String getDownloadUrlStringFormat() {
			return DOWNLOAD_URL;
		}

		@Override
		@Nonnull JAXBObject getJAXBObject() {
			return JAXBObject.TRAINING_CENTER_DATABASE;
		}
	},
	GPX {
		private static final @Nonnull String DOWNLOAD_URL = "https://connect.garmin.com/modern/proxy/download-service/export/gpx/activity/%s";

		@Override
		@Nonnull String getDownloadUrlStringFormat() {
			return DOWNLOAD_URL;
		}

		@Override
		@Nonnull JAXBObject getJAXBObject() {
			return JAXBObject.GPX_TYPE;
		}
	};

	private static final @Nonnull Log logger = Log.getInstance();
	private static final @Nonnull String URL_ACTIVITIES = "https://connect.garmin.com/activities";
	private static final @Nonnull String URL_SIGN_IN = "http://connect.garmin.com/en-US/signin";

	private static final int GARMIN_DOWNLOAD_SUCCESS = HttpStatus.SC_OK;
	private static final int GARMIN_DOWNLOAD_FORBIDDEN = HttpStatus.SC_FORBIDDEN;

	// Load "garmin.properties" file.
	private static final @Nonnull Properties garminProperties = new Properties();
	static {
		String propertiesFile = "/garmin.properties";
		logger.out(Level.FINER, "loading %s", propertiesFile);
		try (InputStream inputStream = GarminDataType.class.getResourceAsStream(propertiesFile)) {
			garminProperties.load(inputStream);
		}
		catch (IOException ioe) {
			throw new ExceptionInInitializerError(ioe);
		}
	}

	// Garmin login form entity.
	private static final @Nonnull UrlEncodedFormEntity GARMIN_LOGIN_FORM_ENGITY = createGarminLoginFormEntity();
	private static @Nonnull UrlEncodedFormEntity createGarminLoginFormEntity() {
		try {
			return new UrlEncodedFormEntity(
					ImmutableList.of(
							new BasicNameValuePair("login", "login"),
							new BasicNameValuePair("login:loginUsernameField", garminProperties.getProperty("GARMIN_USERNAME")),
							new BasicNameValuePair("login:password", garminProperties.getProperty("GARMIN_PASSWORD")),
							new BasicNameValuePair("javax.faces.ViewState", "j_id1")
					)
			);
		} catch (UnsupportedEncodingException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	abstract @Nonnull String getDownloadUrlStringFormat();
	abstract @Nonnull JAXBObject getJAXBObject();


	public static @Nonnull CloseableHttpClient getGarminHttpSession() throws IOException {
		logger.out("Opening garmin HTTP session");

		CloseableHttpClient client = HttpClients.createDefaultHttpClientBuilder().build();

		// 1 - Initial attempt to view activities, which will set cookies and redirect us to sign-in page.
		logger.out(Level.FINER," - getting cookie");
		HttpGet get = new HttpGet(URL_ACTIVITIES);
		try (CloseableHttpResponse response1 = client.execute(get)) {
			EntityUtils.consumeQuietly(response1.getEntity());
			logger.out(Level.FINE, "   - response code: %d", response1.getStatusLine().getStatusCode());

			// 2 - Sign-in attempt
			logger.out(Level.FINE, " - signing in");
			HttpPost post = new HttpPost(URL_SIGN_IN);
			post.setEntity(GARMIN_LOGIN_FORM_ENGITY);
			try (CloseableHttpResponse response2 = client.execute(post)) {
				EntityUtils.consumeQuietly(response2.getEntity());
				logger.out(Level.FINE, "   - response code: %d", response2.getStatusLine().getStatusCode());
			}

			logger.out(" - opened garmin HTTP session");
			return client;
		}
	}

	public @Nonnull CloseableHttpResponse executeGarminHttpRequest(@Nonnull CloseableHttpClient httpClient, int activityId) throws IOException, URISyntaxException {
		Preconditions.checkNotNull(httpClient, "httpClient argument is null.");

		URI uri = generateDownloadUri(activityId);
		logger.out("Executing garmin HTTP request: %s", uri);
		HttpGet get = new HttpGet(uri);
		CloseableHttpResponse closeableHttpResponse = httpClient.execute(get);

		int statusCode = closeableHttpResponse.getStatusLine().getStatusCode();
		logger.out(Level.FINE, " - response code: %d", statusCode);
		if (closeableHttpResponse.getEntity() == null) {
			throw new IOException(String.format("Response entity is null for %s", uri));
		} else if (statusCode == GARMIN_DOWNLOAD_FORBIDDEN) {
			throw new HttpException(String.format("Unable to download data for garmin activity-id %d.<br /></br />Please ensure your garmin activity is not marked as private.", activityId), statusCode);
		} if (statusCode != GARMIN_DOWNLOAD_SUCCESS) {
			throw new HttpException(String.format("Unable to download data for garmin activity-id %d.<br /></br />%s returned HTTP status code %d.", activityId, uri.toString(), statusCode), statusCode);
		}

		return closeableHttpResponse;
	}


	public @Nonnull <T> T downloadAndUnmarshall(@Nonnull CloseableHttpClient httpClient, int activityId) throws JAXBException, IOException, URISyntaxException {
		Preconditions.checkNotNull(httpClient, "httpClient argument is null.");

		try (CloseableHttpResponse response = executeGarminHttpRequest(httpClient, activityId)) {
			try (InputStream inputStream = response.getEntity().getContent()) {
				T output = unmarshall(inputStream);
				EntityUtils.consumeQuietly(response.getEntity());
				return output;
			}
		}
	}

	public @Nonnull <T> T unmarshall(@Nonnull InputStream inputStream) throws JAXBException {
		logger.out("Unmarshalling InputStream to %s", getJAXBObject().name());
		return getJAXBObject().unmarshall(inputStream);
	}

	@Nonnull URI generateDownloadUri(int activityId) throws URISyntaxException {
		return new URI(String.format(getDownloadUrlStringFormat(), activityId));
	}
}
