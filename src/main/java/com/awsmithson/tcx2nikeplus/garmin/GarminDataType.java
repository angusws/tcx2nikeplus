package com.awsmithson.tcx2nikeplus.garmin;


import com.awsmithson.tcx2nikeplus.util.Log;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.topografix.gpx._1._1.GpxType;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;

public enum GarminDataType {
	TCX {
		private static final @Nonnull String DOWNLOAD_URL = "https://connect.garmin.com/proxy/activity-service-1.0/tcx/activity/%d?full=true";

		// JAXBContext and unmarshaller.
		private final JAXBContext JAXB_CONTEXT = createTrainingCenterDatabaseJAXBContect();
		private @Nonnull JAXBContext createTrainingCenterDatabaseJAXBContect() {
			try {
				return JAXBContext.newInstance(TrainingCenterDatabaseT.class);
			} catch (JAXBException je) {
				throw new ExceptionInInitializerError(je);
			}
		}
		private final @Nonnull ThreadLocal<Unmarshaller> UNMARSHALLER = new ThreadLocal<Unmarshaller>() {
			protected synchronized Unmarshaller initialValue() {
				try {
					return JAXB_CONTEXT.createUnmarshaller();
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};


		@Override
		@Nonnull String getDownloadUrlStringFormat() {
			return DOWNLOAD_URL;
		}

		@Override
		@Nonnull Unmarshaller getUnmarshaller() {
			return UNMARSHALLER.get();
		}

		@Override
		@Nonnull Class getUnmarshalledClass() {
			return TrainingCenterDatabaseT.class;
		}
	},
	GPX {
		private static final @Nonnull String DOWNLOAD_URL = "https://connect.garmin.com/proxy/activity-service-1.1/gpx/activity/%s?full=true";

		// JAXBContext and unmarshaller.
		private final JAXBContext JAXB_CONTEXT = createGpxTypeJAXBContect();
		private @Nonnull JAXBContext createGpxTypeJAXBContect() {
			try {
				return JAXBContext.newInstance(GpxType.class);
			} catch (JAXBException je) {
				throw new ExceptionInInitializerError(je);
			}
		}
		private final @Nonnull ThreadLocal<Unmarshaller> UNMARSHALLER = new ThreadLocal<Unmarshaller>() {
			protected synchronized Unmarshaller initialValue() {
				try {
					return JAXB_CONTEXT.createUnmarshaller();
				} catch (JAXBException e) {
					throw new ExceptionInInitializerError(e);
				}
			}
		};

		@Override
		@Nonnull String getDownloadUrlStringFormat() {
			return DOWNLOAD_URL;
		}


		@Override
		@Nonnull Unmarshaller getUnmarshaller() {
			return UNMARSHALLER.get();
		}

		@Override
		@Nonnull Class getUnmarshalledClass() {
			return GpxType.class;
		}
	};



	private static final @Nonnull Log logger = Log.getInstance();
	private static final @Nonnull String URL_ACTIVITIES = "https://connect.garmin.com/activities";
	private static final @Nonnull String URL_SIGN_IN = "https://connect.garmin.com/signin";

	// Load "garmin.properties" file.
	private static final @Nonnull Properties garminProperties = new Properties();
	static {
		try (InputStream inputStream = GarminDataType.class.getResourceAsStream("/garmin.properties")) {
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
	abstract @Nonnull Unmarshaller getUnmarshaller();
	abstract @Nonnull Class getUnmarshalledClass();


	public static @Nonnull CloseableHttpClient getGarminHttpSession() throws IOException {
		logger.out(Level.FINE, "Opening garmin HTTP session");

		CloseableHttpClient client = HttpClientBuilder.create().build();

		// 1 - Initial attempt to view activities, which will set cookies and redirect us to sign-in page.
		HttpGet get = new HttpGet(URL_ACTIVITIES);
		try (CloseableHttpResponse response1 = client.execute(get)) {
			EntityUtils.consume(response1.getEntity());

			// 2 - Sign-in attempt
			HttpPost post = new HttpPost(URL_SIGN_IN);
			post.setEntity(GARMIN_LOGIN_FORM_ENGITY);
			try (CloseableHttpResponse response2 = client.execute(post)) {
				EntityUtils.consume(response2.getEntity());
			}

			return client;
		}
	}

	public @Nonnull <T> T unmarshall(@Nonnull InputStream inputStream) throws JAXBException {
		Preconditions.checkNotNull(inputStream, "inputStream argument is null.");

		//noinspection unchecked
		return (T) getUnmarshaller().unmarshal(new StreamSource(inputStream), getUnmarshalledClass()).getValue();
	}

	public @Nonnull CloseableHttpResponse executeGarminHttpRequest(@Nonnull CloseableHttpClient httpClient, int activityId) throws IOException, URISyntaxException {
		Preconditions.checkNotNull(httpClient, "httpClient argument is null.");

		URI uri = generateDownloadUri(activityId);
		logger.out(Level.INFO, "Executing garmin HTTP request: %s", uri);
		HttpGet get = new HttpGet(uri);
		CloseableHttpResponse closeableHttpResponse = httpClient.execute(get);
		if (closeableHttpResponse.getEntity() == null) {
			throw new IOException(String.format("Response entity is null for %s", uri));
		}
		return closeableHttpResponse;
	}

	@Nonnull <T> T downloadAndUnmarshall(@Nonnull CloseableHttpClient httpClient, int activityId) throws JAXBException, IOException, URISyntaxException {
		Preconditions.checkNotNull(httpClient, "httpClient argument is null.");

		try (CloseableHttpResponse response = executeGarminHttpRequest(httpClient, activityId)) {
			try (InputStream inputStream = response.getEntity().getContent()) {
				return unmarshall(inputStream);
			}
		}
	}

	@Nonnull URI generateDownloadUri(int activityId) throws URISyntaxException {
		return new URI(String.format(getDownloadUrlStringFormat(), activityId));
	}
}
