package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.Recorder;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.SynchRequester;

import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "RegionServlet", urlPatterns={"/region/*"})
public class RegionServlet extends HttpServlet {
    private static final String RECORDER_JNDI = "java:global/lab_08_ejbs/RecorderEJB";
    private static final String regionPage = "/region/regionUI.jsp";

    private String regionQueueName;

    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;
    @EJB private SynchRequester myRequester;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        String region = (String) session.getAttribute("region");
        if(region != null) {
            try {
                regionQueueName = myHierarchyConnectionsRetriever.getMyDestinationName(region);

                resp.setContentType("text/html");
                PrintWriter out = resp.getWriter();

                out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
                out.println("<CENTER> <FONT size=+4> Region page sending requests to " + region.substring(0, 1).toUpperCase() + region.substring(1) + "</FONT> </CENTER> <br> <p> ");

                // LOG FORM
                out.println("<h2>Log new data</h2>");
                out.println("<form action=\"" + req.getContextPath() + "/region/regionUI.jsp\" method=\"GET\">");

                out.println("<label for=\"log_type\">Choose a log type:</label>");
                out.println("<select name=\"log_type\" id=\"log_type\">");
                out.println("<option value=\"swab\">Swab</option>");
                out.println("<option value=\"positive\">Positive</option>");
                out.println("<option value=\"negative\">Negative</option>");
                out.println("<option value=\"dead\">Dead</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"log_quantity\">Insert the quantity to be logged:</label>");
                out.println("<input type=\"text\" name=\"log_quantity\"> <br><br>");

                out.println("<input type=\"submit\" name=\"Submit_Log\">");
                out.println("</form>");

                out.println("<br><br><br>");

                // AGGREGATION REQUEST FORM
                out.println("<h2>Request an aggregation</h2>");
                out.println("<form action=\"" + req.getContextPath() + "/region/regionUI.jsp\" method=\"GET\">");

                out.println("<label for=\"aggr_dest\">Choose a region to connect:</label>");
                out.println("<select name=\"aggr_dest\" id=\"aggr_dest\">");
                List<String> destNames = myHierarchyConnectionsRetriever.getAllNames();
                for(String destName: destNames) {
                    out.println("<option value=\"" + destName + "\">" + destName.toUpperCase() + "</option>");
                }
                out.println("</select> <br><br>");

                out.println("<label for=\"log_aggr_type\">Choose the type to aggregate:</label>");
                out.println("<select name=\"log_aggr_type\" id=\"log_aggr_type\">");
                out.println("<option value=\"swab\">Swab</option>");
                out.println("<option value=\"positive\">Positive</option>");
                out.println("<option value=\"negative\">Negative</option>");
                out.println("<option value=\"dead\">Dead</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"op_type\">Choose the operation to execute:</label>");
                out.println("<select name=\"op_type\" id=\"op_type\">");
                out.println("<option value=\"sum\">Sum</option>");
                out.println("<option value=\"avg\">Average</option>");
                out.println("<option value=\"standard_deviation\">Standard Deviation</option>");
                out.println("</select> <br><br>");

                out.println("<label for=\"start_date\">Beginning of the period:</label>");
                out.println("<input type=\"date\" name=\"start_date\"> <br><br>");

                out.println("<label for=\"end_date\">End of the period:</label>");
                out.println("<input type=\"date\" name=\"end_date\"> <br><br>");

                out.println("<input type=\"submit\" name=\"Submit_aggrReq\">");
                out.println("</form> <br>");

                // list of Aggregation Responses received
                Recorder recorderPerClient = lookupRecorder(session);
                out.println("<FONT size=+1 color=red> Message(s) back from StatefulSessionBean (per client): </FONT>"
                        + "<br>" + recorderPerClient.readResponses().replace("\n", "<br>") + "<br>");

                // Servlet Logic
                if (req.getParameter("Submit_Log") != null) {
                    try {
                        String logType = req.getParameter("log_type");
                        int logQuantity = Integer.parseInt(req.getParameter("log_quantity"));

                        out.println("<FONT size=+1 color=red> Message back from StatelessSessionBean: </FONT>"
                                + "<br>" + logType + "<br>"
                                + "<br>" + logQuantity + "<br>"
                        );
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        System.out.println("> impossible to send log");
                    }
                }

                if (req.getParameter("Submit_aggrReq") != null) {
                    try {
                        String logType = req.getParameter("log_aggr_type");
                        String aggrDest = req.getParameter("aggr_dest");
                        String opType = req.getParameter("op_type");
                        String startDate = req.getParameter("start_date");
                        String endDate = req.getParameter("end_date");

                        if((startDate == null || startDate.equals("")) && (endDate == null || endDate.equals("")))
                            throw new Exception();
                        else if(startDate == null || startDate.equals(""))
                            startDate = endDate;
                        else if(endDate == null || endDate.equals(""))
                            endDate = startDate;

                        AggregationRequest request = new AggregationRequest(
                                logType,
                                aggrDest,
                                opType,
                                startDate,
                                endDate
                        );
                        // send the aggregation request and receive the response
                        AggregationResponse response = myRequester.requestAndReceiveAggregation(regionQueueName, request);
                        if(response == null) {
                            out.println("<FONT size=+1 color=red>RESPONSE = NULL</FONT>");
                        } else {
                            recorderPerClient.addResponse(response);
                            RequestDispatcher disp = getServletContext().getRequestDispatcher(regionPage);
                            if (disp != null) disp.forward(req, resp);
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        System.out.println("> impossible to send aggregation request");
                    }
                }

                out.println("</BODY> </HTML> ");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("> webclient servlet test failed");
                throw new ServletException(ex);
            }
        }
    }


    // method to get the stateful bean ref from session, or lookup it
    private Recorder lookupRecorder(HttpSession session) {

        Recorder rRef;
        rRef = (Recorder) session.getAttribute("cachedRecorderRef");
        if (rRef == null) {

            try {
                Context c = new InitialContext();
                rRef = (Recorder) c.lookup(RECORDER_JNDI);

            } catch (NamingException ne) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "exception caught", ne);
                throw new RuntimeException(ne);
            }

            session.setAttribute("cachedRecorderRef", rRef);
        }
        return rRef;
    }

}
