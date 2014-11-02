package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.convert.ConvertGpx;
import com.awsmithson.tcx2nikeplus.convert.ConvertTcx;
import com.awsmithson.tcx2nikeplus.convert.GpxToRunJson;
import com.awsmithson.tcx2nikeplus.garmin.GarminActivityData;
import com.awsmithson.tcx2nikeplus.garmin.GarminDataType;
import com.awsmithson.tcx2nikeplus.http.Garmin;
import com.awsmithson.tcx2nikeplus.http.NikePlus;
import com.awsmithson.tcx2nikeplus.jaxb.JAXBObject;
import com.awsmithson.tcx2nikeplus.nike.NikeActivityData;
import com.awsmithson.tcx2nikeplus.nike.NikePlusSyncData;
import com.awsmithson.tcx2nikeplus.nike.RunJson;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.ObjectFactory;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.topografix.gpx._1._1.GpxType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;


public enum ServletUploadDataType {
	GARMIN_ACTIVITY_ID {
		@Override
		void process(@Nonnull HttpServletRequest request,
					 @Nonnull PrintWriter out,
					 @Nonnull String nikeAccessToken) throws IOException, ParserConfigurationException, SAXException, JAXBException, DatatypeConfigurationException {
			int garminActivityId = Servlets.getGarminActivityId(Servlets.getRequiredParameter(request, PARAMETER_GARMIN_ACTIVITY_ID));
			logger.out("Received convert-activity-id request, id: %d", garminActivityId);

			// Attempt to process the GPX with the run.json and gpxXML.xml.
			if (!processGpx(garminActivityId, nikeAccessToken, out)) {

				// If that failed for whatever reason (it is new/beta), use our legacy method.
				ImmutableList.Builder<GarminActivityData> garminActivities = ImmutableList.builder();
				try (CloseableHttpClient client = GarminDataType.getGarminHttpSession()) {
					garminActivities.add(new GarminActivityData(Garmin.downloadGarminTcx(client, garminActivityId), Garmin.downloadGarminGpx(client, garminActivityId)));
				}
				processGarminActivityData(request, garminActivities.build(), nikeAccessToken, out);
			}
		}
	},
	GPX_FILE {
		@Override
		void process(@Nonnull HttpServletRequest request, @Nonnull PrintWriter out, @Nonnull String nikeAccessToken) {
			throw new UnsupportedOperationException(String.format("%s not supported yet!", name()));
		}
	},
	TCX_FILE {
		@Override
		void process(@Nonnull HttpServletRequest request,
					 @Nonnull PrintWriter out,
					 @Nonnull String nikeAccessToken) throws IOException, ServletException, JAXBException, ParserConfigurationException, SAXException, DatatypeConfigurationException {
			Part part = request.getPart(PART_GARMIN_TCX_FILE);
			logger.out("Received tcx-file request: %s (%d bytes)", part.getSubmittedFileName(), part.getSize());

			try (InputStream inputStream = part.getInputStream()) {
				// Use JAXB to unmarshall...  We could create an org.w3c.dom.Document directly from the InputStream,
				// but we'll be getting rid of all usage of org.w3c.dom.Document soon, and this will be easier to modify
				// when that time comes.
				TrainingCenterDatabaseT garminTcx = GarminDataType.TCX.unmarshall(inputStream);
				JAXBElement<TrainingCenterDatabaseT> jaxbElement = new ObjectFactory().createTrainingCenterDatabase(garminTcx);
				Document tcxDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				JAXBObject.TRAINING_CENTER_DATABASE.marshal(jaxbElement, tcxDocument);


				// Create a list of GarminActivityData to upload to nike+.
				ImmutableList.Builder<GarminActivityData> garminActivities = ImmutableList.builder();
				for (Document doc : Util.parseMultipleWorkouts(tcxDocument)) {
					garminActivities.add(new GarminActivityData(doc, null));
				}
				processGarminActivityData(request, garminActivities.build(), nikeAccessToken, out);
			}
		}
	};


	private static final Log logger = Log.getInstance();

	private static final String PARAMETER_GARMIN_ACTIVITY_ID = "garminActivityId";
	private static final String PART_GARMIN_TCX_FILE = "garminTcxFile";
	private static final String PARAMETER_CLIENT_TIME_ZONE_OFFSET = "clientTimeZoneOffset";

	private static final String LOG_PROCESSING_FORMAT_STRING = "Processing (GPX beta)";
	private static final String LOG_SUCCESS_FORMAT_STRING = "Conversion & Upload Successful (GPX beta)";
	private static final String LOG_FAILED_FORMAT_STRING = "Conversion & Upload Failed (GPX beta)";

	abstract void process(@Nonnull HttpServletRequest request,
						  @Nonnull PrintWriter out,
						  @Nonnull String nikeAccessToken) throws IOException, ParserConfigurationException, SAXException, JAXBException, DatatypeConfigurationException, ServletException;

