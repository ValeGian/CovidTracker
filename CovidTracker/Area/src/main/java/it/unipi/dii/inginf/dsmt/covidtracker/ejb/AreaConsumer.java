package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;

import it.unipi.dii.inginf.dsmt.covidtracker.intfs.KVManager;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class AreaConsumer  {

    String myName;    //jms/northQueue
    List<String> myRegions;     //jms/valleAostaQueue
    boolean[] checkReceivedDailyReport;
    DailyReport[] receivedDailyReport;
    boolean waitingReport;
    String myParent = "nation";
    KVManager myLog;

    public AreaConsumer(KVManager myKVManager, String myName, List<String> myRegions) {
        this.myName = myName;
        this.myRegions = myRegions;
        checkReceivedDailyReport = new boolean[myRegions.size()];
        receivedDailyReport = new DailyReport[myRegions.size()];
        myLog = myKVManager;
    }


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


    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg) {
        Gson converter = new Gson();
        String senderName = cMsg.getSenderName();
        AggregationRequest aggregationRequested;
        boolean encapsulatedBool = false;

        CommunicationMessage encapsulated = null;
        if (senderName.equals(myParent)) {
            encapsulated = converter.fromJson(cMsg.getMessageBody(), CommunicationMessage.class);

            aggregationRequested = converter.fromJson(encapsulated.getMessageBody(), AggregationRequest.class);
            encapsulatedBool = true;
            CTLogger.getLogger(this.getClass()).info("aggr requested encapsulated: " + aggregationRequested.toString());

        } else
            aggregationRequested = converter.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        String dest = aggregationRequested.getDestination();
        int index = myRegions.indexOf(dest);
        CTLogger.getLogger(this.getClass()).info("l'indice è (area " + myName + "): " + index + " dest: " + dest);


        if (index != -1) { //se index non è -1 vuol dire che il destinatario è una delle mie regioni
            if (!encapsulatedBool) {
                CTLogger.getLogger(this.getClass()).info("inoltro il messaggio a una mia regione " + myName + ": " + cMsg.toString());
                return new Pair<>(myRegions.get(index), cMsg); //ritorno
            } else
                return new Pair<>(myRegions.get(index), encapsulated);

        } else if (dest.equals(myName)) {  //diretto a me e rispondo io
            return new Pair<>("mySelf", cMsg);

        } else {      //diretto a qualcun'altro
            if (encapsulatedBool) { //flooding mismatch
                return null;
            } else {
                CTLogger.getLogger(this.getClass()).info("Sto inoltrando a nazione correttamente");
                return new Pair<>(myParent, cMsg); //inoltro a nazione
            }
        }

    }


    public List<Pair<String, CommunicationMessage>> handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if(!waitingReport) {
            waitingReport = true;
            List<Pair<String, CommunicationMessage>> closureRequests = new ArrayList<>();
            for (int i = 0; i < myRegions.size(); i++) {
                CommunicationMessage responseMessage = new CommunicationMessage();
                responseMessage.setMessageType(MessageType.REGISTRY_CLOSURE_REQUEST);
                responseMessage.setSenderName(myName);
                closureRequests.add(new Pair<>(myRegions.get(i), responseMessage));
            }
            return closureRequests;
        }
        return null;
    }



}
