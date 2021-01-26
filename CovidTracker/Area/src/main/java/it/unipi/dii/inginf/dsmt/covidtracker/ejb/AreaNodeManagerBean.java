package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.AreaNodeManager;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaCenter;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNode;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNorth;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaSouth;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = "AreaNodeManagerEJB")
public class AreaNodeManagerBean implements AreaNodeManager {

    @EJB AreaNorth areaNorth;
    //@EJB AreaCenter areaCenter;
    @EJB AreaSouth areaSouth;

    public AreaNodeManagerBean() {
    }

    @Override
    public AreaNode getArea(String ejb) {
        switch(ejb)
        {
            case "north":
                return areaNorth;
            /*case "center":
                return areaCenter;*/
            case "south":
                return areaSouth;
        }
        return null;
    }
}