	void process(@Nonnull HttpServletRequest request,
				 @Nonnull PrintWriter out,
				 @Nonnull String nikeEmail,
				 @Nonnull char[] nikePassword) throws IOException, ParserConfigurationException, JAXBException, SAXException, DatatypeConfigurationException, ServletException {
		if (NikePlus.isPasswordValid(nikePassword)) {
			// Log into nikeplus, process our input and always end-sync to end our session with Nike+.
			String nikeAccessToken = NikePlus.login(nikeEmail, nikePassword);
			try {
				process(request, out, nikeAccessToken);
			} finally {
				NikePlus.endSync(nikeAccessToken);
			}
		} else {
			ConvertServlet.fail(out, NikePlus.INVALID_PASSWORD_ERROR_MESSAGE);
		}
	}

	@Beta
	private static boolean processGpx(int garminActivityId,
									  @Nonnull String nikeAccessToken,
									  @Nonnull PrintWriter out) {
		logger.out(LOG_PROCESSING_FORMAT_STRING);
		try (CloseableHttpClient closeableHttpClient = GarminDataType.getGarminHttpSession()) {
			GpxType gpxXml = GarminDataType.GPX.downloadAndUnmarshall(closeableHttpClient, garminActivityId);
			return processGpx(gpxXml, nikeAccessToken, out);
		} catch (Throwable t) {
			logger.out(Level.WARNING, t, String.format(LOG_FAILED_FORMAT_STRING));
			return false;
		}
	}

	@Beta
	private static boolean processGpx(@Nonnull GpxType gpxXml,
									  @Nonnull String nikeAccessToken,
									  @Nonnull PrintWriter out) throws IOException, SAXException, ParserConfigurationException, JAXBException {
		RunJson runJson = new GpxToRunJson().convert(gpxXml);
		JsonElement runJsonElement = new Gson().toJsonTree(runJson);

		NikePlusSyncData nikePlusSyncData = new NikePlusSyncData(runJsonElement, gpxXml);
		boolean success = NikePlus.syncData(nikeAccessToken, nikePlusSyncData);
		if (success) {
			logger.out(LOG_SUCCESS_FORMAT_STRING);
			String message = "Conversion & Upload Successful.";
			String nikeActivityId = null;

			// Attempt to generate something like "view your workout at https://secure-nikeplus.nike.com/plus/activity/running/detail/<workout-id>
			String responseData = nikePlusSyncData.getResponseEntityContent();
			if (responseData != null) {
				try {
					logger.out(Level.FINER, " - Nike+ sync response: %s", responseData);
					nikeActivityId = Util.getSimpleNodeValue(Util.generateDocument(responseData), "activityId");
				} catch (Throwable t) {
					logger.out(Level.WARNING, t, "Ignoring exception - occured whilst trying to get nike+ activity-id.");
				}
			}

			ConvertServlet.succeed(out, message, nikeActivityId, runJson.getDuration().longValue(), runJson.getDistance().multiply(new BigDecimal("1000")).doubleValue());
		} else {
			logger.out(LOG_FAILED_FORMAT_STRING);
		}
		return success;
	}

	@Deprecated
	private static void processGarminActivityData(@Nonnull HttpServletRequest request,
												  @Nonnull List<GarminActivityData> garminActivities,
												  @Nonnull String nikeAccessToken,
												  @Nonnull PrintWriter out) throws SAXException, ParserConfigurationException, DatatypeConfigurationException, IOException {
		Preconditions.checkArgument(garminActivities.size() <= 25, String.format("Exceeded maximum activities per upload (25).  Your TCX file contains %d activities.", garminActivities.size()));

		ConvertTcx convertTcx = new ConvertTcx();
		ConvertGpx convertGpx = new ConvertGpx();

		NikeActivityData[] nikeActivitiesData = new NikeActivityData[garminActivities.size()];
		int i = 0;
		for (GarminActivityData garminActivityData : garminActivities) {
			Document runXml = convertTcxDocument(convertTcx, garminActivityData.getTcxDocument(), Integer.parseInt(Servlets.getRequiredParameter(request, PARAMETER_CLIENT_TIME_ZONE_OFFSET)));
			Document gpxXml = (garminActivityData.getGpxDocument() == null)
					? null
					: convertGpxDocument(convertGpx, garminActivityData.getGpxDocument());
			nikeActivitiesData[i++] = new NikeActivityData(runXml, gpxXml);
		}

		// Upload to nikeplus.
		NikePlus.fullSync(nikeAccessToken, nikeActivitiesData);
		String message = "Conversion & Upload Successful.";
		ConvertServlet.succeed(out, message, null, convertTcx.getTotalDurationMillis(), convertTcx.getTotalDistanceMetres());
	}

	@Deprecated
	private static Document convertTcxDocument(ConvertTcx c, Document garminTcxDocument, int clientTimeZoneOffset) throws SAXException, ParserConfigurationException, DatatypeConfigurationException, IOException {
		// Generate the nike+ gpx xml.
		Document doc = c.generateNikePlusXml(garminTcxDocument, "", clientTimeZoneOffset);
		logger.out("Generated nike+ run xml, workout start time: %s.", c.getStartTimeHumanReadable());
		return doc;
	}

	@Deprecated
	private static Document convertGpxDocument(ConvertGpx c, Document garminGpxDocument) throws ParserConfigurationException {
		// Generate the nike+ gpx xml.
		Document doc = c.generateNikePlusGpx(garminGpxDocument);
		logger.out("Generated nike+ gpx xml.");
		return doc;
	}
}
