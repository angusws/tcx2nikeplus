package com.awsmithson.tcx2nikeplus.http;

import com.awsmithson.tcx2nikeplus.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;


public class Garmin
{
	// TODO: Tidy up exception-handling... java 7?
	private static Properties garminProperties;
	static {
		garminProperties = new Properties();
		InputStream in = null;
		try {
			in = Garmin.class.getResourceAsStream("/garmin.properties");
			garminProperties.load(in);
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
	
	private static final String	URL_GARMIN_ACTIVITIES = "https://connect.garmin.com/activities";
	private static final String	URL_GARMIN_SIGN_IN = "https://connect.garmin.com/signin";
	private static final String	URL_GARMIN_TCX = "https://connect.garmin.com/proxy/activity-service-1.0/tcx/activity/%d?full=true";
	private static final String	URL_GARMIN_GPX = "https://connect.garmin.com/proxy/activity-service-1.1/gpx/activity/%s?full=true";

	private static final Log log = Log.getInstance();

	public Garmin() {
	}


	private static final String GARMIN_ID_ERROR = "Invalid Garmin Activity.  Please ensure your garmin workout is not marked as private.";



	public static HttpClient getGarminHttpSession() throws Throwable {

		// Setup
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);
		
		// 1 - Initial attempt to view activities, which will set cookies and redirect us to sign-in page.
		HttpGet get = new HttpGet(URL_GARMIN_ACTIVITIES);
		HttpResponse response = client.execute(get);
		EntityUtils.consume(response.getEntity());

		// 2 - Sign-in attempt
		HttpPost post = new HttpPost(URL_GARMIN_SIGN_IN);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        nameValuePairs.add(new BasicNameValuePair("login", "login"));
        nameValuePairs.add(new BasicNameValuePair("login:loginUsernameField", garminProperties.getProperty("GARMIN_USERNAME")));
        nameValuePairs.add(new BasicNameValuePair("login:password", garminProperties.getProperty("GARMIN_PASSWORD")));
		nameValuePairs.add(new BasicNameValuePair("javax.faces.ViewState", "j_id1"));
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		response = client.execute(post);
		EntityUtils.consume(response.getEntity());

		return client;
	}




	public static Document downloadGarminTcx(HttpClient client, int garminActivityId) throws Throwable {
		return downloadGarminDocument(client, URL_GARMIN_TCX, garminActivityId);
	}
	public static Document downloadGarminGpx(HttpClient client, int garminActivityId) throws Throwable {
		return downloadGarminDocument(client, URL_GARMIN_GPX, garminActivityId);
	}


	private static Document downloadGarminDocument(HttpClient client, String url, int garminActivityId) throws Throwable {
		url = String.format(url, garminActivityId);
		Document doc = null;

		try {
			HttpGet get = new HttpGet(String.format(url, garminActivityId));
			HttpResponse response = client.execute(get);
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.getEntity().getContent());
		}
		catch (Exception e) {
			log.out(e);
			throw new Exception(GARMIN_ID_ERROR);
		}

		if (doc == null) throw new Exception(GARMIN_ID_ERROR);

		log.out("Successfully downloaded data for garmin activity %d.", garminActivityId);
		return doc;
	}

	


	public static void main(String[] args) {

		HttpClient client = null;
		try {
			client = getGarminHttpSession();
			downloadGarminTcx(client, 83638060);
			downloadGarminGpx(client, 83638060);
			client.getConnectionManager().shutdown();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			if (client != null) client.getConnectionManager().shutdown();
		}
		
	}
}

/*
private void syncDataNonGps(String pin, File file) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
	// Load the file, ensuring it is valid xml
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();
	Document doc = db.parse(file);
	doc.normalize();

	syncDataNonGps(pin, doc);
}


private void syncDataGps(String pin, File runXml, File gpxXml) throws MalformedURLException, IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException, KeyManagementException {
	// Load the file, ensuring it is valid xml
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();

	Document runXmlDoc = db.parse(runXml);
	runXmlDoc.normalize();

	Document gpxXmlDoc = db.parse(gpxXml);
	runXmlDoc.normalize();

	syncDataGps(pin, runXmlDoc, gpxXmlDoc);
}
*/
