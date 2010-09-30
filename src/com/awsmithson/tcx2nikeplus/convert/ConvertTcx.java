package com.awsmithson.tcx2nikeplus.convert;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import flanagan.interpolation.CubicSpline;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;



public class ConvertTcx
{

	private static final double				D_METRES_PER_MILE = 1609.344;
	private static final int				MILLIS_PER_HOUR = 60 * 1000 * 60;
	private static final String				DATE_TIME_FORMAT_NIKE = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String				DATE_TIME_FORMAT_HUMAN = "d MMM yyyy HH:mm:ss z";

	// The minimum amount of millis between Trackpoints that I am prepared to create a potential pause/resume for.
	private static final int				MILLIS_POTENTIAL_PAUSE_THRESHOLD = 0;


	private long _totalDuration;
	private double _totalDistance;
	private boolean _forceExcludeHeartRateData;
	private boolean _includeHeartRateData = false;

	private TimeZone _workoutTimeZone;
	private Calendar _calStart;
	private Calendar _calEnd;
	private String _startTimeString;
	private String _startTimeStringHuman;

	private static final Log log = Log.getInstance();


	/**
	 * Converts a garmin tcx file to a nike+ workout xml document.
	 * <p>
	 * This class is a hacky mess just now but it does the job.
	 * @author angus
	 */
	public ConvertTcx() {
	}


	public Document generateNikePlusXml(File tcxFile, String empedID) throws Throwable {
		return generateNikePlusXml(tcxFile, empedID, false);
	}


	public Document generateNikePlusXml(File tcxFile, String empedID, boolean forceExcludeHeartRateData) throws Throwable {
		//try {
			_forceExcludeHeartRateData = forceExcludeHeartRateData;
			Document tcxDoc = Util.generateDocument(tcxFile);
			return generateNikePlusXml(tcxDoc, empedID);
		//}
		//catch (Exception e) {
		//	log.out(e);
		//	return null;
		//}
	}


	public Document generateNikePlusXml(Document inDoc, String empedID) throws Throwable {
		return generateNikePlusXml(inDoc, empedID, null);
	}


	public Document generateNikePlusXml(Document inDoc, String empedID, Integer clientTimeZoneOffset) throws Throwable {
		return generateNikePlusXml(inDoc, empedID, clientTimeZoneOffset, false);
	}

	public Document generateNikePlusXml(Document inDoc, String empedID, Integer clientTimeZoneOffset, boolean forceExcludeHeartRateData) throws Throwable {
		_forceExcludeHeartRateData = forceExcludeHeartRateData;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		//try {
			// Create output document
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document outDoc = db.newDocument();
			
			// Sports Data (root)
			Element sportsDataElement = Util.appendElement(outDoc, "sportsData");

			// Vers
			//Util.appendElement(sportsDataElement, "vers", "8");				// 2010-09-13: I've noticed this is not included in the iphone xml output.

			// Run Summary
			Element runSummary = appendRunSummary(inDoc, sportsDataElement, clientTimeZoneOffset);

			// Template
			appendTemplate(sportsDataElement);

			// Goal Type
			appendGoalType(sportsDataElement);

			// User Info
			appendUserInfo(inDoc, sportsDataElement, empedID);

			// Start Time
			Util.appendElement(sportsDataElement, "startTime", _startTimeString);

			
			// Workout Detail
			appendWorkoutDetail(inDoc, sportsDataElement, runSummary);

			// Test lap durations
			//testLapDuration(inDoc);

			//printDocument(outDoc);
			//writeDocument(outDoc);
			//return new String[] { generateString(outDoc), generateFileName() };
			
			return outDoc;
		//}
		//catch (Exception e) {
		//	log.out(e);
		//	return null;
		//}
	}



