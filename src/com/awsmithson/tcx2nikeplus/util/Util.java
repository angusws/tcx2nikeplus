package com.awsmithson.tcx2nikeplus.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class Util {

	private final static int Connection_Timeout = 20 * 1000;


	private final static Log log = Log.getInstance();


	public static String getSimpleNodeValue(Document doc, String nodeName) {
		return getSimpleNodeValue(doc.getElementsByTagName(nodeName).item(0));
	}

	public static String getSimpleNodeValue(Node node) {
		return node.getChildNodes().item(0).getNodeValue();
	}

	

	public static String generateStringOutput(Document doc) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);

			Writer outWriter = new StringWriter();
			StreamResult result = new StreamResult(outWriter);
			transformer.transform(source, result);
			return outWriter.toString();
		}
		catch (Exception e) {
			log.out(e);
			return null;
		}
	}

	/*
	public static void printDocument(Document doc) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result =  new StreamResult(System.out);
			transformer.transform(source, result);
			//System.out.println();
		}
		catch (Exception e) {
			log.out(e);
		}
	}
	*/

	public static String DocumentToString(Document doc) {
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);
			transformer.transform(source, result);
			return stringWriter.getBuffer().toString();
		}
		catch (Exception e) {
			log.out(e);
		}
		return null;
	}




	public static void writeDocument(Document doc, String filename) {
		Source source = new DOMSource(doc);
		File file = new File(filename);
		Result result = new StreamResult(file);

		try {
			Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
		}
		catch (TransformerConfigurationException ex) {
			log.out(Level.SEVERE, null, ex);
		}
		catch (TransformerException ex) {
			log.out(ex);
		}
	}


	public static Document downloadFile(String location) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
		return downloadFile(location, null);
	}

	public static Document downloadFile(String location, String parameters) throws MalformedURLException, IOException, ParserConfigurationException, SAXException, FileNotFoundException, SocketTimeoutException {
		URL url = new URL(location);
		URLConnection conn = url.openConnection();

		if (parameters != null) {
			// Use post mode
			conn.setDoOutput(true);
			conn.setAllowUserInteraction(false);
			conn.setConnectTimeout(Connection_Timeout);

			// Send query
			PrintStream ps = new PrintStream(conn.getOutputStream());
			ps.print(parameters);
			ps.close();
		}
		
		// Get response
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
		if ((doc != null) && (doc.getDocumentElement() != null))
			doc.getDocumentElement().normalize();

		return doc;
	}


	public static Document generateDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
		if ((doc != null) && (doc.getDocumentElement() != null))
			doc.getDocumentElement().normalize();

		return doc;
	}
}
