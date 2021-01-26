package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces.RegionNode;

import javax.ejb.Remote;

@Remote
public interface RegionNodeManager {
    RegionNode getRegion(String ejb);
}
