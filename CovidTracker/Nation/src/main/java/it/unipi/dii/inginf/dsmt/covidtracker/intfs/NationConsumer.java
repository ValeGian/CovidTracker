package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import javafx.util.Pair;

import javax.ejb.Local;
import java.util.List;

@Local
public interface NationConsumer {

    void initializeParameters(String name, List<String> childrenAreas);

    void handleDailyReport(CommunicationMessage cMsg);

    DailyReport getDailyReport();

    Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg);

}
