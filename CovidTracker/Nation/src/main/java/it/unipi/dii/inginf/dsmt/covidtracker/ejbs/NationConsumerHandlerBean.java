package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationConsumerHandler;
import javafx.util.Pair;

import javax.ejb.Stateful;
import java.util.List;

@Stateful(name = "NationConsumerEJB")
public class NationConsumerHandlerBean implements NationConsumerHandler {

    String myDestinationName;
    List<String> childrenAreas;
    boolean[] isReceivedDailyReport;
    DailyReport[] receivedDailyReport;

    public NationConsumerHandlerBean() {

    }

    @Override
    public void initializeParameters(String nodeName, List<String> childrenAreas) {
        myDestinationName = nodeName;
        this.childrenAreas = childrenAreas;
        isReceivedDailyReport = new boolean[childrenAreas.size()];
        receivedDailyReport = new DailyReport[childrenAreas.size()];
    }

    @Override
    public void handleDailyReport(CommunicationMessage cMsg) {
        String areaQueue = cMsg.getSenderName();
        String body = cMsg.getMessageBody();

        int index = childrenAreas.indexOf(areaQueue);
        if(index != -1){
            isReceivedDailyReport[index] = true;
            receivedDailyReport[index].addAll(new Gson().fromJson(body, DailyReport.class));
        }
    }

    @Override
    public DailyReport getDailyReport() {
        DailyReport aggregatedReport = new DailyReport();
        for(int i = 0; i < receivedDailyReport.length; i++) {
            if(isReceivedDailyReport[i])
                aggregatedReport.addAll(receivedDailyReport[i]);
        }
        resetDailyReports();
        return aggregatedReport;
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg) {
        AggregationRequest aggregationRequested = new Gson().fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        String dest = aggregationRequested.getDestination();
        int index = childrenAreas.indexOf(dest);

        if(index != -1) {
            return new Pair<>(childrenAreas.get(index), cMsg);

        } else if(dest.equals(myDestinationName)) {
            return new Pair<>(myDestinationName, cMsg);

        } else
            return new Pair<>("flood", cMsg);
    }

    //-------------------------------------------------------------------------------------------------------------

    void resetDailyReports() {
        isReceivedDailyReport = new boolean[childrenAreas.size()];
        receivedDailyReport = new DailyReport[childrenAreas.size()];
    }
}
