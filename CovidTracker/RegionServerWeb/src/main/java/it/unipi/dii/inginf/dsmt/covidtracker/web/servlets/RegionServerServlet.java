package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionNode;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "RegionServerServlet", urlPatterns={"/server_region/*"})
public class RegionServerServlet extends HttpServlet {

    @EJB private RegionNode myNode;
    private static final String regionPage = "/server_region/serverRegionUI.jsp";


    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(true);
        String server = (String) session.getAttribute("regionServer");
        myNode.initialize(server);
        if(server != null) {
            try {
                resp.setContentType("text/html");
                PrintWriter out = resp.getWriter();

                out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
                out.println("<CENTER> <FONT size=+4> Homepage for " + server.substring(0, 1).toUpperCase() + server.substring(1) + " Region Server </FONT> </CENTER> <br> <p> ");

                // list of Messages received
                out.println("<h3>Click to refresh messages " + myNode.getNMessages() + " </h3>");
                out.println("<form action=\"" + req.getContextPath() + regionPage + "\" method=\"GET\">");
                out.println("<input type=\"submit\" name=\"Submit_Msg_Refresh\">");
                out.println("</form>");

                out.println("<FONT size=+1 color=red> Aggregation responses: </FONT>"
                        + "<br>" + myNode.readReceivedMessages().replace("\n", "<br>") + "<br>");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("webclient servlet test failed");
                throw new ServletException(ex);
            }
        }
    }
}