	/*
	*/
	/**
	 * Generates a run summary xml element like:
	 * <pre>
	 * {@code
	 * <runSummary>
	 *   <workoutName><![CDATA[Basic]]></workoutName>
	 *   <time>2009-06-12T10:00:00-10:00</time>
	 *   <duration>1760000</duration>
	 *   <durationString>29:20</durationString>
	 *   <distance unit="km">6.47</distance>
	 *   <distanceString>6.47 km</distanceString>
	 *   <pace>4:32 min/km</pace>
	 *   <calories>505</calories>
	 *   <battery></battery>
	 *   <stepCounts>
	 *     <walkBegin>0</walkBegin>
	 *     <walkEnd>0</walkEnd>
	 *     <runBegin>0</runBegin>
	 *     <runEnd>0</runEnd>
	 *   </stepCounts>
	 * </runSummary>
	 * }
	 * </pre>
	 * @param inDoc The garmin tcx document we are reading the data from.
	 * @param sportsDataElement An xml element we have already created "sportsDataElement"
	 * @param clientTimeZoneOffset The client timezone offset to use in the event we are unable to determine workout timezone from the gps coordinates.
	 * @return The runSummary xml element.
	 * @throws DatatypeConfigurationException
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private Element appendRunSummary(Document inDoc, Element sportsDataElement, Integer clientTimeZoneOffset) throws DatatypeConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException {
		Element runSummaryElement = Util.appendElement(sportsDataElement, "runSummary");

		// Workout Name
		//Util.appendCDATASection(runSummaryElement, "workoutName", "Basic");		// 2010-09-13: I've noticed this is not included in the iphone xml output.

		// Start Time
		appendStartTime(inDoc, runSummaryElement, clientTimeZoneOffset);

		// Duration, Duration, Pace & Calories
		appendTotals(inDoc, runSummaryElement);

		// Battery
		Util.appendElement(runSummaryElement, "battery");

		// Step Counts
		//appendStepCounts(runSummaryElement);									// 2010-09-13: I've noticed this is not included in the iphone xml output.

		return runSummaryElement;
	}




	private void appendStartTime(Document inDoc, Element runSummaryElement, Integer clientTimeZoneOffset) throws DatatypeConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException {

		// Garmin:	 2009-06-03T16:59:27.000Z
		// Nike:	 2009-06-03T17:59:27+01:00
		
		// Get the timezone based on the Latitude, Longitude & Time (UTC) of the first TrackPoint
		_workoutTimeZone = getWorkoutTimeZone(inDoc, clientTimeZoneOffset);
		if (_workoutTimeZone == null)
			_workoutTimeZone = TimeZone.getTimeZone("Etc/UTC");

		log.out("Time zone millis offset (vs server): %d", _workoutTimeZone.getRawOffset());

		// Set the the SimpleDateFormat object so that it prints the time correctly:
		// local-time + UTC-difference.
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT_NIKE);
		df.setTimeZone(_workoutTimeZone);
		
		// Get the workout start-time, format for nike+ and add it to the out-document
		Calendar calStart = Util.getCalendarValue(Util.getSimpleNodeValue(inDoc, "Id"));
		Date dateStart = calStart.getTime();
		_startTimeString = df.format(dateStart);
		_startTimeString = String.format("%s:%s", _startTimeString.substring(0, 22), _startTimeString.substring(22));
		Util.appendElement(runSummaryElement, "time", _startTimeString);

		// Generate a human readable start-string we can use for debugging.
		df.applyPattern(DATE_TIME_FORMAT_HUMAN);
		_startTimeStringHuman = df.format(dateStart);

		// We need these later.
		_calStart = calStart;
	}



	// We use the latitude & longitude data along with the http://ws.geonames.org/timezone webservice to
	// deduce in which time zone the workout began.
	private TimeZone getWorkoutTimeZone(Document inDoc, Integer clientTimeZoneOffset) throws DatatypeConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException {
		NodeList positions = inDoc.getElementsByTagName("Position");
		int positionsLength = positions.getLength();

		// Loop through the Position data, we will return as soon as we find one
		// with the required data (latitude & longitude).
		for (int i = 0; i < positionsLength; ++i) {

			Double latitude = null;
			Double longitude = null;
			
			NodeList positionData = positions.item(i).getChildNodes();
			int positionDataLength = positionData.getLength();

			for (int j = 0; j < positionDataLength; ++j) {

				Node n = positionData.item(j);
				String nodeName = n.getNodeName();

				// Latitude at this point
				if (nodeName.equals("LatitudeDegrees")) {
					latitude = Double.parseDouble(Util.getSimpleNodeValue(n));
				}

				// Longitude at this point
				else if (nodeName.equals("LongitudeDegrees")) {
					longitude = Double.parseDouble(Util.getSimpleNodeValue(n));
				}


				if ((latitude != null) && (longitude != null)) {
					// Send a post to the geonames.org webservice with the lat & lng parameters.
					String url = "http://ws.geonames.org/timezone";
					String parameters = String.format("lat=%s&lng=%s", latitude, longitude);
					
					log.out("Looking up time zone for lat/lon: %.4f / %.4f", latitude, longitude);
					try {
						Document response = Util.downloadFile(url, parameters);
						String timeZoneId = Util.getSimpleNodeValue(response, "timezoneId");
						log.out(" - %s", timeZoneId);
						return TimeZone.getTimeZone(timeZoneId);
					}
					catch (Throwable t) {

						// If, for whatever reason we are unable to get the timezone and we have a clientTimeZoneOffset then use that.
						if (clientTimeZoneOffset != null) {
							int hours = clientTimeZoneOffset / 60;
							int minutes = Math.abs(clientTimeZoneOffset) % 60;
							String mm = String.format(((minutes < 10) ? "0%d" : "%d"), minutes);
							String tz = String.format("GMT%s%d:%s", (hours < 0) ? "" : "+", hours, mm);

							log.out("Unable to retrieve workout timezone from http://ws.geonames.org, attempting to use client-timezone %s instead.", tz);
							return TimeZone.getTimeZone(tz);
						}
						else {
							//throw new IOException("Unable to retrieve workout timezone (http://ws.geonames.org is not available right now).  Please try again later.", t);
							TimeZone tz = TimeZone.getDefault();
							log.out(Level.WARNING, "Unable to retrieve workout timezone (http://ws.geonames.org is not available right now).  Using default timezone: %s", tz.getID());
							return tz;
						}
					}
				}
			}
		}

		return null;
	}



	private void appendTotals(Document inDoc, Node parent) {
		double totalSeconds = 0d;
		double totalDistance = 0d;
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
					totalDistance += Double.parseDouble(Util.getSimpleNodeValue(n));

				else if (nodeName.equals("Calories"))
					totalCalories += Integer.parseInt(Util.getSimpleNodeValue(n));
			}
		}


		long totalDuration = (long)(totalSeconds * 1000);

		_totalDuration = totalDuration;
		_totalDistance = totalDistance;

		// Calculate the end-time of the run (for use late to generate the xml filename).
		_calEnd = (Calendar)(_calStart.clone());
		_calEnd.add(Calendar.MILLISECOND, (int)totalDuration);

		// Total Duration
		Util.appendElement(parent, "duration", totalDuration);
		//Util.appendElement(parent, "durationString", getTimeStringFromMillis(totalDuration));		// 2010-09-13: I've noticed this is not included in the iphone xml output.

		// Total Distance
		//totalDistance = totalDistance.divide(BD_1000);
		totalDistance /= 1000;
		Util.appendElement(parent, "distance", String.format("%.4f", totalDistance), "unit", "km");
		//Util.appendElement(parent, "distanceString", String.format("%.2f km", totalDistance));		// 2010-09-13: I've noticed this is not included in the iphone xml output.

		// Pace
		//Util.appendElement(parent, "pace", String.format("%s min/km", getTimeStringFromMillis(calculatePace(totalDuration, totalDistance))));		// 2010-09-13: I've noticed this is not included in the iphone xml output.

		// Calories - FIXME - calories are not calculated properly yet...
		Util.appendElement(parent, "calories", totalCalories);
	}


	/*
	// 2010-09-13: I've noticed this is not included in the iphone xml output.
	private void appendStepCounts(Node parent) {
		Element stepCountsElement = Util.appendElement(parent, "stepCounts");

		Util.appendElement(stepCountsElement, "walkBegin", "0");
		Util.appendElement(stepCountsElement, "walkEnd", "0");
		Util.appendElement(stepCountsElement, "runBegin", "0");
		Util.appendElement(stepCountsElement, "runEnd", "0");
	}
	*/

