package com.awsmithson.tcx2nikeplus.converter;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
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


public class Util {



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
			e.printStackTrace();
			return null;
		}
	}

	public static void printDocument(Document doc) {
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result =  new StreamResult(System.out);
			transformer.transform(source, result);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
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
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (TransformerException ex) {
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}
	}


}
