package it.unipi.dii.inginf.dsmt.covidtracker.intfs;

import it.unipi.dii.inginf.dsmt.covidtracker.communication.CommunicationMessage;

import javax.ejb.Remote;
import javax.jms.Destination;
import javax.jms.Message;

@Remote
public interface Producer {

    void enqueue(final String consumerName, final CommunicationMessage cMsg);
    void enqueue(final String consumerName, final Message outMsg);
    void enqueue(final Destination consumerName, final CommunicationMessage cMsg);

}
