package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.jaxb.JAXBObject;
import com.awsmithson.tcx2nikeplus.nike.NikeActivityData;
import com.awsmithson.tcx2nikeplus.nike.NikePlusSyncData;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.topografix.gpx._1._1.ObjectFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.SetCookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;


public class NikePlus {

	// Load "nikeplus.properties" file.
	private static final @Nonnull Properties nikePlusProperties = new Properties();
	static {
		try (InputStream inputStream = NikePlus.class.getResourceAsStream("/nikeplus.properties")) {
			nikePlusProperties.load(inputStream);
		}
		catch (IOException ioe) {
			throw new ExceptionInInitializerError(ioe);
		}
	}

	private static final @Nonnull String URL_LOGIN_DOMAIN = "secure-nikeplus.nike.com";
	private static final @Nonnull String URL_LOGIN = String.format("https://%s/login/loginViaNike.do?mode=login", URL_LOGIN_DOMAIN);
	private static final @Nonnull String URL_DATA_SYNC = "https://api.nike.com/v2.0/me/sync?access_token=%s";
	private static final @Nonnull String URL_DATA_SYNC_COMPLETE_ACCESS_TOKEN = "https://api.nike.com/v2.0/me/sync/complete";

	private static final @Nonnull String USER_AGENT = "NPConnect";

	private static final int URL_DATA_SYNC_SUCCESS = HttpStatus.SC_OK;

	// The "URL_DATA_SYNC" nike+ service seems to intermittently return 503, HttpComponents can deal with that nicely.
	private static @Nonnull HttpStatusCodeRetryStrategy DATA_SYNC_RETRY_STRATEGY = new HttpStatusCodeRetryStrategy(5, 200, new Predicate<HttpResponse>() {
		@Override
		public boolean apply(@Nullable HttpResponse httpResponse) {
			return (httpResponse == null) || (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE);
		}
	});



	static @Nonnull UrlEncodedFormEntity generateFormNameValuePairs(@Nonnull String ... inputKeyValues) throws UnsupportedEncodingException {
		Preconditions.checkNotNull(inputKeyValues, "inputKeyValues argument is null.");
		int inputLength = inputKeyValues.length;
		Preconditions.checkArgument(inputLength > 0, "No input key/values specified.");
		Preconditions.checkArgument((inputLength % 2) == 0, String.format("Odd number of name-value pairs: %d.", inputLength));

		List<NameValuePair> formParams = new ArrayList<>();
		for (int i = 0; i < inputLength;) {
			formParams.add(new BasicNameValuePair(inputKeyValues[i++], inputKeyValues[i++]));
		}

		return new UrlEncodedFormEntity(formParams, "UTF-8");
	}


	private static @Nonnull SetCookie createCookie(@Nonnull String key, @Nonnull String value) {
		SetCookie cookie = new BasicClientCookie(key, value);
		cookie.setPath("/");
		cookie.setDomain(URL_LOGIN_DOMAIN);
		return cookie;
	}

	/**
	 * Performs a login to nike+, returning the nike+ access_token.
	 * @param nikeEmail The users nike+ email address.
	 * @param nikePassword The users nike+ password.
	 * @return nike+ access_token..
	 * @throws IOException If we are unable to successfully authenticate with Nike+.
	 */
	public static @Nonnull String login(@Nonnull String nikeEmail, @Nonnull char[] nikePassword) throws IOException {
		Preconditions.checkNotNull(nikeEmail, "nikeEmail argument is null.");
		Preconditions.checkNotNull(nikePassword, "nikePassword argument is null.");

		// Create CookieStore for the nikeEmail request.
		CookieStore cookieStore = new BasicCookieStore();
		cookieStore.addCookie(createCookie("app", nikePlusProperties.getProperty("NIKEPLUS_APP")));
		cookieStore.addCookie(createCookie("client_id", nikePlusProperties.getProperty("NIKEPLUS_CLIENT_ID")));
		cookieStore.addCookie(createCookie("client_secret", nikePlusProperties.getProperty("NIKEPLUS_CLIENT_SECRET")));

		// Create the HttpClient, setting the cookie store.
		try (CloseableHttpClient client = HttpClientBuilder.create()
				.setDefaultCookieStore(cookieStore)
				.build()) {

			// Create the HttpPost, set the user-agent and nike+ credentials.
			HttpPost post = new HttpPost(URL_LOGIN);
			post.addHeader("user-agent", USER_AGENT);
			post.setEntity(generateFormNameValuePairs("email", nikeEmail, "password", new String(nikePassword)));

			// Send the HTTP request.
			HttpClientContext httpClientContext = HttpClientContext.create();
			logger.out("Authenticating against Nike+");
			try (CloseableHttpResponse response = client.execute(post, httpClientContext)) {
				HttpEntity httpEntity = response.getEntity();
				// Get the response and iterate through the cookies for "access_token".
				if (httpEntity != null) {
					for (Cookie cookie : httpClientContext.getCookieStore().getCookies()) {
						if (cookie.getName().equals("access_token")) {
							return cookie.getValue();
						}
					}
				} else {
					throw new IOException("Http response empty");
				}
			}
		}

		// If we reach here, we haven't got an access-token back for whatever reason.
		// Throw IOException with this (rather crude) error message.
		throw new IOException("Unable to authenticate with Nike+.<br />Please check email and nikePassword.<br /><br />" +
				"The Nike+ service I connect to seems to not like certain complex characters in the nikePassword, so if you have issues please try simplifying your nikePassword on their website.  " +
				"Apologies, but this is out of my control.");
	}

