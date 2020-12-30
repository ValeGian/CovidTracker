package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import javafx.util.Pair;

import javax.ejb.Remote;

@Remote
public interface RegionConsumer {
    void initializeParameters(String name, String parent);
    Pair<String, CommunicationMessage> handleRegistryClosureRequest(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);
    void handleAggregationResponse(CommunicationMessage cMsg);
    void handleNewData(CommunicationMessage cMsg);
}