	/*
	<template><templateID>8D495DCE</templateID>
	<templateName><![CDATA[Basic]]></templateName>
	</template>
	*/
	private void appendTemplate(Element sportsDataElement) {
		Element templateElement = Util.appendElement(sportsDataElement, "template");
		//Util.appendElement(templateElement, "templateID", "8D495DCE");					// 2010-09-13: I've noticed this is not included in the iphone xml output.
		Util.appendCDATASection(templateElement, "templateName", "Basic");
	}


	// <goal type="" value="" unit=""></goal>
	private void appendGoalType(Element sportsDataElement) {
		Util.appendElement(sportsDataElement, "goal", null, "type", "", "value", "", "unit", "");
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
		Element templateElement = Util.appendElement(sportsDataElement, "userInfo");
		Util.appendElement(templateElement, "empedID", (empedID == null) ? "XXXXXXXXXXX" : empedID);
		Util.appendElement(templateElement, "weight");

		// Upload goes through fine when "Garmin Forerunner 405 is specified as device but on nikeplus.com website some data shows as "invalid"
		//Util.appendElement(templateElement, "device",	getSimpleNodeValue(inDoc, "Name"));		// Garmin Forerunner 405
		Util.appendElement(templateElement, "device", "iPod");								// iPod
		Util.appendElement(templateElement, "calibration");
	}

	


	private void appendWorkoutDetail(Document inDoc, Element sportsDataElement, Element runSummaryElement) throws DatatypeConfigurationException {

		// Generate the workout detail from the garmin Trackpoint data.
		ArrayList<Trackpoint> trackpoints = new ArrayList<Trackpoint>();
		ArrayList<Long> pauseResumeTimes = new ArrayList<Long>();
		generateCubicSplineData(inDoc, trackpoints, pauseResumeTimes);

		// Generates the following CubicSplines: distanceToDuration, durationToDistance, durationToPace, durationToHeartRate.
		CubicSpline[] splines = generateCubicSplines(trackpoints);
		appendSnapShotListAndExtendedData(sportsDataElement, pauseResumeTimes, splines[0], splines[1], splines[2], splines[3]);
		
		// Append heart rate detail to the run summary if required.
		if (_includeHeartRateData) {

			Trackpoint min = null;
			Trackpoint max = null;
			int average = 0;

			for (Trackpoint tp : trackpoints) {
				int heartRate = tp.getHeartRate().intValue();
				if ((min == null) || (heartRate < min.getHeartRate())) min = tp;
				if ((max == null) || (heartRate > max.getHeartRate())) max = tp;
				average += (heartRate * tp.getDurationSinceLastTrackpoint());
			}

			average /= _totalDuration;

			Element heartRateElement = Util.appendElement(runSummaryElement, "heartRate");
			Util.appendElement(heartRateElement, "average", average);
			appendHeartRateSummary(heartRateElement, "minimum", min);
			appendHeartRateSummary(heartRateElement, "maximum", max);
			//Util.appendElement(heartRateElement, "battery", 3);							// 2010-09-13: I've noticed this is not included in the iphone xml output.
		}
		
	}

	private void appendHeartRateSummary(Element heartRateElement, String type, Trackpoint tp) {
		Element heartRateTypeElement = Util.appendElement(heartRateElement, type);
		Util.appendElement(heartRateTypeElement, "duration", tp.getDuration());
		Util.appendElement(heartRateTypeElement, "distance", String.format("%.3f", tp.getDistance()/1000));
		Util.appendElement(heartRateTypeElement, "pace", tp.getPace());
		Util.appendElement(heartRateTypeElement, "bpm", tp.getHeartRate().intValue());
	}



