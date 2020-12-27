package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationConsumer;
import javafx.util.Pair;

import javax.ejb.Stateful;
import java.util.Arrays;
import java.util.List;

@Stateful(name = "NationConsumerEJB")
public class NationConsumerBean implements NationConsumer {

    String myName;
    List<String> childrenAreas;
    boolean[] isConnectedArea;
    boolean[] isReceivedDailyReport;
    DailyReport[] receivedDailyReport;

    public NationConsumerBean() {

    }

    @Override
    public void initializeParameters(String nodeName, List<String> childrenAreas) {
        myName = nodeName;
        this.childrenAreas = childrenAreas;
        isConnectedArea = new boolean[childrenAreas.size()];
        isReceivedDailyReport = new boolean[childrenAreas.size()];
        receivedDailyReport = new DailyReport[childrenAreas.size()];
    }

    @Override
    public List<DailyReport> handleDailyReport(CommunicationMessage cMsg) {
        List<DailyReport> reports = null;

        String areaQueue = cMsg.getSenderName();
        String body = cMsg.getMessageBody();
        int index = childrenAreas.indexOf(areaQueue);
        if(index != -1 && !isReceivedDailyReport[index]){
            isReceivedDailyReport[index] = true;
            receivedDailyReport[index] = new Gson().fromJson(body, DailyReport.class);
            if(allReportsReceived()) {
                reports = Arrays.asList(receivedDailyReport);
                resetDailyReports();
            }
        }
        return reports;
    }

    @Override
    public Pair<String, CommunicationMessage> handleConnectionRequest(CommunicationMessage cMsg) {
        CommunicationMessage outMsg = new CommunicationMessage();
        outMsg.setSenderName(myName);

        String areaQueue = cMsg.getSenderName();
        String areaName = cMsg.getMessageBody();
        int index = childrenAreas.indexOf(areaName);
        if(index != -1 && !isConnectedArea[index]){
            isConnectedArea[index] = true;
            outMsg.setMessageType(MessageType.CONNECTION_ACCEPTED);
        } else {
            outMsg.setMessageType(MessageType.CONNECTION_REFUSED);
        }

        return new Pair<>(areaQueue, outMsg);
    }

    //-------------------------------------------------------------------------------------------------------------

    boolean allReportsReceived() {
        for(int i = 0; i < isConnectedArea.length; i++) {
            if(isConnectedArea[i] && !isReceivedDailyReport[i])
                return false;
        }
        return true;
    }

    void resetDailyReports() {
        isReceivedDailyReport = new boolean[childrenAreas.size()];
        receivedDailyReport = new DailyReport[childrenAreas.size()];
    }
}
