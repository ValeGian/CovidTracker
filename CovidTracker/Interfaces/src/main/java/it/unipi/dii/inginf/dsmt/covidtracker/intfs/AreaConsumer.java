package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import javafx.util.Pair;

import javax.ejb.Remote;
import java.util.List;

@Remote
public interface AreaConsumer {

    void initializeParameters(String name, List<String> myRegions, String parent);
    Pair<String, CommunicationMessage> handleDailyReport(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);
    List<Pair<String, CommunicationMessage>> handleRegistryClosureRequest(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleConnectionRequest(CommunicationMessage cMsg);

    void handleAcceptedConnection();
}