	private void generateCubicSplineData(Document inDoc, ArrayList<Trackpoint> trackpointsStore, ArrayList<Long> pauseResumeTimes) throws DatatypeConfigurationException {
		
		// Setup a trackpoint based on the very start of the run in case there is a long pause right at the start.
		Trackpoint previousTp = new Trackpoint(0l, 0d, 0d, null);
		trackpointsStore.add(previousTp);

		NodeList laps = inDoc.getElementsByTagName("Lap");
		int lapsLength = laps.getLength();
		long lapStartDuration = 0;

		for (int i = 0; i < lapsLength; ++i) {
			Node lap = laps.item(i);
			long lapStartTime = Util.getCalendarNodeValue(lap.getAttributes().getNamedItem("StartTime")).getTimeInMillis();
			long lapDuration = (long)(Double.parseDouble(Util.getSimpleNodeValue(Util.getFirstChildByNodeName(lap, "TotalTimeSeconds"))) * 1000);

			log.out(Level.FINE, "Start of lap %d\tDuration: %d -> %d.", (i + 1), lapStartDuration, (lapStartDuration + lapDuration));

			ArrayList<Trackpoint> lapTrackpointStore = generateLapCubicSplineData(lap, pauseResumeTimes, getLastTrackpointFromArrayList(trackpointsStore), lapStartTime, lapStartDuration, lapDuration);
			trackpointsStore.addAll(lapTrackpointStore);

			lapStartDuration += lapDuration;
			log.out(Level.FINE, ("End of lap.\n"));
		}


		// Remove strange trackpoints (repeat distance/durations).
		// Also, for each trackpoint, add a heart-rate reading if one is required & update the previous-trackpoints reference.
		previousTp = null;

		Iterator<Trackpoint> tpsIt = trackpointsStore.iterator();
		while (tpsIt.hasNext()) {
			Trackpoint tp = tpsIt.next();
			tp.setPreviousTrackpoint(previousTp);

			if ((tp.getDistance() == null) || (tp.isRepeatDistance())) {
				log.out(Level.FINE, "Removing invalid distance trackpoint:\t%s", tp);
				tpsIt.remove();
			}
			else if ((tp.getDuration() == null) || (tp.isRepeatDuration())) {
				log.out(Level.FINE, "Removing invalid duration trackpoint:\t%s", tp);
				tpsIt.remove();
			}
			else {
				if ((_includeHeartRateData) && (tp.getHeartRate() == null))
					tp.setHeartRate(previousTp.getHeartRate());

				previousTp = tp;
				log.out(Level.FINER, "Duration: %d\tDistance: %.4f", tp.getDuration(), tp.getDistance());
			}
		}

		//log.out(Level.FINER, "Workout total duration: %d", _totalDuration);
		Trackpoint penultimateTp = getTrackpointFromEndofArrayList(trackpointsStore, 1);
		if (penultimateTp != null) {
			long pDur = penultimateTp.getDuration();
			double pDist = penultimateTp.getDistance();
			log.out("Trackpoint reading complete...");
			log.out("Penultimate vs Required:  Duration (secs): %d -> %d (%d).  Distance (m): %.0f -> %.0f (%.0f)",
				pDur/1000, _totalDuration/1000, ((_totalDuration - pDur)/1000), pDist, _totalDistance, (_totalDistance - pDist));
		}

	}


	private ArrayList<Trackpoint> generateLapCubicSplineData(Node lap, ArrayList<Long> pauseResumeTimes, Trackpoint previousTp, long lapStartTime, long lapStartDuration, long lapDuration) throws DatatypeConfigurationException {

		ArrayList<Trackpoint> lapTrackpointStore = new ArrayList<Trackpoint>();

		long lapEndDuration = lapStartDuration + lapDuration;

		double lapStartDistance = -1;
		double lapDistance = Double.parseDouble(Util.getSimpleNodeValue(Util.getFirstChildByNodeName(lap, "DistanceMeters")));
		Double lapEndDistance = null;

		// Get the trackpoints for this lap - if there are none then continue to the next lap.
		Node[] tracks = Util.getChildrenByNodeName(lap, "Track");

		if (tracks != null) {
			for (Node track : tracks) {
				// Get the trackpoints for this track - if there are none then continue to the next track.
				Node[] trackpoints = Util.getChildrenByNodeName(track, "Trackpoint");
				if (trackpoints == null) continue;

				for (Node trackpoint : trackpoints) {

					// Loop through the data for this trackpoint storing the data.
					Trackpoint tp = new Trackpoint(previousTp);
					NodeList trackPointData = trackpoint.getChildNodes();
					int trackPointDataLength = trackPointData.getLength();

					for (int i = 0; i < trackPointDataLength && tp != null; ++i) {
						Node n = trackPointData.item(i);
						String nodeName = n.getNodeName();

						// Run duration to this point
						if (nodeName.equals("Time"))
							tp.setDuration((Util.getCalendarNodeValue(n).getTimeInMillis() - lapStartTime) + lapStartDuration);

						// Distance to this point
						else if (nodeName.equals("DistanceMeters")) {
							double distance = Double.parseDouble(Util.getSimpleNodeValue(n));

							// If this is the first trackpoint in the lap with a DistanceMeters value then calculate the start/end distance of the lap.
							if (lapStartDistance == -1) {
								lapStartDistance = distance;
								lapEndDistance = lapStartDistance + lapDistance;
							}

							tp.setDistance(distance);
						}

						// Heart rate bpm
						else if ((!_forceExcludeHeartRateData) && (nodeName.equals("HeartRateBpm"))) {
							NodeList heartRateData = n.getChildNodes();
							int heartRateDataLength = heartRateData.getLength();

								for (int j = 0; j < heartRateDataLength; ++j) {
								Node heartRateNode = heartRateData.item(j);
								if (heartRateNode.getNodeName().equals("Value")) {
									_includeHeartRateData = true;
									tp.setHeartRate(Double.parseDouble(Util.getSimpleNodeValue(heartRateNode)));
								}
							}
						}
					}

					// Store the trackpoint for validation/conversion later.
					if ((tp != null) && ((tp.getDistance() != null) || (trackPointDataLength == 3))) {
						lapTrackpointStore.add(tp);
						previousTp = tp;

						log.out(Level.FINEST, "New raw tcx-file Trackpoint, durations: %d -> %d = %d.  distance: %.4f.", tp.getPreviousDuration(), tp.getDuration(), tp.getDuration() - tp.getPreviousDuration(), tp.getDistance());
					}
				}
			}
		}


		// Add a Trackpoint for the end of the lap if we haven't already got one at that distance/duration.
		Trackpoint lastTp = getLastTrackpointFromArrayList(lapTrackpointStore);
		if ((lapEndDistance != null) && (lastTp.getMostRecentDistance() < lapEndDistance) && (lastTp.getDuration() < lapEndDuration))
			lapTrackpointStore.add(new Trackpoint(lapEndDuration, lapEndDistance, previousTp.getHeartRate(), lastTp));

		validateLapCubicSplineData(lap, lapTrackpointStore, pauseResumeTimes, lapEndDuration);

		long difference = lapTrackpointDurationVsLapEndDuration(lapTrackpointStore, lapEndDuration);
		log.out(Level.FINE, "Lap duration difference after validation:\t%d", difference);

		return lapTrackpointStore;
	}


