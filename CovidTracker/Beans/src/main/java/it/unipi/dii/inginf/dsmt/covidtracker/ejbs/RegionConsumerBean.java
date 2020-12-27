package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumer;
import javafx.util.Pair;

import javax.ejb.Stateful;
import java.util.List;

@Stateful(name = "RegionConsumerEJB")
public class RegionConsumerBean implements RegionConsumer {

    String name;
    boolean registryOpened;
    String myParent;
    int connectionState; //equal to:  0 if the connection is pending
                         //           1 if the connection is been accepted
                         //           -1 if the connection is been refused

    CommunicationMessage myCommunicationMessage;

    @Override
    public void initializeParameters(String name, String parent) {
        this.name = name;
        connectionState = 0;
        myParent = parent;
    }

    @Override
    public Pair<String, CommunicationMessage> handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if (connectionState != 1)
            return null;

        if (registryOpened){
            registryOpened = false;
            myCommunicationMessage.setMessageType(MessageType.DAILY_REPORT);
            //recupero di tutti i dati giornalieri
            myCommunicationMessage.setMessageBody(""); //immissione dei dati nel corpo del messaggio
            return new Pair<>(myParent, myCommunicationMessage);
        }
        return null;
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg){
        if (connectionState != 1)
            return null;

        String requester =  ""; //coda del richiedente da ottenere dal corpo del messaggio
        String aggregationType = ""; //tipo di aggregazione da ottenere dal corpo del messaggio
        String aggregationResult = ""; //qui andranno i risultati dell'aggragazione

        switch (aggregationType){
            //eseguo l'aggregazione richiesta interfacciandomi con Mongo per ottenere i log necessari
        }

        myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        myCommunicationMessage.setMessageBody(aggregationResult);
        return new Pair<>(requester, myCommunicationMessage);
    }

    public void handleAggregationResponse(CommunicationMessage cMsg){
        if (connectionState != 1)
            return;

        String aggregationResult = cMsg.getMessageBody();

        //invio dei risultati dell'aggregazione a myRegionWeb che li mostrerà a video
    }

    public void handleConnectionAccepted(CommunicationMessage cMsg){
        connectionState = 1;
        myParent = cMsg.getSenderName();
    }

    public void handleConnectionRefused(CommunicationMessage cMsg){
        connectionState = -1;
    }
}
