package com.awsmithson.tcx2nikeplus.servlet;

import com.awsmithson.tcx2nikeplus.converter.Convert;
import com.awsmithson.tcx2nikeplus.converter.Util;
import com.awsmithson.tcx2nikeplus.uploader.Upload;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;




public class ConvertServlet extends HttpServlet {
   
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		PrintWriter out = response.getWriter();

		try {

			if (ServletFileUpload.isMultipartContent(request)) {
				try {
					FileItemFactory factory = new DiskFileItemFactory();
					ServletFileUpload upload = new ServletFileUpload(factory);
					List<DiskFileItem> items = upload.parseRequest(request);

					if (items.size() > 0) {

						File inFile = null;
						String empedID = null;
						String pin = null;

						Iterator it = items.iterator();

						while (it.hasNext()) {
							DiskFileItem item = (DiskFileItem) it.next();

							String fieldName = item.getFieldName();

							if (item.isFormField()) {
								// Emped ID
								if ((fieldName.equals("empedID")) && (item.getString().length() > 0))
									empedID = item.getString();

								// Pin
								if ((fieldName.equals("pin")) && (item.getString().length() > 0))
									pin = item.getString();
							}
							else {
								// Input File
								if (fieldName.equals("datafile")) {
									inFile = item.getStoreLocation();

									try {
										// HACK: If we have for whatever reason the file has not been saved then write it's data to where it should be saved.
										if (!(inFile.exists())) item.write(item.getStoreLocation());
									}
									catch (Exception e) {
										e.printStackTrace();
										out.println(String.format("There was an error uploading your tcx file...:<br />%s", e.getStackTrace().toString()));
										return;
									}
								}
							}
						}


						// If we have a file to convert then carry on.
						if ((inFile != null) && (inFile.exists())) {
							Convert c = new Convert();
							Document doc = c.generateNikePlusXml(inFile, empedID);
							String filename = c.generateFileName();

							// If a nikeplus pin hasn't been supplied then just return the nikeplus xml document.
							if (pin == null) {
								String output = Util.generateStringOutput(doc);
								System.out.printf("\ntcx2nikeplus outputting '%s'\n", filename);
								response.setContentType("application/x-download");
								response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
								out.print(output);
							}

							// If we do have a pin then try to send the data to nikeplus.
							else {

								response.setContentType("text/html");

								Upload u = new Upload();

								try {
									System.out.printf("\ntcx2nikeplus uploading '%s'\n", filename);
									u.checkPinStatus(pin);
									u.syncData(pin, doc);
								}
								catch (Exception e) {
									out.println(String.format("There was an error...:<br />%s", e.getStackTrace().toString()));
								}
								finally {
									try {
										u.endSync(pin);
									}
									catch (Exception e) {
										e.printStackTrace();
										out.println(String.format("There was an error...:<br />%s", e.getStackTrace().toString()));
									}
								}
								out.println("<html><body>Congratulations, your workout should have uploaded successfully, if not then apologies - I need to test this some more.<br />");
								out.println(String.format("If you have more workouts then go <a href=\"%s\">back</a> and add them as well.", request.getHeader("referer")));

								out.println("<script type=\"text/javascript\">");
								out.println("\tvar gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");");
								out.println("\tdocument.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));");
								out.println("</script>");
								out.println("<script type=\"text/javascript\">");
								out.println("\ttry {");
								out.println("\t\tvar pageTracker = _gat._getTracker(\"UA-9670019-1\");");
								out.println("\t\tpageTracker._trackPageview();");
								out.println("}");
								out.println("catch(err) {}");
								out.println("</script>");

								out.println("</body></html>");

							}
						}
					}
				}
				catch (FileUploadException fue) {}
			}

		}

		finally {
			// Make sure the PrintWriter is closed.
			out.close();
		}

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