	private void validateLapCubicSplineData(Node lap, ArrayList<Trackpoint> lapTrackpointStore, ArrayList<Long> pauseResumeDurations,  long lapEndDuration) {

		// If we have no trackpoints then we don't need to validate.
		if (lapTrackpointStore.size() == 0) return;

		Node lapTotalTimeSeconds = Util.getFirstChildByNodeName(lap, "TotalTimeSeconds");
		if (lapTotalTimeSeconds == null) return;

		long difference = lapTrackpointDurationVsLapEndDuration(lapTrackpointStore, lapEndDuration);
		log.out(Level.FINE, "Lap duration difference before validation:\t%d", difference);

		boolean paused = false;
		long durationPaused = 0;
		long lengthDecrement = 0;

		Trackpoint previousTp = null;

		// For pause/resmumes
		// - 1. Get the duration of the pause trackpoint to use for the pause/resume duration.
		// - 2. Remove the pause trackpoint.  Our cublic splies will work out what the actual distance was up until this point.
		// - 3. The next trackpoint (or end of lap) will be the resume Trackpoint.
		// - 4. Use the duration data of this resume Trackpoint to calculate the length we were paused for.
		// - 5. Deduct the pause-length from all proceeding durations in the lap.
		Iterator<Trackpoint> tpsIt = lapTrackpointStore.iterator();
		while (tpsIt.hasNext()) {
			Trackpoint tp = tpsIt.next();
			tp.decrementDuration(lengthDecrement);

			// Deal with 3-data pause/resume trackpoints.
			// ==========================================
			// If we are not paused already then this is a 'pause' trackpoint:
			//  - 1. Record the duration.
			//  - 2. Set paused=true.
			//  - 3. Remove the trackpoint.
			//  - 4. Loop again (get the next trackpoint).
			//
			// If we are paused then this is a 'resume' trackpoint:
			//  - 1. Store the pause/resume details, adding the length paused to the lengthDecrement variable.
			//  - 2. Set paused=false
			//  - 3. Remove the trackpoint.
			//  - 4. Loop again (get the next trackpoint).
			if (tp.getDistance() == null) {
				if (!paused) durationPaused = tp.getDuration();
				else lengthDecrement += addPauseResume(pauseResumeDurations, tp, durationPaused);
				paused = !paused;
				tpsIt.remove();
				continue;
			}

			// Deal with normal trackpoints.
			// =============================
			// If we are paused then this is a 'resume' trackpoint:
			//  - 1. Store the pause/resume details
			//  - 2. Add the length paused to the lengthDecrement variable.
			//  - 3. Decrement the current trackpoint with the amount we were paused for.
			//  - 4. Set paused = false.
			if (paused) {
				long lengthPaused = addPauseResume(pauseResumeDurations, tp, durationPaused);
				lengthDecrement += lengthPaused;
				tp.decrementDuration(lengthPaused);
				paused = false;
			}

			tp.setPreviousTrackpoint(previousTp);
			previousTp = tp;
		}

		// After applying the tcx-detailed pause/resumes we still might have trackpoints whose durations exceed
		// the lap time.  I think the primary reason for this is that garmin automatically pauses the device if it is
		// unable to get a satellite waypoint after a certain amount of seconds (seems to be 8 seconds on the 405 with
		// 2.50 firmware).  To counter this I will basically strip away the slowest-paced (hopefully paused) sections of the
		// lap until our final trackpoint has a less than or equal to that of the lap duration.
		difference = lapTrackpointDurationVsLapEndDuration(lapTrackpointStore, lapEndDuration);

		// We allow up to 999 millis as due to rounding errors the lap-duration can be up to 1 second out.
		if (difference > 0) {
			log.out("Lap duration still invalid by %d millis, manually stripping.", difference);
			boolean modified = true;

			do {
				modified = false;
				double paceWorst = Double.MAX_VALUE;
				Trackpoint paceWorstTp = null;
				tpsIt = lapTrackpointStore.iterator();
				while (tpsIt.hasNext()) {
					Trackpoint tp = tpsIt.next();
					// If our previous trackpoint is null then this is the first trackpoint so continue to the next trackpoint.
					if (tp.getPreviousTrackpoint() == null) continue;

					long duration = tp.getDuration();
					long durationPrevious = tp.getPreviousDuration();
					double distance = tp.getDistance();
					double distancePrevious = tp.getPreviousDistance();

					// Calculate distance & duration differences.
					double distanceIncrease = distance - distancePrevious;
					long durationIncrease = duration - tp.getPreviousDuration();

					// if we have a distance increase of zero then remove the trackpoint and decrement all future trackpoints/pause-resumes by the duration we didn't increase for.
					if ((distanceIncrease == 0) && (durationIncrease > 0)) {
						long decrementLength = (difference > durationIncrease) ? durationIncrease : difference;
						log.out(Level.FINE, "Zero-distance decrement:\tDuration %d\tDistance %.4f\tLength: %d", durationPrevious, distancePrevious, decrementLength);
						tpsIt.remove();
						if (tpsIt.hasNext()) {
							Trackpoint nextTp = tpsIt.next();
							nextTp.decrementDuration(decrementLength);
							nextTp.setPreviousTrackpoint(previousTp);
						}
						while (tpsIt.hasNext()) tpsIt.next().decrementDuration(decrementLength);
						decrementPauseResumes(pauseResumeDurations, durationPrevious, decrementLength);
						modified = true;
						break;
					}

					double pace = (durationIncrease > MILLIS_POTENTIAL_PAUSE_THRESHOLD) ? (distanceIncrease / durationIncrease) : Double.MAX_VALUE;
					if (pace < paceWorst) {
						paceWorst = pace;
						paceWorstTp = tp;
					}
				}

				// If we haven't found any zero-distance trackpiont increases then decrement all future trackpoints/pause-resumes from the slowest pace trackpoint pair of the lap.
				if ((paceWorstTp != null) && !(modified)) {
					log.out(Level.FINE, "Slowest-pace decrement:\tDuration %d\tDistance %.4f", paceWorstTp.getPreviousDuration(), paceWorstTp.getPreviousDistance());
					modified = true;
					tpsIt = lapTrackpointStore.iterator();
					while (tpsIt.hasNext()) {
						Trackpoint tp = tpsIt.next();
						if (tp.equals(paceWorstTp)) {
							tp.decrementDuration(1000);
							while (tpsIt.hasNext()) tpsIt.next().decrementDuration(1000);
							decrementPauseResumes(pauseResumeDurations, tp.getDuration(), 1000);

						}
					}
				}
			}
			while ((modified) && ((difference = lapTrackpointDurationVsLapEndDuration(lapTrackpointStore, lapEndDuration)) > 0));
		}
		
	}

