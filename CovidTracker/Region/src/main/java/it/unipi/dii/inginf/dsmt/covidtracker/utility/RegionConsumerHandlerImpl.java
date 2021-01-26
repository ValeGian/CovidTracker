package it.unipi.dii.inginf.dsmt.covidtracker.utility;

import com.google.gson.Gson;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.AggregationRequest;
import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.enums.MessageType;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.RegionConsumerHandler;
import it.unipi.dii.inginf.dsmt.covidtracker.log.CTLogger;
import javafx.util.Pair;

import javax.ejb.Stateful;

@Stateful(name = "RegionConsumerEJB")
public class RegionConsumerHandlerImpl implements RegionConsumerHandler {

    String myName;
    String myDestinationName;
    String myAreaDestinationName;
    CommunicationMessage myCommunicationMessage = new CommunicationMessage();

    @Override
    public void initializeParameters(String myName, String myDestinationName, String myAreaDestinationName) {
        this.myName = myName;
        this.myDestinationName = myDestinationName;
        this.myAreaDestinationName = myAreaDestinationName;
        myCommunicationMessage.setSenderName(myName);
    }

    @Override
    public Pair<String, CommunicationMessage> handleAggregationRequest(CommunicationMessage cMsg){
        Gson gson = new Gson();
        AggregationRequest aggregationRequest = gson.fromJson(cMsg.getMessageBody(), AggregationRequest.class);

        CTLogger.getLogger(this.getClass()).info("sto per selezionare il ramo if o else cmsg: " + aggregationRequest.toString());
        if (aggregationRequest.getDestination().equals(myName)) { //se l'aggregazione é rivolta a me preparo un messaggio di risposta che verrà riempito dal nodo regione con il risultato dell'aggregazione
            CommunicationMessage responseMessage = new CommunicationMessage();
            responseMessage.setMessageType(MessageType.AGGREGATION_RESPONSE);
            CTLogger.getLogger(this.getClass()).info("rispondo io (regione) nome: " + responseMessage.toString());
            return new Pair<>(cMsg.getSenderName(), responseMessage);
        }
        else { //altrimenti preparo un messaggio da inoltrare alla mia regione
            CTLogger.getLogger(this.getClass()).info("invio ad area cmsg: " + cMsg.toString());
            return new Pair<>(myAreaDestinationName, cMsg);
        }
    }

}
