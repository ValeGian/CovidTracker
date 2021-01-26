package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.areaInterfaces.AreaNode;

import javax.ejb.Remote;

@Remote
public interface AreaNodeManager {
    AreaNode getArea(String ejb);
}
