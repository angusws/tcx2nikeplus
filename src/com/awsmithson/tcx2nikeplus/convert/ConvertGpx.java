package com.awsmithson.tcx2nikeplus.convert;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Converts a garmin gpx file to one that can be uploaded to nike+.
 * @author angus
 */
public class ConvertGpx
{
	private static final Log log = Log.getInstance();

	public ConvertGpx() {
	}


	public Document generateNikePlusGpx(File tcxFile) throws Throwable {
		Document tcxDoc = Util.generateDocument(tcxFile);
		return generateNikePlusGpx(tcxDoc);
	}

	public Document generateNikePlusGpx(Document inDoc) throws Throwable {
		
		// Create output document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document outDoc = db.newDocument();

		// gpx (root)
		Element gpxElement = Util.appendElement(outDoc, "gpx", null, "xmlns", "http://www.topografix.com/GPX/1/1", "creator", "NikePlus", "version", "1.1");

		// trk
		Element trk = Util.appendElement(gpxElement, "trk");
		Util.appendCDATASection(trk, "name", "4c888a06");
		Util.appendCDATASection(trk, "desc", "workout");

		// trkseg
		Element trkSeg = Util.appendElement(trk, "trkseg");

		// trkpt's
		appendTrkptElements(inDoc, trkSeg);

		return outDoc;
	}


	/**
	 * Appends all the <trkrpt> data to the output xml.
	 * <p>
	 * I have chosen to iterate through the garmin data and append rather than copy it all over then iterate
	 * through it and remove unrequired data.  The reason for this is that if garmin decided to modify the gpx
	 * schema I do not have to worry about modifying my code to remove additional elements.
	 * At the time of writing (2010-09-13) nike+ use gpx data as follows:
	 * <pre>
	 * {@code
	 * <trkpt lat="51.7944969579" lon="-0.0769600122">
	 *   <ele>50.5970001221</ele>
	 *   <time>2010-09-09T07:18:04Z</time>
	 * </trkpt>
	 * }
	 * </pre>
	 * @param inDoc The garmin gpx document we are reading the data from.
	 * @param trkSeg The xml element in the output document to which we are appending the data.
	 */
	private void appendTrkptElements(Document inDoc, Element trkSeg) {
		
		NodeList trkpts = inDoc.getElementsByTagName("trkpt");

		int trkptsLength = trkpts.getLength();

		for (int i = 0; i < trkptsLength; ++i) {
			Node trkptGarmin = trkpts.item(i);
			NamedNodeMap latlon = trkptGarmin.getAttributes();
			String lon = Util.getSimpleNodeValue(latlon.getNamedItem("lon"));
			String lat = Util.getSimpleNodeValue(latlon.getNamedItem("lat"));

			Element trkptNike = Util.appendElement(trkSeg, "trkpt", null, "lat", lat, "lon", lon);

			NodeList trkptGarminData = trkptGarmin.getChildNodes();
			int trkptGarminDataLength = trkptGarminData.getLength();

			for (int j = 0; j < trkptGarminDataLength; ++j) {
				Node n = trkptGarminData.item(j);
				String nodeName = n.getNodeName();

				if ((nodeName.equals("ele")) || (nodeName.equals("time")))
					Util.appendElement(trkptNike, nodeName, Util.getSimpleNodeValue(n));
			}
		}
	}


	public static void main(String[] args) {
		File inFile = new File(args[0]);

		ConvertGpx c = new ConvertGpx();
		try {
			Document doc = c.generateNikePlusGpx(inFile);
			Util.writeDocument(doc, "gpxXML.xml");
		}
		catch (Throwable t) {
			log.out(t);
		}
	}	
}

