package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import javafx.util.Pair;

import javax.ejb.Local;
import java.util.List;

@Local
public interface AreaConsumer {

    void initializeParameters(String name, List<String> myRegions, String parent);
    void handleDailyReport(CommunicationMessage cMsg);
    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);
    List<Pair<String, CommunicationMessage>> handleRegistryClosureRequest(CommunicationMessage cMsg);
    DailyReport getDailyReport();

}
