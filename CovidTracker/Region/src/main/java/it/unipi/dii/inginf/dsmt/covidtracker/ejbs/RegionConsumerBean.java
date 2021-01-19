package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.DataLog;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.JavaErlServicesClient;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumer;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.KVManager;
import it.unipi.dii.inginf.dsmt.covidtracker.persistence.KVManagerImpl;
import javafx.util.Pair;

import javax.ejb.Stateful;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateful(name = "RegionConsumerEJB")
public class RegionConsumerBean implements RegionConsumer {

    String myName;
    String myParent;
    CommunicationMessage myCommunicationMessage;

    @Override
    public void initializeParameters(String myName, String parent) {
        this.myName = myName;
        myParent = parent;
        myCommunicationMessage.setSenderName(myName);
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg){
        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        if (aggregationRequest.getDestination().equals(myName)) { //se l'aggregazione é rivolta a me preparo un messaggio di risposta che verrà riempito dal nodo regione con il risultato dell'aggregazione
            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            return new Pair<>(cMsg.getSenderName(), myCommunicationMessage);
        }
        else { //altrimenti preparo un messaggio da inoltrare alla mia regione
            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_REQUEST);
            myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
            return new Pair<>(myParent, myCommunicationMessage);
        }
    }

}
