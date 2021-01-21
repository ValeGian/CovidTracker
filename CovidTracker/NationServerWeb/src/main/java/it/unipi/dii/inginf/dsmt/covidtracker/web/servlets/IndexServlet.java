package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationNode;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@WebServlet(name = "IndexServlet", urlPatterns={"/", "/index.jsp"})
public class IndexServlet extends HttpServlet {

    @EJB private NationNode myNode;

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            out.println("<HTML> <HEAD> <TITLE> Covid Tracker </TITLE> </HEAD> <BODY BGCOLOR=white>");
            out.println("<CENTER> <FONT size=+4> Homepage for Nation Server </FONT> </CENTER> <br> <p> ");

            out.println("<h3>Click to close daily registries</h3>");
            out.println("<form method=\"GET\">");
            out.println("<input type=\"submit\" name=\"Submit_Reg_Closure\">");
            out.println("</form>");

            out.println("<br><br>");

            // list of Messages received
            out.println("<h3>Click to refresh messages</h3>");
            out.println("<form method=\"GET\">");
            out.println("<input type=\"submit\" name=\"Submit_Msg_Refresh\">");
            out.println("</form>");

            out.println("<FONT size=+1 color=red> Aggregation responses: </FONT>"
                    + "<br>" + myNode.readReceivedMessages().replace("\n", "<br>") + "<br>");

            if (req.getParameter("Submit_Reg_Closure") != null) {
                myNode.closeDailyRegistry();
                out.println("<FONT size=+1 color=red>Daily registries closed</FONT>");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("webclient servlet test failed");
            throw new ServletException(ex);
        }
    }
}
