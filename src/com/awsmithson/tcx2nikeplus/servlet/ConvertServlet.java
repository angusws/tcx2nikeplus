package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.converter.Convert;
import com.awsmithson.tcx2nikeplus.util.Util;
import com.awsmithson.tcx2nikeplus.uploader.Upload;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.w3c.dom.Node;



public class ConvertServlet extends HttpServlet
{

	private final static Log log = Log.getInstance();
	private final static String NIKE_SUCCESS = "success";

	
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
						if ((fieldName.equals("garminActivityId")) && (item.getString().length() > 0)) {
							// Split the string to obtain the activity-id in case the user
							// enters the full url "http://connect.garmin.com/activity/23512599"
							// instead of just the activityid "23512599".
							String[] split = item.getString().split("/"); 
							garminActivityId = Integer.parseInt(split[split.length-1]);
						}

						// Nike emped id
						else if ((fieldName.equals("nikeEmpedId")) && (item.getString().length() > 0))
							nikeEmpedId = item.getString();

						// Nike pin
						else if ((fieldName.equals("nikePin")) && (item.getString().length() > 0))
							nikePin = item.getString();

						// Client Timezone Offset - will only be used when geonames timezone webservice is unavailable.
						else if ((fieldName.equals("clientTimeZoneOffset")) && (item.getString().length() > 0))
							clientTimeZoneOffset = Integer.parseInt(item.getString());
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

				// If we have a tcx file to convert...
				if ((garminTcxFile != null) && (garminTcxFile.exists())) {
					log.out("Received convert-tcx-file request");
					Document garminTcxDocument = Util.generateDocument(garminTcxFile);
					convertTcxDocument(garminTcxDocument, nikeEmpedId, nikePin, clientTimeZoneOffset, response, out);
				}

				// If we have a garmin activity id then download the garmin tcx file then convert it.
				else if (garminActivityId != null) {
					log.out("Received convert-activity-id request, id: %d", garminActivityId);

					Document garminTcxDocument = null;
					String url = String.format("http://connect.garmin.com/proxy/activity-service-1.0/tcx/activity/%d?full=true", garminActivityId);
					
					try {
						garminTcxDocument = Util.downloadFile(url);
					}
					catch (Exception e) {
						throw new Exception("Invalid Garmin Activity ID.  Please ensure your garmin workout is not marked as private.");
					}

					if (garminTcxDocument == null)
						throw new Exception("Invalid Garmin Activity ID.  Please ensure your garmin workout is not marked as private.");

					log.out("Successfully downloaded garmin activity %d.", garminActivityId);

					convertTcxDocument(garminTcxDocument, nikeEmpedId, nikePin, clientTimeZoneOffset, response, out);
				}

				// If we didn't find a garmin tcx file or garmin activity id then we can't continue...
				else
					throw new Exception("You must supply either a Garmin TCX file or Garmin activity id.");

				// We don't want to call this when we don't have a pin because all we do then is return the xml document as an attachment.
				if (nikePin != null) {

					// There is a 1/100 chance that we remind the user it is possible to donate.
					String message = (new Random().nextInt(100) == 0)
						? "Conversion & Upload Successful.  Please consider donating to help keep this project alive."
						: "Conversion & Upload Successful."
					;

					succeed(out, jout, message);
				}
			}
		}
		catch (Throwable t) {
			fail(out, jout, t.getMessage(), t);
		}
		finally {
			out.close();
		}
	}



	private void convertTcxDocument(Document garminTcxDocument, String nikeEmpedId, String nikePin, Integer clientTimeZoneOffset, HttpServletResponse response, PrintWriter out) throws Throwable {
		// Generate the nike+ xml.
		Convert c = new Convert();
		Document doc = c.generateNikePlusXml(garminTcxDocument, nikeEmpedId, clientTimeZoneOffset);
		log.out("Generated nike+ xml, workout start time: %s.", c.getStartTimeHumanReadable());

		String filename = c.generateFileName();

		// If a nikeplus pin hasn't been supplied then just return the nikeplus xml document.
		if (nikePin == null) {
			String output = Util.generateStringOutput(doc);
			response.setContentType("application/x-download");
			response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
			out.print(output);
		}
		
		// If we do have a pin then try to send the data to nikeplus.
		else {
			Upload u = new Upload();
			try {
				log.out("Uploading to Nike+...");
				log.out(" - Checking pin status...");
				u.checkPinStatus(nikePin);
				log.out(" - Syncing data...");
				
				Document nikeResponse = u.syncData(nikePin, doc);
				//<?xml version="1.0" encoding="UTF-8" standalone="no"?><plusService><status>success</status></plusService>
				if (Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS)) {
					log.out(" - Sync successful.");
					return;
				}

				//<?xml version="1.0" encoding="UTF-8" standalone="no"?><plusService><status>failure</status><serviceException errorCode="InvalidRunError">snapshot duration greater than run (threshold 30000 ms): 82980</serviceException></plusService>
				Node nikeServiceException = nikeResponse.getElementsByTagName("serviceException").item(0);
				throw new Exception(String.format("%s: %s", nikeServiceException.getAttributes().item(0).getNodeValue(), nikeServiceException.getNodeValue()));
			}
			finally {
				log.out(" - Ending sync...");
				Document nikeResponse = u.endSync(nikePin);
				String message = (Util.getSimpleNodeValue(nikeResponse, "status").equals(NIKE_SUCCESS))
					? " - End sync successful."
					: String.format(" - End sync failed: %s", Util.DocumentToString(nikeResponse))
				;
				log.out(message);
			}
		}
	}

	
	

	private void fail(PrintWriter out, JsonObject jout, String errorMessage, Throwable t) throws ServletException {
		if (t != null)
			log.out(Level.SEVERE, t, t.getMessage());

		jout.addProperty("success", false);
		exit(out, jout, -1, errorMessage);
	}

	private void succeed(PrintWriter out, JsonObject jout, String message) throws ServletException {
		jout.addProperty("success", true);
		exit(out, jout, 0, message);
	}


	private void exit(PrintWriter out, JsonObject jout, int errorCode, String errorMessage) throws ServletException {
		JsonObject data = new JsonObject();
		data.addProperty("errorCode", errorCode);
		data.addProperty("errorMessage", errorMessage);

		jout.add("data", data);

		log.out(jout);
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
