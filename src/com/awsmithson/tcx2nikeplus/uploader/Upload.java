package com.awsmithson.tcx2nikeplus.uploader;

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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


public class Upload
{

	private static String URL_CHECK_PIN_STATUS = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/get_pin_status.jsp";
	private static String URL_DATA_SYNC = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/sync.jsp";
	private static String URL_DATA_SYNC_COMPLETE = "https://secure-nikerunning.nike.com/nikeplus/v2/services/device/sync_complete.jsp";

	private static String USER_AGENT = "iTunes/9.0.3 (Macintosh; N; Intel)";

	private final static Log log = Log.getInstance();

	public Upload() {
	}


	public void checkPinStatus(String pin) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {

		// Send data
		URL url = new URL(String.format("%s?%s", URL_CHECK_PIN_STATUS, generateParameter("pin", pin)));
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);

		// Get the response
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();
		//log.out(Util.DocumentToString(doc));

		String pinStatus = Util.getSimpleNodeValue(doc, "pinStatus");

		if (!(pinStatus.equals("confirmed")))
			throw new IllegalArgumentException("The PIN supplied is not valid");
	}

	public void syncData(String pin, File file) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		// Load the file, ensuring it is valid xml
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);
		doc.normalize();

		syncData(pin, doc);
	}

	public Document syncData(String pin, Document doc) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {

		String data = Util.generateStringOutput(doc);

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
		//log.out("    %s", Util.DocumentToString(outDoc));

		return outDoc;
	}

	

	public Document endSync(final String pin) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		URL url = new URL(String.format("%s?%s", URL_DATA_SYNC_COMPLETE, generateParameter("pin", pin)));
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);


		// Get the response
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();
		//log.out("end sync reply: %s", Util.DocumentToString(doc));

		return doc;

		/*
		Thread t = new Thread(new Runnable() {
			public void run() {
				OutputStreamWriter wr = null;
				try {
					// Send data
					URL url = new URL(String.format("%s?%s", URL_DATA_SYNC_COMPLETE, generateParameter("pin", pin)));
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
	

	private String generateParameter(String key, String val) throws UnsupportedEncodingException {
		return String.format("%s=%s", URLEncoder.encode(key, "UTF-8"), URLEncoder.encode(val, "UTF-8"));
	}


	public static void main(String[] args) {
		String pin = args[0];
		File file = new File(args[1]);

		Upload u = new Upload();

		try {
			u.checkPinStatus(pin);
			u.syncData(pin, file);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		finally {
			try {
				u.endSync(pin);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}

