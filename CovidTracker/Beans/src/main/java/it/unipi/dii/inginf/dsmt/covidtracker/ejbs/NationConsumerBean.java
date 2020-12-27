package it.unipi.dii.inginf.dsmt.covidtracker.ejbs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;
import it.unipi.dii.inginf.dsmt.covidtracker.intfs.NationConsumer;
import javafx.util.Pair;

import javax.ejb.Stateful;

@Stateful(name = "NationConsumerEJB")
public class NationConsumerBean implements NationConsumer {

    public NationConsumerBean() {

    }

    @Override
    public Pair<String, CommunicationMessage> handleConnectionRequest(CommunicationMessage cMsg) {
        return null;
    }
}
