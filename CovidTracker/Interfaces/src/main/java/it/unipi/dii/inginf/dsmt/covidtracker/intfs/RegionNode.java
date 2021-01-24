package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;

@Remote
public interface RegionNode {
    void initialize(String myName);
    String readReceivedMessages();
}