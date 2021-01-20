package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionNode;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegionServerServlet", urlPatterns={"/region_server/*"})
public class RegionServerServlet extends HttpServlet {
    @EJB
    private RegionNode myNode;

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
