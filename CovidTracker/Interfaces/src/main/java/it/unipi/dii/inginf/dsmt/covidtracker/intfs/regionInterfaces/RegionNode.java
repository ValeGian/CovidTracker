package it.unipi.dii.inginf.dsmt.covidtracker.intfs.regionInterfaces;

import javax.ejb.Remote;

@Remote
public interface RegionNode {
    String readReceivedMessages();
}