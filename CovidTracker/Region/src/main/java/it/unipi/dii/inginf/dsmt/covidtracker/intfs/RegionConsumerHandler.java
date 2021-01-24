package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import javafx.util.Pair;

import javax.ejb.Local;

@Local
public interface RegionConsumerHandler {
    void initializeParameters(String name, String parent);
    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);
}
