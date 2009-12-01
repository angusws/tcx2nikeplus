package com.awsmithson.tcx2nikeplus.converter;

import flanagan.interpolation.CubicSpline;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class Convert
{

	private static BigDecimal BD_1000 = new BigDecimal(1000);
	private static BigDecimal BD_METRES_PER_MILE = new BigDecimal(1609.344);
	private static double D_KM_PER_MILE = 1.609344;

	private long _totalDuration;
	private BigDecimal _totalDistance;

	private Calendar _calStart;
	private Calendar _calEnd;
	private String _startTimeString;



	public Convert() {
	}


	public Document generateNikePlusXml(File tcxFile, String empedID) {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			// Create and load input document
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document inDoc = db.parse(tcxFile);
			inDoc.getDocumentElement().normalize();

			// Create output document
			Document outDoc = db.newDocument();
			


			// Sports Data (root)
			Element sportsDataElement = appendElement(outDoc, "sportsData");

			// Vers
			appendElement(sportsDataElement, "vers", "2");

			// Run Summary
			appendRunSummary(inDoc, sportsDataElement);

			// Template
			appendTemplate(sportsDataElement);

			// Goal Type
			appendGoalType(sportsDataElement);

			// User Info
			appendUserInfo(inDoc, sportsDataElement, empedID);

			// Start Time
			appendElement(sportsDataElement, "startTime", _startTimeString);

			// Extended Data
			appendSnapShotListAndExtendedData(inDoc, sportsDataElement);



			//printDocument(outDoc);
			//writeDocument(outDoc);
			//return new String[] { generateString(outDoc), generateFileName() };

			return outDoc;
			
		}
		catch (DatatypeConfigurationException ex) {
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (ParserConfigurationException ex) {
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (SAXException ex) {
			Logger.getLogger(Convert.class.getName()).log(Level.SEVERE, null, ex);
		}

		return null;

	
	}



	/*
	<runSummary>
	  <workoutName><![CDATA[Basic]]></workoutName>
	  <time>2009-06-12T10:00:00-10:00</time>
	  <duration>1760000</duration>
	  <durationString>29:20</durationString>
	  <distance unit="km">6.47</distance>
	  <distanceString>6.47 km</distanceString>
	  <pace>4:32 min/km</pace>
	  <calories>505</calories>
	  <battery></battery>
	  <stepCounts>
	    <walkBegin>0</walkBegin>
	    <walkEnd>0</walkEnd>
	    <runBegin>0</runBegin>
	    <runEnd>0</runEnd>
	  </stepCounts>
	</runSummary>
	*/
	private Element appendRunSummary(Document inDoc, Element sportsDataElement) throws DatatypeConfigurationException {
		Element runSummaryElement = appendElement(sportsDataElement, "runSummary");

		// Workout Name
		appendCDATASection(runSummaryElement, "workoutName", "Basic");

		// Start Time
		appendStartTime(inDoc, runSummaryElement);

		// Duration, Duration, Pace & Calories
		appendTotals(inDoc, runSummaryElement);

		// Battery
		appendElement(runSummaryElement, "battery");

		// Step Counts
		appendStepCounts(runSummaryElement);

		return runSummaryElement;
	}


	/*
	 * I do not do much with the time-zone on the Calendar object created from the garmin xml data.
	 * SimpleDateFormat automatically formats it to the users TimeZone when formatting.
	 *
	 * FIXME: This is fine when the user is in the same timezone as where we are running this app
	 * but if the servlet is calling it we should pass the users timezone from their browser.
	 */
	private void appendStartTime(Document inDoc, Element runSummaryElement) throws DatatypeConfigurationException {

		// Garmin:	 2009-06-03T16:59:27.000Z
		// Nike:	 2009-06-03T17:59:27+01:00

		//TimeZone.setDefault(TimeZone.getTimeZone("EST"));

		//SimpleDateFormat gformat = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss.SSS'Z'");
		SimpleDateFormat nikeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

		Calendar gc = getCalendarValue(Util.getSimpleNodeValue(inDoc, "Id"));
		//_timeStartGMT = (Calendar)(gc.clone());
		//gc.setTimeZone(TimeZone.getDefault());
		
		//System.out.println(gc.getTimeInMillis());

		//System.out.println(nikeFormat.format(_timeStartGMT.getTime()));

		//TimeZone tzGarmin = TimeZone.getTimeZone("GMT");
		//System.out.println(tzGarmin);

		//DatatypeFactory.newInstance().newXMLGregorianCalendar(garminDate).toGregorianCalendar();
		//_timeZoneOffset = TimeZone.getDefault().getOffset(_timeStartGMT.getTimeInMillis());
		//System.out.println(_timeZoneOffset);

		//gc.add(Calendar.MILLISECOND, _timeZoneOffset);

		String startTimeString = nikeFormat.format(gc.getTime());

		 // This startTime element has a colon between the hours/minutes in the timezone part!
		startTimeString = String.format("%s:%s", startTimeString.substring(0, 22), startTimeString.substring(22));

		appendElement(runSummaryElement, "time", startTimeString);

		_calStart = gc;
		_startTimeString = startTimeString;
	}

	private void appendTotals(Document inDoc, Node parent) {

		Double totalSeconds = 0d;
		BigDecimal totalDistance = new BigDecimal(0);
		int totalCalories = 0;

		NodeList laps = inDoc.getElementsByTagName("Lap");

		int lapsLength = laps.getLength();

		for (int i = 0; i < lapsLength; ++i) {
			
			NodeList lapData = laps.item(i).getChildNodes();
			int lapInfoLength = lapData.getLength();

			for (int j = 0; j < lapInfoLength; ++j) {
				Node n = lapData.item(j);

				String nodeName = n.getNodeName();

				if (nodeName.equals("TotalTimeSeconds")) 
					totalSeconds += Double.parseDouble(Util.getSimpleNodeValue(n));
				
				else if (nodeName.equals("DistanceMeters"))
					totalDistance = totalDistance.add(new BigDecimal(Util.getSimpleNodeValue(n)));

				else if (nodeName.equals("Calories"))
					totalCalories += Integer.parseInt(Util.getSimpleNodeValue(n));
			}
		}


		long totalDuration = totalSeconds.longValue();

		_totalDuration = totalDuration;
		_totalDistance = totalDistance;

		// Calculate the end-time of the run (for use late to generate the xml filename).
		_calEnd = (Calendar)(_calStart.clone());
		_calEnd.add(Calendar.SECOND, (int)totalDuration);

		// Total Duration
		appendElement(parent, "duration", totalDuration*1000);
		appendElement(parent, "durationString", getTimeStringFromSeconds(totalDuration));

		// Total Distance
		totalDistance = totalDistance.divide(BD_1000);
		appendElement(parent, "distance", String.format("%.4f", totalDistance), "unit", "km");
		appendElement(parent, "distanceString", String.format("%.2f km", totalDistance));

		// Pace
		appendElement(parent, "pace", String.format("%s min/km", getTimeStringFromSeconds(calculatePace(totalDuration, totalDistance)/1000)));

		// Calories - FIXME - calories are not calculated properly yet...
		appendElement(parent, "calories", totalCalories);
	}

	private void appendStepCounts(Node parent) {

		Element stepCountsElement = appendElement(parent, "stepCounts");

		appendElement(stepCountsElement, "walkBegin", "0");
		appendElement(stepCountsElement, "walkEnd", "0");
		appendElement(stepCountsElement, "runBegin", "0");
		appendElement(stepCountsElement, "runEnd", "0");
	}


	/*
	<template><templateID>8D495DCE</templateID>
	<templateName><![CDATA[Basic]]></templateName>
	</template>
	*/
	private void appendTemplate(Element sportsDataElement) {
		Element templateElement = appendElement(sportsDataElement, "template");
		appendElement(templateElement, "templateID", "8D495DCE");
		appendCDATASection(templateElement, "templateName", "Basic");
	}


	// <goal type="" value="" unit=""></goal>
	private void appendGoalType(Element sportsDataElement) {
		appendElement(sportsDataElement, "goal", null, "type", "", "value", "", "unit", "");
	}

	/*
	<userInfo>
	  <empedID>XXXXXXXXXXX</empedID>
	  <weight></weight>
	  <device>iPod</device>
	  <calibration></calibration>
	</userInfo>
	*/
	private void appendUserInfo(Document inDoc, Element sportsDataElement, String empedID) {
		Element templateElement = appendElement(sportsDataElement, "userInfo");
		appendElement(templateElement, "empedID", (empedID == null) ? "XXXXXXXXXXX" : empedID);
		appendElement(templateElement, "weight");

		// Upload goes through fine when "Garmin Forerunner 405 is specified as device but on nikeplus.com website some data shows as "invalid"
		//appendElement(templateElement, "device",	getSimpleNodeValue(inDoc, "Name"));		// Garmin Forerunner 405
		appendElement(templateElement, "device", "iPod");								// iPod
		appendElement(templateElement, "calibration");
	}


	private void appendSnapShotListAndExtendedData(Document inDoc, Element sportsDataElement) throws DatatypeConfigurationException {
		Element snapShotKmListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "kmSplit");
		Element snapShotMileListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "mileSplit");
		Element snapShotClickListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "userClick");

		Element extendedDataListElement	= appendElement(sportsDataElement, "extendedDataList");

		long startDurationAdjusted = _calStart.getTimeInMillis();

		long currentDuration = 0;
		BigDecimal currentDistance = null;

		ArrayList<Long> pauseResumeTimes = new ArrayList<Long>();
		
		NodeList trackPoints = inDoc.getElementsByTagName("Trackpoint");
		int trackPointsLength = trackPoints.getLength();
		//System.out.println(trackPointsLength);

		// Can't use arrays as some trackpoints do not have distnace/duration data so we don't know how large to make the arrays at this point.
		//double[] durations = new double[trackPointsLength];
		//double[] distances = new double[trackPointsLength];

		ArrayList<Double> durations = new ArrayList<Double>();
		ArrayList<Double> distances = new ArrayList<Double>();

		for (int i = 0; i < trackPointsLength; ++i) {

			NodeList trackPointData = trackPoints.item(i).getChildNodes();
			int trackPointDataLength = trackPointData.getLength();

			// Deal with pause/resumes
			if ((trackPointDataLength == 3) && (i+1 < trackPointsLength)) {

				//System.out.println(Util.getSimpleNodeValue(trackPointData.item(1)));

				// I have one workout in which the first trackpoint is just a time, ignore this in case it is a regular occurance/bug.
				// This can also occur when the user has unpaused immeadiately before beginning a new lap, we deal with it below.
				//if (i == 0) continue;

				// Save the pause/resume points so that we can create the snapshot event later (once we can interpolate to get the distance).
				long tpPauseTime = getCalendarNodeValue(trackPointData.item(1)).getTimeInMillis();

				// Get what we are expecting to be the resume track-point-data.
				trackPointData = trackPoints.item(++i).getChildNodes();

				// As we are expecting it to be a resume trackPointData it must also be of length 3, otherwise we are not dealing with a pause/resume pair so ignore the pause.
				if ((trackPointData.getLength()) == 3) {
					long tpResumeTime = getCalendarNodeValue(trackPointData.item(1)).getTimeInMillis();

					// 2009-12-01: Looking at various runs on nike+ it seems the each pause/resume pair now has the same
					// duration/distance.  I can't find my nike+ stuff to check this is 100% accurate but will go with it for now.
					pauseResumeTimes.add(tpPauseTime - startDurationAdjusted);
					pauseResumeTimes.add(tpPauseTime - startDurationAdjusted);

					// Adjust the start time so that future splits are not affected by the paused time period.
					long pauseDuration = tpResumeTime - tpPauseTime;
					startDurationAdjusted += pauseDuration;

					// Continue onto the next trackpoint, we've stored our pause/resume data for later use.
					continue;
				}
			}


			// If we reach this point then this is a normal trackpoint, not a pause/resume.
			Double distance = null;
			Double duration = null;

			for (int j = 0; j < trackPointDataLength; ++j) {
				Node n = trackPointData.item(j);

				String nodeName = n.getNodeName();
				
				// Run duration to this point
				if (nodeName.equals("Time")) {
					Calendar tpTime = getCalendarNodeValue(trackPointData.item(j));
					currentDuration = tpTime.getTimeInMillis() - startDurationAdjusted;
					duration = (double)currentDuration/1000;
					continue;
				}

				// Distance to this point
				if (nodeName.equals("DistanceMeters")) {
					currentDistance = new BigDecimal(Util.getSimpleNodeValue(trackPointData.item(j)));
					distance = currentDistance.divide(BD_1000, MathContext.DECIMAL32).doubleValue();
					continue;
				}
			}

			// If we have valid and new duration/distance combinations then store them for use later.
			if ((duration != null) && (!durations.contains(duration)) && (distance != null) && (!distances.contains(distance))) {
				durations.add(duration);
				distances.add(distance);
			}
		}

		double[] durationsArray = convertToPrimitaveDoubleArray(durations);
		double[] distancesArray = convertToPrimitaveDoubleArray(distances);

		// Generate cubic splines for distance -> duration and duration -> distance.
		// I'd have thought the flanagan classes would have offered some method of inverting the data, but I've not looked into it and this hack will do for now.
		CubicSpline distanceToDuration = new CubicSpline(distancesArray, durationsArray);
		CubicSpline durationToDistance = new CubicSpline(durationsArray, distancesArray);


		// Pause/Resume splits.
		boolean paused = false;
		double distance = 0;
		Iterator<Long> it = pauseResumeTimes.iterator();
		while (it.hasNext()) {
			long time = it.next();
			
			// If we are doing a resume the the distance is always equal to the previous pause, no need to interpolate.
			distance = (!paused) ? interpolate(durationToDistance, (time/1000)) : distance;
			appendSnapShot(snapShotClickListElement, time, distance, "event", (paused) ? "resume" : "pause");
			paused = !paused;
		}

		// Km splits
		int maxKms = _totalDistance.divide(BD_1000, MathContext.DECIMAL32).intValue();
		for (int i = 1; i <= maxKms; ++i) 
			appendSnapShot(snapShotKmListElement, (long)(interpolate(distanceToDuration, i) * 1000), i);

		// Mile splits
		int maxMiles = _totalDistance.divide(BD_METRES_PER_MILE, MathContext.DECIMAL32).intValue();
		for (int i = 1; i <= maxMiles; ++i) 
			appendSnapShot(snapShotMileListElement, (long)(interpolate(distanceToDuration, (i * D_KM_PER_MILE)) * 1000), i*D_KM_PER_MILE);

		// Stop split
		appendSnapShot(snapShotClickListElement, _totalDuration * 1000, _totalDistance.divide(BD_1000, MathContext.DECIMAL32).doubleValue(), "event", "stop");


		/*
		<extendedDataList>
		  <extendedData dataType="distance" intervalType="time" intervalUnit="s" intervalValue="10">0.0. 1.1, 2.3, 4.7, etc</extendedData>
		</extendedDataList>
		*/
		int finalReading = (int)(durationToDistance.getXmax());

		StringBuilder sb = new StringBuilder("0.0");
		for (int i = 10; i < finalReading; i = i + 10) {
			//System.out.printf("%d\t%.4f\n", i, durationToDistance.interpolate(i));
			sb.append(String.format(", %.4f", interpolate(durationToDistance, i)));
		}

		appendElement(extendedDataListElement, "extendedData", sb, "dataType", "distance", "intervalType", "time", "intervalUnit", "s", "intervalValue", "10");
		
	}

	private void appendSnapShot(Element snapShotListElement, long duration, double distance, String ... attributes) {
		Element snapShotElement = appendElement(snapShotListElement, "snapShot", null, attributes);

		appendElement(snapShotElement, "duration", duration);
		appendElement(snapShotElement, "distance", String.format("%.3f", distance));
		//appendElement(snapShotElement, "pace", generateSnapShotPace(durationToDistance, currentDuration));
	}


	private double interpolate(CubicSpline spline, double x) {
		if (x < spline.getXmin()) x = spline.getXmin();
		else if (x > spline.getXmax()) x = spline.getXmax();
		
		return spline.interpolate(x);
	}


	/*
	private int generateSnapShotPace(CubicSpline durationToDistance, long endMillis) {

		long endSeconds = endMillis/1000;

		// If we are trying to ascertain the pace for a duration greater than the max on the spline curve then use the max on the spline curve (this will happen with the "stop" snapshot).
		if (endSeconds > durationToDistance.getXmax())
			endSeconds = (long)durationToDistance.getXmax();

		// Base the pace on the previous 20 seconds, unless duration is less than that, in which case base on the duration.
		long period = (endSeconds >= 20) ? 20 : endSeconds;
		long startSeconds = endSeconds - period;

		
		double startKm = durationToDistance.interpolate(startSeconds);
		double endKm = durationToDistance.interpolate(endSeconds);
		double periodDistance = endKm - startKm;

		int pace = (int)((period * 1000) / periodDistance);
		
		return pace;
	}
	*/



	private Calendar getCalendarNodeValue(Node node) throws DatatypeConfigurationException {
		return getCalendarValue(Util.getSimpleNodeValue(node));
	}

	private Calendar getCalendarValue(String value) throws DatatypeConfigurationException {
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(value).toGregorianCalendar();
	}
	


	private Element appendElement(Node parent, String name) {
		return appendElement(parent, name, null);
	}

	private Element appendElement(Node parent, String name, Object data, String ... attributes) {
		Document doc = (parent.getNodeType() == Node.DOCUMENT_NODE) ? (Document)parent : parent.getOwnerDocument();
		Element e = doc.createElement(name);
		parent.appendChild(e);
		
		if (data != null) e.appendChild(doc.createTextNode(data.toString()));

		for (int i = 0; i < attributes.length; ++i)
			e.setAttribute(attributes[i++], attributes[i]);
		
		return e;
	}

	private void appendCDATASection(Node parent, String name, Object data) {
		Document doc = (parent.getNodeType() == Node.DOCUMENT_NODE) ? (Document)parent : parent.getOwnerDocument();
		Element e = doc.createElement(name);
		parent.appendChild(e);
		e.appendChild(doc.createCDATASection(data.toString()));
	}

	private int calculatePace(long duration, BigDecimal distance) {
		return (int)((duration*1000)/distance.doubleValue());
	}

	private double[] convertToPrimitaveDoubleArray(ArrayList<Double> al) {
		int size = al.size();
		double[] ar = new double[size];
		for (int i = 0; i < size; ++i)
			ar[i] = al.get(i);

		return ar;
	}
	
	private String getTimeStringFromSeconds(long totalSeconds) {

		int hours = (int)(totalSeconds / 3600);
		int remainder = (int)(totalSeconds % 3600);
		int minutes = remainder / 60;
		int seconds = remainder % 60;

		String mm = String.format((hours > 0) && (minutes < 10) ? "0%d" : "%d", minutes);
		String ss = String.format((seconds < 10) ? "0%d" : "%d", seconds);

		return (hours > 0)
			? String.format("%d:%s:%s", hours, mm, ss)
			: String.format("%s:%s", mm, ss)
		;
	}


	public String generateFileName() {
		SimpleDateFormat outfileFormat	= new SimpleDateFormat("yyyy-MM-dd HH;mm;ss'.xml'");
		return outfileFormat.format(_calEnd.getTime());
	}





	public static void main(String[] args) {
		File inFile = new File(args[0]);

		String empedID = (args.length >= 2) ? args[1] : null;

		Convert c = new Convert();
		Document doc = c.generateNikePlusXml(inFile, empedID);
		Util.writeDocument(doc, c.generateFileName());

	}



	/*
	public static void main(String[] args) {

		File inFile = new File(args[0]);

		Convert c = new Convert();
		c.genereateNikePlusXml(inFile);
	}
	*/
}

