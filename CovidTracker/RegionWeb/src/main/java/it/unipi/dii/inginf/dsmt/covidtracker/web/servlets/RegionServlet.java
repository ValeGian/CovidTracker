package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "RegionServlet", urlPatterns={"/region/*"})
public class RegionServlet extends HttpServlet {

    private String regionQueueName;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        String sessionRegion = (String) session.getAttribute("region");
        if(sessionRegion != null)
            regionQueueName = sessionRegion;

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        try {
            out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
            out.println("<CENTER> <FONT size=+4> Region page sending requests to " + regionQueueName + "</FONT> </CENTER> <br> <p> ");

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

            out.println("<h2>Request an aggregation</h2>");
            out.println("<form action=\"" + req.getContextPath() + "/region/regionUI.jsp\" method=\"GET\">");

            out.println("<label for=\"log_type\">Choose the type to aggregate:</label>");
            out.println("<select name=\"log_type\" id=\"log_type\">");
            out.println("<option value=\"swab\">Swab</option>");
            out.println("<option value=\"positive\">Positive</option>");
            out.println("<option value=\"negative\">Negative</option>");
            out.println("<option value=\"dead\">Dead</option>");
            out.println("</select> <br><br>");

            out.println("<label for=\"aggr_type\">Choose the type of the aggregation:</label>");
            out.println("<select name=\"aggr_type\" id=\"aggr_type\">");
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


            String region = req.getParameter("log_type");
            if (region != null) {
                out.println("<FONT size=+1 color=red> Message back from StatelessSessionBean: </FONT>"
                        + "<br>"+ region + "<br>");
            }

            out.println("</BODY> </HTML> ");


        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("webclient servlet test failed");
            throw new ServletException(ex);
        }

    }
}
