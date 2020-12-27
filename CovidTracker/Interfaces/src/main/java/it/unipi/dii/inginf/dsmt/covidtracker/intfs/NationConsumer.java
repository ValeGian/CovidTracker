package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import javafx.util.Pair;

import javax.ejb.Remote;

@Remote
public interface NationConsumer {

    Pair<String, CommunicationMessage> handleConnectionRequest(CommunicationMessage cMsg);

}
