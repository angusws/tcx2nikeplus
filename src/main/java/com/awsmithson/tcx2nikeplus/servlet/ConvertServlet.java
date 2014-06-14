package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.convert.ConvertGpx;
import com.awsmithson.tcx2nikeplus.convert.ConvertTcx;
import com.awsmithson.tcx2nikeplus.garmin.GarminActivityData;
import com.awsmithson.tcx2nikeplus.garmin.GarminDataType;
import com.awsmithson.tcx2nikeplus.http.Garmin;
import com.awsmithson.tcx2nikeplus.http.NikePlus;
import com.awsmithson.tcx2nikeplus.nike.NikeActivityData;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;



public class ConvertServlet extends HttpServlet
{

	private static final Log log = Log.getInstance();


	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = null;
		JsonObject jout = null;

		try {

			jout = new JsonObject();
			out = response.getWriter();

			if (ServletFileUpload.isMultipartContent(request)) {
				FileItemFactory factory = new DiskFileItemFactory();
				ServletFileUpload upload = new ServletFileUpload(factory);
				List<DiskFileItem> items = upload.parseRequest(request);

				File garminTcxFile = null;
				Integer garminActivityId = null;
				String nikeEmail = null;
				char[] nikePassword = null;
				Integer clientTimeZoneOffset = null;

				// Iterate through the uploaded items
				for (DiskFileItem item : items) {
					String fieldName = item.getFieldName();

					if (item.isFormField()) {
						// Garmin activity id
						if (haveFieldValue(item, fieldName, "garminActivityId")) garminActivityId = getGarminActivityId(item);

						// Nike email address
						if (haveFieldValue(item, fieldName, "nikeEmail")) nikeEmail = item.getString();

						// Nike password
						if (haveFieldValue(item, fieldName, "nikePassword")) nikePassword = item.getString().toCharArray();

						// Client Timezone Offset - will only be used when geonames timezone webservice is unavailable.
						if (haveFieldValue(item, fieldName, "clientTimeZoneOffset")) clientTimeZoneOffset = Integer.parseInt(item.getString());
					}
					else {
						// Garmin tcx file
						if (fieldName.equals("garminTcxFile")) {
							garminTcxFile = item.getStoreLocation();

							try {
								// HACK: If we have for whatever reason the file has not been saved then write it's data to where it should be saved.
								if (!(garminTcxFile.exists())) item.write(item.getStoreLocation());
							}
							catch (Exception e) {
								throw new Exception("There was an error uploading your tcx file");
							}
						}
					}
				}

				List<GarminActivityData> garminActivitiesData = Lists.newArrayList();

				if (garminActivityId != null) {
					// If we have a garmin actvity ID, download the garmin tcx & gpx data and add to our garmin-activities list..
					log.out("Received convert-activity-id request, id: %d", garminActivityId);
					try (CloseableHttpClient client = GarminDataType.getGarminHttpSession()) {
						garminActivitiesData.add(new GarminActivityData(
								Garmin.downloadGarminTcx(client, garminActivityId),
								Garmin.downloadGarminGpx(client, garminActivityId)));
					}
				} else if ((garminTcxFile != null) && (garminTcxFile.exists())) {
					// If we have a tcx-file to convert it may consist of multiple activities, parse them out as individual TCX documents.
					log.out("Received convert-tcx-file request");
					for (Document tcxDocument : Util.parseMultipleWorkouts(garminTcxFile)) {
						garminActivitiesData.add(new GarminActivityData(tcxDocument, null));
					}
				} else {
					throw new NullPointerException("You must supply either a Garmin activity id or provide a TCX file.");
				}

				if (garminActivitiesData.size() > 25) {
					log.out("Activities: %d", garminActivitiesData.size());
					throw new IllegalArgumentException(String.format("Exceeded maximum activities per upload (25).  Your TCX file contains %d activities.", garminActivitiesData.size()));
				}


				ConvertTcx convertTcx = new ConvertTcx();
				ConvertGpx convertGpx = new ConvertGpx();
				NikeActivityData[] nikeActivitiesData = new NikeActivityData[garminActivitiesData.size()];
				int i = 0;
				for (GarminActivityData garminActivityData : garminActivitiesData) {
					Document runXml = convertTcxDocument(convertTcx, garminActivityData.getTcxDocument(), clientTimeZoneOffset);
					Document gpxXml = (garminActivityData.getGpxDocument() == null)
							? null
							: convertGpxDocument(convertGpx, garminActivityData.getGpxDocument());
					nikeActivitiesData[i++] = new NikeActivityData(runXml, gpxXml);
				}

				// Upload to nikeplus.
				NikePlus u = new NikePlus();
				u.fullSync(nikeEmail, nikePassword, nikeActivitiesData);
				String message = "Conversion & Upload Successful.";
				succeed(out, jout, message, convertTcx.getTotalDurationMillis(), convertTcx.getTotalDistanceMetres());

			}
		}
		catch (Throwable t) {
			String msg = ((t != null) && (t.getMessage() != null)) ? t.getMessage() : "Unknown error, please contact me and include details of tcx-file/garmin-activity-id.";
			fail(out, jout, msg, t);
		}
		finally {
			out.close();
		}
	}
	

	/**
	 * Check if the DiskFileItem fieldName matches that of requiredFieldName and that the item value has non-zero length.
	 * @param item DiskFileItem to check.
	 * @param itemFieldName the field name of the DiskFileItem.
	 * @param requiredFieldName the field name we are searching for.
	 * @return
	 */
	private boolean haveFieldValue(DiskFileItem item, String itemFieldName, String requiredFieldName) {
		return ((itemFieldName.equals(requiredFieldName)) && (item.getString().length() > 0));
	}
	
	/**
	 * Split the string to obtain the activity-id in case the user
	 * enters the full url "http://connect.garmin.com/activity/23512599"
	 * instead of just the activityid "23512599".
	 * @param input
	 * @return
	 */
	private int getGarminActivityId(DiskFileItem input) {
		String[] split = input.getString().split("/");
		return Integer.parseInt(split[split.length-1]);
	}

	private Document convertTcxDocument(ConvertTcx c, Document garminTcxDocument, int clientTimeZoneOffset) throws Throwable {
		// Generate the nike+ gpx xml.
		Document doc = c.generateNikePlusXml(garminTcxDocument, "", clientTimeZoneOffset);
		log.out("Generated nike+ run xml, workout start time: %s.", c.getStartTimeHumanReadable());
		return doc;
	}

	private Document convertGpxDocument(ConvertGpx c, Document garminGpxDocument) throws Throwable {
		// Generate the nike+ gpx xml.
		Document doc = c.generateNikePlusGpx(garminGpxDocument);
		log.out("Generated nike+ gpx xml.");
		return doc;
	}

	private void fail(PrintWriter out, JsonObject jout, String errorMessage, Throwable t) throws ServletException {
		log.out("Failing... Error message: %s", errorMessage);

		//errorMessage = String.format("Nike+ are making ongoing changes to their site which may affect the converter.  Please try again later - I am modifying the converter to keep up with the changes<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Nike+ have made changes which have broken the converter.  I need to make significant changes to the converter to make it work again and hope to fixed by Sunday 16th December.<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Nike+ have made changes to their website and the converter no longer works.  I am on vacation just now but please check back in early June (2014), hopefully I'll have had a chance to fix it by then.  Check the 'news' tab for updates.<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Error message: %s<br /><br />Please check the FAQ, if you can't find an answer there and your problem persists please contact me.", errorMessage);
		errorMessage = String.format("Error message: %s", errorMessage);

		// FIX-ME: Tidy this up!
		if (t != null) {
			if (t.getMessage() == null) log.out(t);
			else log.out(Level.SEVERE, t, t.getMessage());
		}

		jout.addProperty("success", false);
		exit(out, jout, -1, errorMessage);
	}

	private void succeed(PrintWriter out, JsonObject jout, String message, long workoutDuration, double workoutDistance) throws ServletException {
		log.out("success duration: %d", workoutDuration);
		log.out("success distance: %d", Math.round(workoutDistance));
		jout.addProperty("success", true);
		exit(out, jout, 0, message);
	}


	private void exit(PrintWriter out, JsonObject jout, int errorCode, String errorMessage) throws ServletException {
		JsonObject data = new JsonObject();
		data.addProperty("errorCode", errorCode);
		data.addProperty("errorMessage", errorMessage);

		jout.add("data", data);

		log.out("%s\n", jout);
		out.println(jout);
	}

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

}
