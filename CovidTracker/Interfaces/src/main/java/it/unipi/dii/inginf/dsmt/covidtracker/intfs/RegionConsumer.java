package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import javafx.util.Pair;

import javax.ejb.Local;

@Local
public interface RegionConsumer {
    void initializeParameters(String name, String parent);
    String handleRegistryClosureRequest(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleAggregationResponse(CommunicationMessage cMsg);
}
