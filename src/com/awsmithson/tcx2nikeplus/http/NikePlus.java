package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.google.gson.stream.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;


public class NikePlus {
	
	// TODO: Tidy up exception-handling... java 7?
	private static Properties nikePlusProperties;
	static {
		nikePlusProperties = new Properties();
		InputStream in = null;
		try {
			in = NikePlus.class.getResourceAsStream("/nikeplus.properties");
			nikePlusProperties.load(in);
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		finally {
			try {
				if (in != null) in.close();
			}
			catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}
	
	private static final String URL_LOGIN = String.format("https://api.nike.com/nsl/v2.0/user/login?client_id=%s&client_secret=%s&app=%s",
			nikePlusProperties.getProperty("NIKEPLUS_CLIENT_ID"),
			nikePlusProperties.getProperty("NIKEPLUS_CLIENT_SECRET"),
			nikePlusProperties.getProperty("NIKEPLUS_APP"));
	private static final String URL_DATA_SYNC = "https://api.nike.com/v2.0/me/sync?access_token=%s";
	private static final String URL_DATA_SYNC_COMPLETE_ACCESS_TOKEN = "https://api.nike.com/v2.0/me/sync/complete";
	
	private static final String USER_AGENT = "NPConnect";

	private static final int URL_DATA_SYNC_SUCCESS = 200;
	
	private static final Log log = Log.getInstance();

	private String _accessToken;
	
	public NikePlus() {
	}


	private UrlEncodedFormEntity generateFormNVPs(String... nvps) throws UnsupportedEncodingException {
		int length = nvps.length;
		if ((length % 2) != 0) throw new IllegalArgumentException(String.format("Odd number of name-value pairs: %d", length));

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		for (int i = 0; i < length;) {
			formparams.add(new BasicNameValuePair(nvps[i++], nvps[i++]));
		}
		UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formparams, "UTF-8");
		
		return formEntity;
	}
	
	
	/**
	 * Logins into Nike+, setting the access_token, expires_in, refresh_token and pin
	 * @param login The login String (email address).
	 * @param password The login password.
	 * @return Nike+ pin.
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws UnsupportedEncodingException
	 */
	public void login(String login, char[] password) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {		
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

		try {
			//HttpPost post = new HttpPost(String.format(URL_LOGIN, prop.getProperty("NIKEPLUS_CLIENT_ID"), prop.getProperty("NIKEPLUS_CLIENT_SECRET"), prop.getProperty("NIKEPLUS_APP")));
			HttpPost post = new HttpPost(URL_LOGIN);
			post.addHeader("user-agent", USER_AGENT);
			post.setEntity(generateFormNVPs("email", login, "password", new String(password)));
	
			HttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			if (entity != null) {				
				// Parse a json response, looking for a "pin" value.
				JsonReader reader = null;
				InputStream responseContent = entity.getContent();
				try {
					reader = new JsonReader(new InputStreamReader(responseContent, "UTF-8"));
					reader.setLenient(true);
					reader.beginObject();
					while (reader.hasNext()) {
						//String name = reader.nextName();
						//log.out(Level.FINEST, name);
						//if (reader.nextName().equals("pin")) return reader.nextString();
						
						String name = reader.nextName();
						//log.out(Level.INFO, "name: %s", name);
						if (name.equals("access_token")) _accessToken = reader.nextString();
						else reader.skipValue();
					}
					reader.endObject();
				}
				catch (IllegalStateException ise ) {}	// Hack for when a json structure is not returned - will throw IllegalArgException below.
				finally {
					if (reader != null) reader.close();
				}

				// If we reach here, we haven't got an access-token back for whatever reason.
				if (_accessToken == null) throw new IllegalArgumentException("Unable to authenticate with nike+.<br />Please check email and password.");
			}
			else throw new NullPointerException("Http response empty");
		}
		finally {
			client.getConnectionManager().shutdown();
		}
	}

	/**
	 * Calls fullSync converting the File objects to Document.
	 * @param pin Nike+ pin.
	 * @param runXml Nike+ workout xml.
	 * @param gpxXml Nike+ gpx xml.
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public void fullSync(String nikeEmail, char[] nikePassword, File runXml, File gpxXml) throws ParserConfigurationException, SAXException, IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		fullSync(nikeEmail, nikePassword, db.parse(runXml), ((gpxXml != null) ? db.parse(gpxXml) : null));
	}
	
	private void fullSync(String nikeEmail, char[] nikePassword, Document runXml, Document gpxXml) throws KeyManagementException, MalformedURLException, NoSuchAlgorithmException, IOException, ParserConfigurationException, SAXException {
		fullSync(nikeEmail, nikePassword, new Document[] { runXml }, ((gpxXml != null) ? new Document[] { gpxXml } : null));
	}


	/**
	 * Does a full synchronisation cycle (check-pin-status, sync, end-sync) with nike+ for the given credentials and xml document(s).
	 * @param pin Nike+ pin.
	 * @param runXml Nike+ workout xml array.
	 * @param gpxXml Nike+ gpx xml array.
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public void fullSync(String nikeEmail, char[] nikePassword, Document[] runXml, Document[] gpxXml) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, NoSuchAlgorithmException, KeyManagementException {
		
		log.out("Uploading to Nike+...");
		log.out(" - Authenticating...");
		login(nikeEmail, nikePassword);
		
		try {		
			/*
			if (_accessToken == null) {
				log.out(" - Checking pin status...");
				checkPinStatus(pin);
			}
			*/
			
			log.out(" - Syncing data...");
			//boolean haveGpx = ((gpxXml != null) && (gpxXml.length == runXml.length));
			boolean error = false;
			int activitiesLength = runXml.length;
			
			for (int i = 0; i < activitiesLength; ++i) {
				log.out("   - Syncing: %d", (i+1));
				
				// Upload
				//log.out(Util.documentToString(runXml[i]));
				/*
				Document nikeResponse = (haveGpx) 
					? syncDataGps(runXml[i], gpxXml[i]) 
					: syncDataNonGps(runXml[i])
				;
				*/
				//Document nikeResponse = syncDataGps(runXml[i], gpxXml[i]);
				
				// Validate nike response.
				//error |= isUploadSuccess(nikeResponse);
				
				Document gpx = (gpxXml != null) ? gpxXml[i] : null;
				
				// If syncDataGps returns false - ensure we error.
				error |= (!syncData(runXml[i], gpx));
			}
			
			if (error) throw new RuntimeException("There was a problem uploading to nike+.  Please try again later, if the problem persists contact me with details of the activity-id or tcx file.");
		}
		finally {
			log.out(" - Ending sync...");
			/*
			if (_accessToken != null) endSyncAccessToken(_accessToken);
			else {
				Document nikeResponse = endSyncPin(pin);
				log.out((Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS))
					? " - End sync successful."
					: String.format(" - End sync failed: %s", Util.documentToString(nikeResponse))
				);
			}
			*/
			endSync();
		}
	}
	
	/*
	private boolean isUploadSuccess(Document nikeResponse) throws ParserConfigurationException, SAXException, IOException {
		return (!(Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS)));
	}
	*/
	
	/*
	private void checkPinStatus(String pin) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);

		HttpPost post = new HttpPost(String.format("%s?%s", URL_CHECK_PIN_STATUS, Util.generateHttpParameter("pin", pin)));
		post.addHeader("user-agent", USER_AGENT);

		try {
			HttpResponse response = client.execute(post);		
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
				EntityUtils.consume(entity);
				outDoc.normalize();
				log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
	
				String pinStatus = Util.getSimpleNodeValue(outDoc, "pinStatus");
				
				if (!(pinStatus.equals("confirmed")))
					throw new IllegalArgumentException("The PIN supplied is not valid");
			}
			else throw new NullPointerException("Http response empty");
		}
		finally {
			client.getConnectionManager().shutdown();
		}
	}
	*/
	
	/*
	private Document syncDataNonGps(String pin, Document doc) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {

		String data = Util.documentToString(doc);

		// Send data
		URL url = new URL(URL_DATA_SYNC);
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);
		conn.setRequestProperty("pin", URLEncoder.encode(pin, "UTF-8"));
		conn.setRequestProperty("content-type", "text/xml");
		conn.setRequestProperty("content-length", String.valueOf(data.length()));
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();

		// Get the response
		Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
		wr.close();
		
		outDoc.normalize();
		log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
		
		return outDoc;
	}
	*/

	private boolean syncData(Document runXml, Document gpxXml) throws ClientProtocolException, IOException, IllegalStateException, SAXException, ParserConfigurationException {		
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

		try {
			HttpPost post = new HttpPost(String.format(URL_DATA_SYNC, _accessToken));
			post.addHeader("user-agent", USER_AGENT);
			post.addHeader("appid", "NIKEPLUSGPS");
			//post.setEntity(generateFormNVPs("access_token", _accessToken));
			
			// Add run data to the request.
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
			reqEntity.addPart("runXML", new SpoofFileBody(Util.documentToString(runXml), "runXML.xml"));
			if (gpxXml != null) reqEntity.addPart("gpxXML", new SpoofFileBody(Util.documentToString(gpxXml), "gpxXML.xml"));
			post.setEntity(reqEntity);
	
			HttpResponse response = client.execute(post);
			return (URL_DATA_SYNC_SUCCESS == response.getStatusLine().getStatusCode());
			
			/*
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
				EntityUtils.consume(entity);
				outDoc.normalize();
				//log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
				return outDoc;
			}
			else throw new NullPointerException("Http response empty");
			*/
			/*
			if (entity != null) {				
				// Parse a json response, looking for a "pin" value.
				JsonReader reader = null;
				try {
					reader = new JsonReader(new InputStreamReader(entity.getContent(), "UTF-8"));
					reader.setLenient(true);
					reader.beginObject();
					while (reader.hasNext()) {
						//String name = reader.nextName();
						//log.out(Level.FINEST, name);
						//if (reader.nextName().equals("pin")) return reader.nextString();
						
						String name = reader.nextName();
						log.out(Level.INFO, "name: %s", name);
						reader.skipValue();
					}
					reader.endObject();
				}
				finally {
					if (reader != null) reader.close();
				}

				// If we reach here, we haven't got an access-token back for whatever reason.
				if (_accessToken == null) throw new IllegalArgumentException("Unable to authenticate with nike+.");
			}
			else throw new NullPointerException("Http response empty");
			*/
		}
		finally {
			client.getConnectionManager().shutdown();
		}
		
		
		
		/*
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);

		HttpPost post = new HttpPost(URL_DATA_SYNC);
		post.addHeader("user-agent", "NPConnect");
		post.addHeader("pin", URLEncoder.encode(pin, "UTF-8"));
		
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
		reqEntity.addPart("runXML", new SpoofFileBody(Util.documentToString(runXml), "runXML.xml"));
		if (gpxXml != null) reqEntity.addPart("gpxXML", new SpoofFileBody(Util.documentToString(gpxXml), "gpxXML.xml"));
		post.setEntity(reqEntity);

		try {
			HttpResponse response = client.execute(post);		
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
				EntityUtils.consume(entity);
				outDoc.normalize();
				log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
				return outDoc;
			}
			else throw new NullPointerException("Http response empty");
		}
		finally {
			client.getConnectionManager().shutdown();
		}
		*/
	}


	private Document endSync() throws ClientProtocolException, IOException, IllegalStateException, SAXException, ParserConfigurationException {
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);

		try {
			//HttpPost post = new HttpPost(URL_DATA_SYNC_COMPLETE_ACCESS_TOKEN);
			HttpPost post = new HttpPost(String.format("%s?%s", URL_DATA_SYNC_COMPLETE_ACCESS_TOKEN, Util.generateHttpParameter("access_token", _accessToken)));
			//post.addHeader("user-agent", "com.nike.nikeplus-gps/7660 (unknown, iPhone OS 6.0.1, iPhone, Scale/2.000000)");
			post.addHeader("user-agent", USER_AGENT);
			post.addHeader("appId", "NIKEPLUSGPS");
			//post.addHeader("Accept-Encoding", "gzip, deflate");
			//post.addHeader("Accept-Language", "en-GB, en, fr, de, ja, nl, it, es, pt, pt-PT, da, fi, nb, sv, ko, zh-Hans, zh-Hant, ru, pl, tr, uk, ar, hr, cs, el, he, ro, sk, th, id, ms, ca, hu, vi, en-us;q=0.8");
			//post.addHeader("Accept", "application/json");

			//post.setEntity(generateFormNVPs("access_token", _accessToken));
			
			HttpResponse response = client.execute(post);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
				EntityUtils.consume(entity);
				outDoc.normalize();
				log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
				return outDoc;
			}
			else throw new NullPointerException("Http response empty");
		}
		finally {
			client.getConnectionManager().shutdown();
		}
	}

	/*
	private Document endSyncPin(final String pin) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);

		HttpPost post = new HttpPost(String.format("%s?%s", URL_DATA_SYNC_COMPLETE_PIN, Util.generateHttpParameter("pin", pin)));
		post.addHeader("user-agent", USER_AGENT);

		try {
			HttpResponse response = client.execute(post);		
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
				EntityUtils.consume(entity);
				outDoc.normalize();
				log.out(Level.FINER, "\t%s", Util.documentToString(outDoc));
				return outDoc;
			}
			else throw new NullPointerException("Http response empty");
		}
		finally {
			client.getConnectionManager().shutdown();
		}
	}
	*/
}
