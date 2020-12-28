package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.AreaConsumer;

import javafx.util.Pair;

import javax.ejb.Stateful;
import java.util.ArrayList;
import java.util.List;

@Stateful(name = "AreaConsumerEJB")
public class AreaConsumerBean implements AreaConsumer {
    String myDestinationName;
    List<String> myRegions;
    boolean[] connectedRegions;
    boolean[] checkReceivedDailyReport;
    DailyReport[] receivedDailyReport;
    boolean waitingReport;
    boolean connected;
    String myParent;


    @Override
    public void initializeParameters(String myDestinationName, List<String> myRegions, String myParent) {
        this.myDestinationName = myDestinationName;
        this.myRegions = myRegions;
        connectedRegions = new boolean[myRegions.size()];
        checkReceivedDailyReport = new boolean[myRegions.size()];
        receivedDailyReport = new DailyReport[myRegions.size()];
        this.myParent = myParent;
    }


    @Override
    public Pair<String, CommunicationMessage> handleDailyReport(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String body = cMsg.getMessageBody();
        int index = myRegions.indexOf(senderQueue);
        if(waitingReport && index != -1 && !checkReceivedDailyReport[index]){
            receivedDailyReport[index] = new Gson().fromJson(body, DailyReport.class);
            checkReceivedDailyReport[index] = true;
            if(AllReportArrived())
                return new Pair<>(myParent, getDailyReport());
        }
        return null;
    }


    private CommunicationMessage getDailyReport() {

        DailyReport responseReport = new DailyReport();
        for(int i = 0; i < checkReceivedDailyReport.length; i++) {
            if(checkReceivedDailyReport[i]) {
                checkReceivedDailyReport[i] = false;
                responseReport.addAll(receivedDailyReport[i]);
            }
        }
        waitingReport = false;
        CommunicationMessage responseMessage = new CommunicationMessage();
        responseMessage.setMessageType(MessageType.DAILY_REPORT);
        responseMessage.setSenderName(myDestinationName);
        responseMessage.setMessageBody(new Gson().toJson(responseReport));

        return responseMessage;
    }

    private boolean AllReportArrived() {
        for(int i = 0; i < connectedRegions.length; i++){
            if(checkReceivedDailyReport[i] != connectedRegions[i])
                return false;
        }
        return true;
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg) {
        AggregationRequest aggregationRequested = new Gson().fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        String dest = aggregationRequested.getDestination();
        int index = myRegions.indexOf(dest);

        if(index != -1) {//se index non è -1 vuol dire che il destinatario è una delle mie regioni e quindi la invio io
            return new Pair<>(myRegions.get(index), cMsg);

        }else if(dest.equals(myDestinationName)) {  //diretto a me e rispondo io
            return new Pair<>("mySelf", cMsg);

        }else //diretto a qualcun'altro e inoltro a nazione
            return new Pair<>(myParent, cMsg);
    }

    @Override
    public List<Pair<String, CommunicationMessage>> handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if(!waitingReport) {
            waitingReport = true;
            List<Pair<String, CommunicationMessage>> closureRequests = new ArrayList<>();
            for (int i = 0; i < myRegions.size(); i++) {
                CommunicationMessage responseMessage = new CommunicationMessage();
                responseMessage.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
                closureRequests.add(new Pair<>(myRegions.get(i), responseMessage));
            }
            return closureRequests;
        }
        return null;
    }

    @Override
    public Pair<String, CommunicationMessage> handleConnectionRequest(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        int index = myRegions.indexOf(senderQueue);
        CommunicationMessage responseMessage = new CommunicationMessage();
        if(index != -1 && !connectedRegions[index]){
            connectedRegions[index] = true;
            responseMessage.setMessageType(MessageType.CONNECTION_ACCEPTED);
        }else
            responseMessage.setMessageType(MessageType.CONNECTION_REFUSED);

        return new Pair<>(senderQueue, responseMessage);
    }

    @Override
    public void handleAcceptedConnection() {
        connected=true;
    }

    @Override
    public Pair<String, CommunicationMessage> requestConnectionToParent() {
        CommunicationMessage connectionRequest = new CommunicationMessage();
        connectionRequest.setMessageType(MessageType.CONNECTION_REQUEST);
        connectionRequest.setSenderName(myDestinationName);

        return new Pair<>(myParent, connectionRequest);

    }
}