	/**
	 * Perform a full synchronisation cycle (check-pin-status, sync, end-sync) with nike+ for the given credentials and garmin activities.
	 * @param nikeEmail The users nike+ email address.
	 * @param nikePassword The users nike+ password.
	 * @param nikeActivitiesData Nike activities data to upload.
	 * @throws IOException If there was a problem communicating with nike+.
	 */
	@Deprecated
	public static void fullSync(@Nonnull String nikeEmail, @Nonnull char[] nikePassword, @Nonnull NikeActivityData... nikeActivitiesData) throws IOException {
		Preconditions.checkNotNull(nikeEmail, "nikeEmail argument is null.");
		Preconditions.checkNotNull(nikePassword, "nikePassword argument is null.");
		Preconditions.checkNotNull(nikeActivitiesData, "garminActivitiesData argument is null.");

		logger.out("Uploading to Nike+...");
		logger.out(" - Authenticating...");
		String nikeAccessToken = login(nikeEmail, nikePassword);
		fullSync(nikeAccessToken, nikeActivitiesData);
	}

	@Deprecated
	public static void fullSync(@Nonnull String nikeAccessToken, @Nonnull NikeActivityData... nikeActivitiesData) throws IOException {
		try {
			logger.out(" - Syncing data...");
			for (NikeActivityData nikeActivityData : nikeActivitiesData) {
				if (!syncData(nikeAccessToken, nikeActivityData)) {
					throw new IOException("There was a problem uploading to nike+.  Please try again later, if the problem persists contact me with details of the activity-id or tcx file.");
				}
			}
		}
		finally {
			logger.out(" - Ending sync...");
			endSync(nikeAccessToken);
		}
	}

	@Deprecated
	private static boolean syncData(@Nonnull String accessToken, @Nonnull NikeActivityData nikeActivityData) throws IOException {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(String.format(URL_DATA_SYNC, accessToken));
			post.addHeader("user-agent", USER_AGENT);
			post.addHeader("appid", "NIKEPLUSGPS");

			// Add "runXML" data to the request.
			MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
					.addPart("runXML", new SpoofFileBody(Util.documentToString(nikeActivityData.getRunXML()), "runXML.xml"));

			// If we have GPX data, add it to the request.
			if (nikeActivityData.getGpxXML() != null) {
				multipartEntityBuilder.addPart("gpxXML", new SpoofFileBody(Util.documentToString(nikeActivityData.getGpxXML()), "gpxXML.xml"));
			}

			post.setEntity(multipartEntityBuilder.build());
			try (CloseableHttpResponse response = client.execute(post)) {
				int statusCode = response.getStatusLine().getStatusCode();
				return (URL_DATA_SYNC_SUCCESS == statusCode);
			}
		}
	}

	public static boolean syncData(@Nonnull String accessToken, @Nonnull NikePlusSyncData... nikePlusSyncDatas) throws IOException, JAXBException {
		Preconditions.checkNotNull(accessToken, "accessToken argument is null.");
		Preconditions.checkNotNull(nikePlusSyncDatas, "nikePlusSyncDatas argument is null.");
		Preconditions.checkArgument(nikePlusSyncDatas.length > 0, "No nikePlusSyncData to sync");

		// TODO: we must return some object which details which workouts succeeded/failed.
		boolean success = true;

		for (NikePlusSyncData nikePlusSyncData : nikePlusSyncDatas) {
			try (CloseableHttpClient client = HttpClientBuilder
					.create()
					.setServiceUnavailableRetryStrategy(DATA_SYNC_RETRY_STRATEGY)
					.build()) {
				HttpPost post = new HttpPost(String.format(URL_DATA_SYNC, accessToken));
				post.addHeader("user-agent", USER_AGENT);
				post.addHeader("appid", "NIKEPLUSGPS");

				try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
					JAXBObject.GPX_TYPE.marshal(new ObjectFactory().createGpx(nikePlusSyncData.getGpxXML()), byteArrayOutputStream);

					HttpEntity httpEntity = MultipartEntityBuilder.create()
							.addPart("run", new StringBody(new Gson().toJson(nikePlusSyncData.getRunJson()), ContentType.APPLICATION_JSON))
							.addBinaryBody("gpxXML", byteArrayOutputStream.toByteArray(), ContentType.TEXT_PLAIN, null)
							.build();
					post.setEntity(httpEntity);

					logger.out("Posting to Nike+ (max retries on 503 response: %d)", serviceUnavailableMaxRetries);
					try (CloseableHttpResponse response = client.execute(post)) {
						int statusCode = response.getStatusLine().getStatusCode();
						logger.out(" - Nike+ sync response: %s - %s", statusCode, EntityUtils.toString(response.getEntity()));
						if (statusCode != URL_DATA_SYNC_SUCCESS) {
							success = false;
						}
					}
				}
			}
		}

		return success;
	}

	public static void endSync(@Nonnull String accessToken) throws IOException {
		Preconditions.checkNotNull(accessToken, "accessToken argument is null.");

		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost post = new HttpPost(String.format("%s?%s", URL_DATA_SYNC_COMPLETE_ACCESS_TOKEN, Util.generateHttpParameter("access_token", accessToken)));
			post.addHeader("user-agent", USER_AGENT);
			post.addHeader("appId", "NIKEPLUSGPS");

			try (CloseableHttpResponse response = client.execute(post)) {
				HttpEntity httpEntity = response.getEntity();
				if (httpEntity != null) {
					try (InputStream inputStream = httpEntity.getContent()) {
						Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
						outDoc.normalize();
						logger.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
					} catch (ParserConfigurationException | SAXException e) {
						logger.out(e);
					}
				} else {
					throw new NullPointerException("Http response empty");
				}
			}
		}
	}
}
