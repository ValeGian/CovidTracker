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
    boolean registryOpened;
    String myParent;
    KVManager kvDB = new KVManagerImpl();
    CommunicationMessage myCommunicationMessage;
    Map<String, List<DataLog>> dataLogs; //logs received from web servers, the key is the day of the dataLog (format dd/MM/yyyy)
                                             //and the value is the list of logs received in that day
    Map<AggregationRequest, String> aggregationToAnswer = new HashMap<AggregationRequest, String>();

    @Override
    public void initializeParameters(String myName, String parent) {
        this.myName = myName;
        myParent = parent;
    }

    @Override
    public String handleRegistryClosureRequest(CommunicationMessage cMsg) {
        if (myParent.equals(cMsg.getSenderName()))
            return myParent;

        return null;
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg){
        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        if (aggregationRequest.getDestination().equals(myName)) { //se l'aggregazione é rivolta a me preparo un messaggio di risposta che verrà riempito dal nodo regione con il risultato dell'aggregazione
            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            return new Pair<>(cMsg.getSenderName(), myCommunicationMessage);
        }
        else { //altrimenti la salvo nelle aggregazioni pendenti e la inoltro alla mia zona
            aggregationToAnswer.put(aggregationRequest, cMsg.getSenderName());
            myCommunicationMessage.setMessageType(MessageType.AGGREGATION_REQUEST);
            myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));
            return new Pair<>(myParent, myCommunicationMessage);
        }
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationResponse(CommunicationMessage cMsg){
        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);
        myCommunicationMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
        myCommunicationMessage.setMessageBody(gson.toJson(aggregationRequest));

        //rimuovo la richiesta di aggregazione dalle aggragazioni pendenti
        AggregationRequest key = aggregationRequest;
        key.setResult(-1.0);
        String regionWeb = aggregationToAnswer.remove(key);
        if (regionWeb != null)
            return new Pair<>(regionWeb, myCommunicationMessage);
        return null;
    }
}