	/**
	 * Get the difference (in millis) between the duration of the final trackpoint in the trackpoint-store and the expected lap-end-duration.
	 * @param lapTrackpointStore An ArrayList of trackpoints for the lap - the last element in this collection should have duration less than lapEndDuration.
	 * @param lapEndDuration The expected duration at the end of the lap.
	 * @return The difference (positive if the trackpoint store exceeds the expected lap-end-duration).
	 */
	private long lapTrackpointDurationVsLapEndDuration(ArrayList<Trackpoint> lapTrackpointStore, long lapEndDuration) {
		return (lapTrackpointStore.size() == 0)
			? 0
			: roundToNearestThousand(getLastTrackpointFromArrayList(lapTrackpointStore).getDuration()) - roundToNearestThousand(lapEndDuration)
		;
	}

	/**
	 * Use this to round lap durations.  The duration specified in the <code>&lt;Lap&gt;</code> element is to the nearest
	 * millisecond whereas the data in the <code>&lt;Trackpoint&gt;</code> elements is to the nearest second.
	 * @param n The number to round to the nearest thousand.
	 * @return The rounded number.
	 */
	private long roundToNearestThousand(long n) {
		return (Math.round((double)n / 1000) * 1000);
	}


	/**
	 * Decrement pause-resumes which are greater than the durationMinimum.
	 * @param al The ArrayList of pause-resumes to modify.
	 * @param durationMinimum We only modifiy entries with a value greater than this.
	 * @param decrementLength How much to decrement the entries.
	 */
	private void decrementPauseResumes(ArrayList<Long> al, long durationMinimum, long decrementLength) {
		Iterator prdIt = al.iterator();
		int index = 0;
		while (prdIt.hasNext()) {
			Long prDuration = (Long)prdIt.next();
			if (prDuration > durationMinimum)
				al.set(index, prDuration - decrementLength);
			index++;
		}
	}
	

	/**
	 * Call this when we find a 'resume' trackpoint the next trackpoint immeadiately following a 'pause' trackpoint.
	 * We add the pause/resume details to the pauseResumeTImes ArrayList and return the length of time (in millis) that the
	 * device was paused for.
	 * <p>
	 * Nike+ currently just use the same duration/distance to represent both the pause and the resume so I only store the pauseTime.
	 * @param pauseResumeTimes The store of pause/resume times.  If not provided then we don't store the times.
	 * @param resumeTp The 'resume' trackpoint.
	 * @param durationPaused The duration of the previous 'pause' trackpoint.
	 * @return The length of time the device was paused for (so we can update decrement future trackpoint durations).
	 */
	private long addPauseResume(ArrayList<Long> pauseResumeTimes, Trackpoint resumeTp, long durationPaused) {
		long durationResume = resumeTp.getDuration();
		long pauseLength = (durationResume- durationPaused);
		if (pauseResumeTimes != null) {
			pauseResumeTimes.add(durationPaused);
			log.out(Level.FINER, "Adding pause/resume.\tDuration %d\tDistance %.4f.\tlength %d", durationPaused, resumeTp.getDistance(), pauseLength);
		}
		return pauseLength;
	}
	

	/**
	 * Generates the following CubicSplines: distanceToDuration, durationToDistance, durationToPace, durationToHeartRate.
	 * @param trackpoints The Trackpoint data from which to build the CubicSplines.
	 * @return A CubicSpline array in represnting the data listed in the description above.
	 */
	private CubicSpline[] generateCubicSplines(ArrayList<Trackpoint> trackpoints) {
		int tpsSize = trackpoints.size();
		double[] durationsArray = new double[tpsSize];
		double[] distancesArray = new double[tpsSize];
		double[] pacesArray = new double[tpsSize];
		double[] heartRatesArray = new double[tpsSize];

		populateDurationsAndDistancesArrays(trackpoints, durationsArray, distancesArray, pacesArray, heartRatesArray);
		
		// Generate cubic splines for distance -> duration, duration -> distance & distance -> pace.
		// I'd have thought the flanagan classes would have offered some method of inverting the data, but I've not looked into it and this hack will do for now.
		CubicSpline distanceToDuration = new CubicSpline(distancesArray, durationsArray);
		CubicSpline durationToDistance = new CubicSpline(durationsArray, distancesArray);
		CubicSpline durationToPace = new CubicSpline(durationsArray, pacesArray);

		// Heartrate cubic spline
		CubicSpline durationToHeartRate = new CubicSpline(durationsArray, heartRatesArray);
		
		return new CubicSpline[] { distanceToDuration, durationToDistance, durationToPace, durationToHeartRate };
	}


