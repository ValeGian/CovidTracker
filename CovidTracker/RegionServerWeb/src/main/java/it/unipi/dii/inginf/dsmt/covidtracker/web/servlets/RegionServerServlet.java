package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.*;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;

import javax.ejb.EJB;
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
    private static final String regionPage = "/server_region/serverRegionUI.jsp";

    @EJB RegionValleDAosta regionValleDAosta;
    @EJB RegionPiemonte regionPiemonte;
    @EJB RegionToscana regionToscana;
    @EJB RegionSicilia regionSicilia;
    /*@EJB RegionLiguria regionLiguria;
    @EJB RegionLombardia regionLombardia;
    @EJB RegionTrentinoAltoAdige regionTrentinoAltoAdige;
    @EJB RegionVeneto regionVeneto;
    @EJB RegionFriuliVeneziaGiulia regionFriuliVeneziaGiulia;
    @EJB RegionEmiliaRomagna regionEmiliaRomagna;
    @EJB RegionUmbria regionUmbria;
    @EJB RegionMarche regionMarche;
    @EJB RegionLazio regionLazio;
    @EJB RegionAbruzzo regionAbruzzo;
    @EJB RegionMolise regionMolise;
    @EJB RegionCampania regionCampania;
    @EJB RegionPuglia regionPuglia;
    @EJB RegionBasilicata regionBasilicata;
    @EJB RegionCalabria regionCalabria;
    @EJB RegionSardegna regionSardegna;

     */

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(true);
        String server = (String) session.getAttribute("regionServer");

        if(server != null) {
            RegionNode myNode = getRegion(server);
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

    public RegionNode getRegion(String ejb) {

        switch(ejb)
        {
            case "valledaosta":
                return regionValleDAosta;
            case "piemonte":
                return regionPiemonte;
            case "toscana":
                return regionToscana;
            case "sicilia":
                return regionSicilia;
            /*case "liguria":
                return regionLiguria;
            case "lombardia":
                return regionLombardia;
            case "trentinoaltoadige":
                return regionTrentinoAltoAdige;
            case "veneto":
                return regionVeneto;
            case "friuliveneziagiulia":
                return regionFriuliVeneziaGiulia;
            case "emilliaromagna":
                return regionEmiliaRomagna;
            case "umbria":
                return regionUmbria;
            case "marche":
                return regionMarche;
            case "lazio":
                return regionLazio;
            case "abruzzo":
                return regionAbruzzo;
            case "molise":
                return regionMolise;
            case "campania":
                return regionCampania;
            case "puglia":
                return regionPuglia;
            case "basilicata":
                return regionBasilicata;
            case "calabria":
                return regionCalabria;
            case "sardegna":
                return regionSardegna;

             */
        }
        return null;
    }
}
