package com.awsmithson.tcx2nikeplus.converter;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import flanagan.interpolation.CubicSpline;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
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

	private static final double				D_METRES_PER_MILE = 1609.344;
	private static final String				DATE_TIME_FORMAT_NIKE = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String				DATE_TIME_FORMAT_HUMAN = "d MMM yyyy HH:mm:ss z";

	private long _totalDuration;
	private double _totalDistance;

	private TimeZone _workoutTimeZone;
	private Calendar _calStart;
	private Calendar _calEnd;
	private String _startTimeString;
	private String _startTimeStringHuman;

	private final static Log log = Log.getInstance();


	public Convert() {
	}


	public Document generateNikePlusXml(File tcxFile, String empedID) throws Throwable {
		//try {
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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		//try {
			// Create and load input document
			DocumentBuilder db = dbf.newDocumentBuilder();

			// Create output document
			Document outDoc = db.newDocument();
			


			// Sports Data (root)
			Element sportsDataElement = appendElement(outDoc, "sportsData");

			// Vers
			appendElement(sportsDataElement, "vers", "2");

			// Run Summary
			appendRunSummary(inDoc, sportsDataElement, clientTimeZoneOffset);

			// Template
			appendTemplate(sportsDataElement);

			// Goal Type
			appendGoalType(sportsDataElement);

			// User Info
			appendUserInfo(inDoc, sportsDataElement, empedID);

			// Start Time
			appendElement(sportsDataElement, "startTime", _startTimeString);

			
			// Workout Detail
			appendWorkoutDetail(inDoc, sportsDataElement);


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
	private Element appendRunSummary(Document inDoc, Element sportsDataElement, Integer clientTimeZoneOffset) throws DatatypeConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException {
		Element runSummaryElement = appendElement(sportsDataElement, "runSummary");

		// Workout Name
		appendCDATASection(runSummaryElement, "workoutName", "Basic");

		// Start Time
		appendStartTime(inDoc, runSummaryElement, clientTimeZoneOffset);

		// Duration, Duration, Pace & Calories
		appendTotals(inDoc, runSummaryElement);

		// Battery
		appendElement(runSummaryElement, "battery");

		// Step Counts
		appendStepCounts(runSummaryElement);

		return runSummaryElement;
	}




	private void appendStartTime(Document inDoc, Element runSummaryElement, Integer clientTimeZoneOffset) throws DatatypeConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException {

		// Garmin:	 2009-06-03T16:59:27.000Z
		// Nike:	 2009-06-03T17:59:27+01:00
		
		// Get the timezone based on the Latitude, Longitude & Time (UTC) of the first TrackPoint
		_workoutTimeZone = getWorkoutTimeZone(inDoc, clientTimeZoneOffset);
		if (_workoutTimeZone == null)
			_workoutTimeZone = TimeZone.getTimeZone("Etc/UTC");

		log.out(_workoutTimeZone.getRawOffset());

		// Set the the SimpleDateFormat object so that it prints the time correctly:
		// local-time + UTC-difference.
		SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT_NIKE);
		df.setTimeZone(_workoutTimeZone);
		
		// Get the workout start-time, format for nike+ and add it to the out-document
		Calendar calStart = getCalendarValue(Util.getSimpleNodeValue(inDoc, "Id"));
		Date dateStart = calStart.getTime();
		_startTimeString = df.format(dateStart);
		_startTimeString = String.format("%s:%s", _startTimeString.substring(0, 22), _startTimeString.substring(22));
		appendElement(runSummaryElement, "time", _startTimeString);

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
						else
							throw new IOException("Unable to retrieve workout timezone (http://ws.geonames.org is not available right now).  Please try again later.", t);
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
		appendElement(parent, "duration", totalDuration);
		appendElement(parent, "durationString", getTimeStringFromMillis(totalDuration));

		// Total Distance
		//totalDistance = totalDistance.divide(BD_1000);
		totalDistance /= 1000;
		appendElement(parent, "distance", String.format("%.4f", totalDistance), "unit", "km");
		appendElement(parent, "distanceString", String.format("%.2f km", totalDistance));

		// Pace
		appendElement(parent, "pace", String.format("%s min/km", getTimeStringFromMillis(calculatePace(totalDuration, totalDistance))));

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

	


	private void appendWorkoutDetail(Document inDoc, Element sportsDataElement) throws DatatypeConfigurationException {
		ArrayList<Trackpoint> trackpoints = new ArrayList<Trackpoint>();
		ArrayList<Long> pauseResumeTimes = new ArrayList<Long>();

		// Generate the workout detail from the garmin Trackpoint data.
		generateCubicSplineData(inDoc, trackpoints, pauseResumeTimes);

		//System.out.printf("totalDistance: %.4f\ttpsDistance: %.4f\t\ttotalDuration: %d\ttpsDuration: %d\n",
		//		_totalDistance, trackpoints.get(trackpoints.size()-1).getDistance(),
		//		_totalDuration, trackpoints.get(trackpoints.size()-1).getDuration());

		// Validate our data and attempt to fix it if there are any problems.
		validateCubicSplineData(trackpoints, pauseResumeTimes);

		appendSnapShotListAndExtendedData(sportsDataElement, trackpoints, pauseResumeTimes);
	}


	private Node[] getChildrenByNodeName(Node parent, String nodeName) {

		ArrayList<Node> al = new ArrayList<Node>();
		
		NodeList children = parent.getChildNodes();
		int childCount = children.getLength();

		for (int i = 0; i < childCount; ++i) {
			Node n = children.item(i);
				if (n.getNodeName().equals(nodeName))
					al.add(n);
		}
		
		return (al.size() > 0) ? al.toArray(new Node[0]) : null;
	}


	private void generateCubicSplineData(Document inDoc, ArrayList<Trackpoint> trackpointsStore, ArrayList<Long> pauseResumeTimes) throws DatatypeConfigurationException {
		long startDurationOriginal = _calStart.getTimeInMillis();
		long startDurationAdjusted = startDurationOriginal;
		
		// Setup a trackpoint based on the very start of the run in case there is a long pause right at the start.
		Trackpoint previousTp = new Trackpoint(0l, 0d, null);
		trackpointsStore.add(previousTp);

		NodeList laps = inDoc.getElementsByTagName("Lap");
		int lapsLength = laps.getLength();

		for (int i = 0; i < lapsLength; ++i) {

			// Get the trackpoints for this lap - if there are none then continue to the next lap.
			Node[] tracks = getChildrenByNodeName(laps.item(i), "Track");
			if (tracks == null) continue;

			// Get the lap start & end duration so that if there are any trackpoints outwith these times we can ignore them.
			//long lapStartDuration = getCalendarNodeValue(laps.item(i).getAttributes().getNamedItem("StartTime")).getTimeInMillis();
			//long lapEndDuration = lapStartDuration + (long)(Double.parseDouble(Util.getSimpleNodeValue(getChildrenByNodeName(laps.item(i), "TotalTimeSeconds")[0])) * 1000);

			for (Node track : tracks) {

				// Ugly hack to prepare a array of nodes (elements only) which we can easily refer to by index later when we need to jump-about between nodes to calculate pause/resume.
				ArrayList<Node> al = new ArrayList<Node>();

				Node child = track.getFirstChild();
				do {
					if (child.getNodeType() == child.ELEMENT_NODE)
						al.add(child);
				}
				while ((child = child.getNextSibling()) != null);

				/*
				for (int z = 0; z < track.getChildNodes().getLength(); ++z) {
					if (track.getChildNodes().item(z).getNodeType() == track.getChildNodes().item(z).ELEMENT_NODE)
						al.add(track.getChildNodes().item(z));
				}
				*/
				
				Node[] trackPoints = al.toArray(new Node[0]);
				int trackPointsLength = trackPoints.length;

				// Always generate a potential-pause on the first trackpoint of each lap/track in case the watch was paused at the start.
				boolean forcePotentialPause = true;

				for (int j = 0; j < trackPointsLength; ++j) {

					NodeList trackPointData = trackPoints[j].getChildNodes();
					int trackPointDataLength = trackPointData.getLength();

					//System.out.printf("%s\t%d\n", trackPointData.toString(), trackPointData.getLength());

					// First deal with pause/resume pairs.
					// 2010-04-28: Some workouts have multiple tracks and the first trackpoint in each is a simple time-only trackpoint - ignore these.
					if ((trackPointDataLength == 3) && (j+1 < trackPointsLength) && (j > 0)) {

						// Get what we are expecting to be the resume track-point-data.
						// 2010-03-15: This doesn't work because there are workouts (see garmin-connect-id 25956404) that have a big list of times whilst the device is paused.
						//trackPointData = trackPoints.item(++i).getChildNodes();

						int pauseIndex = j;
						//System.out.println((trackPoints.item(j+1).getChildNodes().getLength() == 3) && (j+2 < trackPointsLength));
						//System.out.println(trackPoints.item(j+2).getChildNodes().getLength());

						// The trackpoint before the next "full" trackpoint will be used as our resume time.
						while ((trackPoints[j+1].getChildNodes().getLength() == 3) && (j+2 < trackPointsLength))
							trackPointData = trackPoints[++j].getChildNodes();

						// If we don't have a resume trackpoint then use the next real trackpoint as the resume.
						int resumeIndex = (pauseIndex == j) ? (j + 1) : j;

						// As we are expecting it to be a resume trackPointData it must also be of length 3, otherwise we are not dealing with a pause/resume pair so ignore the pause.
						if ((trackPointData.getLength()) == 3) {

							// Create a pause/resume event.
							long timePaused = createPauseResume(pauseResumeTimes, trackPoints, pauseIndex, resumeIndex, startDurationAdjusted);
							startDurationAdjusted += timePaused;
							//lapEndDuration += timePaused;
							forcePotentialPause = false;

							// Continue onto the next trackpoint, we've stored our pause/resume data for later use.
							continue;
						}
					}


					// If we reach this point then this is a normal trackpoint, not a pause/resume.
					Double distance = null;
					Long duration = null;

					// Loop through the data for this trackpoint until we have duration & distance data which we can store.
					for (int k = 0; k < trackPointDataLength; ++k) {
						Node n = trackPointData.item(k);
						String nodeName = n.getNodeName();

						// Run duration to this point
						if (nodeName.equals("Time")) {
							duration = getCalendarNodeValue(n).getTimeInMillis();
							// 2010-04-04: If we have a duration outwith the duration of the current lap then ignore the trackpoint.  See Oliver Schulz's 2010-03-15 workout (emailed to me).
							// No longer needed since we added the laps/tracks breakdown.
							//if ((duration < lapStartDuration) || (duration > lapEndDuration)) {
								//System.out.printf("%d\t%d\t%d\n", duration, lapEndDuration, (duration - lapEndDuration));
								//break;
							//}

							duration -= startDurationAdjusted;
						}

						// Distance to this point
						else if (nodeName.equals("DistanceMeters"))
							distance = Double.parseDouble(Util.getSimpleNodeValue(n));

						// We require valid duration & distance data to create a Trackpoint - otherwise just ignore and move on to the next iteration.
						if ((duration != null) && (distance != null)) {
							
							// I have a run where my 405 froze and when it rebooted it started recording distances lower than what it
							// had already recorded!  Garmin Connect ID: 27181644 trackpoint at 2010-03-16T18:12:11.000Z.
							// If we find a workout like this then ignore the trackpoints that are "wrong" and force a potential-pause addition
							// for the next valid trackpoint we find.
							if (distance < previousTp.getDistance()) {
								forcePotentialPause = true;
								break;
							}

							Trackpoint tp = new Trackpoint(duration, distance, previousTp);
							previousTp = tp;

							// If this trackpoint has the same duration as the last trackpoint do not add it to the trackpoints list.
							// 2010-04-06: We now also generate a potential-pause on the previous trackpoint in the device was paused prior to these identical-duration trackpoints.
							if (tp.isRepeatDuration()) {
								tp.getPreviousTrackPoint().generatePotentialPauseDuration();
								break;
							}

							// If this trackpoint distance is the same as the previous one then keep track of it, we might need to
							// create a pause/resume pair from it later to ensure our calculated distance does not exceed the total distance.
							if ((forcePotentialPause) || (tp.isRepeatDistance())) {
								tp.generatePotentialPauseDuration();
								forcePotentialPause = false;
							}

							// We have valid and new duration/distance combinations so store them for use later and move onto the next Trackpoint.
							trackpointsStore.add(tp);

							// Break out of the loop and look at the next trackpoint - we have all the data we require from this one.
							break;
						}
					}
				}
			}
		}
	}



	private long createPauseResume(ArrayList<Long> pauseResumeTimes, Node[] trackPoints, int pauseIndex, int resumeIndex, long startDurationAdjusted) throws DatatypeConfigurationException {

		long tpPauseTime = getCalendarNodeValue(trackPoints[pauseIndex].getChildNodes().item(1)).getTimeInMillis();
		long tpResumeTime = getCalendarNodeValue(trackPoints[resumeIndex].getChildNodes().item(1)).getTimeInMillis();
		
		// Save the pause/resume points so that we can create the snapshot event later (once we can interpolate to get the distance).
		// 2009-12-01: Looking at various runs on nike+ it seems the each pause/resume pair now has the same duration/distance
		// (using the pause event).  I can't find my nike+ stuff to check this is 100% accurate but will go with it for now.
		long startDuration = tpPauseTime - startDurationAdjusted;
		if (startDuration > 0) pauseResumeTimes.add(startDuration);

		// Return the length of time (in millis) that the device was paused.
		return tpResumeTime - tpPauseTime;
	}



	// 2010-01-08: Ross sent me one of Liz's activities (garmin activity id 21883594) that had various normal pauses in it then also had 2 sections
	// whereby there were no trackpoints for 35 and 60 seconds respectively.  On the immeadiately following trackpoints the DistanceMeters value
	// is the same as before the gaps.  It seems these should have been marked as pause sections but for some reason were not.
	// When these times are included in the workout data it ends up that the splits (nike+ snapshots) end up exceeding the total workout duration.
	// To counter this problem I now keep a track of such abnormalities in generateCubicSplineData(...) and if the snapshots end up exceeding the workout
	// duration I create pause/resumes until I have valid data.
	// Nike+ currently allows a threshold of 30000ms over the "total duration".
	private void validateCubicSplineData(ArrayList<Trackpoint> trackpoints, ArrayList<Long> pauseResumeTimes) {
		long calculatedDuration = trackpoints.get(trackpoints.size()-1).getDuration();
		boolean retry = true;

		while ((retry) && (calculatedDuration > (_totalDuration + 30000))) {

			// Retry will only be set to true again whilst we are able to create pause/resume splits.  When it
			// remains false we will exit the loop.
			retry = false;

			// Don't "create" a pause for anything shorter than 2 seconds.
			long currentMaxPause = 2000;
			int removeTpIndex = -1;
			int index = 0;

			// Go through the trackpoints fo find the one that has the bigest potential-pause-duration.
			Iterator<Trackpoint> tpsIt = trackpoints.iterator();
			while (tpsIt.hasNext()) {
				Trackpoint tp = tpsIt.next();

				if (tp.getPotentialPauseDuration() > currentMaxPause) {
					currentMaxPause = tp.getPotentialPauseDuration();
					removeTpIndex = index;
				}
				++index;
			}

			// If we found a trackpoint then we create a pause/resume split for it
			// and decrement all durations greater than the duration of the pause/resume split.
			if (removeTpIndex != -1) {
				Trackpoint tp = trackpoints.get(removeTpIndex);
				long pauseDuration = tp.getPotentialPauseDuration();

				// Decrement all post-trackpoint trackpoint durations.
				for (int i = removeTpIndex + 1; i < trackpoints.size(); ++i)
					trackpoints.get(i).decrementDuration(pauseDuration);

				// Decrement all post-trackpoint pause/resume durations.
				Iterator<Long> prsIt = pauseResumeTimes.iterator();
				ArrayList<Long> prsNew = new ArrayList<Long>();
				while (prsIt.hasNext()) {
					long pr = prsIt.next();
					if (pr > tp.getDuration()) {
						prsIt.remove();
						prsNew.add(pr - pauseDuration);
					}
				}
				pauseResumeTimes.addAll(prsNew);

				// Update these variables so we can use them to check whether to loop again.
				calculatedDuration -= pauseDuration;
				retry = true;

				// Leave this debug statement in for now, I am interested to see in the logs how often this happens.
				log.out("Removing trackpoint with duration %d", pauseDuration);

				// Remove the pause/resume trackpoints from the list that will be used for the cubic-spline data.
				trackpoints.remove(tp.getPreviousTrackPoint());
				trackpoints.remove(tp);

				// Add a pause/resume split for the removed trackpoints.
				pauseResumeTimes.add(tp.getPreviousTrackPoint().getDuration());
			}
		}
		
		// The ArrayList will be out of order if we've had to "create" pause/resume splits.
		Collections.sort(pauseResumeTimes);

		// Remove any Trackpoints which have duplicate distances.
		Iterator<Trackpoint> tpsIt = trackpoints.iterator();
		while (tpsIt.hasNext()) {
			Trackpoint tp = tpsIt.next();
			if (tp.getDistance() == tp.getPreviousDistance())
				tpsIt.remove();
		}
	}


	private void appendSnapShotListAndExtendedData(Element sportsDataElement, ArrayList<Trackpoint> trackpoints, ArrayList<Long> pauseResumeTimes) {
		int tpsSize = trackpoints.size();
		double[] durationsArray = new double[tpsSize];
		double[] distancesArray = new double[tpsSize];

		populateDurationsAndDistancesArrays(trackpoints, durationsArray, distancesArray);
		

		// Generate cubic splines for distance -> duration and duration -> distance.
		// I'd have thought the flanagan classes would have offered some method of inverting the data, but I've not looked into it and this hack will do for now.
		CubicSpline distanceToDuration = new CubicSpline(distancesArray, durationsArray);
		CubicSpline durationToDistance = new CubicSpline(durationsArray, distancesArray);

		Element snapShotKmListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "kmSplit");
		Element snapShotMileListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "mileSplit");
		Element snapShotClickListElement = appendElement(sportsDataElement, "snapShotList", null, "snapShotType", "userClick");


		// Pause/Resume splits.
		Iterator<Long> prIt = pauseResumeTimes.iterator();
		while (prIt.hasNext()) {
			long time = prIt.next();
			
			// 2009-12-01: Looking at various runs on nike+ it seems the each pause/resume pair now has the same duration/distance
			// (using the pause event).  I can't find my nike+ stuff to check this is 100% accurate but will go with it for now.
			double distance = interpolate(durationToDistance, time);
			appendSnapShot(snapShotClickListElement, time, distance, "event", "pause");
			appendSnapShot(snapShotClickListElement, time, distance, "event", "resume");
		}

		// Km splits
		for (int i = 1000; i <= _totalDistance; i += 1000)
			appendSnapShot(snapShotKmListElement, (long)(interpolate(distanceToDuration, i)), i);

		// Mile splits
		for (double i = D_METRES_PER_MILE; i <= _totalDistance; i += D_METRES_PER_MILE)
			appendSnapShot(snapShotMileListElement, (long)(interpolate(distanceToDuration, i)), i);

		// Stop split
		appendSnapShot(snapShotClickListElement, _totalDuration, _totalDistance, "event", "stop");


		// ExtendedDataList
		appendExtendedDataList(sportsDataElement, durationToDistance);
		
	}


	// Copy data from the trackpoint ArrayList into the durations & distances arrays in preparation for creating CubicSplines.
	private void populateDurationsAndDistancesArrays(ArrayList<Trackpoint> trackpoints, double[] durations, double[] distances) {
		Iterator<Trackpoint> tpsIt = trackpoints.iterator();
		int i = 0;
		while (tpsIt.hasNext()) {
			Trackpoint tp = tpsIt.next();
			durations[i] = tp.getDuration();
			distances[i++] = tp.getDistance();
		}
	}
	

	/*
	<extendedDataList>
	  <extendedData dataType="distance" intervalType="time" intervalUnit="s" intervalValue="10">0.0. 1.1, 2.3, 4.7, etc</extendedData>
	</extendedDataList>
	*/
	private void appendExtendedDataList(Element sportsDataElement, CubicSpline durationToDistance) {
		int finalReading = (int)(durationToDistance.getXmax());
		double previousDistance = 0;
		StringBuilder sb = new StringBuilder("0.0");

		for (int i = 10000; i < finalReading; i = i + 10000) {
			double distance = (interpolate(durationToDistance, i))/1000;

			// 2010-06-18: Phillip Purcell emailed me two workouts on 2010-06-14 which did not draw graphs on nikeplus.com
			// A bit of debugging showed that some of the interpolated distances in these workouts wereless than the
			// previous distance (impossible).  Hacked this fix so that this will never happen.
			if (distance < previousDistance) distance = previousDistance;
			sb.append(String.format(", %.4f", distance));
			previousDistance = distance;
		}

		Element extendedDataListElement	= appendElement(sportsDataElement, "extendedDataList");
		appendElement(extendedDataListElement, "extendedData", sb, "dataType", "distance", "intervalType", "time", "intervalUnit", "s", "intervalValue", "10");
	}


	private void appendSnapShot(Element snapShotListElement, long durationMillis, double distanceMetres, String ... attributes) {
		Element snapShotElement = appendElement(snapShotListElement, "snapShot", null, attributes);

		appendElement(snapShotElement, "duration", durationMillis);
		appendElement(snapShotElement, "distance", String.format("%.3f", distanceMetres/1000));
		//appendElement(snapShotElement, "pace", generateSnapShotPace(durationToDistance, currentDuration));
	}


	private double interpolate(CubicSpline spline, double x) {
		if (x < spline.getXmin()) x = spline.getXmin();
		else if (x > spline.getXmax()) x = spline.getXmax();
		
		return spline.interpolate(x);
	}



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


	private int calculatePace(long duration, double distance) {
		return (int)((duration)/distance);
	}


	private String getTimeStringFromMillis(long totalMillis) {
		long totalSeconds = totalMillis / 1000;

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
		SimpleDateFormat outfileFormat = new SimpleDateFormat("yyyy-MM-dd HH;mm;ss'.xml'");
		outfileFormat.setTimeZone(_workoutTimeZone);
		return outfileFormat.format(_calEnd.getTime());
	}

	public String getStartTimeHumanReadable() {
		return _startTimeStringHuman;
	}




	public static void main(String[] args) {
		File inFile = new File(args[0]);

		String empedID = (args.length >= 2) ? args[1] : null;

		Convert c = new Convert();
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
		private long		_duration;
		private double		_distance;
		private Trackpoint	_previousTrackPoint;
		private long		_potentialPauseDuration;

		public Trackpoint(long duration, double distance, Trackpoint previousTrackPoint) {
			_duration = duration;
			_distance = distance;
			_previousTrackPoint = previousTrackPoint;
		}


		protected long getDuration() {
			return _duration;
		}

		protected double getDistance() {
			return _distance;
		}

		protected Trackpoint getPreviousTrackPoint() {
			return _previousTrackPoint;
		}

		protected long getPotentialPauseDuration() {
			return _potentialPauseDuration;
		}

		protected boolean isRepeatDuration() {
			return (_previousTrackPoint != null) && (_duration == _previousTrackPoint.getDuration());
		}

		protected boolean isRepeatDistance() {
			return (_previousTrackPoint != null) && (_distance == _previousTrackPoint.getDistance());
		}

		protected double getPreviousDistance() {
			return (_previousTrackPoint == null) ? 0 : _previousTrackPoint.getDistance();
		}

		protected void generatePotentialPauseDuration() {
			_potentialPauseDuration = (_previousTrackPoint == null) ? 0 : (_duration - _previousTrackPoint.getDuration());
		}

		protected void decrementDuration(long millis) {
			_duration -= millis;
		}
	}



	/*
	private void testLapDuration(Document inDoc) throws DatatypeConfigurationException {
		NodeList laps = inDoc.getElementsByTagName("Lap");

		//long startDurationAdjusted = _calStart.getTimeInMillis();
		//long currentDuration = 0;

		int lapsLength = laps.getLength();

		for (int i = 0; i < lapsLength; ++i) {

			System.out.printf("\nLap %d\n=====\n", i+1);

			NodeList lapData = laps.item(i).getChildNodes();
			int lapInfoLength = lapData.getLength();

			//System.out.println(Util.getSimpleNodeValue(laps.item(i).getAttributes().item(0)));
			//System.out.println(getCalendarNodeValue(laps.item(i).getAttributes().item(0)).getTime());
			long startDurationAdjusted = getCalendarNodeValue(laps.item(i).getAttributes().item(0)).getTimeInMillis();
			long lapDuration = 0;
			long totalPauseDuration = 0;

			for (int j = 0; j < lapInfoLength; ++j) {
				Node n = lapData.item(j);

				String nodeName = n.getNodeName();

				if (nodeName.equals("TotalTimeSeconds"))
					System.out.printf("TotalTimeSeconds:\t%.0f\n", Double.parseDouble(Util.getSimpleNodeValue(n)) * 1000);
			}



			NodeList trackPoints = ((Element)laps.item(i)).getElementsByTagName("Trackpoint");
			int trackPointsLength = trackPoints.getLength();

			for (int j = 0; j < trackPointsLength; ++j) {

				NodeList trackPointData = trackPoints.item(j).getChildNodes();
				int trackPointDataLength = trackPointData.getLength();

				// Deal with pause/resumes
				if ((trackPointDataLength == 3) && (j+1 < trackPointsLength)) {

					// Save the pause/resume points so that we can create the snapshot event later (once we can interpolate to get the distance).
					long tpPauseTime = getCalendarNodeValue(trackPointData.item(1)).getTimeInMillis();

					// Get what we are expecting to be the resume track-point-data.
					trackPointData = trackPoints.item(++j).getChildNodes();

					// As we are expecting it to be a resume trackPointData it must also be of length 3, otherwise we are not dealing with a pause/resume pair so ignore the pause.
					if ((trackPointData.getLength()) == 3) {
						long tpResumeTime = getCalendarNodeValue(trackPointData.item(1)).getTimeInMillis();

						// Adjust the start time so that future splits are not affected by the paused time period.
						long pauseDuration = tpResumeTime - tpPauseTime;
						startDurationAdjusted += pauseDuration;
						totalPauseDuration += pauseDuration;

						// Continue onto the next trackpoint, we've stored our pause/resume data for later use.
						continue;
					}
				}


				// If we reach this point then this is a normal trackpoint, not a pause/resume.
				for (int k = 0; k < trackPointDataLength; ++k) {
					Node n = trackPointData.item(k);

					String nodeName = n.getNodeName();

					// Run duration to this point
					if (nodeName.equals("Time")) {
						Calendar tpTime = getCalendarNodeValue(n);
						lapDuration = tpTime.getTimeInMillis() - startDurationAdjusted;
						//duration = (double)currentDuration/1000;
						continue;
					}
				}
			}

			System.out.printf("Calculated lap time:\t%d\n", (lapDuration));
			System.out.printf("Total time paused:\t%d\n", (totalPauseDuration));


		}

		System.out.println();
	}
	*/


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


	/*
	private double[] convertToPrimitaveDoubleArray(ArrayList<Double> al) {
		int size = al.size();
		double[] ar = new double[size];
		for (int i = 0; i < size; ++i)
			ar[i] = al.get(i);

		return ar;
	}
	*/
	
}

