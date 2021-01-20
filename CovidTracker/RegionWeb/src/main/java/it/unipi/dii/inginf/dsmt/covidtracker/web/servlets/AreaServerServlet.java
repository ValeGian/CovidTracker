package it.unipi.dii.inginf.dsmt.covidtracker.web.servlets;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.AreaNode;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "AreaServerServlet", urlPatterns={"/area_server/*"})
public class AreaServerServlet extends HttpServlet {
    @EJB
    private AreaNode myNode;

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
