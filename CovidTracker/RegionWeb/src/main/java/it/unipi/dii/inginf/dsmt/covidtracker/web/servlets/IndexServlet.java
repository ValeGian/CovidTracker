package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.HierarchyConnectionsRetriever;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

//import javax.ejb.EJB;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "IndexServlet", urlPatterns={"/", "/index.jsp"})
public class IndexServlet extends HttpServlet {

    private static final String regionPage = "/region/regionUI.jsp";

    //@EJB static HierarchyConnectionsRetriever myHierarchyConnectionsRetriever;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        try {
            out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
            out.println("<CENTER> <FONT size=+2> Homepage for all Web Clients </FONT> </CENTER> <br> <p> ");
            out.println("<form method=\"POST\">");
            out.println("<label for=\"region\">Choose a region to connect:</label>");
            out.println("<select name=\"region\" id=\"region\">");

            //List<Pair<String, String>> regions = myHierarchyConnectionsRetriever.getAllRegionsInfo();
            //for(Pair<String, String> region: regions) {
            out.println("<option value=\"prova1\">Prova1</option>");
            out.println("<option value=\"prova2\">Prova2</option>");
            out.println("<option value=\"prova3\">Prova3</option>");
            out.println("<option value=\"prova4\">Prova4</option>");
                //out.println("<option value=\"" + region.getValue() + "\">" + region.getKey().toUpperCase() + "</option>");
            //}

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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
