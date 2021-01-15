package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationResponse;

import javax.ejb.Remote;

@Remote
public interface Recorder {
    String readResponses();
    void addResponse(AggregationResponse response);
}
