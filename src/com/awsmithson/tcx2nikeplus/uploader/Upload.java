package com.awsmithson.tcx2nikeplus.uploader;

import com.awsmithson.tcx2nikeplus.converter.Util;
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

	private static String USER_AGENT = "iTunes/8.1.1 (Macintosh; N; Intel)";

	public Upload() {
	}


	public void checkPinStatus(String pin) throws IOException, MalformedURLException, ParserConfigurationException, SAXException, UnsupportedEncodingException {

		String data = generateParameter("pin", pin);

		// Send data
		URL url = new URL(URL_CHECK_PIN_STATUS);
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();

		// Get the response
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();

		String pinStatus = Util.getSimpleNodeValue(doc, "pinStatus");

        wr.close();

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

	public void syncData(String pin, Document doc) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {

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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document outDoc = db.parse(conn.getInputStream());
		outDoc.normalize();
		Util.printDocument(outDoc);

		wr.close();
	}


	public void endSync(String pin) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {

		String data = generateParameter("pin", pin);

		// Send data
		URL url = new URL(URL_DATA_SYNC_COMPLETE);
		URLConnection conn = url.openConnection();
		conn.setRequestProperty("user-agent", USER_AGENT);
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();

		// Get the response
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(conn.getInputStream());
		doc.normalize();

        wr.close();
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

