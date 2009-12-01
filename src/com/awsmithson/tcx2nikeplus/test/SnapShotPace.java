package com.awsmithson.tcx2nikeplus.test;

import flanagan.interpolation.CubicSpline;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class SnapShotPace {

	public SnapShotPace() {
	}



	// Prints out the pace at <duration> seconds into the run.
	public void printPace(File nikePlusFile, int period, double[] kms) {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			// Create and load input document
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document inDoc = db.parse(nikePlusFile);
			inDoc.getDocumentElement().normalize();


			// Read in the extendedData element
			String extendedData = getSimpleNodeValue(inDoc, "extendedData");

			//System.out.println(extendedData);

			String[] splits = extendedData.split(", ");

			//System.out.println(splits.length);

			//System.out.println(splits[100]);

			double[] millis = new double[splits.length];
			double[] distance = new double[splits.length];

			for (int i = 0; i < splits.length; ++i) {
				millis[i] = i*10*1000;
				distance[i] = Double.parseDouble(splits[i]);
			}

			CubicSpline distanceToMillis = new CubicSpline(distance, millis);
			CubicSpline millisToDistance = new CubicSpline(millis, distance);





			for (int i = 1; i < 180; i = i + 1) {
				if ((getPace(distanceToMillis, millisToDistance, kms[0], i*1000) > 283200) && (getPace(distanceToMillis, millisToDistance, kms[0], i*1000) < 284000))
					System.out.printf("%d\t%f\n", i, getPace(distanceToMillis, millisToDistance, kms[0], i*1000));
			}


			for (double km : kms) {
				System.out.printf("%f\t%f\n", km, getPace(distanceToMillis, millisToDistance, km, period*1000));
			}



		}
		catch (Exception e) {
			e.printStackTrace();
		}
		

	}


	private double getPace(CubicSpline distanceToMillis, CubicSpline millisToDistance, double endKm, double periodMillis) {

		/*
		double periodDistance = 0.08;				// distance in km
		double startKm = endKm - periodDistance;

		double endMillis = distanceToMillis.interpolate(endKm);
		double startMillis = distanceToMillis.interpolate(startKm);
		*/




		//double periodMillis = 60 * 1000;			// duration in milliseconds
		double endMillis = distanceToMillis.interpolate(endKm);
		double startMillis = endMillis - periodMillis;
		double startKm = millisToDistance.interpolate(startMillis);
		double periodDistance = endKm - startKm;
		
		
		double totalMillis = endMillis - startMillis;

		//double startKm = millisToDistance.interpolate(endKm)

		//System.out.printf("\nstartMillis: %f\tendMillis: %f\tstartKm: %f\tendKm: %f\ttime: %f\tdistance%f\n", startMillis, endMillis, startKm, endKm, totalMillis, periodDistance);

		return totalMillis / periodDistance;
	}



	private String getSimpleNodeValue(Document doc, String nodeName) {
		return getSimpleNodeValue(doc.getElementsByTagName(nodeName).item(0));
	}

	private String getSimpleNodeValue(Node node) {
		return node.getChildNodes().item(0).getNodeValue();
	}





	public static void main(String[] args) {

		File inFile = new File(args[0]);

		int period = Integer.parseInt(args[1]);
		double[] kms = new double[args.length - 2];
		for (int i = 2; i < args.length; ++i) {
			kms[i-2] = Double.parseDouble(args[i]);
		}
		

		SnapShotPace c = new SnapShotPace();
		c.printPace(inFile, period, kms);
	}



}
