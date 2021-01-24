package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionNode;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;

@WebServlet(name = "RegionServerServlet", urlPatterns={"/server_region/*"})
public class RegionServerServlet extends HttpServlet {
    private static final String REGION_NODE_JNDI = "java:global/Region_ejb_exploded/RegionNodeEJB";
    private static final String regionPage = "/server_region/serverRegionUI.jsp";


    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(true);
        String server = (String) session.getAttribute("regionServer");

        if(server != null) {
            RegionNode myNode = lookupSessionNode(session, server);
            try {
                resp.setContentType("text/html");
                PrintWriter out = resp.getWriter();

                out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
                out.println("<CENTER> <FONT size=+4> Homepage for " + server.substring(0, 1).toUpperCase() + server.substring(1) + " Region Server </FONT> </CENTER> <br> <p> ");

                // list of Messages received
                out.println("<h3>Click to refresh messages </h3>");
                out.println("<form action=\"" + req.getContextPath() + regionPage + "\" method=\"POST\">");
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

    private RegionNode lookupSessionNode(HttpSession session, String name) {
        RegionNode rNode = (RegionNode) session.getAttribute("cachedRegionNodeRef");
        if (rNode == null) {
            try {
                Context c = new InitialContext();
                rNode = (RegionNode) c.lookup(REGION_NODE_JNDI);
                rNode.initialize(name);

            } catch (NamingException ne) {
                System.out.println(ne.getMessage());
                throw new RuntimeException(ne);
            }
            session.setAttribute("cachedRegionNodeRef", rNode);
        }
        return rNode;
    }
}
