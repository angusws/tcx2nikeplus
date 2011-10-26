package com.awsmithson.tcx2nikeplus.http;

import static com.awsmithson.tcx2nikeplus.Constants.*;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class NikePlus
{

	private static final String URL_GENERATE_PIN = "https://secure-nikerunning.nike.com/nikeplus/v2/services/app/generate_pin.jsp";
	private static final String URL_CHECK_PIN_STATUS = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/get_pin_status.jsp";
	private static final String URL_DATA_SYNC_NON_GPS = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/sync.jsp";
	private static final String URL_DATA_SYNC_GPS = "https://secure-nikerunning.nike.com/gps/sync/plus/iphone";
	private static final String URL_DATA_SYNC_COMPLETE = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/sync_complete.jsp";

	private static final String USER_AGENT = "iTunes/9.0.3 (Macintosh; N; Intel)";

	private static final Log log = Log.getInstance();

	public NikePlus() {
	}


	/**
	 * Retrieves the pin from nike+ for the given login/password..
	 * @param login The login String (email address).
	 * @param password The login password.
	 * @return Nike+ pin.
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws UnsupportedEncodingException
	 */
	public String generatePin(String login, String password) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {

		// Send data
		URL url = new URL(String.format("%s?%s&%s", URL_GENERATE_PIN, Util.generateHttpParameter("login", login), Util.generateHttpParameter("password", password)));
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);

		// Get the response
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();
		//log.out(Util.documentToString(doc));

		String pinStatus = Util.getSimpleNodeValue(doc, "status");

		if (!(pinStatus.equals("success")))
			throw new IllegalArgumentException("The email and password supplied are not valid");

		return Util.getSimpleNodeValue(doc, "pin");
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
	private void fullSync(String pin, File runXml, File gpxXml) throws ParserConfigurationException, SAXException, IOException, MalformedURLException, NoSuchAlgorithmException, KeyManagementException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		fullSync(pin, db.parse(runXml), ((gpxXml != null) ? db.parse(gpxXml) : null));
	}

	/**
	 * Does a full synchronisation cycle (check-pin-status, sync, end-sync) with nike+ for the given pin and xml document(s).
	 * @param pin Nike+ pin.
	 * @param runXml Nike+ workout xml.
	 * @param gpxXml Nike+ gpx xml.
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public void fullSync(String pin, Document runXml, Document gpxXml) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, NoSuchAlgorithmException, KeyManagementException {
		
		boolean haveGpx = (gpxXml != null);
		runXml.normalizeDocument();
		if (haveGpx) gpxXml.normalizeDocument();

		log.out("Uploading to Nike+...");
		
		try {
			log.out(" - Checking pin status...");
			checkPinStatus(pin);

			log.out(" - Syncing data...");
			Document nikeResponse = (haveGpx)
				? syncDataGps(pin, runXml, gpxXml)
				: syncDataNonGps(pin, runXml)
			;

			//<?xml version="1.0" encoding="UTF-8" standalone="no"?><plusService><status>success</status></plusService>
			if (Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS)) {
				log.out(" - Sync successful.");
				return;
			}

			//<?xml version="1.0" encoding="UTF-8" standalone="no"?><plusService><status>failure</status><serviceException errorCode="InvalidRunError">snapshot duration greater than run (threshold 30000 ms): 82980</serviceException></plusService>
			NodeList nikeServiceExceptionL = nikeResponse.getElementsByTagName("serviceException");
			if ((nikeServiceExceptionL != null) && (nikeServiceExceptionL.getLength() > 0)) {
				Node nikeServiceException = nikeServiceExceptionL.item(0);
				throw new RuntimeException(String.format("%s: %s", nikeServiceException.getAttributes().item(0).getNodeValue(), Util.getSimpleNodeValue(nikeServiceException)));
			}
			else {
				log.out(Util.documentToString(nikeResponse));

				String nikeError = Util.getSimpleNodeValue(nikeResponse, "error");
				log.out(nikeError);
				if (nikeError.indexOf("<?xml ") == -1)
					throw new RuntimeException(String.format("Nike+ Error: %s.\nThis is likely to be a problem at Nike+'s end.\nPlease try again later, contact me if the problem persists.", nikeError));

				else {
					/*
					2011-02-19 - Oh dear nikeplus...  What is this xml nonsense you are coming out with?!?  Representing an xml document within an xml node?  Tasty.
					 <?xml version="1.0" encoding="UTF-8" standalone="no"?>
					 <response>
					 <status>failure</status>
					 <error>

					 <![CDATA[Failed to sync runXml: problem syncing runXML response from device sync service:
					 <?xml version="1.0" encoding="UTF-8"?>
					 <plusService>
					 <status>failure</status>
					 <serviceException errorCode="InvalidRunError">snapshot pace invalid. dist: 5.0 duration: -4074967</serviceException>
					 </plusService>]]>

					 </error>
					 </response>
					*/
					// Failed to sync runXml: problem syncing runXML response from device sync service:<?xml version="1.0" encoding="UTF-8"?><plusService><status>failure</status><serviceException errorCode="InvalidRunError">snapshot pace invalid. dist: 5.0 duration: -4074967</serviceException></plusService>
					nikeError = nikeError.substring(nikeError.indexOf("<?xml"));
					log.out(nikeError);
					nikeResponse = Util.generateDocument(nikeError);
					Node nikeServiceException = nikeResponse.getElementsByTagName("serviceException").item(0);

					//InvalidRunError: snapshot pace invalid. dist: 5.0 duration: -4074967
					throw new RuntimeException(String.format("%s: %s", nikeServiceException.getAttributes().item(0).getNodeValue(), Util.getSimpleNodeValue(nikeServiceException)));
				}
			}
		}
		finally {
			log.out(" - Ending sync...");
			Document nikeResponse = endSync(pin);
			log.out((Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS))
				? " - End sync successful."
				: String.format(" - End sync failed: %s", Util.documentToString(nikeResponse))
			);
		}
	}


	private void checkPinStatus(String pin) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {

		// Send data
		URL url = new URL(String.format("%s?%s", URL_CHECK_PIN_STATUS, Util.generateHttpParameter("pin", pin)));
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);

		// Get the response
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();
		//log.out(Util.documentToString(doc));

		String pinStatus = Util.getSimpleNodeValue(doc, "pinStatus");

		if (!(pinStatus.equals("confirmed")))
			throw new IllegalArgumentException("The PIN supplied is not valid");
	}
	
	private Document syncDataNonGps(String pin, Document doc) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {

		String data = Util.documentToString(doc);

		// Send data
		URL url = new URL(URL_DATA_SYNC_NON_GPS);
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
		//log.out("    %s", Util.documentToString(outDoc));

		return outDoc;
	}


	private Document syncDataGps(String pin, Document runXml, Document gpxXml) throws IOException, ParserConfigurationException, SAXException, NoSuchAlgorithmException, KeyManagementException {
		HttpClient client = new DefaultHttpClient();
		client = HttpClientNaiveSsl.wrapClient(client);

		HttpPost post = new HttpPost(URL_DATA_SYNC_GPS);
		post.addHeader("user-agent", "NPConnect");
		post.addHeader("pin", URLEncoder.encode(pin, "UTF-8"));
		
		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
		reqEntity.addPart("runXML", new SpoofFileBody(Util.documentToString(runXml), "runXML.xml"));
		reqEntity.addPart("gpxXML", new SpoofFileBody(Util.documentToString(gpxXml), "gpxXML.xml"));
		post.setEntity(reqEntity);

		HttpResponse response = client.execute(post);		
		HttpEntity entity = response.getEntity();
		Document outDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(entity.getContent());
		EntityUtils.consume(entity);
		outDoc.normalize();

		return outDoc;
	}


	private Document endSync(final String pin) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		URL url = new URL(String.format("%s?%s", URL_DATA_SYNC_COMPLETE, Util.generateHttpParameter("pin", pin)));
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);


		// Get the response
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();
		//log.out("end sync reply: %s", Util.documentToString(doc));

		return doc;

		/*
		Thread t = new Thread(new Runnable() {
			public void run() {
				OutputStreamWriter wr = null;
				try {
					// Send data
					URL url = new URL(String.format("%s?%s", URL_DATA_SYNC_COMPLETE, Util.generateHttpParameter("pin", pin)));
					URLConnection conn = url.openConnection();
					conn.setRequestProperty("user-agent", USER_AGENT);
				}
				catch (Throwable t) {
					log.out(t);
				}
				finally {
					try {
						if (wr != null) wr.close();
					}
					catch (Throwable t) {
						log.out(t);
					}
				}
			}
		});

		// Start the end-sync thread - and leave it to run in the background.
		t.start();
		*/
	}


	public static void main(String[] args) {
		String pin = args[0];
		File runXml = new File(args[1]);
		File gpxXml = (args.length > 2) ? new File(args[2]) : null;

		NikePlus u = new NikePlus();
		try {
			u.fullSync(pin, runXml, gpxXml);
		}
		catch (Throwable t) {
			t.printStackTrace();
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
