package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javax.ejb.Remote;

@Remote
public interface AreaNode {
    void initialize(String myName);
}