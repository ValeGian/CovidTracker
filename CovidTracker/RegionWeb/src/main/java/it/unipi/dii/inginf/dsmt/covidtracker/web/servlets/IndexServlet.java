package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;

import javax.ejb.EJB;
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

@WebServlet(name = "IndexServlet", urlPatterns={"/", "/index.jsp"})
public class IndexServlet extends HttpServlet {

    private static final String regionPage = "/region/regionUI.jsp";

    @EJB private HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        try {
            out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
            out.println("<CENTER> <FONT size=+2> Homepage for all Web Clients </FONT> </CENTER> <br> <p> ");
            out.println("<form method=\"POST\">");
            out.println("<label for=\"region\">Choose a region to connect:</label>");
            out.println("<select name=\"region\" id=\"region\">");

            List<String> regionNames = myHierarchyConnectionsRetriever.getAllRegionsName();
            for(String regionName: regionNames) {
                out.println("<option value=\"" + regionName + "\">" + regionName.toUpperCase() + "</option>");
            }

            out.println("</select> <br><br>");
            out.println("<input type=\"submit\" name=\"Submit\">");
            out.println("</form>");

            String region = req.getParameter("region");
            if (region != null) {
                HttpSession session = req.getSession(true);
                session.setAttribute("region", region);

                RequestDispatcher disp = getServletContext().getRequestDispatcher(regionPage);
                if (disp != null) disp.forward(req, resp);
            }

            out.println("</BODY> </HTML> ");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("webclient servlet test failed");
            throw new ServletException(ex);
        }
    }
}
