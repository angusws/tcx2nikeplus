package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.convert.ConvertGpx;
import com.awsmithson.tcx2nikeplus.convert.ConvertTcx;
import com.awsmithson.tcx2nikeplus.http.Garmin;
import com.awsmithson.tcx2nikeplus.http.NikePlus;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.client.HttpClient;
import org.w3c.dom.Document;



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
				boolean gpsUpload = false;
				String nikeEmail = null;
				String nikePassword = null;
				String nikeEmpedId = null;
				String nikePin = null;
				Integer clientTimeZoneOffset = null;

				// Iterate through the uploaded items
				Iterator it = items.iterator();
				while (it.hasNext()) {
					DiskFileItem item = (DiskFileItem) it.next();
					String fieldName = item.getFieldName();

					if (item.isFormField()) {
						// Garmin activity id
						if (haveFieldValue(item, fieldName, "garminActivityId")) garminActivityId = getGarminActivityId(item);

						// GPS
						if ((garminActivityId != null) && (haveFieldValue(item, fieldName, "chkGps"))) gpsUpload = true;

						// Nike email address
						if (haveFieldValue(item, fieldName, "nikeEmail")) nikeEmail = item.getString();

						// Nike password
						if (haveFieldValue(item, fieldName, "nikePassword")) nikePassword = item.getString();

						// Nike emped id
						if (haveFieldValue(item, fieldName, "nikeEmpedId")) nikeEmpedId = item.getString();

						// Nike pin
						if (haveFieldValue(item, fieldName, "nikePin")) nikePin = item.getString();

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
				
				NikePlus u = new NikePlus();

				// Get the nikePin if we have an email and password.
				if ((nikePin == null) && ((nikeEmail != null) && (nikePassword != null)))
					nikePin = u.generatePin(nikeEmail, nikePassword);

				Document garminTcxDocument = null;
				Document garminGpxDocument = null;

				// If we have a tcx file to convert...
				if ((garminTcxFile != null) && (garminTcxFile.exists())) {
					log.out("Received convert-tcx-file request");
					garminTcxDocument = Util.generateDocument(garminTcxFile);
				}

				// If we have a garmin activity id then download the garmin tcx file then convert it.
				else if (garminActivityId != null) {
					log.out("Received convert-activity-id request, id: %d", garminActivityId);
					HttpClient client = Garmin.getGarminHttpSession();
					garminTcxDocument = Garmin.downloadGarminTcx(client, garminActivityId);
					if (gpsUpload) garminGpxDocument = Garmin.downloadGarminGpx(client, garminActivityId);
					client.getConnectionManager().shutdown();

					//garminTcxDocument = Garmin.downloadGarminTcx(garminActivityId, null);
					//if (gpsUpload) garminGpxDocument = Garmin.downloadGarminGpx(garminActivityId, null);
				}

				// If we didn't find a garmin tcx file or garmin activity id then we can't continue...
				else throw new Exception("You must supply either a Garmin TCX file or Garmin activity id.");


				// Generate the output nike+ workout xml.
				ConvertTcx cTcx = new ConvertTcx();
				ConvertGpx cGpx = new ConvertGpx();
				Document runXml = convertTcxDocument(cTcx, garminTcxDocument, nikeEmpedId, clientTimeZoneOffset);
				Document gpxXml = (gpsUpload) ? convertGpxDocument(cGpx, garminGpxDocument) : null;

				// If a nikeplus pin hasn't been supplied then just return the nikeplus xml document.
				if (nikePin == null) returnOutputTcxDocument(cTcx, runXml, response, out);

				// If we did have a nikeplus pin then continue with the upload to nikeplus.
				else {
					u.fullSync(nikePin, runXml, gpxXml);
					String message = "Conversion & Upload Successful.";
					succeed(out, jout, message, cTcx.getTotalDurationMillis(), cTcx.getTotalDistanceMetres());
				}
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

	private Document convertTcxDocument(ConvertTcx c, Document garminTcxDocument, String nikeEmpedId, Integer clientTimeZoneOffset) throws Throwable {
		// Generate the nike+ tcx xml.
		Document doc = c.generateNikePlusXml(garminTcxDocument, nikeEmpedId, clientTimeZoneOffset);
		log.out("Generated nike+ run xml, workout start time: %s.", c.getStartTimeHumanReadable());
		return doc;
	}

	private Document convertGpxDocument(ConvertGpx c, Document garminGpxDocument) throws Throwable {
		// Generate the nike+ gpx xml.
		Document doc = c.generateNikePlusGpx(garminGpxDocument);
		log.out("Generated nike+ gpx xml.");
		return doc;
	}

	private void returnOutputTcxDocument(ConvertTcx c, Document doc, HttpServletResponse response, PrintWriter out) {
		String filename = c.generateFileName();
		String output = Util.generateStringOutput(doc);
		response.setContentType("application/x-download");
		response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
		out.print(output);
		log.out("");
	}
	/*
	private void Upload(String nikePin, Document runXml, Document gpxXml) throws Throwable {
		Upload u = new Upload();
		u.fullSync(nikePin, runXml, gpxXml);
	}
	*/
	
	

	private void fail(PrintWriter out, JsonObject jout, String errorMessage, Throwable t) throws ServletException {

		log.out("Failing... Error message: %s", errorMessage);

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



    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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
    }// </editor-fold>

}