	private void appendSnapShotListAndExtendedData(Element sportsDataElement, ArrayList<Long> pauseResumeTimes, CubicSpline distanceToDuration, CubicSpline durationToDistance, CubicSpline durationToPace, CubicSpline durationToHeartRate) {
		Element snapShotKmListElement = Util.appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "kmSplit");
		Element snapShotMileListElement = Util.appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "mileSplit");
		Element snapShotClickListElement = Util.appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "userClick");

		// Pause/Resume splits.
		Iterator<Long> prIt = pauseResumeTimes.iterator();
		while (prIt.hasNext()) {
			long pauseDuration = prIt.next();

			// Sometimes a pause/resume happens at the very end of a workout - if this is the case then we will not have decremented the
			// durations properly so just leave them out - there's no point in documeting a pause directly before the end of the workout anyway.
			if (pauseDuration < _totalDuration) {
				// 2009-12-01: Looking at various runs on nike+ it seems the each pause/resume pair now has the same duration/distance
				// (using the pause event).  I can't find my nike+ stuff to check this is 100% accurate but will go with it for now.
				double distance = interpolate(durationToDistance, pauseDuration);
				long pace = (long)(interpolate(durationToPace, pauseDuration));
				int heartRateBpm = (int)(interpolate(durationToHeartRate, pauseDuration));
				appendSnapShot(snapShotClickListElement, pauseDuration, distance, pace, heartRateBpm, "event", "pause");
				appendSnapShot(snapShotClickListElement, pauseDuration, distance, pace, heartRateBpm, "event", "resume");
			}
		}

		// Km splits
		for (int i = 1000; i <= _totalDistance; i += 1000) {
			double duration = (long)(interpolate(distanceToDuration, i));
			appendSnapShot(snapShotKmListElement, (long)duration, i, (long)(interpolate(durationToPace, duration)), (int)(interpolate(durationToHeartRate, duration)));
		}

		// Mile splits
		for (double i = D_METRES_PER_MILE; i <= _totalDistance; i += D_METRES_PER_MILE) {
			double duration = (long)(interpolate(distanceToDuration, i));
			appendSnapShot(snapShotMileListElement, (long)duration, i, (long)(interpolate(durationToPace, duration)), (int)(interpolate(durationToHeartRate, duration)));
		}

		// Stop split
		appendSnapShot(snapShotClickListElement, _totalDuration, _totalDistance, (long)(interpolate(durationToPace, _totalDuration)), (int)(interpolate(durationToHeartRate, _totalDuration)), "event", "stop");

		// ExtendedDataLists
		appendExtendedDataList(sportsDataElement, durationToDistance, durationToPace, durationToHeartRate);
	}


	// Copy data from the trackpoint ArrayList into the durations & distances arrays in preparation for creating CubicSplines.
	private void populateDurationsAndDistancesArrays(ArrayList<Trackpoint> trackpoints, double[] durations, double[] distances, double[] paces, double[] heartRates) {
		Iterator<Trackpoint> tpsIt = trackpoints.iterator();
		int i = 0;
		while (tpsIt.hasNext()) {
			Trackpoint tp = tpsIt.next();
			if (_includeHeartRateData) heartRates[i] = tp.getHeartRate();
			durations[i] = tp.getDuration();
			distances[i] = tp.getDistance();
			paces[i++] = tp.getPace();
		}
	}
	

	/*
	<extendedDataList>
	  <extendedData dataType="distance" intervalType="time" intervalUnit="s" intervalValue="10">0.0. 1.1, 2.3, 4.7, etc</extendedData>
	</extendedDataList>
	*/
	private void appendExtendedDataList(Element sportsDataElement, CubicSpline durationToDistance, CubicSpline durationToPace, CubicSpline durationToHeartRate) {
		int finalReading = (int)(durationToDistance.getXmax());
		double previousDistance = 0;
		StringBuilder sbDistance = new StringBuilder("0.0");
		StringBuilder sbSpeed = new StringBuilder("0.0");
		StringBuilder sbHeartRate = new StringBuilder();

		if (_includeHeartRateData) sbHeartRate.append((int)(interpolate(durationToHeartRate, 0d)));

		for (int i = 10000; i < finalReading; i = i + 10000) {

			// Distance
			double distance = (interpolate(durationToDistance, i))/1000;

			// 2010-06-18: Phillip Purcell emailed me two workouts on 2010-06-14 which did not draw graphs on nikeplus.com
			// A bit of debugging showed that some of the interpolated distances in these workouts were  less than the
			// previous distance (impossible).  Hacked this fix so that this will never happen.
			if (distance < previousDistance) distance = previousDistance;
			sbDistance.append(String.format(", %.6f", distance));
			previousDistance = distance;

			// Speed
			sbSpeed.append(String.format(", %.6f", MILLIS_PER_HOUR/(interpolate(durationToPace, i))));

			// Heart Rate
			if (_includeHeartRateData) sbHeartRate.append(String.format(", %d", (int)(interpolate(durationToHeartRate, i))));
		}

		Element extendedDataListElement	= Util.appendElement(sportsDataElement, "extendedDataList");
		Util.appendElement(extendedDataListElement, "extendedData", sbDistance, "dataType", "distance", "intervalType", "time", "intervalUnit", "s", "intervalValue", "10");
		Util.appendElement(extendedDataListElement, "extendedData", sbSpeed, "dataType", "speed", "intervalType", "time", "intervalUnit", "s", "intervalValue", "10");

		if (_includeHeartRateData)
			Util.appendElement(extendedDataListElement, "extendedData", sbHeartRate, "dataType", "heartRate", "intervalType", "time", "intervalUnit", "s", "intervalValue", "10");

	}


	private void appendSnapShot(Element snapShotListElement, long durationMillis, double distanceMetres, long paceMillisKm, int heartRateBpm, String ... attributes) {
		Element snapShotElement = Util.appendElement(snapShotListElement, "snapShot", null, attributes);

		Util.appendElement(snapShotElement, "duration", durationMillis);
		Util.appendElement(snapShotElement, "distance", String.format("%.3f", distanceMetres/1000));
		Util.appendElement(snapShotElement, "pace", paceMillisKm);
		if (_includeHeartRateData)
			Util.appendElement(snapShotElement, "bpm", heartRateBpm);
	}


	private double interpolate(CubicSpline spline, double x) {
		if (x < spline.getXmin()) x = spline.getXmin();
		else if (x > spline.getXmax()) x = spline.getXmax();

		return spline.interpolate(x);
	}
	


	public String generateFileName() {
		SimpleDateFormat outfileFormat = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss'.xml'");
		outfileFormat.setTimeZone(_workoutTimeZone);
		return outfileFormat.format(_calEnd.getTime());
	}

	public String getStartTimeHumanReadable() {
		return _startTimeStringHuman;
	}


	private Trackpoint getLastTrackpointFromArrayList(ArrayList<Trackpoint> al) {
		return getTrackpointFromEndofArrayList(al, 0);
	}

	private Trackpoint getTrackpointFromEndofArrayList(ArrayList<Trackpoint> al, int placesFromEnd) {
		return ((al != null) && (al.size() > placesFromEnd)) ? al.get(al.size() - (placesFromEnd + 1)) : null;
	}




	public static void main(String[] args) {
		File inFile = new File(args[0]);

		String empedID = (args.length >= 2) ? args[1] : null;

		ConvertTcx c = new ConvertTcx();
		try {
			Document doc = c.generateNikePlusXml(inFile, empedID);
			Util.writeDocument(doc, c.generateFileName());
		}
		catch (Throwable t) {
			log.out(t);
		}
	}


	


	private class Trackpoint
	{
		private static final int		PACE_MILLIS = 20 * 10000;		// How many milli seconds of data to use when calculating pace.

		private Long		_duration;
		private Double		_distance;
		private Double		_heartRate;
		private Trackpoint	_previousTrackpoint;

		public Trackpoint(Trackpoint previousTrackpoint) {
			_previousTrackpoint = previousTrackpoint;
		}

		/**
		 * A representatino of a garmin <trackpoint> element.
		 * @param duration Current (relative to start of workout) duration in millis.
		 * @param distance Current distance in metres.
		 * @param heartRate Current heartrate in bpm.
		 * @param previousTrackpoint The previously recorded trackpoint, or null if this is the first trackpoint.
		 */
		public Trackpoint(long duration, double distance, Double heartRate, Trackpoint previousTrackpoint) {
			_duration = duration;
			_distance = distance;
			_heartRate = heartRate;
			_previousTrackpoint = previousTrackpoint;
		}


		protected Long getDuration() {
			return _duration;
		}

		protected Double getDistance() {
			return _distance;
		}

		protected Double getHeartRate() {
			return _heartRate;
		}

		protected void setDuration(Long duration) {
			_duration = duration;
		}

		protected void setDistance(Double distance) {
			_distance = distance;
		}

		protected void setHeartRate(Double heartRate) {
			_heartRate = heartRate;
		}

		protected void setPreviousTrackpoint(Trackpoint previousTrackpoint) {
			_previousTrackpoint = previousTrackpoint;
		}


		/**
		 * Nike+ uses the number of millis it takes to complete 1km as their pace figure.
		 * @return Nike+ pace.
		 */
		protected long getPace() {
			// This can give skewed pace figures as we might have a trackpoint 2 seconds ago with slightly inaccurate distance resulting in wildly chaotic pace data.
			//Trackpoint previous = getPreviousDifferentTrackpoint();

			// I use PACE_MILLIS previous seconds of data to calculate pace (avoiding the problem caused by getPreviousDifferntTrackpoint).
			long startDuration = (_duration > PACE_MILLIS) ? _duration - PACE_MILLIS : 0;
			Trackpoint startTp = getPreviousTrackpoint();
			while ((startTp != null) && (startTp.getDuration() > startDuration))
				startTp = startTp.getPreviousTrackpoint();

			long splitDuration = (startTp == null) ? _duration : (_duration - startTp.getDuration());
			double splitDistance = (startTp == null) ? _distance : (_distance - startTp.getDistance());

			return (long)((1000d/splitDistance) * splitDuration);
		}

		protected Trackpoint getPreviousTrackpoint() {
			return _previousTrackpoint;
		}

		protected Double getMostRecentDistance() {
			Trackpoint tp = this;
			while ((tp != null)) {
				if (tp.getDistance() != null) return tp.getDistance();
				tp = tp.getPreviousTrackpoint();
			}
			return 0d;
		}

		protected long getDurationSinceLastTrackpoint() {
			return (_previousTrackpoint == null) ? 0 : (_duration - _previousTrackpoint.getDuration());
		}

		protected boolean isRepeatDuration() {
			return (_previousTrackpoint != null) && (_duration.equals(_previousTrackpoint.getDuration()));
		}

		protected boolean isRepeatDistance() {
			return (_previousTrackpoint != null) && (_distance.equals(_previousTrackpoint.getDistance()));
		}

		protected Long getPreviousDuration() {
			return (_previousTrackpoint == null) ? 0 : _previousTrackpoint.getDuration();
		}

		protected Double getPreviousDistance() {
			return (_previousTrackpoint == null) ? 0 : _previousTrackpoint.getDistance();
		}

		protected void incrementDuration(long millis) {
			_duration += millis;
		}

		protected void decrementDuration(long millis) {
			_duration -= millis;
		}

		
		@Override
		public String toString() {
			return String.format("Duration (difference): %d\t(%d)\t\tDistance: %.4f", _duration, (_duration - ((_previousTrackpoint == null) ? 0 : _previousTrackpoint.getDuration())), _distance);
		}
	}
}