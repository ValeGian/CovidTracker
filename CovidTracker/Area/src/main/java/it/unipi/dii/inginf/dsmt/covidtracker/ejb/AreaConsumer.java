package it.unipi.dii.inginf.dsmt.covidtracker.ejb;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DailyReport;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;

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
    String myParent = "jms/nationQueue";
    KVManagerImpl myLog;



    public AreaConsumer(KVManagerImpl myKVManager, String myName, List<String> myRegions) {
        this.myName = myName;
        this.myRegions = myRegions;
        checkReceivedDailyReport = new boolean[myRegions.size()];
        receivedDailyReport = new DailyReport[myRegions.size()];
        myLog = myKVManager;
    }


    public void handleDailyReport(CommunicationMessage cMsg) {

        CTLogger.getLogger(this.getClass()).info("entro in handleDailyReport");

        String senderQueue = cMsg.getSenderName();
        String body = cMsg.getMessageBody();
        int index = myRegions.indexOf(senderQueue);
        if(waitingReport && index != -1 && !checkReceivedDailyReport[index]){
            receivedDailyReport[index] = new Gson().fromJson(body, DailyReport.class);
            checkReceivedDailyReport[index] = true;
        }
    }


    public DailyReport getDailyReport() {

        CTLogger.getLogger(this.getClass()).info("entro in getDailyReport");

        DailyReport responseReport = new DailyReport();
        waitingReport = false;
        for(int i = 0; i < checkReceivedDailyReport.length; i++) {
            if(checkReceivedDailyReport[i]) {
                checkReceivedDailyReport[i] = false;
                responseReport.addAll(receivedDailyReport[i]);
            }
        }
        DailyReport d = new DailyReport(); d.addTotalPositive(118);

        responseReport.addAll(d);
        return responseReport;
    }


    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg) {

        CTLogger.getLogger(this.getClass()).info("entro in handleAggregationRequest");

        CTLogger.getLogger(this.getClass()).info("MyDest: " + myName + " MyParent: " + myParent + " MyRegions: " + myRegions.get(1));

        Gson converter = new Gson();
        String senderName = cMsg.getSenderName();
        AggregationRequest aggregationRequested;
        boolean encapsulatedBool = false;

        if (senderName.equals(myParent)) {
            CTLogger.getLogger(this.getClass()).info("entro quando lo invia il padre");
            CommunicationMessage encapsulated = converter.fromJson(cMsg.getMessageBody(), CommunicationMessage.class);
            CTLogger.getLogger(this.getClass()).info("messaggio incapsulato: " + encapsulated.toString());

            aggregationRequested = converter.fromJson(encapsulated.getMessageBody(), AggregationRequest.class);
            encapsulatedBool = true;

        } else
            aggregationRequested = converter.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        String dest = aggregationRequested.getDestination();
        CTLogger.getLogger(this.getClass()).info("Lo deve ricevere: " + dest);


        int index = myRegions.indexOf(dest);


        if (index != -1) { //se index non è -1 vuol dire che il destinatario è una delle mie regioni
            return new Pair<>(myRegions.get(index), cMsg); //ritorno

        } else if (dest.equals(myName)) {  //diretto a me e rispondo io
            return new Pair<>("mySelf", cMsg);

        } else {      //diretto a qualcun'altro
            if (encapsulatedBool) { //flooding mismatch
                CTLogger.getLogger(this.getClass()).info("Entro encapsulated");
                return null;
            } else {
                CTLogger.getLogger(this.getClass()).info("Entro in fondo");
                return new Pair<>(myParent, cMsg); //inoltro a nazione
            }
        }

    }


    public List<Pair<String, CommunicationMessage>> handleRegistryClosureRequest(CommunicationMessage cMsg) {
        CTLogger.getLogger(this.getClass()).info("entro in handleRegistryClosureRequest");

        if(!waitingReport) {
            CTLogger.getLogger(this.getClass()).info("entro in waiting");

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
