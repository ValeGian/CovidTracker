package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaCenter;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNorth;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaSouth;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "AreaServerServlet", urlPatterns={"/server_area/*"})
public class AreaServerServlet extends HttpServlet {

    private static final String areaPage = "/server_area/serverAreaUI.jsp";

    @EJB AreaNorth areaNorth;
    @EJB AreaCenter areaCenter;
    @EJB AreaSouth areaSouth;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        String server = (String) session.getAttribute("areaServer");

        AreaNode myNode = getArea(server);

        if(server != null) {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            try {
                out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
                out.println("<CENTER> <FONT size=+4> Homepage for " + server.toUpperCase() + " Server </FONT> </CENTER> <br> <p> ");


                out.println("<br><br>");

                // list of Messages received
                out.println("<h3>Click to refresh messages</h3>");
                out.println("<form action=\"" + req.getContextPath() + areaPage + "\" method=\"GET\">");
                out.println("<input type=\"submit\" name=\"Submit_Msg_Refresh\">");
                out.println("</form>");

                out.println("<FONT size=+1 color=red> Aggregation responses: </FONT>"
                        + "<br>" + myNode.readReceivedMessages().replace("\n", "<br>") + "<br>");

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("webclient servlet test failed");
                out.println("<FONT size=+1 color=red>" + ex.getMessage() + "</FONT>");
                throw new ServletException(ex);
            }
        }
    }

    public AreaNode getArea(String ejb) {

        switch(ejb)
        {
            case "north":
                return areaNorth;
            case "center":
                return areaCenter;
            case "south":
                return areaSouth;
        }
        return null;
    }

}

