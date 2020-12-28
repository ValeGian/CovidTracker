package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

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
    boolean[] checkReceivedDailyReport;
    DailyReport[] receivedDailyReport;
    boolean waitingReport;
    String myParent;


    @Override
    public void initializeParameters(String myDestinationName, List<String> myRegions, String myParent) {
        this.myDestinationName = myDestinationName;
        this.myRegions = myRegions;
        checkReceivedDailyReport = new boolean[myRegions.size()];
        receivedDailyReport = new DailyReport[myRegions.size()];
        this.myParent = myParent;
    }


    @Override
    public void handleDailyReport(CommunicationMessage cMsg) {
        String senderQueue = cMsg.getSenderName();
        String body = cMsg.getMessageBody();
        int index = myRegions.indexOf(senderQueue);
        if(waitingReport && index != -1 && !checkReceivedDailyReport[index]){
            receivedDailyReport[index] = new Gson().fromJson(body, DailyReport.class);
            checkReceivedDailyReport[index] = true;
        }
    }


    public DailyReport getDailyReport() {

        DailyReport responseReport = new DailyReport();
        waitingReport = false;
        for(int i = 0; i < checkReceivedDailyReport.length; i++) {
            if(checkReceivedDailyReport[i]) {
                checkReceivedDailyReport[i] = false;
                responseReport.addAll(receivedDailyReport[i]);
            }
        }
        return responseReport;
    }


    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg) {

        Gson converter = new Gson();
        String senderName = cMsg.getSenderName();
        AggregationRequest aggregationRequested;
        boolean encapsulatedBool = false;

        if(senderName.equals(myParent)){
            CommunicationMessage encapsulated = converter.fromJson(cMsg.getMessageBody(), CommunicationMessage.class);
            aggregationRequested = converter.fromJson(encapsulated.getMessageBody(), AggregationRequest.class);
            encapsulatedBool = true;

        }else
            aggregationRequested = converter.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        String dest = aggregationRequested.getDestination();
        int index = myRegions.indexOf(dest);

        if(index != -1) //se index non è -1 vuol dire che il destinatario è una delle mie regioni
                return new Pair<>(myRegions.get(index), cMsg); //ritorno
        else if(dest.equals(myDestinationName)) {  //diretto a me e rispondo io
            return new Pair<>("mySelf", cMsg);

        }else{      //diretto a qualcun'altro
            if(encapsulatedBool) //flooding mismatch
                return null;
            else
                return new Pair<>(myParent, cMsg); //inoltro a nazione
        }

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



}
