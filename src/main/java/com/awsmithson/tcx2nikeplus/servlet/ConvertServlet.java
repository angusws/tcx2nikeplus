package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.util.Log;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;

@MultipartConfig(
		location="/tmp",
		maxFileSize=1024*1024*8,		// 8MB
		maxRequestSize=1024*1024*20		// 20MB
)
public class ConvertServlet extends HttpServlet {

	private static final Log log = Log.getInstance();

	private static final String PARAMETER_NIKE_EMAIL = "nikeEmail";
	private static final String PARAMETER_NIKE_PASSWORD = "nikePassword";
	private static final String PARAMETER_GARMIN_ID_CHECKBOX = "fsGarminId-checkbox";
	private static final String PARAMETER_TCX_FILE_CHECKBOX = "fsTcxFile-checkbox";

	/**
	 * Handles the HTTP <code>POST</code> method.
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Preconditions.checkNotNull(request, "request argument is null.");
		Preconditions.checkNotNull(response, "response argument is null.");

		response.setContentType("text/html;charset=UTF-8");

		try (PrintWriter out = response.getWriter()) {
			try {
				String nikeEmail = Servlets.getRequiredParameter(request, PARAMETER_NIKE_EMAIL);
				char[] nikePassword = Servlets.getRequiredParameter(request, PARAMETER_NIKE_PASSWORD).toCharArray();

				// This seems horrible (to look through checkbox paramter names rather than a group, but it's what
				// the client sends at the moment, so it'll do until we change that.
				if (Servlets.requestParameterEquals(request, PARAMETER_GARMIN_ID_CHECKBOX, "on")) {
					//int garminId = Integer.parseInt(getRequiredParameter(request, PARAMETER_GARMIN_ACTIVITY_ID));
					ServletUploadDataType.GARMIN_ACTIVITY_ID.process(request, out, nikeEmail, nikePassword);
				} else if (Servlets.requestParameterEquals(request, PARAMETER_TCX_FILE_CHECKBOX, "on")) {
					ServletUploadDataType.TCX_FILE.process(request, out, nikeEmail, nikePassword);
				}
			} catch (Throwable t) {
				String msg = (t.getMessage() != null) ? t.getMessage() : "Unknown error, please contact me and include details of tcx-file/garmin-activity-id.";
				fail(out, msg, t);
			}
		}
	}

	private static void fail(@Nonnull PrintWriter out, @Nonnull String errorMessage, @Nonnull Throwable t) {
		log.out("Failing... Error message: %s", errorMessage);

		//errorMessage = String.format("Nike+ are making ongoing changes to their site which may affect the converter.  Please try again later - I am modifying the converter to keep up with the changes<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Nike+ have made changes which have broken the converter.  I need to make significant changes to the converter to make it work again and hope to fixed by Sunday 16th December.<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Nike+ have made changes to their website and the converter no longer works.  I am on vacation just now but please check back in early June (2014), hopefully I'll have had a chance to fix it by then.  Check the 'news' tab for updates.<br /><br />Error message: %s", errorMessage);
		//errorMessage = String.format("Error message: %s<br /><br />Please check the FAQ, if you can't find an answer there and your problem persists please contact me.", errorMessage);
		errorMessage = String.format("Error message: %s", errorMessage);

		if (t.getMessage() == null) {
			log.out(t);
		} else {
			log.out(Level.SEVERE, t, t.getMessage());
		}

		JsonObject jsonOutput = new JsonObject();
		jsonOutput.addProperty("success", false);
		exit(out, jsonOutput, -1, errorMessage);
	}

	static void succeed(@Nonnull PrintWriter out, @Nonnull String message, long workoutDuration, double workoutDistance) {
		Preconditions.checkNotNull(out, "out argument is null.");
		Preconditions.checkNotNull(message, "message argument is null.");

		log.out("success duration: %d", workoutDuration);
		log.out("success distance: %d", Math.round(workoutDistance));
		JsonObject jsonOutput = new JsonObject();
		jsonOutput.addProperty("success", true);
		exit(out, jsonOutput, 0, message);
	}


	private static void exit(@Nonnull PrintWriter out, @Nonnull JsonObject jsonOutput, int errorCode, @Nonnull String message) {
		Preconditions.checkNotNull(out, "out argument is null.");
		Preconditions.checkNotNull(jsonOutput, "jsonOutput argument is null.");
		Preconditions.checkNotNull(message, "message argument is null.");

		JsonObject data = new JsonObject();
		data.addProperty("errorCode", errorCode);
		data.addProperty("errorMessage", message);

		jsonOutput.add("data", data);
		log.out("%s", jsonOutput);
		out.println(jsonOutput);
	}

	/**
	 * Returns a short description of the servlet.
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Converts & uploads data for a given garmin-id or tcx-file to a nike+ account with the supplied crednetials.";
	}
}
